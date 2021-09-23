import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.Serializable;

public class Server implements Serializable {

    public interface ServiceHandler {
        public void handle(SSLSocket socket) throws IOException, ClassNotFoundException;
    }

    /**
     * Metodo che permette di avviare un servizio su una porta specifica. E' possibile indicare la necessità di autenticazione tramite il parametro auth.
     * @param handler
     * @param servicePort
     * @param auth
     * @throws IOException
     */
    public void startService(ServiceHandler handler, int servicePort, boolean auth) throws IOException {
        SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sSock = (SSLServerSocket) socketFactory.createServerSocket(servicePort);
        sSock.setNeedClientAuth(auth);
        new Thread(() -> {
            while (true) {
                try {
                    SSLSocket sslSock = (SSLSocket) sSock.accept();
                    new Thread(() -> {
                        try {
                            handler.handle(sslSock);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } catch (IOException ex) {
                    System.out.println("AUTH " + auth);
                    ex.printStackTrace();
                }
            }
        }).start();
    }

}
