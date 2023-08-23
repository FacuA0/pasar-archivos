package pasararchivos;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.TransferHandler;

/**
 * @author Facu
 */
public class ListTransferHandler extends TransferHandler {
    public Panel panel;
    
    public ListTransferHandler(Panel panel) {
        super();
        this.panel = panel;
    }
    
    public boolean canImport(TransferHandler.TransferSupport soporte) {
        if (!soporte.isDrop()) {
            return false;
        }
        
        soporte.setDropAction(COPY);
        return soporte.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }
    
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }
        
        DefaultListModel modelo;
        try {
            JList lista = (JList) info.getComponent();
            modelo = (DefaultListModel) lista.getModel();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        List<File> archivos;
        try {        
            archivos = (List<File>) info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
        }
        catch (Exception e) {
            System.err.println("Error");
            e.printStackTrace();
            return false;
        }
        
        modelo.clear();
        
        for (File archivo: archivos) {
            modelo.add(modelo.size(), archivo.getAbsolutePath());
        }
        
        panel.habilitarBoton();
        
        return true;
    }
}
