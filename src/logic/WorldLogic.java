package logic;

import gameobject.GameObject;
import gameobject.HUD;
import gameobject.TextObject;
import graphics.*;

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

        // create informative message
        TextObject info = new TextObject(this.font, "(1) - return to origin; (2) - double pos; (3) - halve pos; (4) - reload HUD; (5) - move pos; (6) - return pos;"); // create game name
        info.setScale(0.1f); // scale down
        this.hud.addObject(info, new HUD.HUDPositionSettings(-1f, -1f, true, 0.02f)); // add to bottom left

        // create more text
        TextObject pPos = new TextObject(this.font, "(0, 0)"); // create player position text
        pPos.setScale(0.1f); // scale down
        this.hud.addObject(pPos, new HUD.HUDPositionSettings(null, info, -1f, 1f, 0.02f)); // put pos above info text
    }

    /**
     * Detects input in the Window to update GameObjects accordingly
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
        if (window.isKeyPressed(GLFW_KEY_1)) this.world.getObject(0).givePosAnim(new PositionalAnimation(0f, 0f, 1f));
        if (window.isKeyPressed(GLFW_KEY_2)) this.hud.getObject(3).setScale(0.2f);
        if (window.isKeyPressed(GLFW_KEY_3)) this.hud.getObject(3).setScale(0.1f);
        if (window.isKeyPressed(GLFW_KEY_4)) this.hud.ensureAllPlacements();
        if (window.isKeyPressed(GLFW_KEY_5)) {
            this.hud.moveObject(3, new HUD.HUDPositionSettings(1f, 1f, true, 0.02f), 1f);
        }
        if (window.isKeyPressed(GLFW_KEY_6)) {
            this.hud.moveObject(3, new HUD.HUDPositionSettings(null, this.hud.getObject(2), -1f, 1f, 0.02f), 1f);
        }
    }

    /**
     * Updates this WorldLogic
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval); // call GameLogic's update
        ((TextObject)this.hud.getObject(3)).setText("(" + String.format("%.2f", this.world.getObject(0).getX()) + ", " +
                String.format("%.2f", this.world.getObject(0).getY()) + ")");
        if (!this.hud.getObject(3).posAnimating()) this.hud.ensurePlacement(3);
    }
}
