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
import javax.swing.JOptionPane;

/**
 * @author Facu
 */
public class Transferencia extends Thread {
    private static final int PUERTO = 9060;
    private static final String LOCK = "lock";
    private static Transferencia servidor;
    private static ServerSocket server;
    public static Progreso panelEnviar, panelRecibir;
    Elementos item; // Sólo modo ENVIAR
    Socket socket; // Sólo modo RECIBIR
    Modo modo;
    
    public Transferencia(Modo modo) {
        this.modo = modo;
    }
    
    public static void init() throws RuntimeException {
        try {
            server = new ServerSocket(PUERTO);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        panelEnviar = new Progreso();
        panelRecibir = new Progreso();
        
        servidor = new Transferencia(Modo.ESCUCHAR);
        servidor.start();
    }
    
    public static void transferir(String[] archivos, String direccion) {
        InetAddress ip;
        try {
            ip = InetAddress.getByName(direccion);
        } catch (UnknownHostException ex) {return;}
        
        for (String ruta : archivos) {
            File archivo = new File(ruta);
            Elementos e = new Elementos(ip, archivo);
            
            Transferencia enviar = new Transferencia(Modo.ENVIAR);
            enviar.item = e;
            enviar.start();
        }
    }
    
    @Override
    public void run() {
        switch (modo) {
            case ESCUCHAR:
                escuchar();
                break;
            case ENVIAR:
                enviar();
                break;
            case RECIBIR:
                recibir();
                break;
            default:
                break;
        }
    }
    
    /**
     * Código que sirve como servidor que escucha transferencias entrantes.
     * Cuando llega una nueva transferencia, se crea un nuevo hilo Transferencia
     * con el modo RECIBIR dedicado a recibir el archivo.
     */
    private void escuchar() {
        while (true) {
            try {
                Socket socket = server.accept();
                
                Transferencia recibir = new Transferencia(Modo.RECIBIR);
                recibir.socket = socket;
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
    
    private void enviar() {
        try {
            String nombre = item.archivo.getName();
            FileInputStream fileIO = new FileInputStream(item.archivo);

            // Abrir ventana de progreso
            panelEnviar.setVisible(true);
            panelEnviar.setModo(Progreso.Modo.ENVIAR);
            panelEnviar.setNombre(nombre);

            // Crear conexión
            Socket socket = new Socket(item.ip, 9060);
            OutputStream stream = socket.getOutputStream();

            // Comprobar que la longitud del nombre no supere los 255 bytes
            byte[] nombreBytes = nombre.getBytes();
            if (nombreBytes.length > 255) {
                throw new Exception("Nombre de archivo muy grande");
            }

            // Byte 1: Longitud del nombre de archivo
            stream.write((byte) ((nombreBytes.length + 128) % 256 - 128));

            // Bytes: Nombre de archivo
            stream.write(nombreBytes);

            // Enviar última modificación de archivo
            long modificado = item.archivo.lastModified();
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
            long largo = item.archivo.length();
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

            long progress = 0;
            long velocidad = 0;
            boolean fin = false;

            // Enviar el contenido del archivo
            while (!fin) {
                byte[] bytes = fileIO.readNBytes(4096);
                stream.write(bytes);
                progress += bytes.length;
                velocidad += bytes.length;

                // Cada cierto tiempo, actualizar la ventana de progreso
                if (System.currentTimeMillis() - time > 16) {
                    panelEnviar.setDatos(progress, largo, velocidad * 62);
                    time = System.currentTimeMillis();
                    velocidad = 0;
                }
                if (bytes.length == 0) fin = true;
            }

            panelEnviar.setDatos(progress, largo, 0);

            // Cerrar todo
            fileIO.close();
            socket.close();

            panelEnviar.setVisible(false);
            //JOptionPane.showMessageDialog(PasarArchivos.panel, "El archivo fue transferido con éxito");
        }
        catch (FileNotFoundException e) {
            System.err.println("Hubo un error al abrir el archivo.");
            e.printStackTrace();
            JOptionPane.showMessageDialog(PasarArchivos.panel, "Error al abrir el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(PasarArchivos.panel, "Hubo un error de entrada/salida.", "Error de IO", JOptionPane.ERROR_MESSAGE);
            return;
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(PasarArchivos.panel, "Hubo un error general.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void recibir() {
        try {
            InputStream stream = socket.getInputStream();

            // Recibir longitud del nombre de archivo
            int l = stream.read();

            if (l == -1) 
                throw new Exception("Se cerró la conexión de forma temprana");

            // Recibir nombre de archivo
            byte[] nombreBytes = stream.readNBytes(l);

            if (nombreBytes.length < l) 
                throw new Exception("Se cerró la conexión de forma temprana");

            String nombre = new String(nombreBytes);

            // Abrir ventana de progreso
            panelRecibir.setVisible(true);
            panelRecibir.setModo(Progreso.Modo.RECIBIR);
            panelRecibir.setNombre(nombre);

            // Recibir fecha de modificación
            byte[] modificadoB = stream.readNBytes(8);
            long modificado = 0;

            // Es un quilombo convertir de long a byte array y viceversa
            for (int i = 0; i < modificadoB.length; i++) 
                modificado = escribirArrayNumero(modificado, modificadoB, i);

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

            FileOutputStream fileIO = new FileOutputStream(archivo);

            // Recibir longitud de archivo
            byte[] longitud = stream.readNBytes(8);
            long largo = 0;

            // Es un quilombo convertir de long a byte array y viceversa
            for (int i = 0; i < longitud.length; i++) 
                largo = escribirArrayNumero(largo, longitud, i);

            long progress = 0;
            boolean fin = false;

            long time = System.currentTimeMillis();
            long velocidad = 0;

            // Empezar a guardar el archivo
            while (!fin) {
                byte[] bytes = stream.readNBytes((int) Math.min(largo - progress, 4096));
                progress += bytes.length;
                velocidad += bytes.length;
                fileIO.write(bytes);

                // Cada cierto tiempo, actualizar la ventana de progreso.
                if (System.currentTimeMillis() - time > 16) {
                    panelRecibir.setDatos(progress, largo, velocidad * 62);
                    time = System.currentTimeMillis();
                    velocidad = 0;
                }
                if (progress >= largo) fin = true;
            }

            panelRecibir.setDatos(progress, largo, 0);

            // Cerrar todo
            socket.close();
            fileIO.close();

            archivo.setLastModified(modificado);

            panelRecibir.setVisible(false);
            //JOptionPane.showMessageDialog(PasarArchivos.panel, "La transferencia fue recibida con éxito.");
        } 
        catch (IOException ex) {
            System.err.println("Error en la recepción");
            ex.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private long escribirArrayNumero(long numero, byte[] lista, int posicion) {
        int bits = (lista.length - posicion - 1) * 8;
        
        if (lista[posicion] >= 0) 
            return numero | ((long) lista[posicion] << bits);
        else 
            return numero | ((long) (lista[6] + 256) << bits);
    }
    
    private static enum Modo {
        ENVIAR,
        RECIBIR,
        ESCUCHAR
    }
    
    public static class Elementos {
        public InetAddress ip;
        public File archivo;
        
        public Elementos(InetAddress ip, File archivo) {
            this.ip = ip;
            this.archivo = archivo;
        }
    }
}
