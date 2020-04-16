package gameobject;

import graphics.Model;
import graphics.ShaderProgram;

/**
 * Represents a single gameobject.GameObject
 */
public class GameObject {

    /**
     * Data
     */
    private float x, y; // world position
    private Model model; // model

    /**
     * Constructs this gameobject.GameObject
     * @param x the world x position to place this gameobject.GameObject at
     * @param y the world y position to place this gameobject.GameObject at
     * @param model the model to render for this gameobject.GameObject
     */
    public GameObject(float x, float y, Model model) {
        this.x = x; // set x
        this.y = y; // set y
        this.model = model; // set model
    }

    /**
     * Renders this gameobject.GameObject using the given ShaderProgram
     * This function will assume the given ShaderProgram is already bound
     * @param sp the ShaderProgram to use to render this gameobject.GameObject
     */
    public void render(ShaderProgram sp) {
        sp.setUniform("x", this.x); // set x
        sp.setUniform("y", this.y); // set y
        this.model.render(); // render model
    }

    /**
     * Cleans up this gameobject.GameObject
     */
    public void cleanup() {
        this.model.cleanup(); // cleanup model
    }
}
