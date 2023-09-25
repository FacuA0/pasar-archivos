package pasararchivos;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
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
    private static ClientFinder emitter;
    private static ClientFinder[] listeners;
    private static ConcurrentHashMap<String, Clientes> pares;
    private static String nombreHost;
    private static DatagramSocket[] sockets;
    private static InetAddress[] direcciones;
    int indice;
    Mode mode;
    
    private ClientFinder(Mode mode, int indice) {
        this.mode = mode;
        this.indice = indice;
    }
    
    public static void init() throws RuntimeException {
        nombreHost = obtenerNombreHost();
        
        try {
            direcciones = obtenerDireccionesLocales();
        
            sockets = new DatagramSocket[direcciones.length];
            for (int i = 0; i < direcciones.length; i++) {
                sockets[i] = new DatagramSocket(PUERTO, direcciones[i]);
            }
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        if (nombreHost == null) {
            nombreHost = "Desconocido";
        }
        
        pares = new ConcurrentHashMap();
        
        emitter = new ClientFinder(Mode.EMITTER, 0);
        emitter.start();
        
        listeners = new ClientFinder[direcciones.length];
        for (int i = 0; i < direcciones.length; i++) {
            listeners[i] = new ClientFinder(Mode.RECEIVER, i);
            listeners[i].start();
        }
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
    
    private static String obtenerNombreHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            return null;
        }
    }
    
    private static InetAddress[] obtenerDireccionesLocales() throws IOException {
        if (nombreHost == null) {
            DatagramSocket socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 10000);
            InetAddress direccion = socket.getLocalAddress();
            
            return new InetAddress[] {direccion};
        }
        
        /*
        Socket sock = new Socket(InetAddress.getByName("8.8.8.8"), 53);
        InetAddress direccion = sock.getLocalAddress();
        sock.close();
        */
        
        return InetAddress.getAllByName(nombreHost);
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
        InetAddress broadcast4, broadcast6, broadcast;
        try {
            broadcast4 = InetAddress.getByName("255.255.255.255");
            broadcast6 = InetAddress.getByName("ff02::1");
        } catch (UnknownHostException ex) {
            System.err.println("Error al crear dirección de broadcast.");
            return;
        }
        
        byte[] contenido = ("Usuario:" + nombreHost).getBytes();
        
        while (true) {
            DatagramPacket packet;
            try {
                System.out.println("Enviando paquetes");
                for (int i = 0; i < sockets.length; i++) {
                    if (direcciones[i] instanceof Inet6Address) {
                        broadcast = broadcast6;
                    }
                    else {
                        broadcast = broadcast4;
                    }
                    packet = new DatagramPacket(contenido, contenido.length, broadcast, PUERTO);
                    sockets[i].send(packet);
                }
                
            } catch (IOException ex) {
                ex.printStackTrace();
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
                sockets[indice].receive(paquete);
                
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
                if (origen.getHostAddress().equals(direcciones[indice].getHostAddress())) {
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
