package logic;

import gameobject.GameObject;
import gameobject.ROC;
import gameobject.ui.ListObject;
import gameobject.ui.TextButton;
import gameobject.ui.TextInputObject;
import gameobject.ui.TextObject;
import graphics.*;
import story.Story;
import utils.Global;
import utils.MouseInputEngine;
import utils.Node;
import utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/*
 * NewGameLogic.java
 * Ambulare
 * Jacob Oaks
 * 5/5/20
 */

/**
 * Dictates the logic the game will follow when the user is creating a new game. Will prompt the user to select a story,
 * then prompt them for character customization. Similar to the main menu logic, new game logic is broken up into a
 * series of phases allow clean animation to occur. Different updates occur based on the current phase. See the updatee
 * method for more information.
 */
public class NewGameLogic extends GameLogic {

    /**
     * Static Data
     */
    private static final int MIN_NAME_LENGTH = 3;  // the minimum length for a valid player name
    private static final int MAX_NAME_LENGTH = 12; // the maximum length for a valid player name

    /**
     * Gets all stories (res-relative and data directory relative) to display to the user when choosing a story. For
     * resource-relative stories, this will look at stories/stories.node. For each child listed in that node-file, the
     * value will be assumed to be the path to the story folder. For data directory stories, Ambulare/stories/ will
     * be checked for story folders. For all story folders found/specified as described above, they must have a valid
     * story_info.node node-file within their directory to be populated in the list returned by this method. See
     * story.Story's constructor for more information on how to format a story info node-files
     *
     * @return a list of all stories described above
     */
    private static List<Story> getStories() {
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
                    "gamelogic.NewGameLogic", "getStories()", true); // crash
        }
        return stories; // return the compiled list of stories
    }

    /**
     * A list object's list item tailored towards showing info about a single story during new game story selection
     */
    private static class StoryListItem extends GameObject implements ListObject.ListItem {

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

    /**
     * Represents a generic text button list item
     */
    private static class ListTextButton extends TextButton implements ListObject.ListItem {

        /**
         * Constructor
         *
         * @param font the font to use for the text button
         * @param text the text to display on the text button
         */
        public ListTextButton(Font font, String text) {
            super(font, text); // call text button's constructor
        }
    }

    /**
     * Members
     */
    private TextInputObject nameInput; // used to gather user input for player name
    private Node toTransfer;           // data transferred to world logic when new game creation has finished
    private float time;                // a generic timer used for timing within each phase of the animation
    private int phase;                 // the phase of the new game animation

    /**
     * Initializes the new game ui objects
     *
     * @param window the window
     */
    @Override
    public void initOthers(Window window) {

        // create list object for story selection
        List<Story> stories = getStories(); // get all available stories to play
        List<ListObject.ListItem> listItems = new ArrayList<>(); // create an empty list of list items
        for (Story s : stories) { // for each story
            StoryListItem sli = new StoryListItem(s); // create a story list item
            sli.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // give it a mouse release callback
                Node startingArea = s.isResRelative() ? (Node.resToNode(s.getAbsStartingAreaPath()))
                        : Node.fileToNode(s.getAbsStartingAreaPath(), false); // get starting area node
                if (startingArea == null) // if there is no starting area node-file at the provided path
                    Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("starting_area",
                            "Story", s.getStartingAreaPath() + " is an invalid path", false)),
                            "logic.NewGameLogic", "initOthers(Window)", true); // crash
                else { // otherwise
                    toTransfer = new Node(); // create the transfer data node
                    toTransfer.addChild(s.toNode()); // add story to transfer data
                    this.phase = 1; // then move to phase 1
                }
            });
            listItems.add(sli); // add the formatted story list item to the list items list
        }
        // create a list text button to open the data directory stories folder
        ListTextButton openStoryFolder = new ListTextButton(Global.FONT, "(open stories folder)");
        openStoryFolder.setScale(0.4f, 0.4f); // scale thee button down by about half
        openStoryFolder.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when clicked
            Utils.openNativeFileExplorer(Utils.getDataDir() + "/stories/"); // open stories folder in data dir
        });
        listItems.add(openStoryFolder); // add open story folder button to the list items list
        // create a list object containing the list items
        ListObject lo = new ListObject(listItems, 0.05f, new Material(new float[]{1f, 1f, 1f, 0f}));

        // create name input text input object for player name select
        this.nameInput = new TextInputObject(window, Global.FONT, "(name)",
                Global.getThemeColor(Global.ThemeColor.GRAY),
                Global.getThemeColor(Global.ThemeColor.GREEN)); // create name input text input object
        this.nameInput.setAcceptInput(false); // do not accept input until at the correct phase
        this.nameInput.setMinLength(MIN_NAME_LENGTH); // set the minimum length to be the minimum name length
        this.nameInput.setMaxLength(MAX_NAME_LENGTH); // set the maximum length to be the maximum name length
        this.nameInput.setEnterCallback(() -> { // when a valid name is chosen
            this.phase = 5; // go to phase 5
        });

        // add ui elements to their starting positions outside of view and fly them in
        this.roc.addStaticObject(lo, new ROC.PositionSettings(0f, -2f - (1f / Global.ar), false,
                0f)); // add the story list below
        this.roc.moveStaticObject(0, new ROC.PositionSettings(0f, 0f, false, 0f),
                0.5f); // fly in story list
        // add a text object prompting the player to select a story above
        this.roc.addStaticObject(new TextObject(Global.FONT, "Choose a story:"),
                new ROC.PositionSettings(0f, 2f + (1f / Global.ar), true, 0.2f));
        this.roc.moveStaticObject(1, new ROC.PositionSettings(0f, 1f, true, 0.2f),
                0.5f); // fly in story prompt
        TextButton returnButton = new TextButton(Global.FONT, "Return"); // create a button to return to main menu
        returnButton.setScale(0.5f, 0.5f); // scale return button by about half
        returnButton.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when return is clicked
            if (GameLogic.logicChange == null) { // if there is no logic change data currently
                GameLogic.logicChange = new LogicChange(new MainMenuLogic(), 0.5f); // switch to main menu
                GameLogic.logicChange.useTransferData(new Node()); // give an empty node to signify not a startup
                this.roc.fadeOut(new float[]{0f, 0f, 0f, 0f}, 0.5f); // begin fade out to black
            }
        });
        this.roc.addStaticObject(returnButton, new ROC.PositionSettings(0f, -2f - (1f / Global.ar),
                true,0.05f)); // add return button below
        this.roc.moveStaticObject(2, new ROC.PositionSettings(0f, -1f, true, 0.02f),
                0.5f); // fly in return button
        this.roc.useBackground(new GameObject(Model.getStdGridRect(1, 1), new Material(
                new Texture("/textures/ui/menu_back.png", true)))); // use background

    }

    /**
     * Responds to keyboard input by notifying the name input text input object of the keyboard input
     *
     * @param key    the key in question
     * @param action the action of the key (GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT)
     */
    @Override
    public void keyboardInput(int key, int action) {
        this.nameInput.keyboardInput(key, action); // delegate to name input text input object
    }

    /**
     * Updates the animation and ui elements of the new game logic depending on the phase
     *
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval); // update regular game logic member
        switch (this.phase) { // switch on the phase

            // PHASE 0 (no update necessary): wait for a story to be chosen

            case 1: // PHASE 1: fly story selection ui elements out of view
                this.roc.moveStaticObject(0, new ROC.PositionSettings(0f, -2f - (1f / Global.ar),
                        false, 0f), 0.5f); // fly list downwards
                this.roc.moveStaticObject(2, new ROC.PositionSettings(0f, -2f - (1f / Global.ar),
                        false, 0f), 0.5f); // fly return button downwards
                this.roc.moveStaticObject(1, new ROC.PositionSettings(0f, 2f + (1f / Global.ar),
                        false, 0f), 0.5f); // fly prompt upwards
                this.phase = 2; // advance to phase 2
                break;

            case 2: // PHASE 2: wait for story selection ui elements to be out of view
                this.time += interval; // keep track of time
                if (this.time >= 0.5f) { // if half of a second has passed
                    this.phase = 3; // advance to phase 3
                    this.time = 0f; // reset timer
                }
                break;

            case 3: // PHASE 3: update ui for name input and fly in name input ui elements
                // change prompt text to prompt for a name nows
                ((TextObject) this.roc.getStaticGameObject(1)).setText("Enter a name:");
                this.roc.getStaticGameObject(1).setScale(0.5f, 0.5f); // scale the prompt down by about half
                this.nameInput.setAcceptInput(true); // accept name input now
                this.roc.addStaticObject(this.nameInput, new ROC.PositionSettings(0f, 2f + (1f / Global.ar),
                        false, 0f)); // add name input to ROC out of view above
                TextButton finish = new TextButton(Global.FONT, "Accept"); // create a button to pressed when done
                finish.setScale(0.5f, 0.5f); // scale finish button down by about half
                finish.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when finish is pressed
                    // simulate enter being pressed on the name input because this will automatically check for validity
                    this.nameInput.keyboardInput(GLFW_KEY_ENTER, GLFW_RELEASE);
                });
                this.roc.addStaticObject(finish, new ROC.PositionSettings(null, this.nameInput, 0f, -1f,
                        0.1f)); // add finish button below name input
                this.roc.moveStaticObject(1, new ROC.PositionSettings(null, this.nameInput, 0f,
                        1f, 0.1f), 0f); // move prompt to above name input
                this.roc.moveStaticObject(3, new ROC.PositionSettings(0f, 0f, false,
                        0f), 0.5f); // fly name input ui down from about
                this.roc.moveStaticObject(2, new ROC.PositionSettings(0f, -1f, true,
                        0.02f),0.5f); // fly return button back in
                this.phase = 4; // advance to phase 4

            case 4: // PHASE 4: ensure prompt and finish button follow name input
                this.roc.ensurePlacement(1); // ensure prompt follows name input
                this.roc.ensurePlacement(4); // ensure finish button follows name input
                break;

            case 5: // PHASE 5: save name to transfer data and fly out name input ui elements
                this.roc.moveStaticObject(1, new ROC.PositionSettings(0f, 2f + (1f / Global.ar),
                        false, 0f), 0.5f); // fly out name input above
                this.roc.moveStaticObject(2, new ROC.PositionSettings(0f, -2f - (1f / Global.ar),
                        false, 0f), 0.5f); // fly out return button below
                this.roc.moveStaticObject(3, new ROC.PositionSettings(0f, -2f - (1f / Global.ar),
                        false, 0f), 0.5f); // fly out finish button below
                this.phase = 6; // advance to phase 6
                this.time = 0f; // reset timer
                this.toTransfer.addChild("name", this.nameInput.getText()); // save player name to transfer data
                break;

            case 6: // PHASE 6: wait for fly out to finish
                this.roc.ensurePlacement(4); // make sure prompt follows name input
                this.time += interval; // keep track of time
                if (this.time >= 0.5f) { // if half of a second has passed
                    GameLogic.logicChange = new LogicChange(new WorldLogic(), 0.5f); // change to world logic
                    GameLogic.logicChange.useTransferData(this.toTransfer); // use story's starting area and user's name
                    this.roc.fadeOut(new float[]{0f, 0f, 0f, 0f}, 0.5f); // fade out the ROC
                    this.phase = 7; // advance to phase 7
                }
                break;
        }
    }
}
