/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package pasararchivos;

import java.awt.FontMetrics;
import javax.swing.JOptionPane;

/**
 *
 * @author Facu
 */
public class PanelProgreso extends javax.swing.JPanel {
    private Progreso.Datos datos;
    boolean cancelando = false;
    
    /**
     * Creates new form PanelProgreso
     */
    public PanelProgreso(Progreso.Datos datos) {
        initComponents();
        
        this.datos = datos;
    }
    
    public void setMaximoBarra(int maximo) {
        barraProgreso.setMaximum(maximo);
    }
    
    public void setValorBarra(int valor) {
        barraProgreso.setValue(valor);
    }
    
    public void setModoBarra(boolean indeterminado) {
        barraProgreso.setIndeterminate(indeterminado);
    }
    
    public void setNombreArchivo(String nombre) {
        if (cancelando) return;
        
        nombre = abreviarNombreArchivo(nombre);
        
        if (datos.modo == Progreso.Modo.ENVIAR) {
            txtNombre.setText("Enviando " + nombre);
        }
        else {
            txtNombre.setText("Recibiendo " + nombre);
        }
    }
    
    public void setCantidad(int indice, int total) {
        txtCantidad.setText(indice + "/" + total);
        
        // El texto será visible si se transfiere más de un archivo.
        txtCantidad.setVisible(total > 1);
    }
    
    public void setDatos(String porcentaje, String pasados, String total, String velocidad) {
        txtDatos.setText(porcentaje + " (" + pasados + " / " + total + ") - " + velocidad);
    }
    
    private String abreviarNombreArchivo(String archivo) {
        FontMetrics medidas = txtNombre.getFontMetrics(txtNombre.getFont());
        int anchoNombre = medidas.stringWidth(archivo);
        int anchoTotal = 260;
        
        // El nombre entra sin problemas
        if (anchoNombre <= anchoTotal) {
            return archivo;
        }
        
        // Separar nombre y extensión y medir cada uno
        int indiceSufijo = archivo.lastIndexOf(".");
        
        String sufijo = indiceSufijo > 0 ? archivo.substring(indiceSufijo) : "";
        String nombre = indiceSufijo > 0 ? archivo.substring(0, indiceSufijo) : archivo;
        String puntos = indiceSufijo > 0 ? ".." : "...";
        String nombreCorto = "";
        
        for (int i = Math.min(nombre.length() - 1, 120); i > 1; i--) {
            nombreCorto = nombre.substring(0, i) + puntos + sufijo;
            anchoNombre = medidas.stringWidth(nombreCorto);
            
            if (anchoNombre <= anchoTotal) break;
        }
        
        return nombreCorto;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        txtNombre = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        txtCantidad = new javax.swing.JLabel();
        barraProgreso = new javax.swing.JProgressBar();
        txtDatos = new javax.swing.JLabel();
        btnCancelar = new javax.swing.JButton();

        setMaximumSize(new java.awt.Dimension(32767, 80));

        jPanel1.setMaximumSize(new java.awt.Dimension(16, 16));
        jPanel1.setPreferredSize(new java.awt.Dimension(376, 16));
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));

        txtNombre.setText("Conectando...");
        jPanel1.add(txtNombre);
        jPanel1.add(filler1);

        txtCantidad.setText("0/0");
        jPanel1.add(txtCantidad);

        barraProgreso.setIndeterminate(true);
        barraProgreso.setMaximumSize(new java.awt.Dimension(32767, 24));
        barraProgreso.setPreferredSize(new java.awt.Dimension(146, 24));

        txtDatos.setText("0% (0 MB / 0,0 MB) - 0,0 MB/s");

        btnCancelar.setBackground(new java.awt.Color(242, 242, 242));
        btnCancelar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/cancelar.png"))); // NOI18N
        btnCancelar.setToolTipText("Cancelar transferencia");
        btnCancelar.setBorder(null);
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(barraProgreso, javax.swing.GroupLayout.PREFERRED_SIZE, 376, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(txtDatos)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCancelar)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(barraProgreso, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtDatos)
                    .addComponent(btnCancelar))
                .addContainerGap(8, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        String titulo = "Detener transferencia";
        String mensaje = "¿Deseas detener la transferencia?";
        
        int detener = JOptionPane.showConfirmDialog(this, mensaje, titulo, JOptionPane.YES_NO_OPTION);
        if (detener != JOptionPane.YES_OPTION) return;
        
        cancelando = true;
        txtNombre.setText("Cancelando transferencia...");
        
        datos.hilo.detener();
    }//GEN-LAST:event_btnCancelarActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JProgressBar barraProgreso;
    private javax.swing.JButton btnCancelar;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel txtCantidad;
    private javax.swing.JLabel txtDatos;
    private javax.swing.JLabel txtNombre;
    // End of variables declaration//GEN-END:variables
}
