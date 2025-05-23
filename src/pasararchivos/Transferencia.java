package pasararchivos;

import dev.dirs.UserDirectories;

import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * @author Facu
 */
public class Transferencia {
    private static final int PUERTO = 9060;
    private static final String LOCK = "lock";
    private static Servidor servidor;
    private static File rutaGuardado;
    public static Progreso panelProgreso;
    
    public static void init() throws RuntimeException {
        servidor = new Servidor();
        servidor.start();
        
        panelProgreso = new Progreso();
        
        obtenerRutaGuardado();
    }
    
    
    public static void transferir(File[] archivos, InetAddress direccion) {
        Elementos e = new Elementos(direccion, archivos);

        Envio enviar = new Envio(e);
        enviar.start();
    }
    
    private static void obtenerRutaGuardado() {
        //UserDirectories carpetas = UserDirectories.get();
        //String descargas = carpetas.downloadDir;
        String usuario = System.getProperty("user.home");
        
        File guardado = new File(usuario, "Pasar Archivos");
        rutaGuardado = guardado;
        
        if (!guardado.exists()) {
            if (!guardado.mkdirs()) {
                PasarArchivos.logError(null, "No hay destino", "No se pudo crear la carpeta de guardado por defecto. Usando la del usuario.");
                rutaGuardado = new File(usuario);
            }
        }
        else if (guardado.isFile()) {
            PasarArchivos.logError(null, "No hay destino", "La carpeta de guardado por defecto es un archivo. Usando la del usuario.");
            rutaGuardado = new File(usuario);
        }
        
        System.out.println("Ruta provisoria: " + rutaGuardado);
        
        new Thread(() -> {
            UserDirectories carpetas = UserDirectories.get();
            String descargas = carpetas.downloadDir;
            System.out.println("Ruta real: " + descargas);
            
            if (descargas == null) return;
            
            File guardado2 = new File(descargas, "Pasar Archivos");
            if (!guardado2.exists()) {
                if (!guardado2.mkdirs()) {
                    PasarArchivos.logError(null, "No hay destino", "No se pudo crear la carpeta de guardado real. Usando la carpeta por defecto.");
                    return;
                }
            }
            else if (guardado2.isFile()) {
                PasarArchivos.logError(null, "No hay destino", "La carpeta de guardado real es un archivo. Usando la carpeta por defecto.");
                return;
            }
            
            rutaGuardado = guardado2;
            
            try {
                Thread.sleep(3000);
            }
            catch (InterruptedException e) {}
            
            if (!guardado2.equals(guardado) && guardado2.isDirectory()) {
                guardado.delete();
            }
        }, "Ruta Guardado").start();
    }
    
    /**
     * Hilo que sirve como servidor que escucha transferencias entrantes.
     * Cuando llega una nueva transferencia, se crea un nuevo hilo Transferencia
     * con el modo RECIBIR dedicado a recibir el archivo.
     */
    public static class Servidor extends Thread {
        ServerSocket server;
        
        Servidor() throws RuntimeException {
            setName("Hilo-Servidor");
            
            try {
                server = new ServerSocket(PUERTO);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public void run() {
            while (true) {
                try {
                    Socket socket = server.accept();

                    Recepcion recibir = new Recepcion(socket);
                    recibir.start();
                } 
                catch (IOException e) {
                    PasarArchivos.logError(e, "Error en la recepción", "Hubo un error al recibir un nuevo cliente.");
                }
            }
        }
    }
    
    /**
     * Hilo que realiza una transferencia de archivos al host remoto.
     * Es creado por el método transferir() cada vez que el usuario confirma
     * una transferencia hacia un dispositivo remoto. Maneja el envío de cada
     * archivo y sus metadatos al otro dispositivo por medio de una única
     * conexión TCP.
     */
    public static class Envio extends Thread implements Transferidor {
        Elementos items;
        boolean cerrar = false, cancelado = false;
        InputStream input;
        
        Envio(Elementos items) {
            this.items = items;
            setName("Hilo-Envio");
        }
        
        @Override
        public void run() {
            Socket socket;
            DataOutputStream stream;
            
            // Abrir barra en la ventana de progreso
            int idPanel = panelProgreso.agregarTransferencia(this, Progreso.Modo.ENVIAR, items.ip);
            
            // Crear conexión
            try {
                socket = new Socket(items.ip, 9060);
                stream = new DataOutputStream(socket.getOutputStream());
                input = socket.getInputStream();
            }
            catch (SocketException e) {
                String mensaje = "No se pudo conectar con el dispositivo. Probablemente esté inactivo o el programa no está abierto. Vuelva a intentarlo.";
                PasarArchivos.error(panelProgreso, e, "Error al conectar", mensaje);
                panelProgreso.removerTransferencia(idPanel);
                return;
            }
            catch (IOException e) {
                String mensaje = "Hubo un error de entrada/salida al crear el socket.";
                PasarArchivos.error(panelProgreso, e, "Error de I/O", mensaje);
                panelProgreso.removerTransferencia(idPanel);
                return;
            }
            
            // Hilo para detectar cierre de socket remoto (cancelación remota)
            Thread hiloCancelar = new Thread(this::hiloCancelacion);
            hiloCancelar.start();

            // Empezar a enviar datos
            try {
                
                // Enviar la cantidad de archivos a transferir
                stream.writeShort(items.archivos.length);

                // Envío de archivos
                for (int i = 0; i < items.archivos.length; i++) {
                    File archivo = items.archivos[i];

                    String nombre = archivo.getName();

                    panelProgreso.setNombreArchivo(idPanel, nombre, i + 1, items.archivos.length);

                    FileInputStream fileIO;
                    try {
                        fileIO = new FileInputStream(archivo);
                    }
                    catch (IOException e) {
                        PasarArchivos.logWarning(e, "Error al abrir", "No se pudo abrir el archivo. Pasando al siguiente.");

                        // Enviar longitud de nombre 0, lo que indica que no hay archivo
                        stream.write(0);
                        continue;
                    }

                    // Comprobar que la longitud del nombre no supere los 255 bytes
                    byte[] nombreBytes = nombre.getBytes(StandardCharsets.UTF_8);
                    if (nombreBytes.length > 255) {
                        PasarArchivos.logWarning(null, "Nombre muy largo", "El nombre de archivo es muy largo. Pasando al siguiente.");

                        // Enviar longitud de nombre 0, lo que indica que no hay archivo
                        stream.write(0);
                        continue;
                    }

                    // Byte 1: Longitud del nombre de archivo
                    stream.write(nombreBytes.length);

                    // Bytes: Nombre de archivo
                    stream.write(nombreBytes);

                    // Enviar última modificación de archivo
                    stream.writeLong(archivo.lastModified());

                    // Enviar longitud de archivo
                    long largo = archivo.length();
                    stream.writeLong(largo);

                    long time = System.currentTimeMillis();

                    long transcurrido;
                    long progreso = 0;
                    long velocidad = 0;

                    // Enviar el contenido del archivo
                    byte[] bytes = new byte[4096];
                    while (true) {
                        int len = fileIO.read(bytes);
                        if (len == -1) break;
                        
                        stream.write(bytes, 0, len);
                        
                        progreso += len;
                        velocidad += len;

                        // Cada cierto tiempo, actualizar la ventana de progreso
                        transcurrido = System.currentTimeMillis() - time;
                        if (transcurrido > 16) {
                            panelProgreso.setDatos(idPanel, progreso, largo, velocidad * 1000 / transcurrido);
                            time = System.currentTimeMillis();
                            velocidad = 0;
                        }
                        
                        if (cerrar) break;
                    }

                    panelProgreso.setDatos(idPanel, progreso, largo, 0);

                    // Cerrar archivo
                    fileIO.close();
                    
                    if (cerrar) break;
                }
            }
            catch (IOException e) {
                try {
                    hiloCancelar.join(1000);
                }
                catch (InterruptedException e2) {}
                
                String titulo, mensaje;
                if (cancelado) {
                    titulo = "Transferencia cancelada";
                    mensaje = "El receptor decidió cancelar la transferencia de archivos.";
                    PasarArchivos.dialogo(panelProgreso, titulo, mensaje);
                    PasarArchivos.logWarning(e, titulo, mensaje);
                }
                else {
                    titulo = "Error de I/O";
                    mensaje = "Hubo un error de I/O al enviar el archivo.";
                    PasarArchivos.error(panelProgreso, e, titulo, mensaje);
                }
            }
            catch (Exception e) {
                String mensaje = "Hubo un error durante la transferencia.";
                PasarArchivos.error(panelProgreso, e, "Error general", mensaje);
            }
            
            panelProgreso.finalizar(idPanel);

            // Cerrar socket
            try {
                socket.close();
            }
            catch (IOException e) {
                String mensaje = "Hubo un error al cerrar el socket.";
                PasarArchivos.logWarning(e, "Error general", mensaje);
            }
            
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {}
            
            panelProgreso.removerTransferencia(idPanel);
        }
        
        @Override
        public void detener() {
            cerrar = true;
        }
        
        private void hiloCancelacion() {
            try {
                while (input.read() != -1) {}
                
                cancelado = true;
            }
            catch (IOException e) {}
        }
    }
    
    /**
     * Hilo que recibe una transferencia individual de archivos. Es creado por
     * el servidor cada vez que llega una nueva conexión de un host remoto.
     * Maneja la recepción de datos de la conexión remota y guarda los
     * archivos entrantes en el escritorio del equipo.
     */
    public static class Recepcion extends Thread implements Transferidor {
        Socket socket;
        boolean cerrar = false;
        
        Recepcion(Socket socket) {
            this.socket = socket;
            setName("Hilo-Recepcion-" + socket.getInetAddress().getHostAddress());
        }
        
        @Override
        public void run() {
            DataInputStream stream;
            try {
                stream = new DataInputStream(socket.getInputStream());
            }
            catch (IOException e) {
                String mensaje = "Hubo un error de entrada/salida al obtener el flujo de datos del socket.";
                PasarArchivos.error(panelProgreso, e, "Error de I/O", mensaje);
                return;
            }

            // Agregar transferencia a la ventana de progreso
            int idPanel = panelProgreso.agregarTransferencia(this, Progreso.Modo.RECIBIR, socket.getInetAddress());
            
            File archivo = null, destino = rutaGuardado;
            FileOutputStream fileIO = null;
            boolean operacion = false, completo = false;
            int omitidos = 0;

            try {
                // Recibir cantidad de archivos a transferir
                int cantidad = stream.readUnsignedShort();
                
                for (int ind = 0; ind < cantidad; ind++) {

                    // Recibir longitud del nombre de archivo
                    int largo = stream.readUnsignedByte();

                    // Omitir archivo
                    if (largo == 0) {
                        System.err.println("El archivo " + (ind + 1) + " fue omitido.");
                        omitidos++;
                        continue;
                    }

                    // Recibir nombre de archivo
                    byte[] nombreBytes = new byte[largo];
                    stream.readFully(nombreBytes);

                    String nombre = new String(nombreBytes, StandardCharsets.UTF_8);

                    // Cambiar nombre en la ventana de progreso
                    panelProgreso.setNombreArchivo(idPanel, nombre, ind + 1, cantidad);

                    // Recibir fecha de modificación
                    long modificado = stream.readLong();

                    // Determinar el nombre final del archivo considerando duplicados
                    String ruta = nombre;
                    
                    // Si no hay punto, o el punto se encuentra al principio, se toma todo el nombre.
                    int punto = nombre.lastIndexOf(".");
                    String sufijo = punto > 0 ? nombre.substring(punto) : "";
                    String nombre2 = punto > 0 ? nombre.substring(0, punto) : nombre;
                    
                    archivo = new File(destino, ruta);
                    for (int i = 2; archivo.exists(); i++) {
                        ruta = nombre2 + " (" + i + ")" + sufijo;
                        archivo = new File(destino, ruta);
                    }

                    archivo.createNewFile();
                    operacion = true;

                    try {
                        fileIO = new FileOutputStream(archivo);
                    }
                    catch (IOException e) {
                        String mensaje = "Hubo un error al intentar guardar el archivo entrante en el sistema.";
                        PasarArchivos.error(panelProgreso, e, "Error de I/O", mensaje);
                        break;
                    }

                    // Recibir longitud de archivo
                    long longitud = stream.readLong();

                    long progreso = 0;
                    long velocidad = 0;
                    long time = System.currentTimeMillis(), transcurrido;
                    byte[] bytes = new byte[4096];

                    // Empezar a guardar el archivo
                    while (true) {
                        int len = (int) Math.min(longitud - progreso, bytes.length);
                        stream.readFully(bytes, 0, len);
                        fileIO.write(bytes, 0, len);
                        
                        progreso += len;
                        velocidad += len;

                        // Cada cierto tiempo, actualizar la ventana de progreso.
                        transcurrido = System.currentTimeMillis() - time;
                        if (transcurrido > 16) {
                            panelProgreso.setDatos(idPanel, progreso, longitud, velocidad * 1000 / transcurrido);
                            time = System.currentTimeMillis();
                            velocidad = 0;
                        }
                        
                        if (cerrar || progreso == longitud) break;
                        
                        if (progreso > longitud) throw new IOException("Se recibieron más bytes de lo debido.");
                    }
                    
                    operacion = false;
                    completo = progreso == longitud;

                    panelProgreso.setDatos(idPanel, progreso, longitud, 0);

                    // Cerrar archivo
                    fileIO.close();
                    fileIO = null;

                    archivo.setLastModified(modificado);
                    
                    if (cerrar) break;
                }
            }
            catch (EOFException e) {
                System.out.println("El emisor canceló la transferencia.");
                String mensaje = "El emisor decidió cancelar la tansferencia de archivos.";
                PasarArchivos.dialogo(panelProgreso, "Transferencia cancelada", mensaje);
            }
            catch (IOException ex) {
                String mensaje = "Hubo un error de entrada/salida al recibir el archivo.";
                PasarArchivos.error(panelProgreso, ex, "Error de I/O", mensaje);
            }
            catch (Exception e) {
                String mensaje = "Hubo un error al recibir el archivo.";
                PasarArchivos.error(panelProgreso, e, "Error ocurrido", mensaje);
            }
            finally {
                if (fileIO != null) {
                    try {
                        fileIO.close();
                    }
                    catch (IOException e) {}
                }
                
                if (archivo != null && (operacion || cerrar && !completo)) {
                    archivo.delete();
                }
            }
            
            if (omitidos > 0) {
                String mensaje = "Se omitieron " + omitidos + " archivos en la transferencia por uno o varios errores en el dispositivo origen.";
                PasarArchivos.advertir(panelProgreso, "Archivos omitidos", mensaje);
            }
            
            panelProgreso.finalizar(idPanel);

            try {
                socket.setSoLinger(cerrar, 3000);
                socket.close();
            }
            catch (IOException e) {
                String mensaje = "Hubo un error al cerrar el socket.";
                PasarArchivos.logWarning(e, "Error de I/O", mensaje);
            }
            
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {}
            
            panelProgreso.removerTransferencia(idPanel);
        }
        
        @Override
        public void detener() {
            cerrar = true;
        }
    }
    
    public static interface Transferidor {
        public void detener();
    }
    
    public static class Elementos {
        public InetAddress ip;
        public File[] archivos;
        
        public Elementos(InetAddress ip, File[] archivos) {
            this.ip = ip;
            this.archivos = archivos;
        }
    }
}
