package logic;

import gameobject.ROC;
import gameobject.TextButton;
import gameobject.TextObject;
import gameobject.gameworld.WorldObject;
import graphics.Material;
import graphics.Model;
import graphics.Texture;
import graphics.Window;
import utils.Global;
import utils.Pair;
import utils.PhysicsEngine;
import utils.Transformation;

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
     * @param window the window
     */
    @Override
    protected void initOthers(Window window) {
        super.initOthers(window); // call super so that FPS displaying objects are added to HUD
        this.window = window; // save reference to window

        // create and add player
        player = new WorldObject(Model.getStdGridRect(1, 2),
                new Material(new float[] {0.0f, 0.0f, 1.0f, 1.0f})); // create as a 1 x 2 blue rectangle
        player.setPos(5, 2f); // set player's position
        player.getPhysicsProperties().rigid = true; // make rigid
        this.roc.getGameWorld().addObject(player); // add to ROC
        this.roc.getGameWorld().getCam().follow(player); // tell camera to follow it

        // create another random object
        WorldObject o = new WorldObject(Model.getStdGridRect(1, 1), new Material(
                new float[] {1.0f, 0.0f, 1.0f, 1.0f})); // as a pink square
        // move the object to 3, 1
        o.setX(3f);
        o.setY(1f);
        this.roc.getGameWorld().addObject(o); // add to ROC

        // create and add dirt floor and walls
        Material dirt = new Material(new Texture("/textures/dirt.png")); // dirt material
        for (int i = 0; i < 10; i++) { // this loop builds the dirt floor and walls
            WorldObject po = new WorldObject(Model.getStdGridRect(1, 1), dirt); // create floor dirt
            po.getPhysicsProperties().gravity = 0f; // disable gravity on dirt
            po.setX(i * Global.GRID_CELL_SIZE); // calculate x
            po.getPhysicsProperties().rigid = true; // make dirt rigid
            this.roc.getGameWorld().addObject(po); // add to ROC
            po = new WorldObject(Model.getStdGridRect(1, 1), dirt); // create wall dirt
            po.getPhysicsProperties().gravity = 0f; // disable gravity on dirt
            po.setX(i > 4 ? 9 * Global.GRID_CELL_SIZE : 0 * Global.GRID_CELL_SIZE); // calculate x
            po.setY(1 + (i > 4 ? ((i - 4) * Global.GRID_CELL_SIZE) : i * Global.GRID_CELL_SIZE)); // calculate y
            po.getPhysicsProperties().rigid = true; // make dirt rigid
            this.roc.getGameWorld().addObject(po); // add to ROC
        }

        // create and add player position text
        this.roc.addStaticObject(new TextObject(Global.FONT, "(0, 0)"), // player pos text
                new ROC.PositionSettings(-1f, -1f, true, 0.02f));
        this.roc.getStaticGameObject(2).setScale(0.05f, 0.05f); // scale text down

        // create and add text button
        this.roc.addStaticObject(new TextButton(Global.FONT, "Exit", 1),
                new ROC.PositionSettings(1f, -1f, true,
                0.02f)); // create text button
        this.roc.getStaticGameObject(3).setScale(0.05f, 0.05f); // scale button down
        this.roc.ensureAllPlacements(); // ensure ROC static object placements
    }

    /**
     * Responds to keyboard input by moving the player
     * @param key the key in question
     * @param action the action of the key (GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT)
     */
    @Override
    public void keyboardInput(int key, int action) {
        if (key == GLFW_KEY_SPACE && action == GLFW_PRESS) { // if space is pressed
            if (PhysicsEngine.somethingUnder(player)) player.setVY(10f); // have played jump if there is something under
        }
    }

    /**
     * Reacts to mouse input
     * @param x the normalized and de-aspected x position of the mouse if hover event, 0 otherwise
     * @param y the normalized and de-aspected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     */
    @Override
    public void mouseInput(float x, float y, int action) {
        super.mouseInput(x, y, action); // call super mouse input
        if (action == GLFW_HOVERED) { // if hover event
            Pair pos = new Pair(x, y); // create pair of mouse position
            Transformation.useCam(pos, this.roc.getGameWorld().getCam()); // transform to camera-view
            if (player.getFittingBox().contains(pos.x, pos.y)) { // if mouse is hovering player
                player.setMaterial(new Material(new float[]{(float)Math.random(), (float)Math.random(),
                        (float)Math.random(), 1.0f})); // change player color when mouse hovers
            }
        }
    }

    /**
     * Responds to button clicks by printing out the MIID of the button
     * @param MIID the ID of the object that was clicked
     */
    @Override
    public void clicked(int MIID) {
        if (MIID == 1) exitButtonPressed = true;
    }

    /**
     * Updates the world logic
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        if (exitButtonPressed) window.close(); // if exit was pressed, close window
        super.update(interval);
        Pair pos = new Pair(player.getX(), player.getY()); // pair player position
        Transformation.alignToGrid(pos); // align position to the grid
        if (((TextObject)this.roc.getStaticGameObject(2)).setText("(" + String.format("%.2f", pos.x) +
                ", " + String.format("%.2f", pos.y) + ")")) // change player pos text
            this.roc.ensurePlacement(2); // update placement if changed
        player.setVX(0); // reset player velocity
        if (window.isKeyPressed(GLFW_KEY_D)) player.incrementVX(4); // rightwards movement
        if (window.isKeyPressed(GLFW_KEY_A)) player.incrementVX(-4); // leftwards movement
    }
}
