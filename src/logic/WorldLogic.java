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
        Player hudPlayer = new Player("/tile/player.txt"); // create small player for HUD
        hudPlayer.setScale(0.5f);
        this.hud.addObject(hudPlayer, new HUD.HUDPositionSettings(1f, -1f, true, 0.2f));
    }

    /**
     * Detects input in the window and updates game objects accordingly
     * @param window the window
     */
    @Override
    public void input(Window window) {
        player.stop(false); // stop moving
        if (window.isKeyPressed(GLFW_KEY_W)) player.moveForward(); // move player forward if W pressed
    }

    /**
     * Reacts to mouse input by rotating the player to follow the mouse
     * @param x the normalized and projected x position of the mouse if hover event, 0 otherwise
     * @param y the normalized and projected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     */
    @Override
    public void mouseInput(float x, float y, int action) {
        super.mouseInput(x, y, action); // call super
        if (action == GLFW_HOVERED) {
            Coord pos = new Coord(x, y); // bundle into coordinate
            Transformation.useCam(pos, this.world.getCam()); // convert to camera-view coordinates
            player.setRotRad((float)Math.atan2(pos.y - player.getY(),
                    pos.x - player.getX())); // rotate player if mouse movement event
        }
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
