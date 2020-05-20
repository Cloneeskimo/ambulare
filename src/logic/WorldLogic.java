package logic;

import gameobject.ROC;
import gameobject.gameworld.Area;
import gameobject.gameworld.Entity;
import gameobject.ui.EnhancedTextObject;
import gameobject.ui.TextButton;
import gameobject.ui.TextObject;
import gameobject.ui.TexturedButton;
import graphics.MSAT;
import graphics.Material;
import graphics.Model;
import graphics.Texture;
import story.Story;
import utils.*;

import static org.lwjgl.glfw.GLFW.*;

/*
 * WorldLogic.java
 * Ambulare
 * Jacob Oaks
 * 4/19/20
 */

/**
 * Dictates the logic the game will follow when the user is out in the world. When switching to this logic, transfer
 * data must be provided with the following children: (1) "name": a name to give to the player, (2) "story": the story
 * converted into a node (the result of Story.toNode())
 */
public class WorldLogic extends GameLogic {

    /**
     * ROC (UI Element) Tags
     */
    private static final int TAG_RETURN = 1;     // return button
    private static final int TAG_AREA = 2;       // area name text

    /**
     * Members
     */
    Entity player; // reference to player
    Story story;   // the current story in use

    /**
     * Initializes the world logic by loading the area given by the transfer data
     */
    @Override
    protected void initOthers() {
        super.initOthers(); // call super so that FPS displaying objects are added to HUD
        if (this.transferData == null) { // if no transfer data was given
            Utils.handleException(new Exception("world logic initiated without transfer data. Transfer data is needed" +
                    "to create an area for the game world"), this.getClass(), "initOthers", true); // crash
        }
        this.initPlayer(); // initialize player
        this.initStoryAndArea(); // initialize the story and the area
        player.setPos(Transformation.getCenterOfCell(this.story.getStartingPos())); // move to story's starting position
        this.initHUDObjects(); // initialize hud objects
        Global.resetAccumulator = true; // flag that the engine's accumulator should be reset after expensive loading
        this.roc.fadeIn(new float[]{0f, 0f, 0f, 1f}, 1f); // fade in the ROC
    }

    /**
     * Initializes the current story and starting area
     */
    private void initStoryAndArea() {
        this.story = new Story(transferData.getChild("story")); // create story from transfer data node
        Node startingArea = Node.pathContentsToNode(this.story.getStartingAreaPath()); // get starting area node
        this.roc.useGameWorld(new Area(startingArea), this.player); // create game world with area
        this.roc.getGameWorld().useStory(this.story); // give the story to the game world
        this.roc.getGameWorld().useAreaChangeCallback(() -> { // when the area changes, update the area name text
            ((TextObject)this.roc.getStaticGameObject(TAG_AREA)).setText(this.roc.getGameWorld().getArea().getName());
        });
    }

    /**
     * Initializes the player
     */
    private void initPlayer() {
        // create player entity
        Sound[] playerStepSounds = new Sound[]{ // create player step sounds
                new Sound(new Utils.Path("/sounds/step1.ogg", true)),
                new Sound(new Utils.Path("/sounds/step2.ogg", true)),
                new Sound(new Utils.Path("/sounds/step3.ogg", true)),
                new Sound(new Utils.Path("/sounds/step4.ogg", true))
        };
        Sound playerJumpSound = new Sound(new Utils.Path("/sounds/jump1.ogg", true)); // jump sound
        Sound playerLandSound = new Sound(new Utils.Path("/sounds/land1.ogg", true)); // land sound
        player = new Entity(this.transferData.getChild("name").getValue(), Model.getStdGridRect(1, 2),
                new Material(new MSAT(new Utils.Path("/textures/entity/player.png", true),
                        new MSAT.MSATState[]{
                                new MSAT.MSATState(2, 0.5f),
                                new MSAT.MSATState(1, 1f),
                                new MSAT.MSATState(12, 0.035f)
                        })));
        player.useSounds(playerStepSounds, playerJumpSound, playerLandSound); // give sounds to player
        // lower player bounding width slightly to fit better and appear more aesthetically
        player.setBoundingWidth(0.9f);
        player.setBoundingHeight(0.9f);
        player.getPhysicsProperties().rigid = true; // make player rigid
    }

    /**
     * Initializes the world logic's hud objects
     */
    private void initHUDObjects() {

        // create and add return button
        TexturedButton returnButton = new TextButton(Global.font, "Return").solidify(); // create return button
        returnButton.setScale(0.6f, 0.6f); // scale return button down by about half
        returnButton.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { // when pressed
            if (GameLogic.logicChange == null) { // if no logic change is currently underway
                GameLogic.logicChange = new LogicChange(new MenuLogic(), 0.5f); // change to main menu
                this.roc.fadeOut(new float[]{0f, 0f, 0f, 0f}, 0.5f); // fade out ROC
            }
        });
        this.roc.addStaticObject(returnButton, TAG_RETURN, false, new ROC.PositionSettings(1f, -1f,
                true, 0.02f)); // add return button to ROC

        // create and add area name
        this.roc.addStaticObject(new TextObject(Global.font, this.roc.getGameWorld().getArea().getName()), TAG_AREA,
                false, new ROC.PositionSettings(1f, 1f, true, 0.04f));
        this.roc.getStaticGameObject(TAG_AREA).setScale(0.8f, 0.8f); // scale area name

        // ensure all hud items are placed correctly
        this.roc.ensureAllPlacements();
    }

    /**
     * Responds to keyboard input by moving the player
     *
     * @param key    the key in question
     * @param action the action of the key (GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT)
     */
    @Override
    public void keyboardInput(int key, int action) {
        if (key == GLFW_KEY_SPACE && action == GLFW_PRESS) this.player.attemptJump(); // space -> jump
        this.roc.getGameWorld().keyboardInput(key, action); // give keyboard input to game world
    }

    /**
     * Reacts to mouse input
     *
     * @param x      the x of the mouse in window coordinates, or the horizontal scroll factor if scrolling input
     * @param y      the y of the mouse in window coordinates, or the vertical scroll factor if scrolling input
     * @param action the action of the mouse (GLFW_HOVER, GLFW_PRESS, GLFW_RELEASE, or SCROLL)
     * @param x      the normalized and de-aspected x position of the mouse if hover event, 0 otherwise
     * @param y      the normalized and de-aspected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     */
    @Override
    public void mouseInput(float x, float y, int action) {
        super.mouseInput(x, y, action); // call super mouse input
    }

    /**
     * Updates the world logic
     *
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval); // update game logic members

        /*
         * Update Player
         */
        int vx = 0; // calculate player horizontal velocity starting at zero
        if (Global.gameWindow.isKeyPressed(GLFW_KEY_D)) vx += 4; // if D is pressed, move player to the right
        if (Global.gameWindow.isKeyPressed(GLFW_KEY_A)) vx -= 4; // if A is pressed, move player to the left
        player.setVX(vx); // update player's horizontal velocity
        if (vx == 0) player.setIsMoving(false); // if the horizontal velocity is zero, update player's moving flag
        else { // otherwise
            player.setIsMoving(true); // flag that the player is moving
            player.setFacing(vx > 0); // make player face the correct direction
        }

        /*
         * Update Debug Info
         */
        if (Global.debugInfo.visible()) { // if the debug info is visible
            // update player position on the debug info
            Global.debugInfo.setField("pos", "(" + player.getX() + ", " + player.getY() + ")");
            // update player velocity on the debug info
            Global.debugInfo.setField("v", "(" + player.getVX() + ", " + player.getVY() + ")");
            // update the slope field on the debug info
            PhysicsEngine.SlopeType slope = player.getPhysicsProperties().onSlope();
            Global.debugInfo.setField("slope", slope != null ? slope.toString() : "false");
        }
    }

    /**
     * Cleans up by removing fields from the debug info that are specific to world logic
     */
    @Override
    public void cleanup() {
        super.cleanup(); // cleanup normal game logic members
        Global.debugInfo.removeField("pos"); // remove player position field
        Global.debugInfo.removeField("v"); // remove player velocity field
        Global.debugInfo.removeField("slope"); // remove slope field
    }
}
