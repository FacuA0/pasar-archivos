package pasararchivos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JOptionPane;

/**
 * @author Facu
 */
public class Transferencia extends Thread {
    private static final int PUERTO = 9060;
    private static final String LOCK = "lock";
    private static Servidor servidor;
    public static Progreso panelProgreso;
    
    public static void init() throws RuntimeException {
        servidor = new Servidor();
        servidor.start();
        
        panelProgreso = new Progreso();
    }
    
    public static void transferir(String[] rutas, String direccion) {
        InetAddress ip;
        try {
            ip = InetAddress.getByName(direccion);
        } catch (UnknownHostException ex) {
            JOptionPane.showMessageDialog(PasarArchivos.panel, "Hubo un error parseando la dirección IP del destino.", "Error de IP", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        File[] archivos = new File[rutas.length];
        for (int i = 0; i < rutas.length; i++) {
            archivos[i] = new File(rutas[i]);
        }
        Elementos e = new Elementos(ip, archivos);

        Envio enviar = new Envio(e);
        enviar.start();
    }
    
    /**
     * Hilo que sirve como servidor que escucha transferencias entrantes.
     * Cuando llega una nueva transferencia, se crea un nuevo hilo Transferencia
     * con el modo RECIBIR dedicado a recibir el archivo.
     */
    public static class Servidor extends Thread {
        ServerSocket server;
        
        Servidor() throws RuntimeException {
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
                catch (IOException ex) {
                    System.err.println("Error en la recepción");
                    ex.printStackTrace();
                }
                catch (Exception e) {
                    e.printStackTrace();
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
    public static class Envio extends Thread {
        Elementos items;
        
        Envio(Elementos items) {
            this.items = items;
        }
        
        @Override
        public void run() {
            Socket socket;
            OutputStream stream;
            try {
                // Crear conexión
                socket = new Socket(items.ip, 9060);
                stream = socket.getOutputStream();
            }
            catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(PasarArchivos.panel, "Hubo un error de entrada/salida al crear el socket.", "Error de I/O", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Abrir barra en la ventana de progreso
            int idPanel = panelProgreso.agregarTransferencia(Progreso.Modo.ENVIAR, items.ip);

            // Empezar a enviar datos
            try {

                // Enviar la cantidad de archivos a transferir
                byte[] cantidad = new byte[2];
                cantidad[0] = (byte) ((items.archivos.length >> 8) & 0xFF);
                cantidad[1] = (byte) (items.archivos.length & 0xFF);
                stream.write(cantidad);

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
                        System.err.println("Hubo un error de I/O al intentar leer un archivo.");

                        // Enviar longitud de nombre 0, lo que indica que no hay archivo
                        stream.write(0);
                        continue;
                    }

                    // Comprobar que la longitud del nombre no supere los 255 bytes
                    byte[] nombreBytes = nombre.getBytes();
                    if (nombreBytes.length > 255) {
                        System.err.println("Nombre de archivo muy grande.");

                        // Enviar longitud de nombre 0, lo que indica que no hay archivo
                        stream.write(0);
                        continue;
                    }

                    // Byte 1: Longitud del nombre de archivo
                    stream.write(nombreBytes.length);

                    // Bytes: Nombre de archivo
                    stream.write(nombreBytes);

                    // Enviar última modificación de archivo
                    long modificado = archivo.lastModified();
                    byte[] modificadoB = new byte[8];
                    modificadoB[0] = (byte) ((modificado >> 56) & 0xFF);
                    modificadoB[1] = (byte) ((modificado >> 48) & 0xFF);
                    modificadoB[2] = (byte) ((modificado >> 40) & 0xFF);
                    modificadoB[3] = (byte) ((modificado >> 32) & 0xFF);
                    modificadoB[4] = (byte) ((modificado >> 24) & 0xFF);
                    modificadoB[5] = (byte) ((modificado >> 16) & 0xFF);
                    modificadoB[6] = (byte) ((modificado >> 8) & 0xFF);
                    modificadoB[7] = (byte) (modificado & 0xFF);
                    stream.write(modificadoB);

                    // Enviar longitud de archivo
                    long largo = archivo.length();
                    byte[] longitud = new byte[8];
                    longitud[0] = (byte) ((largo >> 56) & 0xFF);
                    longitud[1] = (byte) ((largo >> 48) & 0xFF);
                    longitud[2] = (byte) ((largo >> 40) & 0xFF);
                    longitud[3] = (byte) ((largo >> 32) & 0xFF);
                    longitud[4] = (byte) ((largo >> 24) & 0xFF);
                    longitud[5] = (byte) ((largo >> 16) & 0xFF);
                    longitud[6] = (byte) ((largo >> 8) & 0xFF);
                    longitud[7] = (byte) (largo & 0xFF);
                    stream.write(longitud);

                    long time = System.currentTimeMillis();

                    long progreso = 0;
                    long velocidad = 0;
                    boolean fin = false;

                    // Enviar el contenido del archivo
                    while (!fin) {
                        byte[] bytes = fileIO.readNBytes(4096);
                        stream.write(bytes);
                        progreso += bytes.length;
                        velocidad += bytes.length;

                        // Cada cierto tiempo, actualizar la ventana de progreso
                        if (System.currentTimeMillis() - time > 16) {
                            panelProgreso.setDatos(idPanel, progreso, largo, velocidad * 62);
                            time = System.currentTimeMillis();
                            velocidad = 0;
                        }
                        if (bytes.length == 0) fin = true;
                    }

                    panelProgreso.setDatos(idPanel, progreso, largo, 0);

                    // Cerrar archivo
                    fileIO.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(PasarArchivos.panel, "Hubo un error de entrada/salida mientras se transferían los archivos.", "Error de I/O", JOptionPane.ERROR_MESSAGE);
            }
            catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(PasarArchivos.panel, "Hubo un error general durante la transferencia: " + e.toString(), "Error general", JOptionPane.ERROR_MESSAGE);
            }

            panelProgreso.removerTransferencia(idPanel);

            // Cerrar socket
            try {
                socket.close();
            }
            catch (IOException e) {
                System.err.println("Error al cerrar socket: " + e.toString());
            }
        }
        
        public void detener() {
            // Rellenar
        }
    }
    
    /**
     * Hilo que recibe una transferencia individual de archivos. Es creado por
     * el servidor cada vez que llega una nueva conexión de un host remoto.
     * Maneja la recepción de datos de la conexión remota y guarda los
     * archivos entrantes en el escritorio del equipo.
     */
    public static class Recepcion extends Thread {
        Socket socket;
        
        Recepcion(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            InputStream stream;
            try {
                stream = socket.getInputStream();
            }
            catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(PasarArchivos.panel, "Hubo un error de entrada/salida al obtener el flujo de datos del socket.", "Error de I/O", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Agregar transferencia a la ventana de progreso
            int idPanel = panelProgreso.agregarTransferencia(Progreso.Modo.RECIBIR, socket.getInetAddress());

            try {
                // Recibir cantidad de archivos a transferir
                int cantidad = 0;
                byte[] cantidadBytes = stream.readNBytes(2);
                cantidad = escribirArrayNumero(cantidad, cantidadBytes, 0);
                cantidad = escribirArrayNumero(cantidad, cantidadBytes, 1);

                for (int ind = 0; ind < cantidad; ind++) {

                    // Recibir longitud del nombre de archivo
                    int largo = stream.read();

                    if (largo == -1) {
                        System.err.println("Se cerró la conexión de manera temprana.");
                        break;
                    }

                    // Omitir archivo
                    if (largo == 0) {
                        System.err.println("El archivo " + (ind + 1) + " fue omitido.");
                        continue;
                    }

                    // Recibir nombre de archivo
                    byte[] nombreBytes = stream.readNBytes(largo);

                    // No entiendo el sentido de comprobar si la conexión se cierra de forma temprana
                    if (nombreBytes.length < largo) {
                        System.err.println("Se cerró la conexión de manera temprana.");
                        break;
                    }

                    String nombre = new String(nombreBytes);

                    // Cambiar nombre en la ventana de progreso
                    panelProgreso.setNombreArchivo(idPanel, nombre, ind + 1, cantidad);

                    // Recibir fecha de modificación
                    byte[] modificadoBytes = stream.readNBytes(8);
                    long modificado = 0;

                    // Es un quilombo convertir de long a byte array y viceversa
                    for (int i = 0; i < modificadoBytes.length; i++) 
                        modificado = escribirArrayNumero(modificado, modificadoBytes, i);

                    // Determinar el nombre final del archivo considerando duplicados
                    String ruta = System.getProperty("user.home") + "/Desktop/" + nombre;
                    for (int i = 2; new File(ruta).exists(); i++) {
                        int punto = nombre.lastIndexOf(".");
                        String sufijo = punto != -1 ? nombre.substring(punto) : "";
                        String nombre2 = nombre.substring(0, punto);
                        ruta = System.getProperty("user.home") + "/Desktop/" + nombre2 + " (" + i + ")" + sufijo;
                    }

                    File archivo = new File(ruta);
                    archivo.createNewFile();

                    FileOutputStream fileIO;
                    try {
                        fileIO = new FileOutputStream(archivo);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }

                    // Recibir longitud de archivo
                    byte[] longitudBytes = stream.readNBytes(8);
                    long longitud = 0;

                    // Es un quilombo convertir de long a byte array y viceversa
                    for (int i = 0; i < longitudBytes.length; i++) 
                        longitud = escribirArrayNumero(longitud, longitudBytes, i);

                    long progreso = 0;
                    boolean fin = false;

                    long time = System.currentTimeMillis();
                    long velocidad = 0;

                    // Empezar a guardar el archivo
                    while (!fin) {
                        byte[] bytes = stream.readNBytes((int) Math.min(longitud - progreso, 4096));
                        progreso += bytes.length;
                        velocidad += bytes.length;
                        fileIO.write(bytes);

                        // Cada cierto tiempo, actualizar la ventana de progreso.
                        if (System.currentTimeMillis() - time > 16) {
                            panelProgreso.setDatos(idPanel, progreso, longitud, velocidad * 62);
                            time = System.currentTimeMillis();
                            velocidad = 0;
                        }

                        if (progreso >= longitud) fin = true;
                    }

                    panelProgreso.setDatos(idPanel, progreso, longitud, 0);

                    // Cerrar archivo
                    fileIO.close();

                    archivo.setLastModified(modificado);
                }
            } 
            catch (IOException ex) {
                System.err.println("Error en la recepción");
                ex.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            panelProgreso.removerTransferencia(idPanel);

            try {
                socket.close();
            }
            catch (IOException e) {
                System.err.println("Hubo un error al cerrar el socket.");
            }
        }
        
        public void detener() {
            // Rellenar
        }
    }
    
    private static long escribirArrayNumero(long numero, byte[] lista, int posicion) {
        int bits = (lista.length - posicion - 1) * 8;
        
        if (lista[posicion] >= 0) 
            return numero | ((long) lista[posicion] << bits);
        else 
            return numero | ((long) (lista[posicion] + 256) << bits);
    }
    
    private static int escribirArrayNumero(int numero, byte[] lista, int posicion) {
        int bits = (lista.length - posicion - 1) * 8;
        
        if (lista[posicion] >= 0) 
            return numero | ((int) lista[posicion] << bits);
        else 
            return numero | ((int) (lista[posicion] + 256) << bits);
    }
    
    private static enum Modo {
        ENVIAR,
        RECIBIR,
        ESCUCHAR
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
