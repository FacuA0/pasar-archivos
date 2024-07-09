package pasararchivos;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
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
    
    @Override
    public boolean canImport(TransferHandler.TransferSupport soporte) {
        if (!soporte.isDrop()) {
            return false;
        }
        
        soporte.setDropAction(COPY);
        return soporte.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }
    
    @Override
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
            PasarArchivos.error(e, "Error de soltar", "Hubo un error al realizar la operación de arrastrar y soltar.");
            return false;
        }
        
        List<File> archivos;
        try {        
            archivos = (List<File>) info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
        }
        catch (UnsupportedFlavorException | IOException e) {
            PasarArchivos.error(e, "Error de soltar", "Hubo un error al realizar la operación de arrastrar y soltar.");
            return false;
        }
        
        panel.archivos = new File[archivos.size()];
        for (int i = 0; i < archivos.size(); i++) {
            panel.archivos[i] = archivos.get(i);
        }
        
        panel.actualizarArchivos();
        
        return true;
    }
}
