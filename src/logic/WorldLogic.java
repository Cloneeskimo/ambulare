package logic;

import gameobject.ROC;
import gameobject.TextButton;
import gameobject.TextObject;
import gameobject.gameworld.Area;
import gameobject.gameworld.Entity;
import gameobject.gameworld.WorldObject;
import graphics.*;
import utils.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Dictates the logic the game will follow when the user is out in the world
 */
public class WorldLogic extends GameLogic {

    /**
     * Members
     */
    Window window;                      // reference to the window for exit button
    Entity player;                      // reference to player
    boolean exitButtonPressed = false;  // whether exit has been pressed

    /**
     * Initializes any members
     *
     * @param window the window
     */
    @Override
    protected void initOthers(Window window) {
        super.initOthers(window); // call super so that FPS displaying objects are added to HUD
        this.window = window; // save reference to window
        // create game world with starting area
        this.roc.useGameWorld(window.getHandle(), new Area(Node.resToNode("/mainstory/areas/area.amb")));

        // create and add player
        Texture left = new Texture("/textures/player/player_left.png", true);
        Texture right = new Texture("/textures/player/player_right.png", true);
        player = new Entity(Model.getStdGridRect(1, 2),
                new Entity.EntityMaterial(left, right, right, left));
        player.setBoundingWidth(0.95f);
        player.setBoundingHeight(0.95f);
        player.setPos(Transformation.getCenterOfCell(new Pair<>(3, 5))); // move to grid cell 3, 5
        player.getPhysicsProperties().rigid = true; // male player rigid
        this.roc.addToWorld(player); // add player to world
        this.roc.getGameWorld().getCam().follow(player); // make camera follow player

        // create a random object
        WorldObject o = new WorldObject(Model.getStdGridRect(1, 1), new Material(
                new float[]{1.0f, 0.0f, 0.0f, 1.0f})); // as a pink square
        o.setScale(0.3f, 0.3f); // scale down a lot
        o.getPhysicsProperties().bounciness = 0.9f; // make very bouncy
        o.getPhysicsProperties().mass = 0.6f; // make it light
        o.setPos(Transformation.getCenterOfCell(new Pair<>(2, 7))); // move to grid cell 2, 7
        this.roc.addToWorld(o); // add to ROC

        // create another random object
        o = new WorldObject(Model.getStdGridRect(1, 1), new Material(
                new float[]{1.0f, 0.0f, 0.0f, 1.0f})); // as a pink square
        o.setScale(0.3f, 0.3f); // scale down a lot
        o.getPhysicsProperties().bounciness = 0.9f; // make very bouncy
        o.getPhysicsProperties().mass = 0.6f; // make it light
        o.setPos(Transformation.getCenterOfCell(new Pair<>(5, 7))); // move to grid cell 5, 7
        this.roc.addToWorld(o); // add to ROC

        // create and add player position text
        this.roc.addStaticObject(new TextObject(Global.FONT, "(0, 0)"), // player pos text
                new ROC.PositionSettings(-1f, -1f, true, 0.02f));
        this.roc.getStaticGameObject(2).setScale(0.6f, 0.6f); // scale text down

        // create and add text button
        this.roc.addStaticObject(new TextButton(Global.FONT, "Exit", 1),
                new ROC.PositionSettings(1f, -1f, true,
                        0.02f)); // create text button
        this.roc.getStaticGameObject(3).setScale(0.6f, 0.6f); // scale button down
        this.roc.ensureAllPlacements(); // ensure ROC static object placements

        // create and add area name
        this.roc.addStaticObject(new TextObject(Global.FONT, this.roc.getGameWorld().getArea().getName()),
                new ROC.PositionSettings(0f, 1f, true, 0.1f)); // create area name
        this.roc.getStaticGameObject(4).setScale(0.8f, 0.8f); // scale area name

        // create and add reset info
        this.roc.addStaticObject(new TextObject(Global.FONT, "Press enter to reset"),
                new ROC.PositionSettings(0f, -1f, true, 0.1f)); // create reset info
        this.roc.getStaticGameObject(5).setScale(0.7f, 0.7f); // scale area name

        // fade in
        this.roc.fadeIn(new float[]{0.0f, 0.0f, 0.0f, 1.0f}, 3f);
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
            player.givePosAnim(new PositionalAnimation(3.5f, 5.5f, null, 1.0f)); // reset player
            // reset first random object
            roc.getGameWorld().getWorldObject(1).givePosAnim(new PositionalAnimation(2.5f, 7.5f, null,
                    1.0f));
            // reset second random object
            roc.getGameWorld().getWorldObject(2).givePosAnim(new PositionalAnimation(5.5f, 7.5f, null,
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
     * Responds to button clicks by printing out the MIID of the button
     *
     * @param MIID the ID of the object that was clicked
     */
    @Override
    public void clicked(int MIID) {
        if (MIID == 1) exitButtonPressed = true;
    }

    /**
     * Updates the world logic
     *
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        if (exitButtonPressed) window.close(); // if exit was pressed, close window
        super.update(interval);
        Pair pos = new Pair(player.getX(), player.getY()); // pair player position
        Transformation.getGridCell(pos);
        if (((TextObject) this.roc.getStaticGameObject(2)).setText("(" + String.format("%.2f", pos.x) +
                ", " + String.format("%.2f", pos.y) + ")")) // change player pos text
            this.roc.ensurePlacement(2); // update placement if changed
        player.setVX(0); // reset player velocity
        if (window.isKeyPressed(GLFW_KEY_D)) {
            player.incrementVX(4); // rightwards movement
            player.setFacing(true);
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            player.incrementVX(-4); // leftwards movement
            player.setFacing(false);
        }
    }
}
