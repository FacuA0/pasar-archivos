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
    private static Transferencia envio, recepcion;
    private static ServerSocket server;
    private static ArrayList<Elementos> pendientes;
    public static Transfiriendo panelEnviar, panelRecibir;
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
        
        pendientes = new ArrayList();
        panelEnviar = new Transfiriendo();
        panelRecibir = new Transfiriendo();
        
        envio = new Transferencia(Modo.ENVIAR);
        recepcion = new Transferencia(Modo.RECIBIR);
        
        envio.start();
        recepcion.start();
    }
    
    public static void transferir(String[] archivos, String direccion) {
        InetAddress ip;
        try {
            ip = InetAddress.getByName(direccion);
        } catch (UnknownHostException ex) {return;}
        
        synchronized (Transferencia.class) {            
            for (String ruta : archivos) {
                File archivo = new File(ruta);
                Elementos e = new Elementos(ip, archivo);
                pendientes.add(e);
            }
        }
    }
    
    public void run() {
        if (modo == Modo.ENVIAR) {
            enviar();
        }
        else {
            recibir();
        }
    }
    
    private void enviar() {
        while (true) {
            if (pendientes.isEmpty()) {
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException ex) {}
                
                continue;
            }
            
            Elementos item;
            synchronized (Transferencia.class) {
                item = pendientes.remove(0);
            }
            
            try {
                String nombre = item.archivo.getName();
                FileInputStream fileIO = new FileInputStream(item.archivo);
                
                // Abrir ventana de progreso
                panelEnviar.setVisible(true);
                panelEnviar.setModo(Modo.ENVIAR);
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
    }
    
    private void recibir() {
        while (true) {
            try {
                Socket socket = server.accept();
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
                panelRecibir.setModo(Modo.RECIBIR);
                panelRecibir.setNombre(nombre);
                
                // Recibir fecha de modificación
                byte[] modificadoB = stream.readNBytes(8);
                long modificado = 0;
                if (modificadoB[0] >= 0) modificado = modificado | ((long) modificadoB[0] << 56);
                else modificado = modificado | ((long) (modificadoB[0] + 256) << 56);
                if (modificadoB[1] >= 0) modificado = modificado | ((long) modificadoB[1] << 48);
                else modificado = modificado | ((long) (modificadoB[1] + 256) << 48);
                if (modificadoB[2] >= 0) modificado = modificado | ((long) modificadoB[2] << 40);
                else modificado = modificado | ((long) (modificadoB[2] + 256) << 40);
                if (modificadoB[3] >= 0) modificado = modificado | ((long) modificadoB[3] << 32);
                else modificado = modificado | ((long) (modificadoB[3] + 256) << 32);
                if (modificadoB[4] >= 0) modificado = modificado | ((long) modificadoB[4] << 24);
                else modificado = modificado | ((long) (modificadoB[4] + 256) << 24);
                if (modificadoB[5] >= 0) modificado = modificado | ((long) modificadoB[5] << 16);
                else modificado = modificado | ((long) (modificadoB[5] + 256) << 16);
                if (modificadoB[6] >= 0) modificado = modificado | ((long) modificadoB[6] << 8);
                else modificado = modificado | ((long) (modificadoB[6] + 256) << 8);
                if (modificadoB[7] >= 0) modificado = modificado | ((long) modificadoB[7]);
                else modificado = modificado | ((long) (modificadoB[7] + 256));
                
                // Determinar el nombre final del archivo considerando duplicados
                String ruta = System.getProperty("user.home") + "/Desktop/" + nombre;
                int i = 2;
                while (new File(ruta).exists()) {
                    int punto = nombre.lastIndexOf(".");
                    String sufijo = punto != -1 ? nombre.substring(punto) : "";
                    String nombre2 = nombre.substring(0, punto);
                    ruta = System.getProperty("user.home") + "/Desktop/" + nombre2 + " (" + (i++) + ")" + sufijo;
                }
                
                File archivo = new File(ruta);
                archivo.createNewFile();
                
                FileOutputStream fileIO = new FileOutputStream(archivo);
                
                // Recibir longitud de archivo
                byte[] longitud = stream.readNBytes(8);
                long largo = 0;
                if (longitud[0] >= 0) largo = largo | ((long) longitud[0] << 56);
                else largo = largo | ((long) (longitud[0] + 256) << 56);
                if (longitud[1] >= 0) largo = largo | ((long) longitud[1] << 48);
                else largo = largo | ((long) (longitud[1] + 256) << 48);
                if (longitud[2] >= 0) largo = largo | ((long) longitud[2] << 40);
                else largo = largo | ((long) (longitud[2] + 256) << 40);
                if (longitud[3] >= 0) largo = largo | ((long) longitud[3] << 32);
                else largo = largo | ((long) (longitud[3] + 256) << 32);
                if (longitud[4] >= 0) largo = largo | ((long) longitud[4] << 24);
                else largo = largo | ((long) (longitud[4] + 256) << 24);
                if (longitud[5] >= 0) largo = largo | ((long) longitud[5] << 16);
                else largo = largo | ((long) (longitud[5] + 256) << 16);
                if (longitud[6] >= 0) largo = largo | ((long) longitud[6] << 8);
                else largo = largo | ((long) (longitud[6] + 256) << 8);
                if (longitud[7] >= 0) largo = largo | ((long) longitud[7]);
                else largo = largo | ((long) (longitud[7] + 256));
                
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
    }
    
    private long escribirArrayNumero(long numero, byte[] lista, int posicion) {
        int bits = (lista.length - posicion - 1) * 8;
        
        if (lista[posicion] >= 0) 
            return numero | ((long) lista[posicion] << bits);
        else 
            return numero | ((long) (lista[6] + 256) << bits);
    }
    
    public static enum Modo {
        ENVIAR,
        RECIBIR
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
