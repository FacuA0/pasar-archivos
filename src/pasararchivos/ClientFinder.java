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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
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
    
    public static void init() throws RuntimeException {
        nombreHost = obtenerNombreHost();
        
        try {
            direcciones = obtenerDireccionesLocales();
            socketGlobal = new DatagramSocket(PUERTO);
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
        ArrayList<DireccionInterfaz> dirs4 = new ArrayList<>();
        Iterator<NetworkInterface> iter = NetworkInterface.getNetworkInterfaces().asIterator();
        
        while (iter.hasNext()) {
            NetworkInterface interfaz = iter.next();
                    
            var addrs = interfaz.getInterfaceAddresses();
            for (InterfaceAddress dir: addrs) {
                if (!(dir.getAddress() instanceof Inet4Address)) continue;
                if (dir.getAddress().isLoopbackAddress()) continue;
                
                dirs4.add(new DireccionInterfaz(dir, interfaz.getIndex()));
            }
        }
        
        return dirs4.toArray(DireccionInterfaz[]::new);
    }
    
    public static class Envio extends Thread {
        long refrescarDirecciones;
        
        // ::FFFF:167.254.255.255
        byte[] broadcastMapeado = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -87, -2, -1, -1};
        
        Envio() {
            setName("Hilo-Emision");
        }
        
        @Override
        public void run() {
            String nombreHostEscapado = nombreHost.replace("\\", "\\\\").replace(" | ", " \\| ");
            
            byte[] contenidoAntiguo = ("Usuario:" + nombreHost).getBytes(StandardCharsets.UTF_8);
            byte[] contenido = ("PasarArchivos | version=1.3 | nombre=" + nombreHostEscapado).getBytes(StandardCharsets.UTF_8);
            
            DatagramPacket packet = new DatagramPacket(contenido, contenido.length);
            packet.setPort(PUERTO);
            
            refrescarDirecciones = System.currentTimeMillis();

            while (true) {
                long t1 = System.currentTimeMillis();
                
                renovarDirecciones();
                
                try {
                    System.out.println("Enviando paquetes");
                    
                    for (DireccionInterfaz direccion: direcciones) {
                        InetAddress destino = obtenerDestino(direccion);
                        packet.setAddress(destino);
                        
                        //System.out.println("- Enviando a " + destino + " (" + direccion.getAddress() + ")");
                        packet.setData(contenidoAntiguo);
                        socketGlobal.send(packet);
                        
                        packet.setData(contenido);
                        socketGlobal.send(packet);
                    }
                    //System.out.println();
                }
                catch (IOException e) {
                    PasarArchivos.logError(e, "Error de paquetes", "Error al enviar paquetes de descubrimiento.");
                }

                removerClientesAntiguos();
                actualizarInterfaz();

                try {
                    long t2 = System.currentTimeMillis();
                    System.out.println("Sleep de emisión: " + Math.max(0, 3000 - (t2 - t1)));
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
                PasarArchivos.logError(e, "Error de direcciones", "Error al renovar direcciones de interfaz.");
            }
        }
        
        // Si es una IPv4 normal, enviarlo a su destino broadcast.
        // Si es de enlace-local, mapearlo a IPv6 para incluir el ámbito destino.
        private InetAddress obtenerDestino(DireccionInterfaz direccion) throws UnknownHostException {
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
    
    public static class EscuchaGlobal extends Thread {
        public EscuchaGlobal() {
            setName("Hilo-EscuchaGlobal");
        }
        
        @Override
        public void run() {
            byte[] datos = new byte[256];
            
            while (true) {
                DatagramPacket paquete = new DatagramPacket(datos, datos.length);
                try {
                    socketGlobal.receive(paquete);

                    // El paquete es muy pequeño. Descartar.
                    if (paquete.getLength() < 8) {
                        continue;
                    }

                    InetAddress origen = paquete.getAddress();
                    assert origen != null;
                    //System.out.println("Socket global: Paquete recibido de " + origen.toString() + " - \"" + new String(datos).trim() + "\"");
                    
                    // Probablemente hayamos recibido una versión de nuestro paquete en formato
                    // IPv4 mapeado a IPv6 (lo cual pasa con direcciones enlace-local).
                    // Previamente se descartaba. Ahora se extrae la versión IPv4 de la dirección.
                    if (origen instanceof Inet6Address) {
                        byte[] prefijo = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1};
                        if (!Arrays.equals(origen.getAddress(), 0, 12, prefijo, 0, 12)) {
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
                    
                    String nombre = "Desconocido";
                    String version = "1.2";
                    String firmaS = new String(datos, 0, 13);
                    
                    if (paquete.getLength() >= 13) {
                        if (firmaS.equals("PasarArchivos")) {
                            String contenido = new String(datos, 0, paquete.getLength(), StandardCharsets.UTF_8);
                            String[] partes = contenido.split(" \\| ");
                            
                            for (int i = 1; i < partes.length; i++) {
                                int igual = partes[i].indexOf("=");
                                if (igual == -1) continue;
                                
                                String clave = partes[i].substring(0, igual);
                                String valor = partes[i].substring(igual + 1);
                                
                                valor = valor.replace(" \\| ", " | ");
                                valor = valor.replace("\\\\", "\\");
                                
                                if (clave.equals("nombre")) {
                                    nombre = valor;
                                }
                                else if (clave.equals("version")) {
                                    version = valor;
                                }
                            }
                        }
                        else if (firmaS.startsWith("Usuario:")) {
                            nombre = new String(datos, 8, paquete.getLength() - 8, StandardCharsets.UTF_8);
                        }
                        else continue;
                    }
                    else if (firmaS.startsWith("Usuario:")) {
                        nombre = new String(datos, 8, paquete.getLength() - 8, StandardCharsets.UTF_8);
                    }
                    else continue;
                    
                    /*
                    // El paquete vino de otro programa y se equivocó de puerto. Descartar.
                    if (!new String(firma).equals("Usuario:")) {
                        continue;
                    }
                    */

                    long ahora = new Date().getTime();

                    synchronized (LOCK) {
                        Clientes nuevo = new Clientes(nombre, version, origen, ahora);
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
        public String version;
        public InetAddress direccion;
        public long fechaEmision;
        
        public Clientes(String nombre, String version, InetAddress direccion, long fechaEmision) {
            this.nombre = nombre;
            this.version = version;
            this.direccion = direccion;
            this.fechaEmision = fechaEmision;
        }
    }
}
