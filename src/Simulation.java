import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Il run del Simulation.main permette di avviare la simulazione del sistema di CT.
 **/
public class Simulation {


    public static final int RISK_CONTACT_TIME = 15; // TEMPO DI RISCHIO CONTAGIO
    public static final int BLE_MAX_DISTANCE = 10; // DISTANZA MASSIMA DEL SEGNALE BLUETOOTH
    public static final double BLE_THRESHOLD = 2.5; // SOGLIA DEL BLUETOOTH ENTRO LA QUALE DUE SMARTPHONE VENGONO CONSIDERATI A CONTATTO RAVVICINATO
    public static final int ID_GENERATION_PERIOD = 2; // PERIODO DI GENERAZIONE DEGLI ID
    public static final int DAY = 1440; // 1 GIORNO , ESPRESSO IN MINUTI
    public static final double RISK_INFECTION = 0.3; // PERCENTUALE DI RISCHIO CONTAGIO , 0.3 = 30%
    private static final int NUM_SMARTPHONE = 40; // NUMERO DI SMARTPHONE / UTENTI ATTIVI DURANTE LA SIMULAZIONE
    private static final int RANDOM_TAMPONI_GIORNALIERI = 2; // NUMERO DI TAMPONI GIORNALIERI A SMARTPHONE RANDOMICI
    public static final HAServer HA = new HAServer();
    private static final int POSITION_INITIAL_AREA = 50; // DIMENSIONE IN METRI DELL'AREA IN CUI VENGONO GENERATI GLI SMARTPHONE

    private static final int SIM_TIME = DAY * 30; // TEMPO DI SIMULAZIONE

	/**
	 Al fine di simulare tutte le funzionalità del sistema si assume che un minuto corrisponda
	 * a una singola iterazione. Il ciclo viene ripetuto "SIM_TIME" volte.
	 */
    public static void main(String[] args) throws Exception {
        int t = 0;
        List<Smartphone> smartphones = new ArrayList<>();
        for (int i = 0; i < NUM_SMARTPHONE; i++) {
            smartphones.add(new Smartphone(new Position(POSITION_INITIAL_AREA), t, i)); // genero gli smartphone con posizione casuale
        }
        for (Smartphone s : smartphones) {
            s.setOtherSmartphones(smartphones); // inizialmente si assume che ogni smartphone "veda" tramite bluetooth tutti gli altri smartphone presenti.
        }
        System.out.println("************************************************************************");
        System.out.println("Inizio simulazione");
        System.out.println("************************************************************************");
        System.out.println("Giorno 1 su " + SIM_TIME / DAY);
		 for (t = 0; t < SIM_TIME; t++) { //inizio simulazione
            DEBUG_sendTimeToCT(t); // viene inviato il tempo attuale di simulazione alla classe CTServer tramite una socket
            for (Smartphone s : smartphones) {
                s.simulateRoutine(t); // operazioni di routine che effettua lo smartphone come: generazione e scambio di ID, svecchiamento di informazioni non più significative, sincronizzazione col server, ecc.
            }
            if (t != 0 && t % (DAY) == 0) {
                System.out.println("************************************************************************");
                System.out.println("Giorno " + ((t / DAY) + 1) + " su " + SIM_TIME / DAY);
                for (int i = 0; i < RANDOM_TAMPONI_GIORNALIERI; i++) {
                    new Utils<Smartphone>().getRandomItem(smartphones).setneedSwab(true); // sceglie casualmente degli smartphone e avvia la procedura del tampone per quell'utente
                }
            }
            if (t % (DAY / 2) == 0) {
                HA.sendUPCMsToCT(); // invia l'UPCM al server di CT
            }
        }
        System.out.println("************************************************************************");
        System.out.println("Fine simulazione");
        System.out.println("************************************************************************");
    }

    private static void DEBUG_sendTimeToCT(int t) throws IOException {
        SSLSocket socket = ClientUtils.connectToWithoutAuth(CTServer.CT_ADDRESS, CTServer.DEBUG_RECEIVE_TIME_SIMULATION);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(t);
    }
}
