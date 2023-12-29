package pasararchivos;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
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
            String mensaje = "La bandeja de íconos del sistema no está disponible para Java. Cerrando aplicación.";
            error(null, "Bandeja de íconos no soportada.", mensaje);
            return;
        }
        
        Panel.initTheme();
        panel = new Panel();
        
        try {
            ClientFinder.init();
            Transferencia.init();
        }
        catch (RuntimeException e) {
            String mensaje;
            if (e.getCause() instanceof BindException) {
                mensaje = "Hubo un error al iniciar la funcionalidad de descubrir otros dispositivos. Probablemente haya otra instancia de la aplicación abierta en segundo plano.";
            }
            else {
                mensaje = "Hubo un error al iniciar la funcionalidad de descubrir otros dispositivos. Cerrando aplicación.";
            }
            
            error(e, "Error de inicio", mensaje);
            //panel.hayInternet(false);
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
    
    public static void mostrarDialogo(String titulo, String mensaje) {
        int tipoInfo = JOptionPane.INFORMATION_MESSAGE;
        JOptionPane.showMessageDialog(panel, mensaje, titulo, tipoInfo);
    }
    
    public static void error(Exception error, String titulo, String mensaje) {
        if (error != null) {
            error.printStackTrace();
        }
        
        String logError = error != null ? error.toString() : "No error.";
        log.log(Level.SEVERE, "{0}: {1} - {2}", new Object[] {titulo, mensaje, logError});
        
        int tipoError = JOptionPane.ERROR_MESSAGE;
        JOptionPane.showMessageDialog(panel, mensaje, titulo, tipoError);
    }
    
    private static void definirRegistro() {
        log.setLevel(Level.WARNING);
        log.addHandler(new java.util.logging.Handler() {
            static FileWriter escritor;
            static boolean archivoCreado = false;
            static final SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
            
            private void crearArchivo() {
                SimpleDateFormat formatoArchivo = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss_SSS");
                try {
                    File registro = new File("./logs/" + formatoArchivo.format(new Date()) + ".txt");
                    registro.getParentFile().mkdirs();
                    registro.createNewFile();
                    
                    escritor = new FileWriter(registro);
                }
                catch (IOException e) {
                    escritor = null;
                    System.err.println("No se pudo inicializar el registro. " + e.toString());
                }
            }
            
            @Override
            public void close() {
                if (escritor == null) return;
                
                try {
                    escritor.close();
                }
                catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "No se pudo cerrar el registro.", "Error de registro", JOptionPane.ERROR_MESSAGE);
                }
            }
            
            @Override
            public void flush() {
                if (escritor == null) return;
                
                try {
                    escritor.flush();
                }
                catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "No se pudo guardar el registro.", "Error de registro", JOptionPane.ERROR_MESSAGE);
                }
            }
            
            @Override
            public void publish(java.util.logging.LogRecord registro) {
                if (!archivoCreado) {
                    crearArchivo();
                    archivoCreado = true;
                }
                
                if (escritor == null) return;
                
                try {
                    String fecha = formatoFecha.format(new Date(registro.getMillis()));
                    String mensaje = "[" + fecha + "] " + registro.getLevel() + " - " + registro.getMessage();
                    escritor.write(mensaje);
                }
                catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "No se pudo enviar mensajes al registro.", "Error de registro", JOptionPane.ERROR_MESSAGE);
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
