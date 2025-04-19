package pasararchivos;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * @author Facu
 */
public class PasarArchivos {
    public static Panel panel;
    public static final Logger log = Logger.getLogger("PasarArchivos");
    public static final String NOMBRE_APP = "Pasar Archivos";
    public static final String VERSION_APP = "1.3.3";
    public static boolean systemTray = true;
    
    /**
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        iniciarRegistro();
        crearIconoBandeja();
        
        Panel.initTheme();
        panel = new Panel();
        
        try {
            ClientFinder.init();
            Transferencia.init();
        }
        catch (RuntimeException e) {
            String mensaje;
            if (e.getCause() instanceof BindException) {
                mensaje = "Hay otra instancia de la aplicación abierta en segundo plano. Cerrando aplicación.";
            }
            else {
                mensaje = "Hubo un error al iniciar la funcionalidad de descubrir otros dispositivos. Cerrando aplicación.";
            }
            
            error(null, e, "Error de inicio", mensaje);
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
    }
    
    public static void dialogo(JFrame ventana, String titulo, String mensaje) {
        int tipoInfo = JOptionPane.INFORMATION_MESSAGE;
        if (ventana == null) ventana = panel;
        
        JOptionPane.showMessageDialog(ventana, mensaje, titulo, tipoInfo);
    }
    
    public static void advertir(JFrame ventana, String titulo, String mensaje) {
        logWarning(null, titulo, mensaje);
        
        int tipoAdvertencia = JOptionPane.WARNING_MESSAGE;
        if (ventana == null) ventana = panel;
        
        JOptionPane.showMessageDialog(ventana, mensaje, titulo, tipoAdvertencia);
    }
    
    public static void error(JFrame ventana, Exception error, String titulo, String mensaje) {
        logError(error, titulo, mensaje);
        
        int tipoError = JOptionPane.ERROR_MESSAGE;
        if (ventana == null) ventana = panel;
        
        JOptionPane.showMessageDialog(ventana, mensaje, titulo, tipoError);
    }
    
    public static void logError(Exception error, String titulo, String mensaje) {
        if (error != null) {
            error.printStackTrace(System.out);
        }
        
        String logMensaje = titulo + ": " + mensaje;
        log.log(Level.SEVERE, logMensaje, error);
    }
    
    public static void logWarning(Exception error, String titulo, String mensaje) {
        if (error != null) {
            error.printStackTrace(System.out);
        }
        
        String logMensaje = titulo + ": " + mensaje;
        log.log(Level.WARNING, logMensaje, error);
    }
    
    private static void iniciarRegistro() {
        log.setLevel(Level.WARNING);
        log.addHandler(new java.util.logging.Handler() {
            static FileWriter escritor;
            static boolean archivoCreado = false;
            static final SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS XXX");
            
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
                
                Throwable error = registro.getThrown();
                Level nivel = registro.getLevel();
                boolean grave = nivel == Level.SEVERE;
                
                String fecha = formatoFecha.format(new Date(registro.getMillis()));
                String prefijo = "[" + fecha + "] " + nivel;
                
                String mensaje = registro.getMessage();
                String errorCorto = error != null && !grave ? " - " + error : "";
                String linea = prefijo + " - " + mensaje + errorCorto + "\n";
                
                try {
                    escritor.write(linea);
                    escritor.flush();
                    
                    if (error != null && grave) {
                        error.printStackTrace(new PrintWriter(escritor, true));
                    }
                }
                catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "No se pudo enviar mensajes al registro.", "Error de registro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
    
    private static void crearIconoBandeja() {
        if (!SystemTray.isSupported()) {
            String mensaje = "La bandeja de íconos del sistema no está disponible para Java.";
            logWarning(null, "Bandeja de íconos no soportada.", mensaje);
            systemTray = false;
            return;
        }
        
        Image image = Toolkit.getDefaultToolkit().createImage("src/images/icono.png");
        
        MenuItem itemSalir = new MenuItem("Salir");
        itemSalir.addActionListener(e -> System.exit(0));
        
        PopupMenu menuIcono = new PopupMenu();
        menuIcono.add(itemSalir);
        
        System.out.println("Creando el ícono de bandeja");
        TrayIcon icono = new TrayIcon(image, NOMBRE_APP, menuIcono);
        icono.setImageAutoSize(true);
        
        icono.addMouseListener(new MouseListener() {
            boolean popupTrigger = false;
            
            @Override 
            public void mouseClicked(MouseEvent e) {
                if (popupTrigger) {
                    popupTrigger = false;
                    return;
                }
                
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
            
            @Override public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) popupTrigger = true;
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) popupTrigger = true;
            }
            
            @Override public void mouseEntered(MouseEvent e) {}
            @Override public void mouseExited(MouseEvent e) {}
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
