package pasararchivos;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.BindException;
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
        Panel.initTheme();
        
        if (!SystemTray.isSupported()) {
            System.err.println("La bandeja de íconos no está soportada.");
            JOptionPane.showMessageDialog(null, "La bandeja de íconos del sistema no está disponible para Java. Cerrando aplicación.", "Error de inicio", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        panel = new Panel();
        
        try {
            ClientFinder.init();
            Transferencia.init();
        }
        catch (RuntimeException e) {
            if (e.getCause() instanceof BindException) {
                JOptionPane.showMessageDialog(null, "Hubo un error al iniciar la funcionalidad de descubrir otros dispositivos. Probablemente haya otra instancia de la aplicación abierta en segundo plano.", "Error de inicio", JOptionPane.ERROR_MESSAGE);
            }
            else {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Hubo un error al iniciar la funcionalidad de descubrir otros dispositivos. Cerrando aplicación.", "Error de inicio", JOptionPane.ERROR_MESSAGE);
            }
            
            System.exit(1);
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
    }
    
    private static void crearIconoBandeja() {
        System.out.println("Creando el ícono de bandeja");
        Image image = Toolkit.getDefaultToolkit().createImage("src/images/icono.png");
        TrayIcon icono = new TrayIcon(image, "Pasar archivos");
        icono.setImageAutoSize(true);
        
        icono.addMouseListener(new MouseListener() {
            @Override 
            public void mouseClicked(MouseEvent e) {
                if (panel.isVisible()) {
                    int estado = panel.getExtendedState();
                    if ((estado & Frame.ICONIFIED) == Frame.ICONIFIED) {
                        panel.setExtendedState(estado - Frame.ICONIFIED);
                    }
                    else {
                        panel.setVisible(false);
                    }
                }
                else {
                    panel.setLocation(e.getX() - panel.getWidth(), e.getY() - panel.getHeight());
                    panel.setVisible(true);
                }
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
