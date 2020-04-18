package logic;

import gameobject.GameObject;
import gameobject.TextObject;
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
     * Initializes any GameObjects to be placed in the World or the HUD
     * @param window the Window in use
     */
    @Override
    protected void initOthers(Window window) {
        super.initOthers(window); // call super so that FPS displaying objects are added to HUD

        // create additional world game object components
        Model m = Model.getStdSquare(); // get standard square model
        Material pMat = new Material(new float[] {1.0f, 0.0f, 0.0f, 1.0f}); // player should just be a red square
        Texture dirt = new Texture("/textures/dirt.png"); // create dirt texture
        Material dMat = new Material(dirt, new float[] {1.0f, 1.0f, 1.0f, 1.0f}, Material.BLEND_MODE.MULTIPLICATIVE); // dirt should just be a dirt square

        // create world additional game objects
        this.world.addObject(new GameObject(0f, 0f, m, pMat)); // create player
        this.world.getObject(0).setScale(1.4f); // scale player
        this.world.getCam().follow(this.world.getObject(0)); // make cam follow player
        this.world.addObject(new GameObject(2f, 0f, m, dMat)); // create dirt

        // create player position text object
        TextObject pPos = new TextObject(this.font, "(" + Float.toString(this.world.getObject(0).getX()) + ", "
                + Float.toString(this.world.getObject(0).getY()) + ")"); // create TextObject
        pPos.setScale(0.1f); // scale down
        this.hud.addIndependentObject(pPos, -1f, -1f, true, 0.02f); // add to HUD
    }

    /**
     * Detects input in the Window to update GameObjects accordingly
     * @param window the window
     */
    @Override
    public void input(Window window) {
        this.world.getObject(0).setVX(0f); // reset horizontal velocity to 0
        this.world.getObject(0).setVY(0f); // reset vertical velocity to 0
        if (window.isKeyPressed(GLFW_KEY_W)) this.world.getObject(0).incrementVY(0.05f); // w -> up
        if (window.isKeyPressed(GLFW_KEY_S)) this.world.getObject(0).incrementVY(-0.05f); // s -> down
        if (window.isKeyPressed(GLFW_KEY_D)) this.world.getObject(0).incrementVX(0.05f); // d -> right
        if (window.isKeyPressed(GLFW_KEY_A)) this.world.getObject(0).incrementVX(-0.05f); // a -> left
    }

    /**
     * Updates this WorldLogic
     */
    @Override
    public void update() {
        super.update(); // call GameLogic's update
        if (((TextObject)this.hud.getObject(2)).setText("(" + String.format("%.2f", this.world.getObject(0).getX()) + ", "
                + String.format("%.2f", this.world.getObject(0).getY()) + ")")) { // update player position text
            this.hud.ensurePlacement(2); // ensure placement if actually updated
        }
    }
}
