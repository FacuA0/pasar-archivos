package pasararchivos;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileWriter;
import java.io.IOException;
import java.net.BindException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * @author Facu
 */
public class PasarArchivos {
    public static Panel panel;
    public static final Logger log = Logger.getLogger("PasarArchivos");
    /**
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        definirRegistro();
        
        if (!SystemTray.isSupported()) {
            System.err.println("La bandeja de íconos no está soportada.");
            JOptionPane.showMessageDialog(null, "La bandeja de íconos del sistema no está disponible para Java. Cerrando aplicación.", "Error de inicio", JOptionPane.ERROR_MESSAGE);
            log.severe("La bandeja de íconos del sistema no está disponible para Java. Cerrando aplicación.");
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
    
    private static void definirRegistro() {
        log.setLevel(Level.SEVERE);
        log.addHandler(new java.util.logging.Handler() {
            static FileWriter escritor;
            static {
                SimpleDateFormat formatoArchivo = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
                try {
                    escritor = new FileWriter("logs/" + formatoArchivo.format(new Date()) + ".txt");
                }
                catch (IOException e) {
                    escritor = null;
                }
            }
            
            @Override
            public void close() {
                if (escritor != null) {
                    try {
                        escritor.close();
                    }
                    catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "No se pudo activar el registro.", "Error de registro", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            
            @Override
            public void flush() {
                if (escritor != null) {
                    try {
                        escritor.flush();
                    }
                    catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "No se pudo activar el registro.", "Error de registro", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            
            @Override
            public void publish(java.util.logging.LogRecord registro) {
                if (escritor == null) return;
                
                try {
                    String mensaje = "[" + registro.getMillis() + "] - " + registro.getMessage();
                    escritor.write(mensaje);
                }
                catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "No se pudo enviar mensajes al registro..", "Error de registro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
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
