import org.h2.jdbcx.JdbcConnectionPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


class Game {

    /**
     * warzone
     *
     * 0 -> sea
     * 1 -> ship present
     * 2 -> ship damaged
     * 3 -> sea damaged
     */

    private int board_size = 8; //Need to change for client as well

    Player currentPlayer;

    public synchronized boolean legalMove(int i, int j, Player player) {

        int[][] opponents_board = currentPlayer.opponent.board;

        if (opponents_board[i][j] == 0) {

            opponents_board[i][j] = 3;
            currentPlayer = currentPlayer.opponent;
            currentPlayer.otherPlayerMoved(i,j);

            return true;

        }
        else if (opponents_board[i][j] == 1){

            opponents_board[i][j] = 2;
            return true;
        }
        else return false;



    }

    class Player extends Thread {

        int board[][] = new int[board_size][board_size];

        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;
        int PlayerNum;

        public Player(Socket socket, int PlayerNum) {

            this.PlayerNum = PlayerNum;

            for (int i = 0; i < board_size; i++) {
                for (int j = 0; j < board_size; j++) {
                    board[i][j] = 0;
                }
            }

            // For trial
            int count = 0;
            while (count < 20) {
                int ii = randomWithRange(0,board_size-1);
                int jj = randomWithRange(0,board_size-1);

                if (board[ii][jj] == 0) {
                    board[ii][jj] = 1;
                    count++;
                }
            }

            this.socket = socket;
            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME ");


                JdbcConnectionPool cp = JdbcConnectionPool.
                        create("jdbc:h2:~/test", "sa", "");
                Connection conn = null;


                while (true) {
                    boolean loggedIn = false;
                    output.println("LOGIN");

                    String inputs = input.readLine();

                    String regex = "[1 2][ ][A-Za-z0-9_-]{4,8}[ ][A-Za-z0-9_-]{4,8}";
                    if (!inputs.matches(regex)) {
                        output.println("MESSAGE Invalid credentials");
                        output.println("LOGIN");
                        continue;
                    }

                    String[] allinputs = inputs.split(" ");

                    int loginOrRegister = Integer.parseInt(allinputs[0]);

                    String username = allinputs[1];
                    String password = allinputs[2];

                    System.out.println(loginOrRegister);
                    System.out.println(username);
                    System.out.println(password);


                    try {

                        conn = cp.getConnection();




                        if (loginOrRegister == 1) {

                            PreparedStatement getPass = conn.prepareStatement("select password from users where id = ?");
                            getPass.setString(1,username);

                            ResultSet passwordresultset = getPass.executeQuery();
                            passwordresultset.next();
                            String fetchedPassword = passwordresultset.getString("PASSWORD");

//                              System.out.println(fetchedPassword);

                            if (BCrypt.checkpw(password, fetchedPassword))
                                loggedIn = true;
                            else
                                System.out.println("Invalid Credentials. Please Try Again.");




                        } else if (loginOrRegister == 2) {

                            String saltedPassword = BCrypt.hashpw(password,BCrypt.gensalt());

                            PreparedStatement insertUser = conn.prepareStatement("insert into users values(?,?)");

                            insertUser.setString(1,username);
                            insertUser.setString(2,saltedPassword);

                            insertUser.execute();

                            loggedIn = true;

                        }

                    }catch (SQLException e) {
                        e.printStackTrace();
                    }



                    if (loggedIn) {
                        break;
                    }
                    else {
                        output.println("MESSAGE Invalid Credentials");
                    }

                }

                conn.close(); cp.dispose();



                output.println("MESSAGE Waiting for opponent to connect");
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        String myboardAsString(int[][] board) {
            String output = "";

            for (int i = 0; i < board_size; i++) {
                for (int j = 0; j < board_size; j++) {
                    output = output + board[i][j];
                }
            }

            return output;
        }

        String opponentsboardTo2d(int[][] board) {
            String output = "";



            for (int i = 0; i < board_size; i++) {
                for (int j = 0; j < board_size; j++) {

                    int output_val = board[i][j];

                    if (output_val == 1) {
                        output_val = 0;
                    }

                    output = output + output_val;
                }
            }

            return output;
        }

        public boolean AmIWinner() {

            int[][] board = currentPlayer.board;

            for (int i = 0; i < board_size; i++) {
                for (int j = 0; j < board_size; j++) {
                    if (board[i][j] == 1)
                        return false;
                }
            }

            return true;


        }

        int randomWithRange(int min, int max)
        {
            int range = (max - min) + 1;
            return (int)(Math.random() * range) + min;
        }

        public boolean IsOpponentWinner() {

            int[][] board = currentPlayer.opponent.board;

            for (int i = 0; i < board_size; i++) {
                for (int j = 0; j < board_size; j++) {
                    if (board[i][j] == 1)
                        return false;
                }
            }

            return true;


        }

        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        public void otherPlayerMoved(int i, int j) {
            output.println("MY "+myboardAsString(board));
            output.println("OP "+opponentsboardTo2d(opponent.board));
            output.println("OPPONENT_MOVED " + i+j);

            if (AmIWinner())
                output.println("VICTORY");
            else if (opponent.AmIWinner())
                output.println("DEFEAT");
        }

        @Override
        public void run() {
            try {
                output.println("MESSAGE All players connected, Warships assigned randomly at warzone locations.\t 0 -> sea\t 1 -> ship present\t 2 -> successful attack 3 -> unsuccessful attack");





                if (PlayerNum == 1) {
                    output.println("MY "+myboardAsString(board));
                    output.println("OP "+opponentsboardTo2d(opponent.board));
                    output.println("START Your move");

                }

                while (true) {
                    String command = input.readLine();

                    if (command.startsWith("QUIT")) {
                        return;
                    }
                    else if (command.startsWith("MOVE")) {
                        int i = Integer.parseInt(""+command.charAt(5));
                        int j = Integer.parseInt(""+command.charAt(6));

                        if (islegalMove(i,j)) {

                            if (IsOpponentWinner()) {
                                output.println("VICTORY");
                                opponent.output.println("DEFEAT");
                            }
                            else {
                                output.println("VALID_MOVE");
                            }
                        }
                        else output.println("INV Innvalid Move");
                    }

                }
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            } finally {
                try {socket.close();} catch (IOException e) {}
            }
        }

        private boolean islegalMove(int i, int j) {

            int[][] opponents_board = opponent.board;

            if (opponents_board[i][j] == 0) {

                opponents_board[i][j] = 3;
                currentPlayer = currentPlayer.opponent;
                currentPlayer.otherPlayerMoved(i,j);

                return true;

            }
            else if (opponents_board[i][j] == 1){

                opponents_board[i][j] = 2;

                output.println("MY "+myboardAsString(board));
                output.println("OP "+opponentsboardTo2d(opponent.board));
                output.println("START Your move");


                return true;
            }
            else return false;

        }
    }
}
