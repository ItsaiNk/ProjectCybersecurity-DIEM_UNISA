import javax.net.ssl.SSLSocket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class Smartphone {

    private static final int DAILY_IDS = 720; // numero di ID che possono essere generati in un giorno
    private SecureRandom PRG_D;
    private SecureRandom PRG_Q;
    private ByteArrayWrapper ID; // ID del dispositivo
    private int DEBUG_ID; // ID univoco dello smartphone assegnato alla generazione dello stesso
    private byte[] D;
    private byte[] Q;
    private Position pos; // posizione dello smartphone in 2D
    private boolean infected;
    private int lastInfectedCheck; // data dell'ultimo tampone positivo
    private List<Smartphone> otherSmartphones; // lista di tutti gli smartphone della simulazione
    private Map<ByteArrayWrapper, ContactTime> contactMap; // mappa contenente ID e tempo di contatto degli smartphone con cui si è entrati in contatto ravvicinato
    private Map<ByteArrayWrapper, Integer> contactWithRisk; // mappa contenente gli ID dei contatti a rischio e i tempi di contatto con gli stessi
    private List<ByteArrayWrapper> generatedID; // lista di ID generati
    private boolean needSwab; // TRUE se deve eseguire la procedura di tampone , FALSE altrimenti
    private ByteArrayWrapper UPCM; // CODICE UPCM
    private int lastSynchronizationTime; // ultima sincronizzazione con il server
    private int contactTimeRisk; // Tempo di contatto complessivo con utenti positivi

    public Smartphone(Position pos, int time, int DEBUG_ID){
        this.pos = pos;
        otherSmartphones = new ArrayList<>();
        init_PRG();
        D = generateByte(PRG_D, 4);
        Q = generateByte(PRG_Q, 4);
        ID = generateID();
        contactMap = new HashMap<>();
        contactWithRisk = new HashMap<>();
        generatedID = new ArrayList<>();
        needSwab = false;
        UPCM = null;
        lastSynchronizationTime = time;
        contactTimeRisk = 0;
        this.DEBUG_ID = DEBUG_ID;
        infected = false;
        lastInfectedCheck = 0;
    }

    private byte[] generateByte(SecureRandom PRG, int dim) {
        byte[] b = new byte[dim];
        PRG.nextBytes(b);
        return b;
    }

    private void init_PRG() {
        PRG_Q = new SecureRandom();
        PRG_D = new SecureRandom();
    }

    public void simulateRoutine(int t) throws Exception {
        if(t%(Simulation.DAY*2) == 0){
            init_PRG(); // ogni 2 giorni reinizializza casualmente i seed del PRG
        }
        if(!infected) {
            if (t % Simulation.DAY == 0) { // ogni giorno vengono effettuate le seguenti operazioni:
                D = generateByte(PRG_D, 4); // generazione della stringa D
                removeOlderIDs(); // rimuove gli ID generati antecedenti agli ultimi 15 giorni.
                removeOlderContacts(t); // ogni giorno vengono svecchiate le informazioni relative agli smartphone con cui si è entrati in contatto precedentemente agli ultimi 20 giorni.
                synchronizePositives(lastSynchronizationTime); // sincronizzazione con il server per il download degli ID positivi
                lastSynchronizationTime = t;
                if (needSwab) {
                    swabProcedure(); // avvia la procedura per effettuare il tampone
                    if(infected){
                        System.out.println("Smartphone " + DEBUG_ID + " infetto dopo tampone");
                        lastInfectedCheck = t;
                    }
                    else{
                        System.out.println("Smartphone " + DEBUG_ID + " non infetto dopo tampone");
                    }
                }
            }
            if (t % Simulation.ID_GENERATION_PERIOD == 0) {
                Q = generateByte(PRG_Q, 4); // generazione della stringa Q
                ID = generateID(); // genera il nuovo ID e lo aggiunge alla lista degli ID generati
                generatedID.add(ID);
                if (ID == null) {
                    throw new RuntimeException("Failed to generate ID");
                }
            }
            for (Smartphone s : otherSmartphones) { // ogni smarphone invia in broadcast il proprio ID , tranne a se stesso. L'ID viene inviato sse la distanza tra i due smarphone è minore di "BLE_MAX_DISTANCE"
                if (!s.equals(this)) {
                    if (pos.computeEuclideanDistance(s.getPos()) <= Simulation.BLE_MAX_DISTANCE) {
                        s.receiveIDFromBLE(ID, pos.computeDistanceBLEWithRandomError(s.getPos()), t);
                    }
                }
            }
            pos.randomMove(); // simula lo spostamente dello smartphone
        }
        else{
            if(UPCM != null) {
                sendIDsToCT(); // se il cittadino risulta infetto vengono inviati gli ID al server di CT
            }
            else {
                if ((t - lastInfectedCheck) % (Simulation.DAY * 7) == 0) {
                    swabProcedure(); // avvia la procedura del tampone dopo 7 giorni dal contagio
                    if (infected) {
                        System.out.println("Smartphone " + DEBUG_ID + " ancora infetto dopo tampone di controllo");
                        lastInfectedCheck = t;
                    }
                    else{
                        System.out.println("Smartphone " + DEBUG_ID + " è guarito");
                    }
                }
            }
            if (t % Simulation.DAY == 0){
                removeOlderIDs();
                removeOlderContacts(t);
            }
        }
    }


    private void removeOlderIDs(){
        if(generatedID.size()>DAILY_IDS*15) {
            generatedID.subList(0, DAILY_IDS).clear();
        }
    }

    private void removeOlderContacts(int t){
        List<ByteArrayWrapper> idToRemove = new ArrayList<>();
        for(ByteArrayWrapper id : contactMap.keySet()){
            ContactTime contactTime = contactMap.get(id);
            if(t-contactTime.getEndContactTime() >= Simulation.DAY*20){
                if(contactWithRisk.containsKey(id)){
                    contactWithRisk.remove(id);
                }
                idToRemove.add(id);
            }
        }
        for (ByteArrayWrapper id : idToRemove){
            contactMap.remove(id);
        }
    }

    private void swabProcedure(){
        this.UPCM = generateUPCM();
        this.infected = Simulation.HA.receiveUPCM(UPCM);
        setneedSwab(false);
    }

    private ByteArrayWrapper generateUPCM(){
        return new ByteArrayWrapper(generateByte(new SecureRandom(), 16));
    }

    private void sendIDsToCT() throws IOException, ClassNotFoundException {
        SSLSocket socket = ClientUtils.connectToWithoutAuth(CTServer.CT_ADDRESS, CTServer.POSITIVE_ID_PORT);
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.writeObject(UPCM);
        if(inputStream.readObject().equals(CTServer.ACK)) {
            outputStream.writeObject(generatedID);
            generatedID.clear();
            UPCM = null;
        }
    }

    private void synchronizePositives(int lastSynchronizationTime) throws IOException {
        SSLSocket socket = ClientUtils.connectToWithoutAuth(CTServer.CT_ADDRESS, CTServer.SYNCHRONIZATION_IDS_PORT);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(lastSynchronizationTime);
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        try {
            List<ByteArrayWrapper> positiveIDsFromServer = (List<ByteArrayWrapper>) in.readObject();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for(ByteArrayWrapper id : contactMap.keySet()){
                ByteArrayWrapper idSHA = new ByteArrayWrapper(digest.digest(id.getData()));
                if(positiveIDsFromServer.contains(idSHA)){
                    ContactTime idContactTime = contactMap.get(id);
                    contactTimeRisk += (idContactTime.getContactTime());
                    contactWithRisk.put(id, contactMap.get(id).getContactTime());
                }
            }
            for(ByteArrayWrapper idToRemove : contactWithRisk.keySet()){
                contactMap.remove(idToRemove);
            }
            if(contactTimeRisk >= Simulation.RISK_CONTACT_TIME ){
                System.out.println("Smartphone " + DEBUG_ID + " richiede tampone gratuito con un tc=" + contactTimeRisk);
                this.checkTamponeGratuito(); // avvia la procedura per verificare se l'utente ha diritto a un tampone gratuito
            }
            else{
                if(Math.random() <= 0.01) {
                    System.out.println("Smartphone " + DEBUG_ID + " prova a imbrogliare CT per tampone gratuito, con un tc=" + contactTimeRisk);
                    this.checkTamponeGratuito();
                }
            }
        } catch (ClassNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void checkTamponeGratuito() {
        try {
            Simulation.HA.procedureFreeSwab(this.contactWithRisk, this);
            if(this.needSwab) {
                contactWithRisk.clear();
                contactTimeRisk = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ByteArrayWrapper generateID() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(D);
            outputStream.write(Q);
        } catch (IOException e) {
            e.printStackTrace();
        }


        byte D_Q[] = outputStream.toByteArray();

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(D_Q);
            return new ByteArrayWrapper(Arrays.copyOfRange(hash, 0, 15));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void setOtherSmartphones(List<Smartphone> otherSmartphones) {
        this.otherSmartphones = otherSmartphones;
    }

    public void receiveIDFromBLE(ByteArrayWrapper ID, double BLEDistanceWithError, int t){
        if(BLEDistanceWithError <= Simulation.BLE_THRESHOLD){ // aggiunge l'ID alla lista contatti se la distanza con errore è minore o uguale alla threshold
            if(!contactMap.containsKey(ID)){
                contactMap.put(ID, new ContactTime(t));
            }
            else {
                ContactTime newContactTime = new ContactTime(contactMap.get(ID).getStartContactTime(), contactMap.get(ID).getEndContactTime()+1);
                contactMap.replace(ID, newContactTime);
            }
        }
    }

    public Position getPos(){
        return pos;
    }

    public int getDEBUG_ID() {
        return DEBUG_ID;
    }

    public void setneedSwab(boolean needSwab) {
        this.needSwab = needSwab;
    }
}
