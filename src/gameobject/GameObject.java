package gameobject;

import graphics.Model;
import graphics.ShaderProgram;
import graphics.Window;

/**
 * Represents a single gameobject.GameObject
 */
public class GameObject {

    /**
     * Data
     */
    private float x, y; // world position
    private float vx, vy; // world velocity
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
        this.vx = this.vy = 0; // initialize velocities to 0
        this.model = model; // set model
    }

    /**
     * Updates this GameObject
     */
    public void update() {
        this.x += this.vx; // update x world position
        this.y += this.vy; // update y world position
    }

    /**
     * Updates this GameObject's horizontal velocity
     * @param vx the new horizontal velocity
     */
    public void setVX(float vx) { this.vx = vx; }

    /**
     * Updates this GameObject's horizontal velocity by adding the given incremental change
     * @param dvx the incremental change
     */
    public void incrementVX(float dvx) { this.vx += dvx; }

    /**
     * Updates this GameObject's vertical velocity
     * @param vy the new vertical velocity
     */
    public void setVY(float vy) { this.vy = vy; }

    /**
     * Updates this GameObject's vertical velocity by adding the given incremental change
     * @param dvy the incremental change
     */
    public void incrementVY(float dvy) { this.vy += dvy; }

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
