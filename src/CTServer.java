import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CTServer extends Server{

    public static final String CT_ADDRESS = "localhost";
    public static final int POSITIVE_ID_PORT = 4500;
    public static final int UPCMS_PORT = 4501;
    public static final int SYNCHRONIZATION_IDS_PORT = 4504;
    public static final int CHECK_RISK_CONTACT = 4503;
    public static final int DEBUG_RECEIVE_TIME_SIMULATION = 5000;
    private Map<ByteArrayWrapper, Integer> PositiveIDs;
    private Map<ByteArrayWrapper, Integer> UPCMs;
    private int t;
    public static final int ACK = 1;
    public static final int NACK = 0;

    public CTServer() {
        super();
        PositiveIDs = new HashMap<>();
        UPCMs = new HashMap<>();
        t = 0;
    }

    /**
     * Protocollo utile per ricevere la lista degli ID dallo smartphone
     * @param CTSocket
     */
    public void receiveIDFromSmartphone(SSLSocket CTSocket) {
        try {
            ObjectOutputStream CTOutput = new ObjectOutputStream(CTSocket.getOutputStream());
            ObjectInputStream CTInput = new ObjectInputStream(CTSocket.getInputStream());
            ByteArrayWrapper userUPCM = (ByteArrayWrapper) CTInput.readObject();
            if(this.UPCMs.containsKey(userUPCM)){
                CTOutput.writeObject(ACK);
                List<ByteArrayWrapper> IDs = (List<ByteArrayWrapper>) CTInput.readObject();
                for(ByteArrayWrapper id : IDs){
                    this.PositiveIDs.put(id,t);
                }
                this.UPCMs.remove(userUPCM);
                CTSocket.close();
            }
            else{
                CTOutput.writeObject(NACK);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Protocollo utile per ricevere la lista degli UPCM da HA
     * @param CTSocket
     */
    public void receiveUPCMsFromHA(SSLSocket CTSocket) {
        try {
            ObjectInputStream CTInput = new ObjectInputStream(CTSocket.getInputStream());
            ObjectOutputStream CTOut = new ObjectOutputStream(CTSocket.getOutputStream());
            List<ByteArrayWrapper> UPCMs = (List<ByteArrayWrapper>) CTInput.readObject();
            for(ByteArrayWrapper upcm : UPCMs){
                this.UPCMs.put(upcm, this.t);
            }
            CTOut.writeObject(ACK);
            CTSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Protocollo utile per verificare l'erogazione di un tampone gratuito da parte di HA
     * @param CTSocket
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void checkRiskContact(SSLSocket CTSocket) throws IOException, ClassNotFoundException {
        ObjectInputStream CTInput = new ObjectInputStream(CTSocket.getInputStream());
        ObjectOutputStream CTOut = new ObjectOutputStream(CTSocket.getOutputStream());
        Map<ByteArrayWrapper, Integer> contactRisk = (Map<ByteArrayWrapper, Integer>) CTInput.readObject();
        int totalContactTime = 0;
        for(ByteArrayWrapper id : contactRisk.keySet()){
            if(PositiveIDs.containsKey(id)){
                totalContactTime += contactRisk.get(id);
            }
        }
        if(totalContactTime >= Simulation.RISK_CONTACT_TIME){
            CTOut.writeObject(ACK);
        }
        else{
            CTOut.writeObject(NACK);
        }
    }

    /**
     * Protocollo utile a inviare la lista degli ID positivi durante la sincronizzazione giornaliera ad uno smartphone
     * @param CTSocket
     * @throws IOException
     */
    public void PositiveIDsSynchSmartphone(SSLSocket CTSocket) throws IOException {
        ObjectInputStream in = new ObjectInputStream(CTSocket.getInputStream());
        int timestamp = 0;
        try {
            timestamp = (int) in.readObject();
            List<ByteArrayWrapper> IDsToSend = new ArrayList<>();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (ByteArrayWrapper id : PositiveIDs.keySet()) {
                if (PositiveIDs.get(id) >= timestamp) {
                    IDsToSend.add(new ByteArrayWrapper(digest.digest(id.getData())));
                }
            }
            ObjectOutputStream out = new ObjectOutputStream(CTSocket.getOutputStream());
            out.writeObject(IDsToSend);
            CTSocket.close();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Procollo utile alla simulazione per ricevere il tempo di simulazione attuale
     * @param CTSocket
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void DEBUG_receiveSimulationTime(SSLSocket CTSocket) throws IOException, ClassNotFoundException {
        ObjectInputStream CTInput = new ObjectInputStream(CTSocket.getInputStream());
        t = (int) CTInput.readObject();
        if(t%Simulation.DAY == 0){
            removeOlderPositiveIDs();
        }
        if(t%(Simulation.DAY/2) == 0){
            removeOlderUPCMs();
        }
    }

    /**
     * Metodo utile per rimuovere dalla lista degli ID positivi quelli successivi i 22 giorni
     */
    private void removeOlderPositiveIDs(){
        for(ByteArrayWrapper id : PositiveIDs.keySet()){
            if(t-PositiveIDs.get(id) >= Simulation.DAY*22){
                this.PositiveIDs.remove(id);
            }
        }
    }

    /**
     * Metodo utile per rimuovere dalla lista degli UPCM abilitati quelli successivi le 36 ore
     */
    private void removeOlderUPCMs(){
        for(ByteArrayWrapper upcm : UPCMs.keySet()){
            if(t-UPCMs.get(upcm) >= Simulation.DAY*1.5){
                this.UPCMs.remove(upcm);
            }
        }
    }

    /**
     * Inizializza i thread per erogare i servizi necessari alla simulazione
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        System.out.println("SERVER STARTING");
        CTServer server = new CTServer();
        server.startService(server::receiveIDFromSmartphone, POSITIVE_ID_PORT, false);
        System.out.println("receiveIDFromSmartphone STARTED");
        server.startService(server::receiveUPCMsFromHA, UPCMS_PORT, true);
        System.out.println("receiveUPCMsFromHA STARTED");
        server.startService(server::DEBUG_receiveSimulationTime, DEBUG_RECEIVE_TIME_SIMULATION, false);
        System.out.println("DEBUG_receiveSimulationTime STARTED");
        server.startService(server::PositiveIDsSynchSmartphone, SYNCHRONIZATION_IDS_PORT, false);
        System.out.println("sendPositiveIDsToSmartphone STARTED");
        server.startService(server::checkRiskContact, CHECK_RISK_CONTACT, true);
        System.out.println("checkRiskContact STARTED");
        System.out.println("SERVER STARTED");
    }
}
