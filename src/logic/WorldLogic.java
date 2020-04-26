package logic;

import gameobject.ROC;
import gameobject.TextButton;
import gameobject.TextObject;
import gameobject.gameworld.Area;
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
    Window window; // reference to the window for exit button
    WorldObject player; // reference to player
    boolean exitButtonPressed = false; // whether exit has been pressed

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
        player = new WorldObject(Model.getStdGridRect(1, 2),
                new Material(new float[] { 0.2f, 0.2f, 1.0f, 1.0f})); // create player as 1x2 blue rectangle
        player.setPos(Transformation.getCenterOfCell(new Pair<>(2, 4))); // move to grid cell 2, 4
        player.getPhysicsProperties().rigid = true; // male player rigid
        this.roc.addToWorld(player); // add player to world
        this.roc.getGameWorld().getCam().follow(player); // make camera follow player

        // create another random object
        WorldObject o = new WorldObject(Model.getStdGridRect(1, 1), new Material(
                new float[]{1.0f, 0.0f, 1.0f, 1.0f})); // as a pink square
        // move the object to 3, 1
        o.setPos(Transformation.getCenterOfCell(new Pair<>(1, 3))); // move to grid cell 1 ,3
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
        } else if (key == GLFW_KEY_ENTER && action == GLFW_RELEASE) {
            player.givePosAnim(new PositionalAnimation(1.5f, 4.5f, player.getRotationRad(), 1.0f));
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
        if (action == GLFW_HOVERED) { // if hover event
            Pair<Float> pos = new Pair<>(x, y); // create pair of mouse position
            Transformation.useCam(pos, this.roc.getGameWorld().getCam()); // transform to camera-view
            if (player.getFittingBox().contains(pos.x, pos.y)) { // if mouse is hovering player
                player.setMaterial(new Material(new float[]{(float) Math.random(), (float) Math.random(),
                        (float) Math.random(), 1.0f})); // change player color when mouse hovers
            }
        }
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
        if (window.isKeyPressed(GLFW_KEY_D)) player.incrementVX(4); // rightwards movement
        if (window.isKeyPressed(GLFW_KEY_A)) player.incrementVX(-4); // leftwards movement
    }
}
