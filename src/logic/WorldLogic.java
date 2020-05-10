package logic;

import gameobject.ROC;
import gameobject.ui.TextButton;
import gameobject.ui.TextObject;
import gameobject.gameworld.Area;
import gameobject.gameworld.Entity;
import graphics.*;
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
     * ROC (UI Element) Tagss
     */
    private static final int TAG_PLAYER_POS = 0; // player position text
    private static final int TAG_RESET = 1;      // reset message text
    private static final int TAG_RETURN = 2;     // return button
    private static final int TAG_AREA = 3;       // area name text

    /**
     * Members
     */
    Window window; // reference to the window for exit button
    Entity player; // reference to player
    Story story;   // the current story in use

    /**
     * Initializes the world logic by loading the area given by the transfer data
     *
     * @param window the window
     */
    @Override
    protected void initOthers(Window window) {
        super.initOthers(window); // call super so that FPS displaying objects are added to HUD
        this.window = window; // save reference to window
        if (this.transferData == null) { // if no transfer data was given
            Utils.handleException(new Exception("world logic initiated without transfer data. Transfer data is needed" +
                    "to create an area for the game world"), "logic.WorldLogic", "initOthers(Window)", true); // crash
        }
        this.initStoryAndArea(); // initialize the story and the area
        this.initWorldObjects(); // initialize world objects
        this.initHUDObjects(); // initialize hud objects
        this.roc.fadeIn(new float[]{0f, 0f, 0f, 1f}, 1f); // fade in the ROC
    }

    /**
     * Initializes the current story and starting area
     */
    private void initStoryAndArea() {
        this.story = new Story(transferData.getChild("story")); // create story from transfer data node
        Node startingArea = story.isResRelative() ? (Node.resToNode(story.getAbsStartingAreaPath()))
                : Node.fileToNode(story.getAbsStartingAreaPath(), false); // get starting area node
        this.roc.useGameWorld(window.getHandle(), new Area(startingArea, this.window)); // create game world with area
    }

    /**
     * Initializes the world logic's world objects
     */
    private void initWorldObjects() {
        // create player entity
        player = new Entity(this.transferData.getChild("name").getValue(), Model.getStdGridRect(1, 2),
                new LightSourceMaterial(new MSAT("/textures/entity/player.png", true, new MSAT.MSATState[]{
                        new MSAT.MSATState(2, 0.5f),
                        new MSAT.MSATState(1, 1f),
                        new MSAT.MSATState(12, 0.025f)
                }), new LightSource(new float[]{1f, 1f, 1f, 1f}, 5f, 1.5f)));
        // lower player bounding width slightly to fit better and appear more aesthetically
        player.setBoundingWidth(0.9f);
        player.setBoundingHeight(0.9f);
        player.setPos(Transformation.getCenterOfCell(this.story.getStartingPos())); // move to story's starting position
        player.getPhysicsProperties().rigid = true; // make player rigid
        this.roc.addToWorld(player); // add player to world
        this.roc.getGameWorld().getCam().follow(player); // make camera follow player

        // create a random object
        Entity o = new Entity("Ball", Model.getStdGridRect(1, 1), new Material(
                new float[]{1.0f, 0.0f, 0.0f, 1.0f})); // as a pink square
        o.setScale(0.3f, 0.3f); // scale down a lot
        o.getPhysicsProperties().bounciness = 0.9f; // make very bouncy
        o.getPhysicsProperties().mass = 0.6f; // make it light
        o.setPos(Transformation.getCenterOfCell(new Pair<>(2, 9))); // move to grid cell 2, 7
        this.roc.addToWorld(o); // add to ROC

        // create another random object
        o = new Entity("Ball", Model.getStdGridRect(1, 1), new Material(
                new float[]{1.0f, 0.0f, 0.0f, 1.0f})); // as a pink square
        o.setScale(0.3f, 0.3f); // scale down a lot
        o.getPhysicsProperties().bounciness = 0.9f; // make very bouncy
        o.getPhysicsProperties().mass = 0.6f; // make it light
        o.setPos(Transformation.getCenterOfCell(new Pair<>(0, 9))); // move to grid cell 5, 7
        this.roc.addToWorld(o); // add to ROC
    }

    /**
     * Initializes the world logic's hud objects
     */
    private void initHUDObjects() {

        // create and add player position text
        this.roc.addStaticObject(new TextObject(Global.FONT, "(0, 0)"), TAG_PLAYER_POS, false,
                new ROC.PositionSettings(-1f, -1f, true, 0.02f)); // create pos text
        this.roc.getStaticGameObject(TAG_PLAYER_POS).setScale(0.6f, 0.6f); // scale text down

        // create and add return button
        TextButton returnButton = new TextButton(Global.FONT, "Return"); // create return button
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
        this.roc.addStaticObject(new TextObject(Global.FONT, this.roc.getGameWorld().getArea().getName()), TAG_AREA,
                false, new ROC.PositionSettings(0f, 1f, true, 0.1f));
        this.roc.getStaticGameObject(TAG_AREA).setScale(0.8f, 0.8f); // scale area name

        // create and add reset info
        this.roc.addStaticObject(new TextObject(Global.FONT, "Press enter to reset"), TAG_RESET, false,
                new ROC.PositionSettings(0f, -1f, true, 0.1f)); // create and add
        this.roc.getStaticGameObject(TAG_RESET).setScale(0.7f, 0.7f); // scale area name

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
        if (key == GLFW_KEY_SPACE && action == GLFW_PRESS) { // if space is pressed
            if (PhysicsEngine.nextTo(player, 0f, -1f)) player.setVY(10f); // jump if there is something under
        } else if (key == GLFW_KEY_ENTER && action == GLFW_RELEASE) { // if enter is pressed
            player.givePosAnim(new PositionalAnimation(1.5f, 9.5f, null, 1.0f)); // reset player
            // reset first random object
            roc.getGameWorld().getWorldObject(1).givePosAnim(new PositionalAnimation(0.5f, 9.5f, null,
                    1.0f));
            // reset second random object
            roc.getGameWorld().getWorldObject(2).givePosAnim(new PositionalAnimation(2.5f, 9.5f, null,
                    1.0f));
        }
    }

    /**
     * Reacts to mouse input
     *
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
        // update player position text on hud
        if (((TextObject) this.roc.getStaticGameObject(TAG_PLAYER_POS)).setText(player.getX() + ", " + player.getY() + ")"))
            this.roc.ensurePlacement(TAG_PLAYER_POS); // update placement if changed
        int vx = 0; // calculate player horizontal velocity starting at zero
        if (window.isKeyPressed(GLFW_KEY_D)) vx += 4; // if D is pressed, move player to the right
        if (window.isKeyPressed(GLFW_KEY_A)) vx -= 4; // if A is pressed, move player to the left
        player.setVX(vx); // update player's horizontal velocity
        if (vx == 0) player.setIsMoving(false); // if the horizontal velocity is zero, update player's moving flag
        else { // otherwise
            player.setIsMoving(true); // flag that the player is moving
            player.setFacing(vx > 0); // make player face the correct direction
        }
    }
}
