package gameobject;

import graphics.Model;
import graphics.ShaderProgram;
import utils.Coord;
import utils.Transformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a collection of items that do not react to a Camera when rendered
 * Any items added to this HUD are considered to be in normalized space when received and will ultimately be
 * converted and maintained by this class in aspect coordinates
 */
public class HUD {

    /**
     * Data
     */
    private List<GameObject> gameObjects; // a List of GameObjects belonging to this HUD
    private ShaderProgram sp; // the ShaderProgram used to render the HUD
    private float ar; // the Window's aspect ratio
    private boolean arAction; // aspect ratio action (for projection)

    /**
     * Constructs this HUD
     * @param ar the Window's aspect ratio
     * @param arAction aspect ratio action (for projection)
     */
    public HUD(float ar, boolean arAction) {
        this.ar = ar; // save aspect ratio for rendering
        this.arAction = arAction; // save aspect ratio action for rendering
        this.gameObjects = new ArrayList<>(); // create GameObject list
        this.initSP(); // initialize ShaderProgram
    }

    /**
     * Initializes this HUD's ShaderProgram
     */
    private void initSP() {
        this.sp = new ShaderProgram("/shaders/hudV.glsl", "/shaders/worldF.glsl"); // create ShaderProgram
        this.sp.registerUniform("x"); // register aspect x uniform
        this.sp.registerUniform("y"); // register aspect y uniform
        this.sp.registerUniform("ar"); // register aspect ratio uniform
        this.sp.registerUniform("arAction"); // register aspect ratio action uniform
        this.sp.registerUniform("isTextured"); // register texture flag uniform
        this.sp.registerUniform("color"); // register color uniform
        this.sp.registerUniform("blend"); // register blend uniform
        this.sp.registerUniform("texSampler"); // register texture sampler uniform
    }

    /**
     * Updates this HUD
     */
    public void update() {
        for (GameObject o : this.gameObjects) o.update(); // update GameObjects
    }

    /**
     * Renders this HUD
     */
    public void render() {
        this.sp.bind(); // bind this HUD's ShaderProgram
        this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.sp.setUniform("ar", this.ar); // set aspect ratio uniform
        this.sp.setUniform("arAction", this.arAction ? 1 : 0); // set aspect ratio action uniform
        for (GameObject o : this.gameObjects) o.render(this.sp); // render each GameObject
        this.sp.unbind(); // unbind this HUD's ShaderProgram
    }

    /**
     * Handles a resize of the Window. It is IMPERATIVE that the hosting class call this every time the Window is
     * resized, or aspect coordinates cannot be maintained by this class automatically
     * @param ar the new aspect ratio
     * @param arAction the new aspect ratio action
     */
    public void resized(float ar, boolean arAction) {
        for (GameObject o : this.gameObjects) { // for each GameObject
            Coord ap = new Coord(o.getX(), o.getY()); // create a Coord object containing its position
            ap.x -= (0.05f + (Model.STD_SQUARE_SIZE / 2)); // TODO: do not assume square, implement getWidth or getHeight()
            ap.y -= (0.05f + (Model.STD_SQUARE_SIZE / 2)); // TODO: do not assume square, implement getWidth or getHeight()
            Transformation.aspectToNorm(ap, this.ar); // convert back to normalized using old aspect ratio
            Transformation.normToAspect(ap, ar); // then convert back to aspect using new aspect ratio
            o.setX(ap.x + 0.05f + (Model.STD_SQUARE_SIZE / 2)); // TODO: do not assume square, implement getWidth or getHeight()
            o.setY(ap.y + 0.05f + (Model.STD_SQUARE_SIZE / 2)); // TODO: do not assume square, implement getWidth or getHeight()
        }
        this.ar = ar; // save new aspect ratio
        this.arAction = arAction; // save new aspect ratio action
    }

    /**
     * Adds the given item to the HUD at its current position. Does not do any Transformations on the position of the
     * given GameObject, so if this is called the caller should be sure the GameObject's position is in aspect space for
     * it to render properly
     * @param o the GameObject to add to this HUD
     */
    public void addObject(GameObject o) { this.gameObjects.add(o); }

    /**
     * Adds a given GameObject
     * @param o the GameObject to add this HUD
     * @param x the normalized x to place the given GameObject at [-1.0f, 1.0f] - will be maintained aspect coordinates automatically
     * @param y the normalized y to place the given GameObject at [-1.0f, 1.0f] - will be maintained aspect coordinates automatically
     */
    public void addObject(GameObject o, float x, float y) {
        Coord pos = new Coord(x, y); // create new Coord object with given coordinates
        Transformation.normToAspect(pos, this.ar); // convert normalized coordinates to aspect coordinates
        o.setX(pos.x + 0.05f + (Model.STD_SQUARE_SIZE / 2)); // TODO: do not assume square, implement getWidth or getHeight()
        o.setY(pos.y + 0.05f + (Model.STD_SQUARE_SIZE / 2)); // TODO: do not assume square, implement getWidth or getHeight()
        this.addObject(o); // add item
    }

    /**
     * Cleans up this HUD
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup ShaderProgram
        for (GameObject o : this.gameObjects) o.cleanup(); // cleanup GameObjects
    }

    /**
     * Represents a position to place an item in this HUD
     */
    public enum Placement {
        TOP_LEFT, // top left of the Window
        TOP_MIDDLE, // top middle of the Window
        TOP_RIGHT, // top right of the Window
        MIDDLE, // middle of the Window
        BOTTOM_LEFT, // bottom left of the Window
        BOTTOM_MIDDLE, // bottom middle of the Window
        BOTTOM_RIGHT, // bottom right of the Window
        LEFT_OF_LAST, // to the left of the last item added to the HUD
        RIGHT_OF_LAST, // to the right of the last item added to the HUD
        ABOVE_LAST, // above the last item added to the HUD
        BELOW_LAST // below the last item added to the HUD
    }
}
