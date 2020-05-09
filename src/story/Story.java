package story;

import gameobject.GameObject;
import gameobject.ui.ListObject;
import gameobject.ui.TextButton;
import gameobject.ui.TextObject;
import graphics.Material;
import graphics.Model;
import graphics.ShaderProgram;
import utils.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
     * Gets all stories (res-relative and data directory relative) installed with a story info node-file. For
     * resource-relative stories, this will look at stories/stories.node. For each child listed in that node-file, the
     * value will be assumed to be the path to the story folder. For data directory stories, Ambulare/stories/ will
     * be checked for story folders. For all story folders found/specified as described above, they must have a valid
     * story_info.node node-file within their directory to be populated in the list returned by this method. See
     * story.Story's constructor for more information on how to format a story info node-files
     *
     * @return a list of all stories described above
     */
    public static List<Story> getStories() {
        List<Story> stories = new ArrayList<>(); // initialize stories list as an empty array list
        try { // wrap in a try-catch to make sure any issues are logged

            // get data directory relative stories
            Utils.ensureDirs("/stories/", true); // make sure stories directory exists
            File dir = new File(Utils.getDataDir() + "/stories"); // create a file at the stories directory
            for (File f : dir.listFiles()) { // for all files within the stories directory
                if (f.isDirectory()) { // if that file itself iss a directory
                    String infoPath = f.getAbsolutePath() + "/story_info.node"; // represent the path of the story info
                    if (Utils.fileExists(infoPath, false)) // if the story info file exists
                        stories.add(new Story(Node.fileToNode(infoPath, false), f.getAbsolutePath(),
                                false)); // attempt to create the story and add it to the lists
                }
            }

            // get resource-relative stories
            Node resStories = Node.resToNode("/stories/stories.node"); // read the stories list node-file
            for (Node n : resStories.getChildren()) { // for each child in the stories list
                String infoPath = n.getValue() + "/story_info.node"; // represent the path of the story info
                if (Utils.fileExists(infoPath, true)) // if the story info file exists
                    stories.add(new Story(Node.resToNode(infoPath), n.getValue(), true)); // create and add
            }
        } catch (Exception e) { // if an exception occurs
            Utils.handleException(new Exception("Unable to load stories list: " + e.getMessage()),
                    "story.Story", "getStories()", true); // crash
        }
        return stories; // return the compiled list of stories
    }

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
                        !n.equals("resource_relative") && !n.equals("starting_position")) // if unrecognized child found
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

    /**
     * A list object's list item tailored towards showing info about a single story
     */
    public static class StoryListItem extends GameObject implements ListObject.ListItem {

        /**
         * Static Data
         */
        private static final float PADDING = 0.02f; // how much padding to place between text and around the edgese

        /**
         * Members
         */
        private final MouseInputEngine.MouseCallback[] mcs = new MouseInputEngine.MouseCallback[4]; /* callback array as
            outlined by the mouse interaction interface */
        private final TextButton name; // text button for name
        private final TextObject author, path; // text objects for author and path

        /**
         * Constructor
         *
         * @param story the story that the story list item should represent
         */
        public StoryListItem(Story story) {
            // call super with a standard square model and with a transparent material
            super(Model.getStdGridRect(1, 1), new Material(new float[]{1f, 1f, 1f, 0f}));
            this.name = new TextButton(Global.FONT, story.getName()); // create name text button with story name
            this.name.setScale(0.8f, 0.8f); // scale story name down a little bit
            this.author = new TextObject(Global.FONT, "author: " + story.getAuthor()); // create author text object
            this.author.setScale(0.4f, 0.4f); // scale author down by a little over half
            this.path = new TextObject(Global.FONT, "path: " + (story.isResRelative() ? "<res>" : "") +
                    story.getPath()); // create path text object with story path
            this.path.setScale(0.3f, 0.3f); // scale path down by about one third
            this.position(); // position the text objects in the story list item
        }

        /**
         * Responds to movement by repositioning the text objects in the story list item
         */
        @Override
        protected void onMove() {
            this.position(); // reposition the text objects
        }

        /**
         * Positions the text objects in the story list item depending on the story list item's position. This method
         * also calculates and performs the appropriate scaling for the story list item to fit all text object members
         */
        private void position() {
            // height of the story list item is the height of the text objects plus padding between and around the edges
            float h = this.name.getHeight() + this.author.getHeight() + this.path.getHeight() + PADDING * 4;
            // width of the story list item is the maximum width of the text objects plus padding on the left and right
            float w = Math.max(Math.max(this.name.getWidth(), this.author.getWidth()), this.path.getWidth()) +
                    PADDING * 2;
            this.model.setScale(w, h); // scale according to the calculate width and height
            // reposition items using a cumulative y variable starting at the bottom of the story list item
            float y = this.getY() - (this.getHeight() / 2) + this.path.getHeight() / 2 + PADDING;
            this.path.setPos(this.getX(), y); // set the path to be on the bottom
            y += this.path.getHeight() / 2 + PADDING + this.author.getHeight() / 2; // increment y
            this.author.setPos(this.getX(), y); // set the author to be above the path
            y += this.author.getHeight() / 2 + PADDING + this.name.getHeight() / 2; // increment y
            this.name.setPos(this.getX(), y); // set the name to be on top
        }

        /**
         * Renders the story list item by rendering each text object component of it
         *
         * @param sp the shader program to use for rendering
         */
        @Override
        public void render(ShaderProgram sp) {
            this.name.render(sp); // render the name of the story
            this.author.render(sp); // render the author of the story
            this.path.render(sp); // render the path of the story
        }

        /**
         * Saves the given mouse input callback to be called when the given mouse input occurs
         *
         * @param type the mouse input type to give a callback for
         * @param mc   the callback
         */
        @Override
        public void giveCallback(MouseInputEngine.MouseInputType type, MouseInputEngine.MouseCallback mc) {
            MouseInputEngine.MouseInteractive.saveCallback(type, mc, this.mcs); // save callback
        }

        /**
         * Responds to mouse interaction by highlighting the name of the story list and by invoking appropriate
         * callbacks
         *
         * @param type the type of mouse input that occurred
         * @param x    the x position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
         *             input engine's camera usage flag for this particular implementing object
         * @param y    the y position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
         */
        @Override
        public void mouseInteraction(MouseInputEngine.MouseInputType type, float x, float y) {
            MouseInputEngine.MouseInteractive.invokeCallback(type, this.mcs, x, y); // invoke necessary callbacks
            this.name.mouseInteraction(type, x, y); // pass mouse interaction to button
        }
    }
}
