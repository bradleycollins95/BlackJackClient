package Server;

import Cards.DeckOfCards;
import Players.Dealer;
import Players.Player;

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
    private DeckOfCards deck;

    /**
     * Constructs a Blackjack game with a specified client socket
     *
     * @param socket The client socket connected to the server
     */
    public BlackJackGame(Socket socket) {
        this.socket = socket;
        this.player = new Player(100);
        this.dealer = new Dealer();
        this.deck = new DeckOfCards();
    }

    @Override
    public void run() {
        //ensure streams are closed properly
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("Welcome to Blackjack!\n");
            out.println("- Blackjack hands are scored by their point total.");
            out.println("- The hand with the highest total wins as long as it doesn't exceed 21.");
            out.println("- A hand with a higher total than 21 is said to bust.");
            out.println("- Cards 2 through 10 are worth their face value, and face cards (jack, queen, king) are also worth 10.");
            out.println("\nYou start with $100.");
            String inputLine;
            while (player.getMoney() > 0) {
                out.println("\nPlace your bet:");
                int bet = Integer.parseInt(in.readLine());

                if (bet > player.getMoney()) {
                    out.println("You cannot bet more than you have.");
                    continue;
                }

                startNewGame();

                out.println("\nYour hand: " + player);
                out.println("\nDealer's hand: " + dealer.getCards().get(0) + " and [hidden]");

                boolean playerTurn = true;

                while (playerTurn) {
                    out.println("\nHit or stand?");
                    inputLine = in.readLine();

                    if (inputLine.equalsIgnoreCase("hit")) {
                        player.addCard(deck.drawCard());
                        out.println("Your hand: " + player);
                        if (player.calculateValue() > 21) {
                            out.println("You bust!");
                            player.newBalance(-bet);
                            playerTurn = false;
                        }
                    } else if (inputLine.equalsIgnoreCase("stand")) {
                        playerTurn = false;
                    } else {
                        out.println("Invalid input! Please type 'hit' to hit, or 'stand' to stand.");
                    }
                }

                if (player.calculateValue() <= 21) {
                    dealer.hit(deck);
                    out.println("\nDealer's hand: " + dealer);

                    if (dealer.calculateValue() > 21 || dealer.calculateValue() < player.calculateValue()) {
                        out.println("\nYou win!");
                        player.newBalance(bet);
                    } else if (dealer.calculateValue() > player.calculateValue()) {
                        out.println("\nDealer wins!");
                        player.newBalance(-bet);
                    } else {
                        System.out.println("\nIt's a tie!");
                    }
                }

                out.println("\nYour current money: $" + player.getMoney());
                out.println("Do you want to play again?");
                inputLine = in.readLine();
                if (inputLine.equalsIgnoreCase("no")) {
                    break;
                }
            }

            out.println("\nGame over! You finished with $" + player.getMoney());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startNewGame() {
        player.clear();
        dealer.clear();
        deck = new DeckOfCards();

        player.addCard(deck.drawCard());
        player.addCard(deck.drawCard());

        dealer.addCard(deck.drawCard());
        dealer.addCard(deck.drawCard());
    }
}
