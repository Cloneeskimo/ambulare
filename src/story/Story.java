package story;

import gameobject.GameObject;
import gameobject.ui.ListObject;
import gameobject.ui.TextButton;
import gameobject.ui.TextObject;
import gameobject.ui.TexturedButton;
import graphics.Material;
import graphics.Model;
import graphics.ShaderProgram;
import utils.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            Utils.Path p = new Utils.Path("/stories/", false); // create path for stories directory
            Utils.ensureDirs(p); // make sure stories directory exists
            for (File f : p.getFile().listFiles()) { // for all files within the stories directory
                if (f.isDirectory()) { // if that file itself is a directory
                    Utils.Path sp = p.add(f.getName() + "/story_info.node"); // create path to story info node-file
                    if (sp.exists()) // if the story info file exists, attempt to create the story and add it to list
                        stories.add(new Story(Node.pathContentsToNode(sp), p));
                }
            }

            // get resource-relative stories node
            Node resStories = Node.pathContentsToNode(new Utils.Path("/stories/stories.node", true));
            for (Node n : resStories.getChildren()) { // for each child in the stories list
                p = new Utils.Path(n.getValue(), true); // create new path to resource-relative story folder
                Utils.Path sp = p.add("/story_info.node"); // create path to story info node-file
                if (sp.exists()) // if the story info file exists
                    stories.add(new Story(Node.pathContentsToNode(sp), p)); // create and add
            }
        } catch (Exception e) { // if an exception occurs
            Utils.handleException(new Exception("Unable to load stories list: " + e.getMessage()),
                    Story.class, "getStories", true); // crash
        }
        return stories; // return the compiled list of stories
    }

    /**
     * Members
     */
    private Pair<Integer> startingPos;   // the starting position the player should be placed at in the starting area
    private Utils.Path folderPath;       // the path to the story's folder
    private Utils.Path startingAreaPath; // the path to the story's starting area
    private String startingArea;         // the path to the story's starting area relative to the story folder
    private String name;                 // the name of the story
    private String author;               // the author/creator of the story

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
     *
     * @param data       the story_info.node node containing the information to create the story from
     * @param folderPath the path to the story's folder
     */
    public Story(Node data, Utils.Path folderPath) {
        this.folderPath = folderPath; // save story folder path as member

        /*
         * Load story information using node loader
         */
        Map<String, Object> story = NodeLoader.loadFromNode("Story", data, new NodeLoader.LoadItem[]{
                new NodeLoader.LoadItem<>("starting_position", null, String.class).makeRequired()
                        .useTest((v, sb) -> {
                    this.startingPos = Pair.strToIntegerPair((String) v);
                    if (this.startingPos == null) {
                        sb.append("Must be two valid integers separated by a space");
                        sb.append("\nFor example: '5 6'");
                        return false;
                    }
                    return true;
                }),
                new NodeLoader.LoadItem<>("starting_area", null, String.class)
                        .makeRequired()
                        .makeValueSensitive()
                        .useTest((v, sb) -> {
                    this.startingArea = (String) v;
                    this.startingAreaPath = this.folderPath.add((String) v);
                    if (!this.startingAreaPath.exists()) {
                        sb.append("Starting area path '" + this.startingAreaPath + "' does not exist");
                        return false;
                    }
                    return true;
                }),
                new NodeLoader.LoadItem<>("name", "Unnamed", String.class).makeValueSensitive(),
                new NodeLoader.LoadItem<>("author", "Anonymous", String.class).makeValueSensitive()
        });

        /*
         * Apply loaded information
         */
        this.name = (String) story.get("name"); // save name as member
        this.author = (String) story.get("author"); // save author as member
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
        this(node, node.getChild("resource_path") != null
                ? new Utils.Path(node.getChild("resource_path"))
                : new Utils.Path(node.getChild("path"))); // call other constructor
    }

    /**
     * @return the story's starting area's path
     */
    public Utils.Path getStartingAreaPath() {
        return this.startingAreaPath;
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
    public Utils.Path getFolderPath() {
        return this.folderPath;
    }

    /**
     * @return the story's starting position
     */
    public Pair<Integer> getStartingPos() {
        return this.startingPos;
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
        node.addChild(this.folderPath.toNode()); // save story folder path
        node.addChild("starting_area", this.startingArea); // save starting area story folder path
        // save starting position
        node.addChild("starting_position", +this.startingPos.x + " " + this.startingPos.y);
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
        private final TexturedButton name; // text button for name
        private final GameObject author, path; // text objects for author and path

        /**
         * Constructor
         *
         * @param story the story that the story list item should represent
         */
        public StoryListItem(Story story) {
            // call super with a standard square model and with a transparent material
            super(Model.getStdGridRect(1, 1), new Material(new float[]{1f, 1f, 1f, 0f}));
            this.name = new TextButton(Global.font, story.getName()).solidify(); // create name button with story name
            this.name.setScale(0.8f, 0.8f); // scale story name down a little bit
            this.author = new TextObject(Global.font, "author: " + story.getAuthor()).solidify(); // author text
            this.author.setScale(0.4f, 0.4f); // scale author down by a little over half
            this.path = new TextObject(Global.font, "path: " + story.getFolderPath()).solidify(); // path text
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
