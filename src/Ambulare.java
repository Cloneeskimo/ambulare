import logic.WorldLogic;

/*
    Ambulare
    created by Jacob Oaks
    Most of this code is extremely thoroughly documented. If you find an unexplained variable or line of code, here
    are the best places to look:
    - if it is a member variable of the class you are looking at, look for its definition at the top of the class
    - if it is a parameter to a method you are looking at, look for its description in the method header
    - look for a comment near what you are seeing pointing the reader to another place with more info
    - if you've checked everywhere and can't find anything, you are probably overthinking it. The only things that
      have little-to-no explanation are those that I figured to be self-explanatory

    I try to follow the following style guide when writing and documenting code:
    - All code and documentation should fit within a 120 character right margin
    - All methods and classes should have their parameters, members, and returns documented
    - Lines that are unclear or need further explanation should have a comment to the right of them explaining their
      purpose. If the code itself is too long to include a sufficiently explanatory comment to its right, put the
      comment above it as a single-line comment. If the code is not very long but the comment is, use a multiline
      comment to the right of it that will align on the left for each line
    - Class member comments should all be aligned
    - In-method comments that take up more than one line should use a JDoc styled multi-line comment, such as how this
      comment
      does
    - Comments do not need to be strictly worded, but they should remain somewhat formal and very clear
    - In methods with many lines of code, blocks of around 2-8 lines should be condensed and given comments describing
      what that chunk of code does above them
    - Comments should not end in periods
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