package pasararchivos;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Facu
 */
public class ClientFinder extends Thread {
    private static final int PUERTO = 9060;
    private static final String LOCK = "lock";
    private static ClientFinder emitter, listener;
    private static ConcurrentHashMap<String, Clientes> pares;
    private static String nombre;
    private static DatagramSocket socket;
    private static InetAddress direccionLocal;
    Mode mode;
    
    private ClientFinder(Mode mode) {
        this.mode = mode;
    }
    
    public static void init() throws RuntimeException {
        try {
            direccionLocal = InetAddress.getByName(getLocalAddress());
            socket = new DatagramSocket(PUERTO, direccionLocal);
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
        catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        listener = new ClientFinder(Mode.RECEIVER);
        emitter = new ClientFinder(Mode.EMITTER);
            
        pares = new ConcurrentHashMap();
        try {
            nombre = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            nombre = "Desconocido";
        }
        
        listener.start();
        emitter.start();
    }
    
    public static HashMap<String, String> getDispositivos() {
        synchronized (LOCK) {
            HashMap<String, String> dispositivos = new HashMap();
            
            int i = 0;
            for (Clientes c: pares.values()) {
                dispositivos.put(c.direccion.getHostAddress(), c.nombre);
            }
            return dispositivos;
        }
    }
    
    private static String getLocalAddress() throws IOException {
        Socket sock = new Socket(InetAddress.getByName("8.8.8.8"), 53);
        InetAddress direccion = sock.getLocalAddress();
        sock.close();
        
        return direccion.getHostAddress();
    }
    
    @Override
    public void run() {
        if (mode == Mode.EMITTER) {
            emit();
        }
        else {
            listen();
        }
    }
    
    public void emit() {
        InetAddress broadcast;
        try {
            broadcast = InetAddress.getByName("255.255.255.255");
        } catch (UnknownHostException ex) {
            System.err.println("Error al crear dirección de broadcast.");
            return;
        }
        
        byte[] contenido = ("Usuario:" + nombre).getBytes();
        
        while (true) {
            DatagramPacket packet;
            try {
                System.out.println("Enviando paquete");
                packet = new DatagramPacket(contenido, contenido.length, broadcast, PUERTO);
                socket.send(packet);
            } catch (IOException ex) {
                System.err.println(ex);
            }
            
            // Remover pares viejos no renovados
            long limite = 6000;
            synchronized (LOCK) {
                for (String clave: pares.keySet()) {
                    long ahora = new Date().getTime();
                    long antes = pares.get(clave).fechaEmision;

                    if (ahora - antes > limite) {
                        pares.remove(clave);
                    }
                }
            }
            
            EventQueue.invokeLater(() -> {
                Panel panel = PasarArchivos.panel;
                if (panel != null && panel.isVisible()) {
                    PasarArchivos.panel.actualizarLista();
                }
            });
            
            try {
                Thread.sleep(3000);
            }
            catch (Exception e) {}
        }
    }
    
    public void listen() {
        while (true) {
            byte[] datos = new byte[72];
            DatagramPacket paquete = new DatagramPacket(datos, datos.length);
            try {
                socket.receive(paquete);
                
                // El paquete es muy pequeño. Descartar.
                if (paquete.getLength() <= 8) {
                    continue;
                }
                
                byte[] firma = new byte[8];
                System.arraycopy(datos, 0, firma, 0, 8);
                
                // El paquete vino de otro programa y se equivocó de puerto. Descartar.
                if (!new String(firma).equals("Usuario:")) {
                    continue;
                }
                
                InetAddress origen = paquete.getAddress();
                
                // Recibimos nuestro propio paquete. Descartar.
                if (origen.getHostAddress().equals(direccionLocal.getHostAddress())) {
                    continue;
                }
                
                long ahora = new Date().getTime();
                
                // Nombre del equipo
                byte[] nombreB = new byte[paquete.getLength() - 8];
                System.arraycopy(datos, 8, nombreB, 0, nombreB.length);
                String nombre = new String(nombreB);
                
                synchronized (LOCK) {
                    Clientes nuevo = new Clientes(nombre, origen, ahora);
                    pares.put(origen.getHostAddress(), nuevo);
                }
            } 
            catch (IOException ex) {
                System.err.println("Error al esperar paquete");
            }
        }
    }
    
    enum Mode {
        EMITTER,
        RECEIVER
    }
    
    public class Clientes {
        public String nombre;
        public InetAddress direccion;
        public long fechaEmision;
        
        public Clientes(String nombre, InetAddress direccion, long fechaEmision) {
            this.nombre = nombre;
            this.direccion = direccion;
            this.fechaEmision = fechaEmision;
        }
    }
}
