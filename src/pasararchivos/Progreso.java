package pasararchivos;

import java.awt.Toolkit;
import java.net.InetAddress;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

/**
 * @author Facu
 */
public class Progreso extends javax.swing.JFrame {
    public static final String LOCK = "lock";
    public ArrayList<Datos> transferencias;
    private BoxLayout layout;

    /**
     * Creates new form Transfiriendo
     */
    public Progreso() {
        initComponents();
        
        // Ventana
        setAutoRequestFocus(true);
        setResizable(false);
        setAlwaysOnTop(true);
        setLocationRelativeTo(null);
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/icono.png")));
        
        // Variables
        transferencias = new ArrayList();
        
        layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);
        
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        
        actualizarBarras();
    }
    
    public int agregarTransferencia(Transferencia.Transferidor hilo, Modo modo, InetAddress direccion) {
        Datos datos = new Datos(hilo, modo, direccion);
        
        synchronized (LOCK) {
            transferencias.add(datos);
            actualizarTitulo();
            actualizarBarras();

            if (transferencias.size() == 1) {
                setVisible(true);
                toFront();
            }
        }
        
        int hash = datos.hashCode();
        return hash;
    }
    
    public void removerTransferencia(int idDatos) {
        synchronized (LOCK) {
            transferencias.remove(getDatos(idDatos));

            if (transferencias.isEmpty()) {
                setVisible(false);
            }
            else {
                actualizarBarras();
            }

            actualizarTitulo();
        }
    }
    
    private void actualizarTitulo() {
        int envios = 0, recepciones = 0;
        
        for (Datos d: transferencias) {
            if (d.modo == Modo.ENVIAR) envios += d.cantidadArchivos;
            else recepciones += d.cantidadArchivos;
        }
        
        String sufijo = (envios + recepciones > 1 ? "archivos..." : "archivo...");
        
        if (envios > 0 && recepciones == 0) {
            setTitle("Enviando " + sufijo);
        }
        else if (envios == 0 && recepciones > 0) {
            setTitle("Recibiendo " + sufijo);
        }
        else if (envios > 0 && recepciones > 0) {
            setTitle("Transfiriendo " + sufijo);
        }
        else {
            setTitle("Barra de progreso");
        }
    }
    
    public void actualizarBarras() {
        panel.removeAll();
        
        for (int i = 0; i < transferencias.size(); i++) {
            if (i > 0) {
                panel.add(new JSeparator());
            }
            panel.add(transferencias.get(i).panel);
        }
        
        int margenes = 0;
        int margenesSep = 0;
        int alturaBarra = 80;
        int cantidad = transferencias.size();
        
        int altura = margenes + (margenesSep + 2) * (cantidad - 1) + alturaBarra * cantidad;
        
        pack();
        if (getHeight() > 600) {
            setSize(getWidth(), 600);
        }
    }
    
    public void setNombreArchivo(int idDatos, String archivo, int indice, int cantidad) {
        Datos datos = getDatos(idDatos);
        datos.nombreArchivo = archivo;
        datos.indice = indice;
        datos.cantidadArchivos = cantidad;
        
        actualizarTitulo();
        
        datos.panel.setCantidad(indice, cantidad);
        datos.panel.setNombreArchivo(archivo);
        datos.panel.setModoBarra(false);
    }
    
    public void setDatos(int idDatos, long pasados, long total, long velocidad) {
        Datos d = getDatos(idDatos);
        d.progreso = pasados;
        d.largoArchivo = total;
        d.velocidad = velocidad;
        
        double progreso = (double) pasados / (double) total;
        
        d.panel.setMaximoBarra(1000);
        d.panel.setValorBarra((int) (progreso * 1000));
        
        if (d.velocidadesPromedio.size() >= 5) {
            d.velocidadesPromedio.remove(0);
        }
        d.velocidadesPromedio.add(velocidad);
        
        // Si nos envían velocidad 0, tiene que ser 0. Si no, mostrar promedio.
        if (velocidad > 0) {
            velocidad = 0;
            for (long v: d.velocidadesPromedio) {
                velocidad += v;
            }
            velocidad /= d.velocidadesPromedio.size();
        }
        
        String porcentaje = Math.floor(progreso * 1000) / 10 + "%";
        String mbPasados = getMedida(pasados);
        String mbTotales = getMedida(total);
        String mbps = getMedida(velocidad) + "/s";
        
        d.panel.setDatos(porcentaje, mbPasados, mbTotales, mbps);
        //txtDatos.setText(pasados + " / " + total + " (" + (progreso * 100) + ")");
    }
    
    public void finalizar(int idDatos) {
        Datos d = getDatos(idDatos);
        d.panel.finalizar();
    }
    
    public void setModo(Modo modo) {
        //this.modo = modo;
    }
    
    private Datos getDatos(int idDatos) {
        for (Datos d: transferencias) {
            if (d.hashCode() == idDatos) {
                return d;
            }
        }
        
        return null;
    }
    
    public static String getMedida(long bytes) {
        String[] sufijos = new String[] {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        
        if (bytes < 1000) {
            return bytes + " B";
        }
        else {
            double bytesD = (double) bytes;
            int indice = (int) Math.log10(bytesD);

            int nivel = Math.min(indice / 3, sufijos.length - 1);
            String sufijo = sufijos[nivel];
            
            return (Math.floor(bytesD / Math.pow(10, -2 + nivel * 3)) / 100) + " " + sufijo;
        }
    }
    /*
    @Override
    public void setVisible(boolean visible) {
        System.out.println("setVisible(" + visible + ")");
        if (visible) {
            setLocationRelativeTo(PasarArchivos.panel);
            promedioVelocidad.clear();
        }
        super.setVisible(visible);
    }
    */
    public static enum Modo {
        ENVIAR,
        RECIBIR
    }
    
    public class Datos {
        String nombreArchivo;
        int cantidadArchivos;
        int indice;
        long progreso;
        long largoArchivo;
        long velocidad;
        InetAddress direccion;
        String nombreHost;
        ArrayList<Long> velocidadesPromedio;
        PanelProgreso panel;
        Transferencia.Transferidor hilo;
        Modo modo;
        
        public Datos(Transferencia.Transferidor hilo, Modo modo, InetAddress direccion) {
            this.modo = modo;
            this.nombreArchivo = "";
            this.cantidadArchivos = 1;
            this.indice = 1;
            this.progreso = 0;
            this.largoArchivo = 0;
            this.velocidad = 0;
            this.velocidadesPromedio = new ArrayList();
            this.direccion = direccion;
            this.nombreHost = "";
            this.hilo = hilo;
            
            this.panel = new PanelProgreso(this);
        }
    }
    
    private void detenerTodo() {
        if (transferencias.size() == 1) {
            setTitle("Cancelando transferencia...");
        }
        else {
            setTitle("Cancelando transferencias...");
        }
        
        for (Datos d: transferencias) {
            d.hilo.detener();
        }
    }
    
    public void detenerTransferencia(PanelProgreso panel) {
        if (transferencias.size() == 1) {
            setTitle("Cancelando transferencia...");
        }
        else {
            setTitle("Cancelando transferencias...");
        }
        
        for (Datos d: transferencias) {
            d.hilo.detener();
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        paneles = new javax.swing.JScrollPane();
        panelMedio = new javax.swing.JPanel();
        panel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Transfiriendo...");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        paneles.setBorder(null);
        paneles.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 375, Short.MAX_VALUE)
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 92, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout panelMedioLayout = new javax.swing.GroupLayout(panelMedio);
        panelMedio.setLayout(panelMedioLayout);
        panelMedioLayout.setHorizontalGroup(
            panelMedioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMedioLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12))
        );
        panelMedioLayout.setVerticalGroup(
            panelMedioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        paneles.setViewportView(panelMedio);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(paneles)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(paneles)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        boolean singular = transferencias.size() == 1;
        String titulo = "Detener transferencia" + (singular ? "" : "s");
        String mensaje = singular ? "¿Deseas detener la transferencia?" : "¿Deseas detener las transferencias?";
        
        int detener = JOptionPane.showConfirmDialog(this, mensaje, titulo, JOptionPane.YES_NO_OPTION);
        if (detener == JOptionPane.YES_OPTION) {
            detenerTodo();
        }
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel panel;
    private javax.swing.JPanel panelMedio;
    private javax.swing.JScrollPane paneles;
    // End of variables declaration//GEN-END:variables
}
