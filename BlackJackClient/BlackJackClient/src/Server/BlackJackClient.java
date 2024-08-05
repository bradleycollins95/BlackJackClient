package Server;

import java.io.*;
import java.net.*;

/**
 * The client class that connects to the server and interacts with the user through the console.
 * This client connects to the Blackjack server at a specified address and port, and reads responses from the
 * server and sends user inputs back to the server.
 * This client handles the communication in a loop until the server closes the connection or the user decides to quit.
 *
 * @author bradley.collins
 */
public class BlackJackClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 55555;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                System.out.println(serverResponse); //prints to console showing server sends
                if (serverResponse.endsWith(":") || serverResponse.trim().endsWith("?")) {
                    String userInput = stdIn.readLine();
                    out.println(userInput);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}