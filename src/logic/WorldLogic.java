package logic;

import graphics.Model;
import graphics.ShaderProgram;
import graphics.Window;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * Lays out logic for engine to follow while in the game world
 */
public class WorldLogic implements GameLogic {

    /**
     * Data
     */
    private ShaderProgram sp; // shader program to use for world
    private Model m; // temporary test model

    /**
     * Initializes this WorldLogic
     * @param window the window
     */
    @Override
    public void init(Window window) {
        this.sp = new ShaderProgram("/shaders/worldV.glsl", "/shaders/worldF.glsl");
        this.m = new Model(
            new float[] { // rectangle positions
                -0.5f,  0.5f,  0.0f,
                -0.5f, -0.5f,  0.0f,
                 0.5f, -0.5f,  0.0f,
                 0.5f,  0.5f,  0.0f
            },
            new float[] { // rectangle colors
                    1.0f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.0f,
                    0.0f, 0.0f, 1.0f,
                    1.0f, 1.0f, 1.0f

            },
            new int[] { 0, 1, 3, 3, 1, 2 } // rectangle indices
        );
    }

    /**
     * Gathers window input
     * @param window the window
     */
    @Override
    public void input(Window window) {

    }

    /**
     * Updates this WorldLogic
     * @param interval the amount of time in seconds since the last update call
     */
    @Override
    public void update(float interval) {

    }

    /**
     * Renders all game objects
     */
    @Override
    public void render() {
        this.sp.bind(); // bind shader program
        this.m.render(); // render mesh
        this.sp.unbind(); // unbind shader program
    }

    /**
     * Clean up components of this WorldLogic that need cleaned up
     */
    @Override
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup shader programs
        this.m.cleanup(); // cleanup mesh
    }
}
