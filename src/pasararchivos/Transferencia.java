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
    public static Transfiriendo panel;
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
        panel = new Transfiriendo();
        
        envio = new Transferencia(Modo.ENVIO);
        recepcion = new Transferencia(Modo.RECEPCION);
        
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
        if (modo == Modo.ENVIO) {
            enviar();
        }
        else {
            recibir();
        }
    }
    
    private void enviar() {
        while (true) {
            if (pendientes.isEmpty()) {
                if (panel.isVisible()) panel.setVisible(false);
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
                
                panel.setVisible(true);
                panel.setNombre(nombre);

                Socket socket = new Socket(item.ip, 9060);
                OutputStream stream = socket.getOutputStream();

                byte[] nombreBytes = nombre.getBytes();
                if (nombreBytes.length > 255) {
                    throw new Exception("Nombre de archivo muy grande");
                }
                
                // Byte 1: Longitud del nombre de archivo
                stream.write((byte) ((nombreBytes.length + 128) % 256 - 128));
                
                // Bytes: Nombre de archivo
                stream.write(nombreBytes);
                
                // Longitud de archivo
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
                while (!fin) {
                    byte[] bytes = fileIO.readNBytes(4096);
                    stream.write(bytes);
                    progress += bytes.length;
                    velocidad += bytes.length;
                    
                    if (System.currentTimeMillis() - time > 16) {
                        panel.setDatos(progress, largo, velocidad * 62);
                        time = System.currentTimeMillis();
                        velocidad = 0;
                    }
                    if (bytes.length == 0) fin = true;
                }
                
                panel.setDatos(progress, largo, 0);
                
                fileIO.close();
                socket.close();
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
                
                // Longitud del nombre de archivo
                int l = stream.read();
                
                if (l == -1) 
                    throw new Exception("Se cerró la conexión de forma temprana");
                
                // Nombre de archivo
                byte[] nombreBytes = stream.readNBytes(l);
                
                if (nombreBytes.length < l) 
                    throw new Exception("Se cerró la conexión de forma temprana");
                
                String nombre = new String(nombreBytes);
                
                File archivo = new File(System.getProperty("user.home") + "/Desktop/" + nombre);
                FileOutputStream fileIO = new FileOutputStream(archivo);
                
                panel.setVisible(true);
                panel.setNombre(nombre);
                
                byte[] longitud = stream.readNBytes(8);
                for (byte b: longitud) {
                    System.out.println(b);
                }
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
                
                //System.out.println(largo);
                
                long progress = 0;
                boolean fin = false;
                
                long time = System.currentTimeMillis();
                
                while (!fin) {
                    byte[] bytes = stream.readNBytes((int) Math.min(largo - progress, 4096));
                    progress += bytes.length;
                    fileIO.write(bytes);
                    if (progress >= largo) fin = true;
                }
                
                System.out.println("Hola 2");
                socket.close();
                System.out.println("Hola 3");
                fileIO.close();
                System.out.println("Hola 4");
                
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
    
    enum Modo {
        ENVIO,
        RECEPCION
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
