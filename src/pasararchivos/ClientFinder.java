package pasararchivos;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * @author Facu
 */
public class ClientFinder {
    private static final int PUERTO = 9060;
    private static final String LOCK = "lock";
    private static Envio emisor;
    private static EscuchaGlobal escuchaGlobal;
    private static ConcurrentHashMap<String, Clientes> pares;
    private static String nombreHost;
    private static DatagramSocket socketGlobal;
    private static DireccionInterfaz[] direcciones;
    //private static Escucha[] listeners;
    //private static DatagramSocket[] sockets;
    
    public static void init() throws RuntimeException {
        nombreHost = obtenerNombreHost();
        
        try {
            direcciones = obtenerDireccionesLocales();
        
            socketGlobal = new DatagramSocket(PUERTO);
            //System.out.println("\nDirección global: " + socketGlobal.getLocalAddress());
            //System.out.println("Dirección global getHostAddres(): " + socketGlobal.getLocalAddress().getHostAddress());
            /*
            //sockets = new DatagramSocket[direcciones.length];
            System.out.println("Direcciones: ");
            for (int i = 0; i < direcciones.length; i++) {
                System.out.println(i + ": " + direcciones[i].toString());
                //sockets[i] = new DatagramSocket(PUERTO, direcciones[i]);
            }
            */
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
        
        escuchaGlobal = new EscuchaGlobal();
        escuchaGlobal.start();
        
        /*
        listeners = new Escucha[direcciones.length];
        for (int i = 0; i < direcciones.length; i++) {
            listeners[i] = new Escucha(i);
            listeners[i].start();
        }*/
    }
    
    public static HashMap<InetAddress, String> getDispositivos() {
        HashMap<InetAddress, String> dispositivos = new HashMap();
        
        synchronized (LOCK) {    
            for (Clientes c: pares.values()) {
                dispositivos.put(c.direccion, c.nombre);
            }
        }
        
        return dispositivos;
    }
    
    private static String obtenerNombreHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            return null;
        }
    }
    
    private static InetAddress obtenerDireccionExterna() throws IOException {
        DatagramSocket socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 10000);
        InetAddress direccion = socket.getLocalAddress();

        return direccion;
    }
    
    private static DireccionInterfaz[] obtenerDireccionesLocales() throws IOException {
        /*
        if (nombreHost == null && false) {
            InetAddress direccion = obtenerDireccionExterna();
            NetworkInterface interfaz = NetworkInterface.getByInetAddress(direccion);
            
            for (InterfaceAddress dirInterfaz: interfaz.getInterfaceAddresses()) {
                if (dirInterfaz.getAddress().equals(direccion)) {
                    return new InterfaceAddress[] {dirInterfaz};
                }
            }
        }
        */
        
        boolean mostrarInterfaces = false;
        if (mostrarInterfaces) {
            System.out.println("Interfaces:");
            Iterator<NetworkInterface> iter = NetworkInterface.getNetworkInterfaces().asIterator();
            while (iter.hasNext()) {
                NetworkInterface interfaces = iter.next();
                System.out.println(interfaces.getName());
                var addrs = interfaces.getInterfaceAddresses();
                if (!addrs.isEmpty()) {
                    for (InterfaceAddress direc: addrs) {
                        System.out.println(direc.toString());
                    }
                    System.out.println("");
                }
            }

            InetAddress[] dirs = InetAddress.getAllByName(nombreHost);

            System.out.println("\nDirecciones:");
            for (InetAddress direc: dirs) {
                System.out.println(direc.toString());
            }
        }
        
        //long t1 = System.nanoTime();
        
        ArrayList<DireccionInterfaz> dirs4 = new ArrayList<>();
        Iterator<NetworkInterface> iter = NetworkInterface.getNetworkInterfaces().asIterator();
        
        //long t2 = System.nanoTime();
        while (iter.hasNext()) {
            NetworkInterface interfaz = iter.next();
                    
            var addrs = interfaz.getInterfaceAddresses();
            for (InterfaceAddress dir: addrs) {
                if (!(dir.getAddress() instanceof Inet4Address)) continue;
                if (dir.getAddress().isLoopbackAddress()) continue;
                
                dirs4.add(new DireccionInterfaz(dir, interfaz.getIndex()));
            }
        }
        
        //System.out.println(System.nanoTime() - t1);
        //System.out.println(System.nanoTime() - t2);
        
        /*
        for (InetAddress dir: dirs) {
            if (!(dir instanceof Inet4Address)) continue;
            
            NetworkInterface interfaz = NetworkInterface.getByInetAddress(dir);
            for (InterfaceAddress dirInterfaz: interfaz.getInterfaceAddresses()) {
                if (dirInterfaz.getAddress().equals(dir)) {
                    //dirs4.add(dirInterfaz);
                }
            }
        }
        */
        
        return dirs4.toArray(DireccionInterfaz[]::new);
    }
    
    public static class Envio extends Thread {
        long refrescarDirecciones;
        //InetAddress broadcast;
        
        Envio() {
            setName("Hilo-Envio");
            /*
            try {
                //broadcast = InetAddress.getByName("255.255.255.255");
                //broadcast6 = InetAddress.getByName("ff02::1");
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
                PasarArchivos.log.log(Level.SEVERE, "Error al crear dirección de broadcast.");
            }
            */
        }
        
        @Override
        public void run() {
            byte[] contenido = ("Usuario:" + nombreHost).getBytes();
            DatagramPacket packet;
            
            refrescarDirecciones = System.currentTimeMillis();

            while (true) {
                long t1 = System.currentTimeMillis();
                
                renovarDirecciones();
                
                try {
                    System.out.println("Enviando paquetes");
                    
                    for (DireccionInterfaz direccion: direcciones) {
                        InetAddress destino = obtenerDestino(direccion);
                        
                        System.out.println("- Enviando a " + destino + " (" + direccion.getAddress() + ")");
                        packet = new DatagramPacket(contenido, contenido.length, destino, PUERTO);
                        socketGlobal.send(packet);
                    }
                    System.out.println();
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                    PasarArchivos.log.log(Level.SEVERE, "Error al enviar paquetes de descubrimiento.");
                }

                removerClientesAntiguos();
                actualizarInterfaz();

                try {
                    long t2 = System.currentTimeMillis();
                    System.out.println("sleep: " + Math.max(0, 3000 - (t2 - t1)));
                    Thread.sleep(Math.max(0, 3000 - (t2 - t1)));
                }
                catch (InterruptedException e) {}
            }
        }
        
        private void renovarDirecciones() {
            try {
                if (System.currentTimeMillis() - refrescarDirecciones >= 12000) {
                    refrescarDirecciones = System.currentTimeMillis();
                    direcciones = obtenerDireccionesLocales();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                PasarArchivos.log.log(Level.SEVERE, "Error al renovar direcciones de interfaz.");
            }
        }
        
        // Si es una IPv4 normal, enviarlo a su destino broadcast.
        // Si es de enlace-local, mapearlo a IPv6 para incluir el ámbito destino.
        private InetAddress obtenerDestino(DireccionInterfaz direccion) throws UnknownHostException {
            byte[] broadcastMapeado = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -87, -2, -1, -1};
            
            if (direccion.getAddress().isLinkLocalAddress() && direccion.ambito != -1) {
                return Inet6Address.getByAddress(null, broadcastMapeado, direccion.ambito);
            }
            else return direccion.getBroadcast();
        }
        
        // Remover pares viejos no renovados
        private void removerClientesAntiguos() {
            long limite = 15000;
            synchronized (LOCK) {
                long ahora = new Date().getTime();
                
                for (String clave: pares.keySet()) {
                    long antes = pares.get(clave).fechaEmision;

                    if (ahora - antes > limite) {
                        pares.remove(clave);
                    }
                }
            }
        }
        
        private void actualizarInterfaz() {
            EventQueue.invokeLater(() -> {
                Panel panel = PasarArchivos.panel;
                if (panel != null && panel.isVisible()) {
                    panel.actualizarLista();
                    panel.hayInternet(direcciones.length > 0);
                }
            });
        }
    }
    
    /*
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
                    String nombreSocket = "Socket " + indice + " (" + sockets[indice].getLocalAddress().toString() + ")";
                    System.out.println(nombreSocket + ": Paquete recibido de " + origen.toString() + " - \"" + new String(datos).trim() + "\"");
                    
                    // Probablemente hayamos recibido una versión de nuestro paquete en formato
                    // IPv4 mapeado a IPv6 (lo cual pasa con direcciones enlace-local). Descartar.
                    if (origen instanceof Inet6Address) {
                        continue;
                    }

                    // Recibimos nuestro propio paquete. Descartar.
                    if (origen.getHostAddress().equals(direcciones[indice].getAddress().getHostAddress())) {
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
*/
    
    public static class EscuchaGlobal extends Thread {
        public EscuchaGlobal() {
            setName("Hilo-EscuchaGlobal");
        }
        
        @Override
        public void run() {
            while (true) {
                byte[] datos = new byte[72];
                DatagramPacket paquete = new DatagramPacket(datos, datos.length);
                try {
                    socketGlobal.receive(paquete);

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
                    System.out.println("Socket global: Paquete recibido de " + origen.toString() + " - \"" + new String(datos).trim() + "\"");
                    
                    // Probablemente hayamos recibido una versión de nuestro paquete en formato
                    // IPv4 mapeado a IPv6 (lo cual pasa con direcciones enlace-local).
                    // Previamente se descartaba. Ahora se extrae la versión IPv4 de la dirección.
                    if (origen instanceof Inet6Address) {
                        if (!Arrays.equals(origen.getAddress(), 0, 12, new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1}, 0, 12)) {
                            continue;
                        }
                        
                        byte[] origenV4 = new byte[4];
                        System.arraycopy(origen.getAddress(), 12, origenV4, 0, 4);
                        
                        origen = InetAddress.getByAddress(origenV4);
                    }

                    // Recibimos nuestro propio paquete. Descartar.
                    boolean duplicado = false;
                    for (DireccionInterfaz direccion: direcciones) {
                        if (origen.equals(direccion.getAddress())) {
                            duplicado = true;
                        }
                    }
                    
                    if (duplicado) continue;
                    

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
    
    public static class DireccionInterfaz {
        public InterfaceAddress direccion;
        public int ambito;
        
        public DireccionInterfaz(InterfaceAddress direccion, int ambito) {
            this.direccion = direccion;
            this.ambito = ambito;
        }
        
        public InetAddress getAddress() {
            return direccion.getAddress();
        }
        
        public InetAddress getBroadcast() {
            return direccion.getBroadcast();
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
