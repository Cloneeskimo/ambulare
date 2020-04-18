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
     * Initializes any GameObjects to be placed in the World or the HUD
     * @param window the Window in use
     */
    @Override
    protected void initOthers(Window window) {

        // create game object components
        Model m = Model.getStdSquare(); // get standard square model
        Material pMat = new Material(new float[] {1.0f, 0.0f, 0.0f, 1.0f}); // player should just be a red square
        Texture dirt = new Texture("/textures/dirt.png"); // create dirt texture
        Material dMat = new Material(dirt, new float[] {1.0f, 1.0f, 1.0f, 0.5f}, Material.BLEND_MODE.MULTIPLICATIVE); // dirt should just be a dirt square
        Material bdMat = new Material(dirt, new float[] {0.4f, 0.4f, 2.0f, 1.0f}, Material.BLEND_MODE.MULTIPLICATIVE); // blue dirt should just be a blue dirt square

        // create game objects
        this.world.addObject(new GameObject(0f, 0f, m, pMat)); // create player
        this.world.getObject(0).setScale(1.4f); // scale player
        this.world.getCam().follow(this.world.getObject(0)); // make cam follow player
        this.world.addObject(new GameObject(1.2f, 0f, m, dMat)); // create dirt
        this.world.addObject(new GameObject(-1.2f, 0f, m, bdMat)); // create blue dirt
        GameObject hudDirt = new GameObject(0f, 0f, m, dMat); // create HUD dirt
        this.hud.addIndependentObject(hudDirt, 1f, 1f, true, 0.05f); // add dirt to HUD
        GameObject hudDirt2 = new GameObject(0f, 0f, m, dMat); // create second HUD dirt
        this.hud.addDependentObject(hudDirt2, hudDirt, -1f, 0f, 0.05f); // add second dirt to HUD
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
        if (window.isKeyPressed(GLFW_KEY_1)) this.hud.getObject(0).setScale(1.0f); // 1 scales first dirt to 1x
        if (window.isKeyPressed(GLFW_KEY_2)) this.hud.getObject(0).setScale(2.0f); // 2 scales second dirt to 2x
        if (window.isKeyPressed(GLFW_KEY_3)) this.renderHUD = !this.renderHUD; // toggle HUD rendering
        if (window.isKeyPressed(GLFW_KEY_4)) this.renderWorld = !this.renderWorld; // toggle World rendering
        if (window.isKeyPressed(GLFW_KEY_H)) this.hud.ensurePlacement(0); // ensure placement of first dirt
        if (window.isKeyPressed(GLFW_KEY_J)) this.hud.ensurePlacement(1); // ensure placement of second dirt
        if (window.isKeyPressed(GLFW_KEY_K)) this.hud.ensureAllPlacements(); // ensure placement of both dirts
    }

}
