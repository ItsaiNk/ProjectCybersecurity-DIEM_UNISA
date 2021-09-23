import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

public class ClientUtils {

    public static SSLContext createSSLContext(String keystoreFilename, char[] keystore_pwd) throws Exception {
        KeyManagerFactory keyFact = KeyManagerFactory.getInstance("SunX509");
        KeyStore clientStore = KeyStore.getInstance("JKS");

        clientStore.load(new FileInputStream(keystoreFilename), keystore_pwd);

        keyFact.init(clientStore, keystore_pwd);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyFact.getKeyManagers(), null, null);

        return sslContext;
    }

    /**
     * Metodo utile a creare una socket TLS senza identificazione del client.
     * @param address
     * @param port
     * @return
     * @throws IOException
     */
    public static SSLSocket connectToWithoutAuth(String address, int port) throws IOException {
        SSLSocketFactory sockFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) sockFactory.createSocket(address,port);
        return socket;
    }

    /**
     * Metodo utile a creare una socket TLS con identificazione del client.
     * @param keystore_filename
     * @param keystore_pwd
     * @param address
     * @param port
     * @return
     * @throws Exception
     */
    public static SSLSocket connectToWithAuth(String keystore_filename, char[] keystore_pwd, String address, int port) throws Exception {
        SSLContext sslContext = createSSLContext(keystore_filename,keystore_pwd);
        SSLSocketFactory sockFactory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) sockFactory.createSocket(address,port);
        return socket;
    }

}
