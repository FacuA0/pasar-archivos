package pasararchivos;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        
        ArrayList<File> archivos;
        try {
            List<File> listaArchivos = (List<File>) info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            archivos = new ArrayList<>(listaArchivos);
            
            // Filtrar carpetas
            archivos.removeIf(f -> !f.isFile());
        }
        catch (UnsupportedFlavorException | IOException e) {
            PasarArchivos.error(null, e, "Error de soltar", "Hubo un error al realizar la operación de arrastrar y soltar.");
            return false;
        }
        
        panel.archivos = archivos.toArray(File[]::new);
        panel.actualizarArchivos();
        
        return true;
    }
}
