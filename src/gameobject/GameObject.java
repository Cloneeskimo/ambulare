package gameobject;

import graphics.Material;
import graphics.Model;
import graphics.ShaderProgram;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

/**
 * Represents a single game object. This is the basic abstraction away from directly dealing with GL commands
 * GameObject's have a Model and a Material both used for rendering, and they also have position and velocity
 * components
 * All positioning and velocity are aspect coordinates if being rendered by a HUD, or world coordinates if being
 * rendered by a World
 */
public class GameObject {

    /**
     * Data
     */
    private float x, y; // position
    private float vx, vy; // velocity
    private Model model; // model to use when rendering
    private Material material; // material to use when rendering

    /**
     * Constructs this GameObject
     * @param x the x position to place this GameObject at
     * @param y the y position to place this GameObject at
     * @param model the model to render for this GameObject
     */
    public GameObject(float x, float y, Model model, Material material) {
        this.x = x; // set x
        this.y = y; // set y
        this.vx = this.vy = 0; // initialize velocities to 0
        this.model = model; // set model
        this.material = material; // set material
    }

    /**
     * Updates this GameObject
     */
    public void update() {
        this.x += this.vx; // update x world position
        this.y += this.vy; // update y world position
    }

    /**
     * @return this GameObject's x position
     */
    public float getX() { return this.x; }

    /**
     * Update's this GameObject's x position
     * @param x the new x position
     */
    public void setX(float x) { this.x = x; }

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
     * @return this GameObject's y position
     */
    public float getY() { return this.y; }

    /**
     * Update's this GameObject's y position
     * @param y the new y position
     */
    public void setY(float y) { this.y = y; }

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
     * Renders this GameObject using the given ShaderProgram
     * This function will assume the given ShaderProgram is already bound and has a certain set of uniforms to handle
     * rendering a GameObject (see method for details)
     * @param sp the ShaderProgram to use to render this GameObject
     */
    public void render(ShaderProgram sp) {
        if (this.material.isTextured()) { // if this object's material is textured
            sp.setUniform("isTextured", 1); // set textured flag to true
            glActiveTexture(GL_TEXTURE0); // set active texture to one in slot 0
            glBindTexture(GL_TEXTURE_2D, this.material.getTexture().getID()); // bind texture
        } else sp.setUniform("isTextured", 0); // set textured flag to false otherwise
        if (this.material.isColored()) { // if this object's material is colored
            float[] color = this.material.getColor(); // and get color instead
            sp.setUniform("color", color[0], color[1], color[2], color[3]); // set color uniform
        }
        Material.BLEND_MODE bm = this.material.getBlendMode(); // get blend mode of this object's Material
        sp.setUniform("blend", bm == Material.BLEND_MODE.NONE ? 0 : (bm == Material.BLEND_MODE.MULTIPLICATIVE ? 1 : 2)); // set blend uniform
        sp.setUniform("x", this.x); // set x
        sp.setUniform("y", this.y); // set y
        this.model.render(); // render model
    }

    /**
     * Cleans up this GameObject
     */
    public void cleanup() {
        this.model.cleanup(); // cleanup model
        this.material.cleanup(); // cleanup material
    }
}
