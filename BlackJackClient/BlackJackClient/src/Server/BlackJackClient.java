package Server;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * The client class that connects to the server and interacts with the user through the console.
 * This client connects to the Blackjack server at a specified address and port, and reads responses from the
 * server and sends user inputs back to the server.
 * This client handles the communication in a loop until the server closes the connection or the user decides to quit.
 *
 * @author bradley.collins
 */
public class BlackJackClient {
    private static final String serverAddress = "localhost";
    private static final int portNumber = 55555;

    public static void main(String[] args) {
        try (Socket socket = new Socket(serverAddress, portNumber);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); //read incoming messages from the server
             PrintWriter out = new PrintWriter(socket.getOutputStream()); //send messages to the server
             Scanner stdIn = new Scanner(System.in)) {

            String serverResponse;

            while ((serverResponse = in.readLine()) != null) {
                JSONObject responseJson = new JSONObject(serverResponse); //parse as a JSON object
                System.out.println(responseJson.getString("message")); //extract the message from the JSON response

                //do we need user input?
                if (responseJson.getBoolean("expectsInput")) {
                    String userInput = stdIn.nextLine();
                    JSONObject requestJson = new JSONObject();
                    requestJson.put("input", userInput);
                    out.println(requestJson.toString()); //send the JSON object to the server
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
