import logic.WorldLogic;

/*
    Ambulare
    created by Jacob Oaks
    Most of this code is extremely thoroughly commented. If you find an unexplained variable or line of code, here
    are the best places to look:
    - if it is a member variable of the class you are looking at, look for its definition at the top of the class
    - if it is a parameter to a method you are looking at, look for its description in the method header
    - look for a comment near what you are seeing pointing the reader to another place with more info
    - if you've checked everywhere and can't find anything, you are probably overthinking it. The only things that
      have little-to-no explanation are those that I figured to be self-explanatory
 */

/**
 * Provides the entry point for the game
 */
public class Ambulare {

    /**
     * Creates and starts the engine
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        GameEngine g = new GameEngine(new WorldLogic()); // create engine and tell it to follow world logic initially
        g.start(); // start the engine
    }
}