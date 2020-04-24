package gameobject.gameworld;

import gameobject.GameObject;
import graphics.Material;
import graphics.Model;
import utils.BoundingBox;
import utils.PhysicsEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends game objects to have physics properties. Namely, they are affected by gravity (customizable) and are able
 * to react to collision with other world objects
 */
public class WorldObject extends GameObject {

    /**
     * Members
     */
    private List<WorldObject> collidables;  // a reference to the list of other objects to consider for collision
    private PhysicsEngine.PhysicsProperties pp; // the properties which describe how this object interacts with physics

    /**
     * Constructor
     * @param model the model to use
     * @param material the material to use
     */
    public WorldObject(Model model, Material material) {
        super(model, material);
        this.collidables = new ArrayList<>();
        this.pp = new PhysicsEngine.PhysicsProperties();
    }

    /**
     * Updates the world object by updating its normal game object properties and also applying gravity
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        // apply gravity but do not go below terminal velocity
        this.vy = Math.max(this.vy - (this.pp.gravity * interval), PhysicsEngine.TERMINAL_VELOCITY);
        super.update(interval);
    }

    /**
     * React to movements by using the physics engine to check for and react to collisionss
     * @param dx the x offset
     * @param dy the y offset
     * @return whether or not any movement actually occurred
     */
    @Override
    public boolean move(float dx, float dy) {
        if (this.pp.collidable) return PhysicsEngine.move(this, dx, dy); // use physics engine
        else return super.move(dx, dy); // if collision for this object is off, handle movement normally
    }

    /**
     * Sets the list of collidables to check for collisions
     * @param collidables the list of collidables
     */
    public void setCollidables(List<WorldObject> collidables) {
        this.collidables = collidables;
    }

    public List<WorldObject> getCollidables() { return this.collidables; }

    /**
     * @return the physics settings for the object
     */
    public PhysicsEngine.PhysicsProperties getPhysicsProperties() { return this.pp; }
}
