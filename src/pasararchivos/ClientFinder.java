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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * @author Facu
 */
public class ClientFinder {
    private static final int PUERTO = 9060;
    private static final String LOCK = "lock";
    private static Envio emisor;
    private static Escucha[] listeners;
    private static ConcurrentHashMap<String, Clientes> pares;
    private static String nombreHost;
    private static DatagramSocket[] sockets;
    private static InetAddress[] direcciones;
    
    public static void init() throws RuntimeException {
        nombreHost = obtenerNombreHost();
        
        try {
            direcciones = obtenerDireccionesLocales();
        
            sockets = new DatagramSocket[direcciones.length];
            //System.out.println("Direcciones: ");
            for (int i = 0; i < direcciones.length; i++) {
                //System.out.println(i + ": " + direcciones[i].getHostAddress());
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
        
        emisor = new Envio();
        emisor.start();
        
        listeners = new Escucha[direcciones.length];
        for (int i = 0; i < direcciones.length; i++) {
            listeners[i] = new Escucha(i);
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
    
    private static InetAddress[] obtenerDireccionExterna() throws IOException {
        DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 10000);
        InetAddress direccion = socket.getLocalAddress();

        return new InetAddress[] {direccion};
    }
    
    private static InetAddress[] obtenerDireccionesLocales() throws IOException {
        if (nombreHost == null || true) {
            return obtenerDireccionExterna();
        }
        
        /*
        Socket sock = new Socket(InetAddress.getByName("8.8.8.8"), 53);
        InetAddress direccion = sock.getLocalAddress();
        sock.close();
        */
        InetAddress[] dirs = InetAddress.getAllByName(nombreHost);
        int cuentaIPv4 = 0;
        for (InetAddress dir: dirs) {
            if (dir instanceof Inet4Address) cuentaIPv4++;
        }
        
        InetAddress[] dirs4 = new InetAddress[cuentaIPv4];
        
        for (int i = 0, j = 0; i < cuentaIPv4; i++) {
            if (dirs[i] instanceof Inet4Address) {
                dirs4[j++] = dirs[i];
            }
        }
        return dirs4;
    }
    
    public static class Envio extends Thread {
        InetAddress broadcast;
        
        Envio() {
            try {
                broadcast = InetAddress.getByName("255.255.255.255");
                //broadcast6 = InetAddress.getByName("ff02::1");
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
                PasarArchivos.log.log(Level.SEVERE, "Error al crear dirección de broadcast.");
            }
        }
        
        @Override
        public void run() {
            byte[] contenido = ("Usuario:" + nombreHost).getBytes();

            while (true) {
                DatagramPacket packet;
                try {
                    System.out.println("Enviando paquetes");
                    for (DatagramSocket socket: sockets) {
                        packet = new DatagramPacket(contenido, contenido.length, broadcast, PUERTO);
                        socket.send(packet);
                    }
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                    PasarArchivos.log.log(Level.SEVERE, "Error al enviar paquetes de descubrimiento.");
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
                        panel.actualizarLista();
                    }
                });

                try {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e) {}
            }
        }
    }
    
    public static class Escucha extends Thread {
        int indice;
        
        Escucha(int indice) {
            this.indice = indice;
        }
        
        @Override
        public void run() {
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
                    System.out.println("Socket " + indice + ": Paquete recibido de " + origen.toString());
                    
                    // Probablemente hayamos recibido una versión de nuestro paquete en formato
                    // IPv4 mapeado a IPv6 (lo cual pasa con direcciones enlace-local). Descartar.
                    if (origen instanceof Inet6Address) {
                        continue;
                    }

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
                    PasarArchivos.log.log(Level.SEVERE, "Error al esperar paquete.");
                }
            }
        }
    }
    
    public static class Clientes {
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
