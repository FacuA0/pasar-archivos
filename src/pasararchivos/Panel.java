package pasararchivos;

import java.awt.Toolkit;
import java.io.File;
import java.util.HashMap;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * @author Facu
 */
public class Panel extends javax.swing.JFrame {

    DefaultListModel modeloArchivos, modeloDispositivos;
    HashMap<String, String> dispositivos;
    
    /**
     * Creates new form Panel
     */
    public Panel() {
        initComponents();
        
        // Ventana
        setAutoRequestFocus(true);
        setResizable(false);
        setAlwaysOnTop(true);
        setLocationRelativeTo(null);
        setIconImage(Toolkit.getDefaultToolkit().getImage("src/images/icono.png"));
        
        // Listas
        modeloArchivos = new DefaultListModel<>();
        listaArchivos.setModel(modeloArchivos);
        listaArchivos.setDropMode(DropMode.ON);
        listaArchivos.setTransferHandler(new ListTransferHandler(this));
        
        modeloDispositivos = new DefaultListModel<>();
        listaDispositivos.setModel(modeloDispositivos);
        
        chooser.setMultiSelectionEnabled(true);
        
        dispositivos = new HashMap();
    }
    
    public void actualizarLista() {
        HashMap<String, String> nuevo = ClientFinder.getDispositivos();
        
        // Añadir elementos 
        for (String clave: nuevo.keySet()) {
            if (!dispositivos.containsKey(clave)) {
                String nombre = nuevo.get(clave) + " (" + clave + ")";
                modeloDispositivos.addElement(nombre);
            }
        }
        
        // Quitar elementos no existentes
        for (String clave: dispositivos.keySet()) {
            if (!nuevo.containsKey(clave)) {
                String nombre = dispositivos.get(clave) + " (" + clave + ")";
                modeloDispositivos.removeElement(nombre);
            }
        }
        
        dispositivos = nuevo;
    }
    
    public void habilitarBoton() {
        btnTransferir.setEnabled(listaDispositivos.getSelectedIndex() != -1 && !modeloArchivos.isEmpty());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        chooser = new javax.swing.JFileChooser();
        texto = new javax.swing.JLabel();
        scrollArchivos = new javax.swing.JScrollPane();
        listaArchivos = new javax.swing.JList<>();
        salir = new javax.swing.JButton();
        seleccionar = new javax.swing.JButton();
        labelDispositivos = new javax.swing.JLabel();
        scrollDispositivos = new javax.swing.JScrollPane();
        listaDispositivos = new javax.swing.JList<>();
        btnTransferir = new javax.swing.JButton();

        chooser.setDialogTitle("Elegir archivos");
        chooser.setFileFilter(null);

        setTitle("Pasar Archivos");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        texto.setText("Hola usuario");

        listaArchivos.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Seleccionar archivo" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        scrollArchivos.setViewportView(listaArchivos);

        salir.setText("Salir");
        salir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salirActionPerformed(evt);
            }
        });

        seleccionar.setText("Seleccionar archivo");
        seleccionar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                seleccionarActionPerformed(evt);
            }
        });

        labelDispositivos.setText("Dispositivos disponibles");

        listaDispositivos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listaDispositivos.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listaDispositivosValueChanged(evt);
            }
        });
        scrollDispositivos.setViewportView(listaDispositivos);

        btnTransferir.setText("Transferir");
        btnTransferir.setEnabled(false);
        btnTransferir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTransferirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollDispositivos, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                    .addComponent(scrollArchivos)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnTransferir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(salir))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(texto)
                            .addComponent(seleccionar)
                            .addComponent(labelDispositivos))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(texto)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(seleccionar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollArchivos, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelDispositivos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollDispositivos, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(salir)
                    .addComponent(btnTransferir))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void salirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salirActionPerformed
        System.exit(0);
    }//GEN-LAST:event_salirActionPerformed

    private void seleccionarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_seleccionarActionPerformed
        int resultado = chooser.showDialog(seleccionar, "Seleccionar");
        
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File[] archivos = chooser.getSelectedFiles();
            
            modeloArchivos.clear();
            
            for (int i = 0; i < archivos.length; i++) {
                modeloArchivos.addElement(archivos[i].getAbsolutePath());
            }
            
            habilitarBoton();
        }
    }//GEN-LAST:event_seleccionarActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        setVisible(false);
    }//GEN-LAST:event_formWindowClosing

    private void btnTransferirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTransferirActionPerformed
       JOptionPane.showMessageDialog(this, "Iniciando transferencia");
        
        String[] archivos = new String[modeloArchivos.size()];
        for (int i = 0; i < archivos.length; i++) {
            archivos[i] = (String) modeloArchivos.get(i);
        }
        
        String direccion = "";
        String seleccion = (String) modeloDispositivos.get(listaDispositivos.getSelectedIndex());
        for (String clave: dispositivos.keySet()) {
            String nombre = dispositivos.get(clave) + " (" + clave + ")";
            if (nombre.equals(seleccion)) {
                direccion = clave;
            }
        }
        Transferencia.transferir(archivos, direccion);
    }//GEN-LAST:event_btnTransferirActionPerformed

    private void listaDispositivosValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listaDispositivosValueChanged
        habilitarBoton();
    }//GEN-LAST:event_listaDispositivosValueChanged

    public static void initTheme() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (info.getName().equals("Nimbus")) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | 
                IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Panel.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnTransferir;
    private javax.swing.JFileChooser chooser;
    private javax.swing.JLabel labelDispositivos;
    private javax.swing.JList<String> listaArchivos;
    private javax.swing.JList<String> listaDispositivos;
    private javax.swing.JButton salir;
    private javax.swing.JScrollPane scrollArchivos;
    private javax.swing.JScrollPane scrollDispositivos;
    private javax.swing.JButton seleccionar;
    private javax.swing.JLabel texto;
    // End of variables declaration//GEN-END:variables
}
