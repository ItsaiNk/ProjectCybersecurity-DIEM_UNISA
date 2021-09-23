import javax.net.ssl.SSLSocket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HAServer extends Server {

    private List<ByteArrayWrapper> UPCMs;
    private static final String HA_KEYSTORE_FILENAME = "ha_keystore.jks";
    private static final char[] HA_PASS = "INSERT_PASS".toCharArray();

    public HAServer() {
        this.UPCMs = new ArrayList<>();
    }

    /**
     * Metodo che causa il contagio di uno smartphone, a seguito di un tampone, con probabilità del 30%. In caso di positività aggiunge l'UPCM dell'utente alla lista da inviare al server.
     * @param UPCM
     * @return
     */
    public boolean receiveUPCM(ByteArrayWrapper UPCM){
        if(Math.random() <= Simulation.RISK_INFECTION) {
            UPCMs.add(UPCM);
            return true;
        }
        return false;
    }

    /**
     * Metodo utile ad avviare la procedura del tampone gratuito. Richiede la verifica da parte del Server di CT.
     * @param contactRisk
     * @param s
     * @throws Exception
     */
    public void procedureFreeSwab(Map<ByteArrayWrapper, Integer> contactRisk, Smartphone s) throws Exception {
        SSLSocket socket = ClientUtils.connectToWithAuth(HA_KEYSTORE_FILENAME, HA_PASS, CTServer.CT_ADDRESS, CTServer.CHECK_RISK_CONTACT);
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        outputStream.writeObject(contactRisk);
        if (inputStream.readObject().equals(CTServer.ACK)) {
            System.out.println("Smartphone " + s.getDEBUG_ID() + " riceve tampone gratuito");
            s.setneedSwab(true);
        }
        else{
            System.out.println("Smartphone " + s.getDEBUG_ID() + " riceve multa per finto tampone gratuito");
            s.setneedSwab(false);
        }
    }

    /**
     * Metodo utile ad inviare al Server di CT la lista di UPCM da abilitare all'upload degli ID.
     * @throws Exception
     */
    public void sendUPCMsToCT() throws Exception {
        if(UPCMs.size()!=0) {
            SSLSocket socket = ClientUtils.connectToWithAuth(HA_KEYSTORE_FILENAME, HA_PASS, CTServer.CT_ADDRESS, CTServer.UPCMS_PORT);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream.writeObject(UPCMs);
            if (!inputStream.readObject().equals(CTServer.ACK)) {
                sendUPCMsToCT();
            }
            UPCMs.clear();
        }
    }
}
