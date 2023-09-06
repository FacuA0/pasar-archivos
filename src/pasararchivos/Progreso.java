package pasararchivos;

import java.awt.Toolkit;
import java.net.InetAddress;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JSeparator;

/**
 * @author Facu
 */
public class Progreso extends javax.swing.JFrame {
    public ArrayList<Datos> transferencias;
    private BoxLayout layout;

    /**
     * Creates new form Transfiriendo
     */
    public Progreso() {
        initComponents();
        
        // Ventana
        setAutoRequestFocus(true);
        setResizable(true);
        setAlwaysOnTop(true);
        setIconImage(Toolkit.getDefaultToolkit().getImage("src/images/icono.png"));
        
        // Variables
        transferencias = new ArrayList();
        
        layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);
        
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        
        actualizarBarras();
    }
    
    public int agregarTransferencia(Modo modo, InetAddress direccion) {
        Datos datos = new Datos(modo, direccion);
        transferencias.add(datos);
        int hash = datos.hashCode();
        actualizarBarras();
        return hash;
    }
    
    public void removerTransferencia(int idDatos) {
        transferencias.remove(getDatos(idDatos));
        actualizarBarras();
    }
    
    private void refrescarTitulo() {
        int envios = 0, recepciones = 0;
        
        for (Datos d: transferencias) {
            if (d.modo == Modo.ENVIAR) envios++;
            else recepciones++;
        }
        
        String sufijo = (envios + recepciones > 1 ? "archivos..." : "archivo...");
        
        if (envios > 0 && recepciones == 0) {
            setTitle("Transfiriendo " + sufijo);
        }
        else if (envios == 0 && recepciones > 0) {
            setTitle("Recibiendo " + sufijo);
        }
        else if (envios > 0 && recepciones > 0) {
            setTitle("Transfiriendo y recibiendo " + sufijo);
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
        
        datos.panel.setNombreArchivo(archivo);
        datos.panel.setCantidad(indice, cantidad);
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
    
    public String getMedida(long bytes) {
        float bytesF = (float) bytes;
        
        if (bytes < 1000)
            return bytes + " B";
        else if (bytes < 10000) // 1 - 10 KB
            return (Math.floor(bytesF / 10) / 100) + " KB";
        else if (bytes < 100000) // 10 - 100 KB
            return (Math.floor(bytesF / 100) / 10) + " KB";
        else if (bytes < 1000000) // 100 KB - 1 MB
            return (bytes / 1000) + " KB";
        else if (bytes < 10000000) // 1 - 10 MB
            return (Math.floor(bytesF / 10000) / 100) + " MB";
        else if (bytes < 100000000) // 10 - 100 MB
            return (Math.floor(bytesF / 100000) / 10) + " MB";
        else if (bytes < 1000000000) // 100 MB - 1 GB
            return (bytes / 1000000) + " MB";
        else if (bytes < 10000000000L) // 1 - 10 GB
            return (Math.floor(bytesF / 10000000) / 100) + " GB";
        else if (bytes < 100000000000L) // 10 - 100 GB
            return (Math.floor(bytesF / 100000000) / 10) + " GB";
        else if (bytes < 1000000000000L) // 100 GB - 1 TB
            return (bytes / 1000000000) + " GB";
        else if (bytes < 10000000000000L) // 1 - 10 TB
            return (Math.floor(bytesF / 10000000000L) / 100) + " TB";
        else if (bytes < 100000000000000L) // 10 - 100 TB
            return (Math.floor(bytesF / 100000000000L) / 10) + " TB";
        else // >= 100 TB
            return (bytes / 1000000000000L) + " TB";
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
        Modo modo;
        
        public Datos(Modo modo, InetAddress direccion) {
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
            
            this.panel = new PanelProgreso();
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

        setTitle("Transfiriendo...");

        paneles.setBorder(null);
        paneles.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 388, Short.MAX_VALUE)
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
                .addContainerGap()
                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel panel;
    private javax.swing.JPanel panelMedio;
    private javax.swing.JScrollPane paneles;
    // End of variables declaration//GEN-END:variables
}
