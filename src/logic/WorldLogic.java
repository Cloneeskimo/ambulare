package logic;

import gameobject.GameObject;
import graphics.Material;
import graphics.Model;
import graphics.Texture;
import graphics.Window;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Dictates the logic the game will follow when the user is out in the world
 */
public class WorldLogic extends GameLogic {

    /**
     * Initializes any members
     * @param window the window
     */
    @Override
    protected void initOthers(Window window) {
        super.initOthers(window); // call super so that FPS displaying objects are added to HUD

        // create world objects
        GameObject player = new GameObject(Model.getStdGridRect(1, 2),
                new Material(new float[] {1.0f, 0f, 0f, 1.0f})); // create player rectangle
        this.roc.addObject(player); // add to ROC
        this.roc.getCam().follow(player); // tell camera to follow it
        GameObject other = new GameObject(Model.getStdGridRect(2, 3),
                new Material(new Texture("/textures/dirt.png"))); // add some dirt
        other.setY(-2.7f); // move down a little
        other.setX(4.2f); // and to the right
        this.roc.addObject(other); // add to ROC
    }

    /**
     * Gets keyboard input and reacts to it
     * @param window the window
     */
    @Override
    public void input(Window window) {

        // these controls mutate the player rectangle mode
        if (window.isKeyPressed(GLFW_KEY_1)) this.roc.getWorldGameObject(0).setScale(2.0f, 2.0f);
        if (window.isKeyPressed(GLFW_KEY_2)) this.roc.getWorldGameObject(0).setScale(1.0f, 1.0f);
        if (window.isKeyPressed(GLFW_KEY_3)) this.roc.getWorldGameObject(0).setRotRad((float)Math.PI / 4);
        if (window.isKeyPressed(GLFW_KEY_4)) this.roc.getWorldGameObject(0).setRotRad(0);

        // these controls move the player rectangle model
        this.roc.getWorldGameObject(0).stop(false);
        if (window.isKeyPressed(GLFW_KEY_W)) this.roc.getWorldGameObject(0).incrementVY(4);
        if (window.isKeyPressed(GLFW_KEY_S)) this.roc.getWorldGameObject(0).incrementVY(-4);
        if (window.isKeyPressed(GLFW_KEY_D)) this.roc.getWorldGameObject(0).incrementVX(4);
        if (window.isKeyPressed(GLFW_KEY_A)) this.roc.getWorldGameObject(0).incrementVX(-4);
    }
}
