package gameobject;

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
    private List<HUDItem> hudItems; // a List of HUDItems belonging to this HUD
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
        this.hudItems = new ArrayList<>(); // create HUDItem list
        this.initSP(); // initialize ShaderProgram
    }

    /**
     * Initializes this HUD's ShaderProgram
     */
    private void initSP() {
        this.sp = new ShaderProgram("/shaders/hudV.glsl", "/shaders/worldF.glsl"); // create ShaderProgram
        this.sp.registerUniform("x"); // register aspect x uniform
        this.sp.registerUniform("y"); // register aspect y uniform
        this.sp.registerUniform("scaleX"); // register x scale uniform
        this.sp.registerUniform("scaleY"); // register y scale uniform
        this.sp.registerUniform("ar"); // register aspect ratio uniform
        this.sp.registerUniform("arAction"); // register aspect ratio action uniform
        this.sp.registerUniform("isTextured"); // register texture flag uniform
        this.sp.registerUniform("color"); // register color uniform
        this.sp.registerUniform("blend"); // register blend uniform
        this.sp.registerUniform("texSampler"); // register texture sampler uniform
    }

    /**
     * Updates this HUD
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        for (HUDItem i : this.hudItems) i.o.update(interval); // update GameObjects
    }

    /**
     * Renders this HUD
     */
    public void render() {
        this.sp.bind(); // bind this HUD's ShaderProgram
        this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
        this.sp.setUniform("ar", this.ar); // set aspect ratio uniform
        this.sp.setUniform("arAction", this.arAction ? 1 : 0); // set aspect ratio action uniform
        for (HUDItem i : this.hudItems) i.o.render(this.sp); // render each GameObject
        this.sp.unbind(); // unbind this HUD's ShaderProgram
    }

    /**
     * Handles a resize of the Window. It is vital that this is called whenever the Window in use is resized, or
     * position of HUD elements will be off
     * @param ar the new aspect ratio
     * @param arAction the new aspect ratio action
     */
    public void resized(float ar, boolean arAction) {
        this.ar = ar; // save new aspect ratio
        this.arAction = arAction; // save new aspect ratio action
        this.ensureAllPlacements(); // ensure all items are correctly paced
    }

    /**
     * Adds a given GameObject to this HUD as independent. This means that its position does not depend on any other
     * GameObject's position.
     * @param o the GameObject to add to this HUD
     * @param nx the normalized x to place the given GameObject at [-1.0f, 1.0f] - will be maintained as aspect coordinates automatically
     * @param ny the normalized y to place the given GameObject at [-1.0f, 1.0f] - will be maintained as aspect coordinates automatically
     * @param accountForSize whether to account for the size of the given GameObject when calculating the position. For
     *                       example, if x and y are -1f, only the top-right of the GameObject would normally be visible
     *                       because (0, 0) is at the center of the model. If this parameter is set to true, the
     *                       position will be modified so as to bump the object into view perfectly. This essentially
     *                       allows for very easy binding of GameObjects to the edge of the Window in use
     * @param padding the amount of padding to use when accounting for size
     */
    public void addIndependentObject(GameObject o, float nx, float ny, boolean accountForSize, float padding) {
        this.hudItems.add(new HUDItem(o, null, nx, ny, accountForSize, padding)); // add to HUDItem list
        this.ensurePlacement(this.hudItems.size() - 1); // place it correctly
    }

    /**
     * Adds a given GameObject to this HUD as dependent. This means that its position will depend on the other given
     * GameObject's position. Dependent GameObjects should only be added AFTER their parent has been added to avoid
     * unexpected behavior when updating all of their positions.
     * @param o the GameObject to add to this HUD
     * @param po the GameObject whose position determines the position of o
     * @param nx how many widths away o should be from po (where the width is the average of both of their widths)
     * @param ny how many heights away o should be from po (where the height is the average of both of their heights)
     * @param padding the amount of additional padding there should be between o and po
     */
    public void addDependentObject(GameObject o, GameObject po, float nx, float ny, float padding) {
        this.hudItems.add(new HUDItem(o, po, nx, ny, false, padding)); // add to HUDItem list
        this.ensurePlacement(this.hudItems.size() - 1); // place it correctly
    }

    /**
     * Accounts for the size of the given GameObject by adjusting its position by half of its size plus some given
     * padding value
     * @param o the GameObject whose size to account for
     * @param pos the aspect coordinates before accounting for size
     * @param padding the amount of padding
     */
    private void accountForSize(GameObject o, Coord pos, float padding) {
        if (pos.x < 0f) pos.x += (o.getWidth() / 2 + padding); // if on the left side, nudge to the right
        else if (pos.x > 0f) pos.x -= (o.getWidth() / 2 + padding); // if on the right side, nudge to the left
        if (pos.y < 0f) pos.y += (o.getHeight() / 2 + padding); // if on bottom side, nudge upwards
        else if (pos.y > 0f) pos.y -= (o.getHeight() / 2 + padding); // if on top side, nudge downwards
    }

    /**
     * Ensures the GameObject at the given index is positioned according to its original position settings. This is
     * good to call after modifying the size of a GameObject being rendered by this HUD in any fashion (scale, width,
     * new Model, etc.)
     * @param i the index of the GameObject whose placement should be ensureed
     */
    public void ensurePlacement(int i) {
        HUDItem hi = this.hudItems.get(i); // get HUDItem
        if (hi.po == null) { // if independently placed
            Coord pos = new Coord(hi.nx, hi.ny); // create new Coord object with given coordinates
            Transformation.normToAspect(pos, this.ar); // convert coordinates to aspect
            if (hi.accountForSize) accountForSize(hi.o, pos, hi.padding); // account for size if flag set to true
            hi.o.setX(pos.x); hi.o.setY(pos.y); // set position
        } else { // if dependent on another GameObject's position
            Coord pos = new Coord(hi.po.getX(), hi.po.getY()); // start at parent GameObject's position
            pos.x += hi.nx * (hi.o.getWidth() / 2 + hi.po.getWidth() / 2 + hi.padding); // adjust x according to nx and padding
            pos.y += hi.ny * (hi.o.getHeight() / 2 + hi.po.getHeight() / 2 + hi.padding); // adjust y according to ny and padding
            hi.o.setX(pos.x); hi.o.setY(pos.y); // set position
        }
    }

    /**
     * Ensures the positions of all GameObject this HUD owns in the order that they were added
     */
    public void ensureAllPlacements() { for (int i = 0; i < this.hudItems.size(); i++) ensurePlacement(i); }

    /**
     * Finds and returns the GameObject at the given index
     * @param i the index to find the GameObject at
     * @return the GameObject
     */
    public GameObject getObject(int i) { return this.hudItems.get(i).o; }

    /**
     * Cleans up this HUD
     */
    public void cleanup() {
        if (this.sp != null) this.sp.cleanup(); // cleanup ShaderProgram
        for (HUDItem i : this.hudItems) i.o.cleanup(); // cleanup GameObjects
    }

    /**
     * Wraps GameObjects inside of a separate class that stores the original placement settings that were assigned when
     * added to the HUD. This is useful so that when the Window in use or the GameObject itself is resized, GameObjects
     * can be appropriately moved to maintain their correct positioning
     */
    private class HUDItem {

        /**
         * Data
         */
        GameObject o; // reference to the wrapped GameObject
        GameObject po; // reference to a parent GameObject whose position determines that of this GameObject (or null if independent)
        float nx, ny; // the normalized coordinates of the GameObject, or the direction of dependency if the GameObject's position is dependent on another GameObject
        boolean accountForSize; // whether the placement of the GameObject should account for its size
        float padding; // the amount of padding when accounting for size

        /**
         * Constructs this HUDItem
         * @param o the GameObject
         * @param po the parent GameObject whose position determines that of this HUDItem (or null if independent)
         * @param nx the normalized x of the GameObject or the direction of dependency if the GameObject's position is dependent on another GameObject
         * @param ny the normalized y of the GameObject or the direction of dependency if the GameObject's position is dependent on another GameObject
         * @param accountForSize whether the placement of the GameObject should account for its size. See addObject()
         *                       for more details on how exactly this works
         * @param padding the amount of padding to use when accounting for size
         */
        public HUDItem(GameObject o, GameObject po, float nx, float ny, boolean accountForSize, float padding) {
            this.o = o; // set GameObject
            this.po = po; // set parent GameObject
            this.nx = nx; // set norm x
            this.ny = ny; // set norm y
            this.accountForSize = accountForSize; // set account for size flag
            this.padding = padding; // set padding
        }
    }
}
