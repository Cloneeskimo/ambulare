package logic;

import gameobject.Entity;
import gameobject.HUD;
import gameobject.TextObject;
import gameobject.Tile;
import graphics.PositionalAnimation;
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
        this.world.addObject(new Tile("/tile/player.txt")); // add player tile
        this.world.addObject(new Entity("/tile/evildirt.txt")); // add evil dirt
        this.world.addObject(new Tile("/tile/stationarydirt.txt")); // add stationary dirt
        this.world.getCam().follow(this.world.getObject(0)); // make camera follow player
        TextObject pPos = new TextObject(this.font, "(0, 0)"); // create player position text
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
        this.world.getObject(0).setVX(0f); // reset horizontal velocity to 0
        this.world.getObject(0).setVY(0f); // reset vertical velocity to 0
        if (window.isKeyPressed(GLFW_KEY_W)) this.world.getObject(0).incrementVY(2f); // w -> up
        if (window.isKeyPressed(GLFW_KEY_S)) this.world.getObject(0).incrementVY(-2f); // s -> down
        if (window.isKeyPressed(GLFW_KEY_D)) this.world.getObject(0).incrementVX(2f); // d -> right
        if (window.isKeyPressed(GLFW_KEY_A)) this.world.getObject(0).incrementVX(-2f); // a -> left
    }

    /**
     * Updates the logic
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval); // call super's update to update world and HUD
        ((TextObject)this.hud.getObject(2)).setText("(" + String.format("%.2f", this.world.getObject(0).getX()) +
                ", " + String.format("%.2f", this.world.getObject(0).getY()) + ")"); // update player position text
        if (!this.hud.getObject(2).posAnimating()) { // if player pos text is not animating, animate it
            this.hud.getObject(2).givePosAnim(new PositionalAnimation(null, null, 360f, 3f));
        }
        this.hud.ensurePlacement(2); // ensure placement of player pos text
    }
}
