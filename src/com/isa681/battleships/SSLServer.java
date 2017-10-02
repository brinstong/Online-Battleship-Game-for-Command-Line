import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;


public class SSLServer {

    private final int DEFAULT_PORT = 8901;

// Put your passwords here. The better way to do this would be to create a properties file or have a enterprise password manager.
    private final String SERVER_KEY_STORE_PASSWORD = "";
    private final String SERVER_TRUST_KEY_STORE_PASSWORD = "";

    private SSLServerSocket serverSocket;

    public static void main(String[] args) throws IOException {
        SSLServer server = new SSLServer();
        server.init();
        server.start();
    }


    public void start() throws IOException {
        System.out.println("BattleShips Server is Now Running");

        if (serverSocket == null) {
            System.out.println("ERROR");
            return;
        }
        while (true) {
            try {
                while (true) {
                    Game game = new Game();
                    Game.Player player1 = game.new Player(serverSocket.accept(), 1);
                    Game.Player player2 = game.new Player(serverSocket.accept(), 2);
                    player1.setOpponent(player2);
                    player2.setOpponent(player1);
                    game.currentPlayer = player1;
                    player1.start();
                    player2.start();
                }
            } finally {
                serverSocket.close();
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

            keyStore.load(new FileInputStream("dataServer/keystore.jks"), SERVER_KEY_STORE_PASSWORD.toCharArray());
            trustKeyStore.load(new FileInputStream("dataServer/cacerts.jks"), SERVER_TRUST_KEY_STORE_PASSWORD.toCharArray());

            keyManagerFactory.init(keyStore, SERVER_KEY_STORE_PASSWORD.toCharArray());
            trustManagerFactory.init(trustKeyStore);

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(DEFAULT_PORT);
            serverSocket.setNeedClientAuth(true);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

