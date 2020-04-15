/**
 * Provides the entry point for the game
 */
public class Game {

    /**
     * Main method creates, initializes, and runs the engine
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        GameEngine g = new GameEngine(); // create engine
        g.start(); // start engine
    }
}