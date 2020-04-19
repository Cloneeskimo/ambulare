package logic;

import gameobject.*;
import graphics.PositionalAnimation;
import graphics.Window;
import utils.Coord;
import utils.Global;
import utils.Transformation;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Dictates the logic the game will follow when the user is out in the world
 */
public class WorldLogic extends GameLogic {

    /**
     * Data
     */
    Player player; // the player
    Coord lastMousePos = new Coord(); // save last mouse position to avoid re-calculating the same value multiple times

    /**
     * Initializes any game objects to be placed in the world or the HUD
     * @param window the window
     */
    @Override
    protected void initOthers(Window window) {
        super.initOthers(window); // call super so that FPS displaying objects are added to HUD

        // create world objects
        this.player = new Player("/tile/player.txt"); // create player
        this.world.getCam().follow(player); // make camera follow player
        this.world.addObject(player); // add him to the world
        this.world.addObject(new Entity("/tile/evildirt.txt")); // create and add evil dirt
        this.world.addObject(new Tile("/tile/stationarydirt.txt")); // add some stationary dirt

        // create HUD objects
        TextObject pPos = new TextObject(Global.FONT, "(0, 0)"); // create player position text
        pPos.setScale(0.1f); // scale down player position text
        this.hud.addObject(pPos, new HUD.HUDPositionSettings(-1f, -1f, true,
                0.02f)); // put player position text into hud at bottom left
    }

    /**
     * Detects input in the window and updates game objects accordingly
     * @param window the window
     */
    @Override
    public void input(Window window) {
        player.stop(false); // stop moving
        Coord mousePos = window.getMousePos(); // get mouse position
        if (!mousePos.equals(lastMousePos)) { // check if different from last mouse position
            this.lastMousePos = new Coord(mousePos); // save new mouse position
            Transformation.normalize(mousePos, window.getWidth(), window.getHeight()); // normalize mouse position
            Transformation.project(mousePos, (float)window.getWidth() / (float)window.getHeight()); // project
            player.setRotRad((float)Math.atan2(mousePos.y, mousePos.x)); // rotate player accordingly
        }
        if (window.isKeyPressed(GLFW_KEY_W)) player.moveForward(); // move player forward if W pressed
    }

    /**
     * Updates the logic
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval); // call super's update to update world and HUD
        if (((TextObject)this.hud.getObject(2)).setText("(" + String.format("%.2f", player.getX()) + ", " +
                String.format("%.2f", player.getY()) + ")")) // update player position text
            this.hud.ensurePlacement(2); // ensure placement of player pos text if it actually changed
    }
}
