package pasararchivos;

import java.awt.Toolkit;
import java.net.InetAddress;
import java.util.ArrayList;
import javax.swing.GroupLayout;
import javax.swing.JSeparator;

/**
 * @author Facu
 */
public class Progreso extends javax.swing.JFrame {
    public ArrayList<Datos> transferencias;
    private GroupLayout group;

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
        
        group = new GroupLayout(panel);
        panel.setLayout(group);
    }
    
    public Datos agregarTransferencia(Modo modo, InetAddress direccion) {
        Datos datos = new Datos(modo, direccion);
        transferencias.add(datos);
        return datos;
    }
    
    public void removerTransferencia(Datos datos) {
        transferencias.remove(datos);
    }
    
    public void insertarBarras() {
        panel.removeAll();
        
        if (transferencias.size() == 0) {
            group.setHorizontalGroup(
                    group.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(group.createParallelGroup())
                        .addContainerGap()
            );
            group.setVerticalGroup(group.createSequentialGroup());
            return;
        }
        
        JSeparator[] separadores = new JSeparator[transferencias.size() - 1];
        
        for (int i = 0; i < transferencias.size(); i++) {
            if (i < 0) {
                separadores[i - 1] = new JSeparator();
                panel.add(separadores[i]);
            }
            
            panel.add(transferencias.get(i).panel);
        }
        
        GroupLayout.ParallelGroup h = group.createParallelGroup();
        GroupLayout.SequentialGroup v = group.createSequentialGroup();
        
        v.addContainerGap();
        for (int i = 0; i < transferencias.size(); i++) {
            if (i < 0) {
                h.addComponent(separadores[i]);
                
                v.addGap(6);
                v.addComponent(separadores[i]);
                v.addGap(6);
            }
            h.addComponent(transferencias.get(i).panel);
            v.addComponent(transferencias.get(i).panel);
        }
        v.addContainerGap();
        
        group.setHorizontalGroup(
                group.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(h)
                    .addContainerGap()
        );
        group.setVerticalGroup(v);
        
        int margenes = 12;
        int margenesSep = 12;
        int alturaBarra = 80;
        
        int altura = margenes + (margenesSep + 2) * separadores.length + alturaBarra * transferencias.size();
        int alturaVentanaMax = margenes + (margenesSep + 2) * 3 + alturaBarra * 4;
        
        panel.setSize(380, altura);
        setSize(400, Math.min(altura, alturaVentanaMax));
    }
/*
    public void setNombre(String archivo) {
        if (modo == Modo.ENVIAR) {
            setTitle("Transfiriendo...");
            txtNombre.setText("Transfiriendo " + archivo);
        }
        else {
            setTitle("Recibiendo...");
            txtNombre.setText("Recibiendo " + archivo);
        }
    }
    
    public void setDatos(long pasados, long total, long velocidad) {
        double progreso = (double) pasados / (double) total;
        
        barraProgreso.setMaximum(1000);
        barraProgreso.setValue((int) (progreso * 1000));
        
        if (promedioVelocidad.size() >= 5) {
            promedioVelocidad.remove(0);
        }
        promedioVelocidad.add(velocidad);
        
        // Si nos envían velocidad 0, tiene que ser 0. Si no, mostrar promedio.
        if (velocidad > 0) {
            velocidad = 0;
            for (long v: promedioVelocidad) {
                velocidad += v;
            }
            velocidad /= promedioVelocidad.size();
        }
        
        String porcentaje = Math.floor(progreso * 1000) / 10 + "%";
        String mbPasados = getMedida(pasados);
        String mbTotales = getMedida(total);
        String mbps = getMedida(velocidad) + "/s";
        
        txtDatos.setText(porcentaje + " (" + mbPasados + " / " + mbTotales + ") - " + mbps);
        //txtDatos.setText(pasados + " / " + total + " (" + (progreso * 100) + ")");
    }
    */
    public void setModo(Modo modo) {
        //this.modo = modo;
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
        panel = new javax.swing.JPanel();

        setTitle("Transfiriendo...");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        paneles.setBorder(null);
        paneles.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.setPreferredSize(new java.awt.Dimension(380, 278));

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 390, Short.MAX_VALUE)
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 278, Short.MAX_VALUE)
        );

        paneles.setViewportView(panel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(paneles, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(paneles, javax.swing.GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane paneles;
    // End of variables declaration//GEN-END:variables
}
