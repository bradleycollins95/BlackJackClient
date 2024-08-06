package Server;

import Cards.DeckOfCards;
import Players.Dealer;
import Players.Player;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles game logic for a single client in a separate thread
 * This class implements the Runnable interface to be executed by a thread
 *
 * @author bradley.collins
 */
public class BlackJackGame implements Runnable {
    private final Socket socket;
    private final Player player;
    private final Dealer dealer;
    private final DeckOfCards deck;

    /**
     * Constructs a Blackjack game with a specified client socket.
     *
     * @param socket The client socket connected to the server.
     */
    public BlackJackGame(Socket socket) {
        this.socket = socket;
        this.player = new Player(100); //initialize player with $100 balance
        this.dealer = new Dealer();
        this.deck = new DeckOfCards();
    }

    /**
     * Runs the game logic for a single client, executed by a thread.
     */
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            sendJsonMessage(out, "Welcome to Blackjack!\n", false);
            sendJsonMessage(out, "- Blackjack hands are scored by their point total.", false);
            sendJsonMessage(out, "- The hand with the highest total wins as long as it doesn't exceed 21.", false);
            sendJsonMessage(out, "- A hand with a higher total than 21 is said to bust.", false);
            sendJsonMessage(out, "- Cards 2 through 10 are worth their face value, and face cards (jack, queen, king) are also worth 10.", false);
            sendJsonMessage(out, "\nYou start with $100.", false);

            boolean playAgain = true;

            //main loop
            while (playAgain && player.getMoney() > 0) {
                sendJsonMessage(out, "\nPlace your bet:", true);
                String inputLine = in.readLine();
                JSONObject requestJson = new JSONObject(inputLine);
                int bet = Integer.parseInt(requestJson.getString("input"));

                if (bet > player.getMoney()) {
                    sendJsonMessage(out, "You cannot bet more than you have.", false);
                } else {
                    player.newBalance(-bet);
                    deck.shuffle();
                    player.clear();
                    dealer.clear();

                    //deal initial cards
                    player.addCard(deck.drawCard());
                    player.addCard(deck.drawCard());
                    dealer.addCard(deck.drawCard());
                    dealer.addCard(deck.drawCard());

                    sendJsonMessage(out, "Your hand: " + player.getCards(), false);
                    sendJsonMessage(out, "Dealer's hand: " + dealer.getCards().get(0) + " and [hidden]", false);

                    //instant blackjack?
                    if (player.handValue() == 21 || dealer.handValue() == 21) {
                        if (player.handValue() == 21) {
                            sendJsonMessage(out, "Blackjack! You win!", false);
                            player.newBalance((int) (bet * 1.5)); //blackjack pays 3:2 if initial cards result in a win
                            continue;
                        }
                        else if (dealer.handValue() == 21) {
                            sendJsonMessage(out, "Blackjack! Dealer wins!", false);
                            player.newBalance(-bet);
                            continue;
                        }
                        else {
                            throw new Error("Idk what happened");
                        }
                    }

                    boolean playerBusted = false;

                    //players turn
                    while (true) {
                        sendJsonMessage(out, "Hit or stand?", true);
                        inputLine = in.readLine();
                        requestJson = new JSONObject(inputLine);
                        String action = requestJson.getString("input").toLowerCase();

                        if (action.equals("hit")) {
                            player.addCard(deck.drawCard());
                            sendJsonMessage(out, "Your hand: " + player.getCards(), false);

                            //bust?
                            if (player.handValue() > 21) {
                                sendJsonMessage(out, "You bust!", false);
                                playerBusted = true;
                                break;
                            }
                        } else if (action.equals("stand")) {
                            break;
                        } else {
                            sendJsonMessage(out, "Invalid action. Please enter 'hit' or 'stand'.", false);
                        }
                    }

                    //dealers turn if player hasn't busted
                    if (!playerBusted) {
                        dealer.hit(deck);
                        sendJsonMessage(out, "Dealer's hand: " + dealer.getCards(), false);

                        //who wins?
                        if (dealer.handValue() > 21) {
                            sendJsonMessage(out, "Dealer busts! You win!", false);
                            player.newBalance(bet * 2);
                        } else if (player.handValue() > dealer.handValue()) {
                            sendJsonMessage(out, "You win!", false);
                            player.newBalance(bet * 2);
                        } else if (player.handValue() < dealer.handValue()) {
                            sendJsonMessage(out, "Dealer wins!", false);
                        } else {
                            sendJsonMessage(out, "Push! You get your bet back.", false);
                            player.newBalance(bet);
                        }
                    }

                    sendJsonMessage(out, "Your money: $" + player.getMoney(), false);
                }

                //play again?
                if (player.getMoney() > 0) {
                    sendJsonMessage(out, "Do you want to play again? (yes/no)", true);
                    inputLine = in.readLine();
                    requestJson = new JSONObject(inputLine);
                    String response = requestJson.getString("input").toLowerCase();

                    if (!response.equals("yes")) {
                        playAgain = false;
                        sendJsonMessage(out, "Thanks for playing! Your final balance is $" + player.getMoney(), false);
                    }
                } else {
                    playAgain = false;
                    sendJsonMessage(out, "Game over! You ran out of money.", false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a JSON message to the client.
     *
     * @param out the PrintWriter to send the message
     * @param message the message to send
     * @param expectsInput whether the message expects a response from the client
     */
    private void sendJsonMessage(PrintWriter out, String message, boolean expectsInput) {
        JSONObject responseJson = new JSONObject();
        responseJson.put("message", message);
        responseJson.put("expectsInput", expectsInput);
        out.println(responseJson.toString());
    }
}
