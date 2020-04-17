package logic;

import gameobject.GameObject;
import graphics.Material;
import graphics.Model;
import graphics.Texture;
import graphics.Window;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;

public class WorldLogic extends GameLogic {

    @Override
    protected void initItems(Window window) {

        // create game object components
        Model m = Model.getStdSquare(); // get standard square model
        Material pMat = new Material(new float[] {1.0f, 0.0f, 0.0f, 1.0f}); // player should just be a red square
        Texture dirt = new Texture("/textures/dirt.png"); // create dirt texture
        Material dMat = new Material(dirt, new float[] {1.0f, 1.0f, 1.0f, 0.5f}, Material.BLEND_MODE.MULTIPLICATIVE); // dirt should just be a dirt square
        Material bdMat = new Material(dirt, new float[] {0.4f, 0.4f, 2.0f, 1.0f}, Material.BLEND_MODE.MULTIPLICATIVE); // blue dirt should just be a blue dirt square

        // create game objects
        this.gameObjects.add(new GameObject(0f, 0f, m, pMat)); // create player
        this.cam.follow(this.gameObjects.get(0)); // make cam follow player
        this.gameObjects.add(new GameObject(1.2f, 0f, m, dMat)); // create dirt
        this.gameObjects.add(new GameObject(-1.2f, 0f, m, bdMat)); // create blue dirt
    }

    @Override
    public void input(Window window) {
        this.gameObjects.get(0).setVX(0f); // reset horizontal velocity to 0
        this.gameObjects.get(0).setVY(0f); // reset vertical velocity to 0
        if (window.isKeyPressed(GLFW_KEY_W)) this.gameObjects.get(0).incrementVY(0.05f); // w -> up
        if (window.isKeyPressed(GLFW_KEY_S)) this.gameObjects.get(0).incrementVY(-0.05f); // s -> down
        if (window.isKeyPressed(GLFW_KEY_D)) this.gameObjects.get(0).incrementVX(0.05f); // d -> right
        if (window.isKeyPressed(GLFW_KEY_A)) this.gameObjects.get(0).incrementVX(-0.05f); // a -> left
    }

}
