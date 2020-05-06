package story;

import utils.Node;
import utils.Pair;
import utils.Utils;

/*
 * Story.java
 * Ambulare
 * Jacob Oaks
 * 5/6/20
 */

/**
 * Represents a single story that a player game progress through. The idea is to have stories be able to be created by
 * anybody who wishes. Stories are able to be loaded from the data directory. This class encompasses important story
 * information. Stories can only be constructed using nodes. One of the constructors uses a node-file that each story
 * must provide. This node-file must be named "story_info.node" and must be within every valid story folder. For
 * data directory stories, each story should have its own folder within the /Ambulare/stories/ directory (which would
 * then contain the story info node-file). Resource-relative stories must have their story info file's resource-relative
 * path listed in res/stories/stories.node. See NewGameLogic's getStories() for more information on how valid stories
 * are picked up. See Story's constructor to see how story_info.node should be formatted
 */
public class Story {

    /**
     * Members
     */
    private Pair<Integer> startingPos;   // the starting position the player should be placed at in the starting area
    private String name = "Unnamed";     // the name of the story
    private String author = "Anonymous"; // the author/creator of the story
    private String path;                 // the path to the story's folder
    private String startingAreaPath;     // the path to the story's starting area (relative to the story's folder path)
    private boolean resRelative;         // whether the story's folder path is resource-relative or not

    /**
     * Constructs the story using a story_info.node node-file and the given story folder path and resource relativity
     * flag. A story_info.node node-file can contain the following children:
     * <p>
     * - starting_area [required]: the path to the starting area's node-file relative to the story folder
     * <p>
     * - starting_position [required]: the position the player should start at in the starting area. Should be formatted
     * as two integers (y after x) with a space in between
     * <p>
     * - name [optional][default: Unnamed]: the name of the story
     * <p>
     * - author [optional][default: Anonymous]: the author/creator of the story
     * <p>
     * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
     * such, when designing stories to be loaded into the game, the logs should be checked often to make sure the
     * loading process is unfolding correctly
     *
     * @param node        the story_info.node node containing the information to create the story from
     * @param path        the path to the story's folder
     * @param resRelative whether the path to the story's folder is resource-relative
     */
    public Story(Node node, String path, boolean resRelative) {
        this.path = path; // save story folder path as member
        this.resRelative = resRelative; // save resource relativity flag as member

        // load starting area and position
        Node startingArea = node.getChild("starting_area"); // load starting area
        if (startingArea == null) // if no starting area given
            Utils.handleException(new Exception(Utils.getImproperFormatErrorLine(
                    "starting_area", "Story", "a starting area path is required",
                    false)), "story.Story", "Story(Node, String, boolean)", true); // crash
        else this.startingAreaPath = startingArea.getValue(); // otherwise save the starting area path
        Node startingPos = node.getChild("starting_position"); // load starting position
        if (startingPos == null) // if no starting position given
            Utils.handleException(new Exception(Utils.getImproperFormatErrorLine(
                    "starting_position", "Story", "a starting position is required",
                    false)), "story.Story", "Story(Node, String, boolean)", true); // crash
        else { // otherwise
            String[] tokens = startingPos.getValue().split(" "); // split position into two tokens
            if (tokens.length < 2) // if there are not two tokens (improperly formatted)
                Utils.handleException(new Exception(Utils.getImproperFormatErrorLine(
                        "starting_position", "Story", "a starting position should be formatted" +
                                "as twoo integers separated by a space (ex: 4 6)", false)), "story.Story",
                        "Story(Node, String, boolean)", true); // crash
            else { // otherwise, try to convert to an integer pair
                try {
                    this.startingPos = new Pair<>(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
                } catch (Exception e) { // if an exception occurs during conversion
                    Utils.handleException(new Exception(Utils.getImproperFormatErrorLine(
                            "starting_position", "Story", "a starting position should be " +
                                    "formatted as two integers separated by a space (ex: 4 6)", false)),
                            "story.Story", "Story(Node, String, boolean)", true); // crash
                }
            }
        }

        // load other children
        try {
            for (Node c : node.getChildren()) { // for each child
                String n = c.getName().toLowerCase(); // get its name in lowercase
                if (n.equals("name")) this.name = c.getValue(); // if name -> save story name as member
                else if (n.equals("author")) this.author = c.getValue(); // if author -> save author as member
                else if (!n.equals("starting_area") && !n.equals("starting_pos") && !n.equals("path") &&
                        !n.equals("resource_relative")) // if an unrecognized child is found
                    Utils.log("Unrecognized child given for story_info.node info:\n" + c + "Ignoring.",
                            "story.Story", "Story(Node, String, boolean)", false); // log and ignore
            }
        } catch (Exception e) {// if any strange exceptions occur
            Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("Story", "Story",
                    e.getMessage(), false)), "story.Story", "Story(Node, String, boolean)", true); // log/crash

        }
    }

    /**
     * Constructs the story with the given node. In this constructor, the node should be identical to a story_info.node
     * node that would be used for the other constructor but with two additional children: (1) "path": the path to the
     * story's folder and (2) "resource_relative": whether the path to the story's folder is resource-relative or not.
     * See the other constructor for more information on how a story_info.node node should be formatted
     *
     * @param node the node to load the information from as described above
     */
    public Story(Node node) {
        this(node, node.getChild("path").getValue(),
                Boolean.parseBoolean(node.getChild("resource_relative").getValue())); // call other constructor
    }

    /**
     * @return the story's starting area's path, relative to the story folder
     */
    public String getStartingAreaPath() {
        return this.startingAreaPath;
    }

    /**
     * @return the story's starting area's path as an absolute path
     */
    public String getAbsStartingAreaPath() {
        return this.path + this.startingAreaPath; // concatenate starting area path with story folder path
    }

    /**
     * @return the story's name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the story's author/creator
     */
    public String getAuthor() {
        return this.author;
    }

    /**
     * @return the story folder's path
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @return the story's starting position
     */
    public Pair<Integer> getStartingPos() {
        return this.startingPos;
    }

    /**
     * @return whether the story's folder path is resource-relative
     */
    public boolean isResRelative() {
        return this.resRelative;
    }

    /**
     * Converts the story to a node for easier data transfer. The result of the can be plugged into the Story(Node)
     * constructor to retrieve the same story
     *
     * @return the story converted as described above
     */
    public Node toNode() {
        Node node = new Node("story"); // create story node
        node.addChild("name", this.name); // save name
        node.addChild("author", this.author); // save author/creator
        node.addChild("path", this.path); // save story folder path
        node.addChild("starting_area", this.startingAreaPath); // save starting area path
        // save starting position
        node.addChild("starting_position", +this.startingPos.x + " " + this.startingPos.y);
        node.addChild("resource_relative", Boolean.toString(this.resRelative)); // save resource relativity
        return node; // return created nodes
    }
}
