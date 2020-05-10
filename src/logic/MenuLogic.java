package logic;

import gameobject.GameObject;
import gameobject.ROC;
import gameobject.gameworld.Area;
import gameobject.gameworld.Block;
import gameobject.ui.ListObject;
import gameobject.ui.TextButton;
import gameobject.ui.TextInputObject;
import gameobject.ui.TextObject;
import graphics.*;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLUtil;
import story.Story;
import utils.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;

/**
 * Lays out the logic for the menu of the game. This logic is divided up into a complex number of phases which determine
 * how the logic will act. In a way, each phase is a "sub-logic". This division of the logic into phases allows for
 * smooth animation and transition all throughout the menu experience for the user. Briefly, the menu consists of: a
 * main menu introduced by a physics simulation using the title; a menu area that is rendered and slowly scrolled as a
 * background to the entire menu process; and a new game menu where a story is selected and a name is chosen for a new
 * game. All of the phases are listed below along with their functions and the phases they lead into:
 * <p>
 * 0 - Waiting for the title object to drop -> 1
 * 1 - Dropping the title and simulating title physics -> 2
 * 2 - Enable ROC, add title and move title to proper position -> 3
 * 3 - Wait for title to be moved into proper position -> 4
 * 4 - Create menu area backdrop, create and add main menu UI, fade in ROC -> 5
 * 6 - Wait for user input:
 * - "New Game" pressed -> 20
 * - "Load Game" pressed -> TODO
 * - "Settings" pressed -> TODO
 * - "Exit" pressed -> Close Program
 * 18 - Resurface original main menu UI -> 6
 * 19 - Wait for new game UI to hide and title to move back up -> 18
 * For any of the below phases, a return button is present that, when pressed, hides all new game UI and switches to
 * phase 19
 * 20 - Hide main menu UI -> 21
 * 21 - Wait for main menu UI to hide -> 22
 * 22 - Create (or surface if already created) new game UI -> 23
 * 23 - Wait for the user to choose a story -> 24
 * 24 - Hide story selection UI -> 25
 * 25 - Show name selection UI -> 26
 * 26 - Wait for a valid name to be chosen -> 27
 * 27 - Hide all UI except title which is moved to center of the screen -> 28
 * 28 - Fade out and transition to world logic -> 29
 */
public class MenuLogic extends GameLogic {

    /**
     * Represents a generic text button list item
     */
    private static class ListTextButton extends TextButton implements ListObject.ListItem {

        /**
         * Constructor
         *
         * @param font the font to use for the text button list item
         * @param text the text to display on the text button lists item
         */
        public ListTextButton(Font font, String text) {
            super(font, text); // call text button's constructor
        }
    }

    /**
     * ROC Static Object (UI Element) Tags
     */
    private static final int TAG_TITLE = 0;      // title
    private static final int TAG_NEW_GAME = 1;   // new game button (main menu)
    private static final int TAG_LOAD_GAME = 2;  // load game button (main menu)
    private static final int TAG_SETTINGS = 3;   // settings button (main menu)
    private static final int TAG_EXIT = 4;       // exit button (main menu)
    private static final int TAG_STORY_LIST = 5; // story list (new game)
    private static final int TAG_PROMPT = 6;     // story selection/name input prompt (new game)
    private static final int TAG_RETURN = 7;     // return button (new game)
    private static final int TAG_NAME = 8;       // name input (new game)
    private static final int TAG_FINISH = 9;     // finish button (new game)

    /**
     * Other Static Data
     */
    private static final int MIN_NAME_LENGTH = 3;   // the minimum length for a valid player name
    private static final int MAX_NAME_LENGTH = 12;  // the maximum length for a valid player name
    private static final float TRANS_SPEED = 0.5f;  // the speed of transition throughout the menus
    private static final float MM_TITLE_POS = 0.7f; // y position of the title in the main menu

    /**
     * Members
     */
    private Camera cam;                // the cam that pans the menu area
    private GameObject title;          // title game object
    private Node toTransfer;           // data transferred to world logic
    private ShaderProgram sp;          /* shader program to render title occasionally. Specifically, the title is
                                          manually rendered during the introductory physics simulation and whenever the
                                          ROC is fading and the goal is to not fade the title */
    private TextButton newGame;        // new game button (main menu)
    private TextButton loadGame;       // load game button (main menu)
    private TextButton settings;       // settings button (main menu)
    private TextButton exit;           // exit button (main menu)
    private TextInputObject nameInput; // used to gather user input for player name (new game)
    private Window window;             // reference to window to close when exit button is pressed
    private float maxCamX;             // x beyond which the menu area camera should start scrolling back
    private float minTitleY;           // the minimum y the title should be during the physics simulation before bounce
    private float time;                // a generic timer used for timing within a phase of the animation
    private float titleV;              // the velocity of the title during the physics simulation
    private int phase;                 // the phase of the menu animation (see comments above class definition above)
    private boolean newGameUICreated;  // whether the new menu UI items have been created and added to the ROC or not

    /**
     * @return a position just above the window
     */
    private static float aboveWindow() {
        return 2f + (1f / Global.ar);
    }

    /**
     * @return a position just below the window
     */
    private static float belowWindow() {
        return -2f - (1f / Global.ar);
    }

    /**
     * Initializes the menu logic by beginning the introductory physics simulation
     *
     * @param window the window
     */
    @Override
    public void initOthers(Window window) {
        this.window = window; // save window reference
        Texture titleTexture = new Texture("/textures/ui/title.png", true); // create the title texture
        float[] titleModelCoords = titleTexture.getModelCoords(128); // get the corresponding model coords
        title = new GameObject(new Model(titleModelCoords, Model.getStdRectTexCoords(), Model.getStdRectIdx()),
                new Material(titleTexture)); // create title object
        title.setScale(0.55f, 0.55f); // scale title by about half
        title.setPos(0f, aboveWindow()); // move title to be up and out of view
        this.renderROC = false; // do not render ROC on startup at first
        initSP(); // initialize the shader programs
    }

    /**
     * Initializes the shader program used to render the title occasionally. Specifically, the title is manually
     * rendered during the introductory physics simulation and whenever the ROC is fading and the goal is to not fade
     * the title
     */
    private void initSP() {
        // create the shader program using the HUD shaders
        sp = new ShaderProgram("/shaders/hud_vertex.glsl", "/shaders/hud_fragment.glsl");
        sp.registerUniform("ar"); // register aspect ratio uniform
        sp.registerUniform("arAction"); // register aspect ratio action uniform
        sp.registerUniform("x"); // register object x uniform
        sp.registerUniform("y"); // register object y uniform
        sp.registerUniform("isTextured"); // register texture flag uniform
        sp.registerUniform("color"); // register color uniform
        sp.registerUniform("blend"); // register blend uniform
        sp.registerUniform("texSampler"); // register texture sampler uniform
    }

    /*
     * Creates the menu area and tells the ROC to use a game world with the menu area
     */
    private void initMenuArea() {
        // tell the ROC to use a game world with the menu area
        this.roc.useGameWorld(this.window.getHandle(), new Area(Node.resToNode("/misc/menu_area.node"), this.window));
        this.cam = this.roc.getGameWorld().getCam(); // save camera handle to make sure it stays within bounds
        // place camera at a random x within the area and at the vertical center of the area
        this.cam.setPos((float) Math.random() * (float) this.roc.getGameWorld().getArea().getBlockMap().length,
                (float) this.roc.getGameWorld().getArea().getBlockMap()[0].length / 2);
        this.cam.setVX(0.5f); // make camera slowly scroll to the right
        this.maxCamX = this.roc.getGameWorld().getArea().getBlockMap().length; // don't allow cam past area
        glfwSetScrollCallback(this.window.getHandle(), (x, y, s) -> {
        }); // disable mouse scroll wheel zooming
    }

    /**
     * Hides the static object in the ROC with the given tag by flying the object out of view below
     *
     * @param tag the ROC tag for the object to hide
     */
    private void hide(int tag) {
        this.roc.moveStaticObject(tag, new ROC.PositionSettings(0, belowWindow(), false, 0f),
                TRANS_SPEED); // transition it to a position below the window
    }

    /**
     * Creates the main menu UI elements and then adds them to the ROC. No checks are needed to see if the elements have
     * already been added (as opposed to the new game UI elements) because, within one lifecycle of the menu logic, once
     * these items are created and added to the ROC, the phase which created and added them will never be revisited
     */
    private void createMainMenuUI() {
        newGame = new TextButton(Global.FONT, "New Game"); // create new game button
        newGame.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when clicked,
            this.phase = 20; // go phase 20 which will transition to the new game sub-menu
        });
        loadGame = new TextButton(Global.FONT, "Load Game"); // create load game button
        loadGame.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when clicked,
            //TODO: Load Game
        });
        settings = new TextButton(Global.FONT, "Settings"); // create settings button
        loadGame.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when clicked,
            //TODO: Settings
        });
        exit = new TextButton(Global.FONT, "Exit"); // create exit button
        exit.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when clicked,
            window.close(); // close the window
        });
        // add the new game button at its correct position
        this.roc.addStaticObject(newGame, TAG_NEW_GAME, false, getMainMenuUIPosition(TAG_NEW_GAME));
        // add the load game button at its correct position
        this.roc.addStaticObject(loadGame, TAG_LOAD_GAME, false, getMainMenuUIPosition(TAG_LOAD_GAME));
        // add the settings button at its correct position
        this.roc.addStaticObject(settings, TAG_SETTINGS, false, getMainMenuUIPosition(TAG_SETTINGS));
        // add the exit button at its correct position
        this.roc.addStaticObject(exit, TAG_EXIT, false, getMainMenuUIPosition(TAG_EXIT));
    }

    /**
     * Ensures that the main menu buttons are following the new game button. This is useful when transitioning away or
     * towards the main menu sub-phases
     */
    private void ensureMainMenuButtonPositions() {
        this.roc.ensurePlacement(TAG_LOAD_GAME); // ensure the load game button follows the new game button
        this.roc.ensurePlacement(TAG_SETTINGS); // ensure the settings button follows the load game button
        this.roc.ensurePlacement(TAG_EXIT); // ensure the exit button follows the settings button
    }

    /**
     * Gets the correct ROC static object position settings for a main menu UI elements
     *
     * @param tag the ROC tag of the element whose position settings to retrieve
     * @return the correct position settings for the main menu UI element with the given tag within main menu phases
     */
    private ROC.PositionSettings getMainMenuUIPosition(int tag) {
        switch (tag) { // switch on the tag
            case TAG_TITLE: // title should be near the top of the window
                return new ROC.PositionSettings(0f, MM_TITLE_POS, true, 0f);
            case TAG_NEW_GAME: // new game button should be below title
                return new ROC.PositionSettings(null, title, 0f, -1f, 0.23f);
            case TAG_LOAD_GAME: // load game button should be below new game button
                return new ROC.PositionSettings(null, newGame, 0f, -1f, 0.1f);
            case TAG_SETTINGS: // settings button should be below load game button
                return new ROC.PositionSettings(null, loadGame, 0f, -1f, 0.1f);
            case TAG_EXIT: // exit button should be below settings button
                return new ROC.PositionSettings(null, settings, 0f, -1f, 0.1f);
            default: // if any other UI element is given
                Utils.handleException(new Exception("Invalid main menu UI ROC tag given: '" + tag + "'. No default " +
                                "main menu position exists for such a tag."), "logic.MenuLogic", "getMainMenuUIPosition(int)",
                        true); // crash as there are no other main menu UI elementss
        }
        return null; // this is here to make the compiler be quiet
    }

    /**
     * Creates the new game sub-menu UI elements
     */
    private void createNewGameUI() {
        if (!this.newGameUICreated) { // only create the elements if they haven't been added to the ROC yet

            // create list object for story selection
            List<Story> stories = Story.getStories(); // get all available stories to play
            List<ListObject.ListItem> listItems = new ArrayList<>(); // create an empty list of list items
            for (Story s : stories) { // for each story
                Story.StoryListItem sli = new Story.StoryListItem(s); // create a story list item
                sli.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when clicked,
                    /* perform a check to make sure the starting area is valid before continuing to create a new game
                       with the selected story */
                    Node startingArea = s.isResRelative() ? (Node.resToNode(s.getAbsStartingAreaPath()))
                            : Node.fileToNode(s.getAbsStartingAreaPath(), false); // get starting area
                    if (startingArea == null) // if there is no starting area node-file at the provided path
                        Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("starting_area",
                                "Story", s.getStartingAreaPath() + " is an invalid path",
                                false)), "logic.MenuLogic", "createNewGameUI()", true); // crash
                    else { // otherwise
                        toTransfer = new Node(); // instantiate the transfer data node to world logic
                        toTransfer.addChild(s.toNode()); // add story to transfer data
                        this.hide(TAG_STORY_LIST); // hide the story list
                        this.hide(TAG_PROMPT); // hide the prompt
                        this.phase = 24; // and transition to the name input phase of the new game sub-menu
                    }
                });
                listItems.add(sli); // add the formatted story list item to the list items list
            }

            // create a list text button to open the data directory stories folder
            ListTextButton openStoryFolder = new ListTextButton(Global.FONT, "(open stories folder)");
            openStoryFolder.setScale(0.4f, 0.4f); // scale the button down by a little over half
            openStoryFolder.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when clicked
                Utils.openNativeFileExplorer(Utils.getDataDir() + "/stories/"); // open stories dir in data dir
            });
            listItems.add(openStoryFolder); // add open story folder button to the list items list

            // create and add a list object containing the list items with a opaque black background
            ListObject lo = new ListObject(listItems, 0.05f, new Material(new float[]{0f, 0f, 0f, 0.4f}));
            this.roc.addStaticObject(lo, TAG_STORY_LIST, false, new ROC.PositionSettings(0f, belowWindow(),
                    false, 0f)); // add the story list at the center of the screen

            // create and add a text object prompting the player to select a story at the top of the window
            this.roc.addStaticObject(new TextObject(Global.FONT, "Choose a story:"), TAG_PROMPT, false,
                    new ROC.PositionSettings(0f, belowWindow(), true, 0.2f));

            // create and add a button to return to the main menu
            TextButton returnButton = new TextButton(Global.FONT, "Return");
            returnButton.setScale(0.8f, 0.8f); // scale return button down slightly
            returnButton.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when clicked,
                this.roc.moveStaticObject(TAG_TITLE, new ROC.PositionSettings(0f, MM_TITLE_POS, false,
                        0f), TRANS_SPEED); // move the title back down to main menu position
                this.hideNewGameUI(); // hide the new game UI
                this.phase = 19; // and go to phase 19 to transition back to the main menu
            });
            this.roc.addStaticObject(returnButton, TAG_RETURN, false, new ROC.PositionSettings(0f,
                    belowWindow(), false, 0f)); // add return button at bottom

            // create and add name input text input object for player name select
            this.nameInput = new TextInputObject(window, Global.FONT, "(name)", // default text is (name)
                    Global.getThemeColor(Global.ThemeColor.GRAY), // default text will be gray
                    Global.getThemeColor(Global.ThemeColor.GREEN)); // input text will be green
            this.nameInput.setScale(1.5f, 1.5f); // scale name input up a by about 3/2
            this.nameInput.setAcceptInput(false); // do not accept input until at the correct phase
            this.nameInput.setMinLength(MIN_NAME_LENGTH); // set the minimum length to be the minimum name length
            this.nameInput.setMaxLength(MAX_NAME_LENGTH); // set the maximum length to be the maximum name length
            this.nameInput.setEnterCallback(() -> { // when a valid name is chosen,
                this.hideNewGameUI(); // hide the new game UI
                this.roc.moveStaticObject(TAG_TITLE, new ROC.PositionSettings(0f, 0f, false,
                        0f), TRANS_SPEED * 2f); // move the title to the center of the window
                this.hide(TAG_NAME); // hide the name input text input object
                this.nameInput.setAcceptInput(false); // do not accept input anymore
                this.toTransfer.addChild("name", this.nameInput.getText()); // save player name to transfer data
                this.phase = 27; // go to phase 27 to transition to world logic
            });
            this.roc.addStaticObject(this.nameInput, TAG_NAME, false, new ROC.PositionSettings(0f,
                    belowWindow(), false, 0f)); // add name input text input object to ROC

            // create and add finish button for name input
            TextButton finish = new TextButton(Global.FONT, "Accept"); // create a button to pressed when done
            finish.setScale(0.8f, 0.8f); // scale finish button down sslightly
            finish.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when clicked,
                // simulate enter being pressed on the name input because this will automatically check for validity
                this.nameInput.keyboardInput(GLFW_KEY_ENTER, GLFW_RELEASE);
            });
            this.roc.addStaticObject(finish, TAG_FINISH, false, new ROC.PositionSettings(0f, belowWindow(),
                    false, 0f)); // add finish button to ROC

            // flag that new game UI has been created to avoid doing it again
            this.newGameUICreated = true;
        }
    }

    /**
     * Hides all new game UI by flying it out below. This is called when the return button is pressed within the
     * new game sub-menu
     */
    private void hideNewGameUI() {
        this.hide(TAG_STORY_LIST); // hide the story list
        this.hide(TAG_PROMPT); // hide the prompt
        this.hide(TAG_RETURN); // hide the return button
        this.hide(TAG_FINISH); // hid the finish button
        this.hide(TAG_NAME); // hide the name input
    }

    /**
     * Handles keyboard input by forwarding it to the name input text input object if it is instantiated
     *
     * @param key    the key in question
     * @param action the action of the key (GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT)
     */
    @Override
    public void keyboardInput(int key, int action) {
        if (this.nameInput != null) this.nameInput.keyboardInput(key, action); // delegate to name input if not null
    }

    /**
     * Responds to mouse input by speeding along the main menu animation process
     *
     * @param x      the normalized and de-aspected x position of the mouse if hover event, 0 otherwise
     * @param y      the normalized and de-aspected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     */
    @Override
    public void mouseInput(float x, float y, int action) {
        super.mouseInput(x, y, action); // handle regular logic mouse inputs
        if (action == GLFW_PRESS) { // if input was a click
            switch (this.phase) { // speed along the animation depending on the phase
                case 0:
                case 1: // if the physics simulation is pending or occurring
                    this.phase = 2; // skip the physics simulation
                    break;
                case 3: // if the title is moving to its correct position after the physics simulation
                    this.roc.getStaticGameObject(TAG_TITLE).stopPosAnimating(); // stop the position animation
                    // instantaneously move the title to its correct main menu position
                    this.roc.moveStaticObject(TAG_TITLE, getMainMenuUIPosition(TAG_TITLE), 0f);
                    phase = 4; // advance to phase 4
                    break;
            }
        }
    }

    /**
     * Updates the menu logic by ensuring the camera stays within the bounds of the menu area and by performing other
     * updates based on the current phase of the menu logic
     *
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval); // update normal logic members (including ROC)
        if (this.cam != null) { // if the menu area has been created and the camera isn't null
            if (this.cam.getX() > this.maxCamX) this.cam.setVX(-0.5f); // if out of bounds on right, make it move left
            else if (this.cam.getX() < 0f) this.cam.setVX(0.5f); // if out of bounds on left, make it move right
        }
        this.updateByPhase(interval); // perform update based on current phase
    }

    /**
     * Performs updates on the menu logic based on the current phase. This is somewhat messy and the phase numbers are
     * nondescript, so for more information, see the list of phases and their purposes above the definition for the
     * menu logic class about
     *
     * @param interval the amount of time in seconds to account for
     */
    private void updateByPhase(float interval) {
        switch (this.phase) { // switch on the current phases

            case 0: // PHASE 0: wait a little before dropping the title
                time += interval; // keep track of time
                if (time > TRANS_SPEED * 2) { // if time is up
                    time = 0f; // reset timer
                    phase = 1; // advance to phase 1
                    // calculate minimum title y (see members)
                    this.minTitleY = ((Global.ar < 1.0f) ? -1f / Global.ar : -1f) + title.getHeight() / 2;
                }
                break;

            case 1: // PHASE 1: perform the title physics simulation
                time += interval; // keep track of time
                // update title's velocity using gravity and the physics engine's terminal velocity
                titleV = Math.max(titleV - (19.6f * interval), PhysicsEngine.TERMINAL_VELOCITY);
                title.setY(title.getY() + (interval * titleV)); // update the title's y based on velocity
                if (title.getY() < this.minTitleY) { // if title is below the bottom of the screen
                    title.setY(this.minTitleY); // move it back to the bottom
                    titleV = -0.5f * titleV; // and create a bounce
                }
                if (time > TRANS_SPEED * 7) phase = 2; // advance to phase 2 if enough time has passed
                break;

            case 2: // PHASE 2: enable ROC, move title to correct position
                this.renderROC = true; // begin to use the ROC
                this.roc.addStaticObject(title, TAG_TITLE, false, new ROC.PositionSettings(0f, -1f,
                        true, 0f)); // add the title to the ROC at the bottom of the screen
                // but positionally animate it to move to the proper position
                this.roc.moveStaticObject(TAG_TITLE, getMainMenuUIPosition(TAG_TITLE), TRANS_SPEED * 3);
                phase = 3; // advance to phase 3

            case 3: // PHASE 3: wait for title to be in proper menu position
                if (!title.posAnimating()) phase = 4; // once the title is done positionally animating, go to phase 4
                break;

            case 4: // PHASE 4: create buttons, create and fade in menu area
                this.initMenuArea(); // create the menu area
                this.createMainMenuUI(); // create the main menu UI elements
                this.renderROC = true; // enable roc usage
                this.roc.fadeIn(new float[]{0f, 0f, 0f, 1f}, TRANS_SPEED * 10); // fade in the ROC (menu area)
                phase = 5; // advance to phase 5s
                break;

            case 5:
                break; // PHASE 5: wait for user input at the main menu

            case 18: // PHASE 18: wait for main menu UI elements to move back into view
                this.ensureMainMenuButtonPositions(); // make sure all buttons move
                if (!newGame.posAnimating()) this.phase = 5; // if done moving, advance to phase 5
                break;

            case 19: // PHASE 19: return to main menu
                if (!title.posAnimating()) { // once title is done moving back
                    // move new game button back into view (others will follow)
                    this.roc.moveStaticObject(TAG_NEW_GAME, getMainMenuUIPosition(TAG_NEW_GAME), TRANS_SPEED);
                    this.phase = 18; // advance to phase 18
                }
                break;

            case 20: // PHASE 20: move title and hide main menu UI elements
                this.roc.moveStaticObject(TAG_TITLE, new ROC.PositionSettings(0f, 1f, true,
                        0.02f), TRANS_SPEED); // move title to top of screen
                this.hide(TAG_NEW_GAME); // hide the new game button (other buttons will follow)
                this.phase = 21; // advance to phase 21
                break;

            case 21: // PHASE 21: wait for title and main menu UI elements to move
                this.ensureMainMenuButtonPositions(); // make sure the main menu buttons all leave
                if (!title.posAnimating()) this.phase = 22; // when the title is done moving, advance to phase 22
                break;

            case 22: // PHASE 22: create (if not done already) new game story selection UI and bring it into view
                this.createNewGameUI(); // create new game UI elements if not done already
                // make sure the prompt has the story selection message showing
                ((TextObject) this.roc.getStaticGameObject(TAG_PROMPT)).setText("Choose a story:");
                this.roc.moveStaticObject(TAG_STORY_LIST, new ROC.PositionSettings(0f, 0f, false,
                        0f), TRANS_SPEED); // move the story list into view
                this.roc.moveStaticObject(TAG_PROMPT, new ROC.PositionSettings(null, title, 0f, -1f,
                        0.05f), TRANS_SPEED); // move the prompt into view
                this.roc.moveStaticObject(TAG_RETURN, new ROC.PositionSettings(0f, -1f, true,
                        0.02f), TRANS_SPEED); // move the return button into view
                this.phase = 23; // advance to phase 23
                break;

            case 23:
                break; // PHASE 23: wait for user to select a storys

            case 24: // PHASE 24: wait for story selection UI to hide
                // when the story selection UI is done moving, advance to phase 25
                if (!this.roc.getStaticGameObject(TAG_STORY_LIST).posAnimating()) this.phase = 25;
                break;

            case 25: // PHASE 25: bring name selection UI into view
                // adjust prompt to ask for a name
                ((TextObject) this.roc.getStaticGameObject(TAG_PROMPT)).setText("Enter a name:");
                this.nameInput.setAcceptInput(true); // accept name input now
                this.roc.moveStaticObject(TAG_PROMPT, new ROC.PositionSettings(null, title, 0f, -1f,
                        0.06f), TRANS_SPEED); // move prompt into view
                this.roc.moveStaticObject(TAG_NAME, new ROC.PositionSettings(0f, 0f, false,
                        0f), TRANS_SPEED); // move name input text input object into view
                // move finish bbutton into view, above return button
                this.roc.moveStaticObject(TAG_FINISH, new ROC.PositionSettings(null,
                        this.roc.getStaticGameObject(TAG_RETURN), 0f, 1f, 0.05f), TRANS_SPEED);
                this.phase = 26; // advance to phase 26
                break;

            case 26:
                break; // PHASES 26: wait for a valid name to be entered

            case 27: // PHASE 27: wait for UI elements to move before world logic transition
                if (!this.title.posAnimating()) this.phase = 28; // if title is done animating, advance to phase 28
                break;

            case 28: // PHASE 28: fade out ROC and switch to world logic
                if (GameLogic.logicChange == null) { // if there is no current logic data
                    GameLogic.logicChange = new LogicChange(new WorldLogic(), TRANS_SPEED); // change to world logic
                    GameLogic.logicChange.useTransferData(this.toTransfer); // use story's starting area and user's name
                    this.roc.fadeOut(new float[]{0f, 0f, 0f, 0f}, TRANS_SPEED); // fade out the ROC
                    this.phase = 29; // advance to phase 29
                }
                break;

            case 29:
                break; // PHASE 29: wait for world logic to be switched to

            default: // UNRECOGNIZED PHASE
                Utils.handleException(new Exception("Unrecognized menu logic phase: '" + this.phase + "'"),
                        "logic.MenuLogic", "updateByPhase(float)", true); // crash for unrecognized phases
        }
    }

    /**
     * Responds to a resize by updating the minimum y position the title can be if the physics simulation is currently
     * underway
     */
    @Override
    public void resized() {
        super.resized(); // call super's resize handling
        // update minimum title y position if in phase 1 (physics simulation)
        if (this.phase == 1) this.minTitleY = ((Global.ar < 1.0f) ? -1f / Global.ar : -1f) + title.getHeight() / 2;
    }

    /**
     * Renders the title during the physics simulation or when the ROC is fading. See members for more information on
     * why/when this is done
     */
    @Override
    public void renderOthers() {
        // if ROC fading (except for when transition to world logic) or the physics simulation is occurring
        if (phase < 3 || (this.roc.fading() && this.phase != 29)) {
            this.sp.bind(); // bind shader program
            this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
            this.sp.setUniform("ar", Global.ar); // set aspect ratio uniform
            this.sp.setUniform("arAction", Global.arAction ? 1 : 0); // set aspect ratio action uniform
            this.title.render(this.sp); // render the title
            this.sp.unbind(); // unbind the shader program
        }
    }
}
