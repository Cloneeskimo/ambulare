package logic;

import gameobject.*;
import graphics.*;
import utils.CollisionDetector;
import utils.Global;
import utils.Pair;
import utils.Transformation;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Dictates the logic the game will follow when the user is out in the world
 */
public class WorldLogic extends GameLogic {

    /**
     * Data
     */
    GameObject player, dirt; // some game objects

    /**
     * Initializes any members
     * @param window the window
     */
    @Override
    protected void initOthers(Window window) {
        super.initOthers(window); // call super so that FPS displaying objects are added to HUD

        // create and add player
        player = new GameObject(Model.getStdGridRect(1, 2),
                new Material(new float[] {1.0f, 0.2f, 0.2f, 1.0f})); // create player rectangle
        player.setRotRad((float)Math.PI / 4); // rotate
        this.roc.addObject(player); // add to ROC
        this.roc.getCam().follow(player); // tell camera to follow it

        // create and add dirt
        dirt = new GameObject(MultiTexCoordModel.getStdMultiTexGridRect(2, 2, 4),
                new Material(new Texture("/textures/dirt_anim.png"))); // add some dirt
        dirt.setY(2f); // move up a little
        dirt.setX(2f); // and to the right
        dirt.giveTexAnim(4, 0.2f); // give texture animation
        this.roc.addObject(dirt); // add to ROC

        // create and add player position text
        this.roc.addObject(new TextObject(Global.FONT, "(0, 0)"), // player pos text
                new RenderableObjectCollection.PositionSettings(-1f, -1f, true, 0.02f));
        this.roc.getStaticGameObject(2).setScale(0.15f, 0.15f); // scale text down

        // create and add text button
        this.roc.addObject(new TextButton(Global.FONT, "Button", 2),
                new RenderableObjectCollection.PositionSettings(1f, 1f, true,
                0.02f)); // create text button
        this.roc.getStaticGameObject(3).setScale(0.2f, 0.2f); // scale down
        this.roc.ensurePlacement(3);

        // create and add texture button
        this.roc.addObject(new TexturedButton(1, 1, "/textures/button_anim.png", 3,
                4, 5, 0.5f, 2),
                new RenderableObjectCollection.PositionSettings(null,
                this.roc.getStaticGameObject(3), 1.0f,
                -1f, 0.02f)); // create textured button
        this.roc.getStaticGameObject(4).setScale(0.25f, 0.25f); // scale down texture button
        this.roc.ensurePlacement(4);
    }

    /**
     * Gets keyboard input and reacts to it
     * @param window the window
     */
    @Override
    public void input(Window window) {
        player.stop(false); // stop player
        if (window.isKeyPressed(GLFW_KEY_W)) player.incrementVY(2); // upwards movement
        if (window.isKeyPressed(GLFW_KEY_S)) player.incrementVY(-2); // downwards movement
        if (window.isKeyPressed(GLFW_KEY_D)) player.incrementVX(2); // rightwards movement
        if (window.isKeyPressed(GLFW_KEY_A)) player.incrementVX(-2); // leftwards movement
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
            Transformation.useCam(pos, this.roc.getCam()); // transform to camera-view
            if (player.getFrame().contains(pos.x, pos.y)) { // if mouse is hovering player
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
        System.out.println("MIID Clicked: " + MIID); // print the ID of the clicked button
    }

    /**
     * Updates the world logic
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval); // call super update
        if (CollisionDetector.colliding(player, dirt)) {
            player.setPos(0f, 0f); // on collision, return to origin
            this.roc.getCam().setPos(0f, 0f); // and return camera to origin
        }
        if (((TextObject)this.roc.getStaticGameObject(2)).setText("(" + String.format("%.2f", player.getX()) +
                ", " + String.format("%.2f", player.getY()) + ")")) // change player pos text
            this.roc.ensurePlacement(2); // update placement if changed
    }
}
