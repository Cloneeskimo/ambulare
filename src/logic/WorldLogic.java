package logic;

import gameobject.*;
import graphics.Material;
import graphics.Model;
import graphics.Window;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Dictates the logic the game will follow when the user is out in the world
 */
public class WorldLogic extends GameLogic {

    /**
     * Initializes any game objects to be placed in the world or the HUD
     * @param window the window
     */
    @Override
    protected void initOthers(Window window) {
        super.initOthers(window); // call super so that FPS displaying objects are added to HUD

        // create world objects
        GameObject square = new GameObject(Model.getStdGridRect(1, 3), new Material(new float[] {1.0f, 0f, 0f, 1.0f}));
        this.world.addObject(square);

        this.world.addObject(new Entity("/tile/evildirt.txt"));
        this.world.getObject(1).setRotRad((float)Math.PI / 4);
    }

    @Override
    public void input(Window window) {
        if (window.isKeyPressed(GLFW_KEY_1)) this.world.getObject(0).setScale(2.0f, 2.0f);
        if (window.isKeyPressed(GLFW_KEY_2)) this.world.getObject(0).setScale(1.0f, 1.0f);
        if (window.isKeyPressed(GLFW_KEY_3)) this.world.getObject(0).setRotRad((float)Math.PI / 4);
        if (window.isKeyPressed(GLFW_KEY_4)) this.world.getObject(0).setRotRad(0);
    }
}
