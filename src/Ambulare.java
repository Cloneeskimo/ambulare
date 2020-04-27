import logic.MainMenuLogic;

/**
 * Ambulare
 * The side-scrolling fantasy RPG
 * created by Jacob Oaks
 */

/**
 * Provides the entry point for the game. This is the main class and its purpose is the start the game engine
 */
public class Ambulare {

    /**
     * Main method - the entry point into the whole program. Just creates and starts the engine
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        GameEngine g = new GameEngine(new MainMenuLogic()); // create the engine and tell it which logic to follow
        g.start(); // start the engine
    }
}