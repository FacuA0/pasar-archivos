package pasararchivos;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;
import javax.swing.JOptionPane;

/**
 * @author Facu
 */
public class PasarArchivos {
    public static Panel panel;
    /**
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        if (!SystemTray.isSupported()) {
            System.err.println("La bandeja de íconos no está soportada.");
            JOptionPane.showMessageDialog(null, "La bandeja de íconos del sistema no está disponible para Java. Cerrando aplicación.", "Error de inicio", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Panel.initTheme();
        panel = new Panel();
        
        try {
            ClientFinder.init();
            Transferencia.init();
        }
        catch (RuntimeException e) {
            if (e.getCause() instanceof BindException) {
                JOptionPane.showMessageDialog(null, "Hubo un error al iniciar la funcionalidad de descubrir otros dispositivos. Probablemente hay otra instancia de la aplicación ejecutándose en segundo plano..", "Error de inicio", JOptionPane.ERROR_MESSAGE);
            }
            else {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Hubo un error al iniciar la funcionalidad de descubrir otros dispositivos. Cerrando aplicación.", "Error de inicio", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        
        boolean abrir = true;
        for (String arg: args) {
            if (arg.equals("--hide")) {
                abrir = false;
            }
        }
        
        if (abrir) {
            panel.setVisible(true);
        }
        
        crearIconoBandeja();
        
        /*
        
        try {
            var redes = NetworkInterface.getNetworkInterfaces();
            while (redes.hasMoreElements()) {
                var interfaz = redes.nextElement();
                System.out.println(interfaz.getName() + " (" + interfaz.getDisplayName() + "):");
                
                var salto = false;
                var ips = interfaz.getInetAddresses();
                while (ips.hasMoreElements()) {
                    var ip = ips.nextElement();
                    System.out.println("|   " + ip.getHostAddress());
                    salto = true;
                }
                
                if (salto) System.out.println("");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(9060);
        } catch (SocketException ex) {
            System.err.println("No se pudo crear el socket");
            return;
        }
        System.out.println("Probando");
        try {
            System.out.println("Probando1");
            //Socket socket2 = new Socket(InetAddress.getByName("8.8.8.8"), 80);
            System.out.println("Probando2");
            //System.out.println(socket2.getLocalAddress());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        
        for (int i = 0; true; i++) {
            byte[] contenido = new byte[2];
            contenido[0] = (byte) (i / 256 % 256 - 128);
            contenido[1] = (byte) (i % 256 - 128);
            
            DatagramPacket packet;
            try {
                packet = new DatagramPacket(contenido, 2, InetAddress.getByName("255.255.255.255"),  9060);
                socket.send(packet);
                socket.receive(packet);
            } catch (Exception ex) {
                System.err.println(ex);
            }
            
            try {
                Thread.sleep(3000);
            }
            catch (Exception e) {}
        }
*/
    }
    
    private static void crearIconoBandeja() {
        System.out.println("Creando el ícono de bandeja");
        Image image = Toolkit.getDefaultToolkit().createImage("src/images/icono.png");
        TrayIcon icono = new TrayIcon(image, "Pasar archivos");
        
        icono.addMouseListener(new MouseListener() {
            @Override 
            public void mouseClicked(MouseEvent e) {
                panel.setLocation(e.getX() - panel.getWidth(), e.getY() - panel.getHeight());
                panel.setVisible(true);
            }
            
            @Override public void mouseEntered(MouseEvent e) {}
            @Override public void mouseExited(MouseEvent e) {}
            @Override public void mousePressed(MouseEvent e) {}
            @Override public void mouseReleased(MouseEvent e) {}
        });
        
        try {
            SystemTray tray = SystemTray.getSystemTray();
            tray.add(icono);
        }
        catch (AWTException e) {
            System.out.println("No se pudo añadir el ícono a la bandeja.");
        }
    }
}
