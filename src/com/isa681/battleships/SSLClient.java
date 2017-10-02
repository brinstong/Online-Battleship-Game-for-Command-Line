import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.util.Scanner;

public class SSLClient {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8901;

// Put your passwords here. The better way to do this would be to create a properties file or have a enterprise password manager.
    private static final String CLIENT_KEY_STORE_PASSWORD = "";
    private static final String CLIENT_TRUST_KEY_STORE_PASSWORD = "";

    private SSLSocket sslSocket;


    private BufferedReader in;
    private PrintWriter out;

    int boardsize = 8; // Need to change for server as well

    int[][] opponents_board;

    public SSLClient() {
        opponents_board = new int[boardsize][boardsize];

        for (int i = 0; i < boardsize; i++) {
            for (int j = 0; j < boardsize; j++) {
                opponents_board[i][j] = 0;
            }
        }
    }

    public void init() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            KeyStore keyStore = KeyStore.getInstance("JKS");
            KeyStore trustKeyStore = KeyStore.getInstance("JKS");

            keyStore.load(new FileInputStream("dataClient/keystore.jks"), CLIENT_KEY_STORE_PASSWORD.toCharArray());
            trustKeyStore.load(new FileInputStream("dataClient/cacerts.jks"), CLIENT_TRUST_KEY_STORE_PASSWORD.toCharArray());

            keyManagerFactory.init(keyStore, CLIENT_KEY_STORE_PASSWORD.toCharArray());
            trustManagerFactory.init(trustKeyStore);

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(DEFAULT_HOST, DEFAULT_PORT);
            sslSocket.setNeedClientAuth(true);

            in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            out = new PrintWriter(sslSocket.getOutputStream(),true);
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    public void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                System.out.println("Welcome to Battleships");
            }

            Scanner scanner = new Scanner(System.in);

            while (true) {


                response = in.readLine();

//                System.out.println(response);


                if (response.startsWith("LOGIN")) {
                    while (true) {
                        System.out.println("Enter Login Details : 1/2{Login/Register} username password");
                        String input = scanner.nextLine();
                        String regex = "[1 2][ ][A-Za-z0-9_-]{4,8}[ ][A-Za-z0-9_-]{4,8}";
                        if (input.matches(regex)) {
                            out.println(input);
                            break;
                        }
                        else {
                            System.out.println("Please use the following format <1/2> <username> <password>");
                            System.out.println("1 : Login \n2 : Register");
                            System.out.println("Username / Password must be between 4 and 8 characters and include only A-Z, a-z, 0-9, _ and -");

                        }




                    }
                }
                else if (response.startsWith("VALID_MOVE")) {
                    System.out.println("Opponents Turn");
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    int i = Integer.parseInt(response.substring(15));
                    int j = Integer.parseInt(response.substring(16));

                    System.out.println("Your Turn... Enter ij to attack");

                    out.println("MOVE " + scanner.nextLine());


                } else if (response.startsWith("VICTORY")) {
                    System.out.println("You win");
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    System.out.println("You lose");
                    break;
                } else if (response.startsWith("START")) {
                    System.out.println(response.substring(6));
                    System.out.println("Your Turn... Enter ij to attack");
                    out.println("MOVE " + scanner.nextLine());
                } else if (response.startsWith("INV") ) {
                    System.out.println(response.substring(4));
                    System.out.println("Invalid Move... Enter ij to attack");
                    out.println("MOVE " + scanner.nextLine());

                } else if (response.startsWith("MESSAGE")) {
                    System.out.println(response.substring(8));
                }
                else if (response.startsWith("MY")) {
                    String warzone = response.substring(3);
                    System.out.println("\n\nYour Warzone");
                    printWarzone(warzone);
                }
                else if (response.startsWith("OP")) {
                    String warzone = response.substring(3);
                    System.out.println("\n\nOpponents Warzone");
                    printWarzone(warzone);
                }
            }
            out.println("QUIT");
        }
        finally {
            sslSocket.close();
        }
    }

    private void printWarzone(String warzone) {
//        System.out.println("Warzone :"+warzone);

        String output = " |\t";

        for (int i = 0; i < boardsize; i++) {
            output = output + i+"\t";
        }
        output = output + "\n-+--";
        for (int i = 0; i < boardsize-1; i++) {
            output = output + "-----";
        }

        System.out.print(output);

        for (int i = 0; i < warzone.length(); i++) {
            if (i%boardsize==0) {
                System.out.println();
                System.out.print(i/boardsize+"|\t");
            }
            System.out.print(warzone.charAt(i) +"\t");

        }
        System.out.println();
    }


    public static void main(String[] args) throws Exception {
//        String serverAddress = "127.0.0.1";
        while (true) {
            SSLClient client = new SSLClient();
            client.init();
//            client.connect(serverAddress);
            client.play();
            break;

        }
    }
}
