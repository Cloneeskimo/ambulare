package gameobject.ui;

import gameobject.GameObject;
import graphics.Material;
import graphics.Model;
import graphics.ShaderProgram;
import utils.MouseInputEngine;
import utils.Pair;
import utils.Utils;

import java.util.List;

/*
 * ListObject.java
 * Ambulare
 * Jacob Oaks
 * 5/4/2020
 */

/**
 * Simulates a list by containing multiple list items stacked on top of each other. The list will fit to the items
 * it contains and will make sure the items stay correctly positioned. See ListObject.ListItem for information on what
 * constitutes a list item. List objects do not support rotation and any calls to rotate them will be ignored and logged
 */
public class ListObject extends GameObject implements MouseInputEngine.MouseInteractive {

    /**
     * Members
     */
    private final MouseInputEngine.MouseCallback[] mcs = new MouseInputEngine.MouseCallback[4]; /* array of mouse
        callbacks as specified by the mouse interactive interface */
    private List<ListItem> items; // list of items in the list. See ListObject.ListItem for more details on list items
    private float padding;        // the amount of padding to put in between list items and around the edge of list
    private ListItem hovered;     /* the last list item within the last that was hovered. This is used to properly
                                     tell list items when they are done being hovered */

    /**
     * Constructor
     *
     * @param items      the items to place in the list
     * @param padding    the amount of padding to place between items and around the edge of the list
     * @param background the material to render as the list's background
     */
    public ListObject(List<ListItem> items, float padding, Material background) {
        super(Model.getStdGridRect(1, 1), background); // call super constructor with square model and background
        this.items = items; // save items as member
        this.padding = padding; // save padding as member
        this.position(); // position the list and the list items correctly
    }

    /**
     * Positions the items within the list correctly and makes sure the list itself is of the appropriate size
     */
    private void position() {
        // start with a width and height of 0
        float w = 0f;
        float h = 0f;
        for (ListItem li : items) { // for each list item
            w = Math.max(w, li.getWidth()); // keep track of the widest one
            h += li.getHeight(); // keep track of the cumulative height
        }
        w += this.padding * 2; // the width of the list is the widest item's width plus two padding
        h += this.padding * (1 + this.items.size()); // height is the cum. height and padding btwn items and above/below
        this.model.setScale(w, h); // scale the list to fit correctly
        float y = this.getY() - (this.getHeight() / 2) + padding; // start from the bottom of the object
        for (int i = this.items.size() - 1; i >= 0; i--) { // and go through each list item (in reverse order)
            float ih2 = this.items.get(i).getHeight() / 2; // calculate the item's half width
            y += ih2; // iterate y by the current item's half-width
            this.items.get(i).setPos(this.getX(), y); // then place the item there
            y += ih2; // iterate y again by the current item's half-width
            y += padding; // add padding before next item
        }
    }

    /**
     * Updates the list and the list items
     */
    @Override
    public void update(float interval) {
        super.update(interval); // update background game object
        // update game object list items
        for (ListItem li : this.items) if (li instanceof GameObject) ((GameObject) li).update(interval);
    }

    /**
     * Renders the list and the list items
     *
     * @param sp the shader program to use to render the list object
     */
    @Override
    public void render(ShaderProgram sp) {
        super.render(sp); // render the list background first
        if (this.visible) for (ListItem li : this.items) li.render(sp); // then render each list item
    }

    /**
     * Reacts to list movement by re-positioning and re-sizing the list and its items appropriately
     */
    @Override
    protected void onMove() {
        this.position(); // reposition list and list items
    }

    /**
     * Scales all list items that are game objects by the given horizontal scaling factor
     *
     * @param x the x scaling factor to use
     */
    @Override
    public void setXScale(float x) {
        for (ListItem li : this.items) { // for each list item in the list
            if (li instanceof GameObject) { // if it is a game object
                ((GameObject) li).setXScale(x); // scale it by the given x factor
            }
        }
        this.position(); // and reposition the list
    }

    /**
     * Scales all list items that are game objects by the given vertical scaling factor
     *
     * @param y the y scaling factor to use
     */
    @Override
    public void setYScale(float y) {
        for (ListItem li : this.items) { // for each list item in the list
            if (li instanceof GameObject) { // if it is a game object
                ((GameObject) li).setYScale(y); // scale it by the given y factor
            }
        }
        this.position(); // and reposition the list
    }

    /**
     * Scales all list items that are game objects by the given horizontal and vertical scaling factors
     *
     * @param x the x scaling factor to use
     * @param y the y scaling factor to use
     */
    @Override
    public void setScale(float x, float y) {
        for (ListItem li : this.items) { // for each list item in the list
            if (li instanceof GameObject) { // if it is a game object
                ((GameObject) li).setXScale(x); // scale it by the given x factor
                ((GameObject) li).setYScale(y); // scale it by the given y factor
            }
        }
        this.position(); // and reposition the list
    }

    /**
     * Reacts to attempts to rotate by logging and ignoring the attempt
     *
     * @param r the attempted new rotation value
     */
    @Override
    public void setRotRad(float r) {
        Utils.log("Attempted to rotate a list object. Ignoring.", this.getClass(), "setRotRad",
                false); // log and ignore the attempt to rotate
    }

    /**
     * Overrides game object positional animation updating by not attempting to update rotation
     *
     * @param interval the amount of time, in seconds, to account for
     */
    @Override
    protected void updatePosAnim(float interval) {
        this.posAnim.update(interval); // update animation
        this.setX(this.posAnim.getX()); // set x position
        this.setY(this.posAnim.getY()); // set y position
        if (this.posAnim.finished()) { // if animation is over
            this.setX(this.posAnim.getFinalX()); // make sure at the correct ending x
            this.setY(this.posAnim.getFinalY()); // make sure at the correct ending y
            this.posAnim = null; // delete the animation
        }
    }

    /**
     * Saves the given mouse input callback to be called when the given mouse input occurs
     *
     * @param type the mouse input type to give a callback for
     * @param mc   the callback
     */
    @Override
    public void giveCallback(MouseInputEngine.MouseInputType type, MouseInputEngine.MouseCallback mc) {
        MouseInputEngine.MouseInteractive.saveCallback(type, mc, this.mcs); // save the callback
    }

    /**
     * Responds to mouse interaction by delegating it to the corresponding list item and invoking appropriate callbacks
     *
     * @param type the type of mouse input that occurred
     * @param x    the x position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
     *             input engine's camera usage flag for this particular implementing object
     * @param y    the y position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
     */
    @Override
    public void mouseInteraction(MouseInputEngine.MouseInputType type, float x, float y) {
        MouseInputEngine.MouseInteractive.invokeCallback(type, this.mcs, x, y); // invoke appropriate callback
        ListItem item = null; // create a null list item reference
        Pair<Float> pos = new Pair<>(x, y); // bundle mouse position into a pair
        for (ListItem li : this.items) { // for each list item
            if (li.getFittingBox().contains(pos)) { // if the mouse is hovering it
                item = li; // save its reference
                break; // break from the loop - no items should overlap anyways unless a negative padding is given
            }
        }
        if (item != null) { // if a list item was found to be hovered
            if (this.hovered != null) { // if there was an item previously being hovered
                // if the new item is not the same as the old item, tell the old item it is no longer being hovered
                if (item != hovered) hovered.mouseInteraction(MouseInputEngine.MouseInputType.DONE_HOVERING, x, y);
            }
            item.mouseInteraction(type, x, y); // delegate mouse interaction to the item itself
            this.hovered = item; // update the currently hovered item
            // if nothing was being hovered but there is a reference to a hovered item, tell it hovering has stopped
        } else if (this.hovered != null) hovered.mouseInteraction(MouseInputEngine.MouseInputType.DONE_HOVERING, x, y);
    }

    /**
     * Defines the functionality that an item within a list object should have, in addition to implementing mouse
     * interaction. All of this functionality is already present for game objects (except for mouse interaction). Thus,
     * making extensions of the game object class that implement this interface is preferable, clean, and reliable
     */
    public interface ListItem extends MouseInputEngine.MouseInteractive {

        /**
         * Should return the accurate width of the list item. This is needed to calculate the entire list's width
         *
         * @return the width of the list item
         */
        float getWidth();

        /**
         * Should return the accurate height of the list item. This is needed to calculate the entire list's height
         *
         * @return the height of the list item
         */
        float getHeight();

        /**
         * Should be used to change the position of the list item. This is needed to ensure the items in the list stay
         * correctly positioned
         *
         * @param x the x position to assign to the list object
         * @param y the y position to assign to the list object
         */
        void setPos(float x, float y);

        /**
         * Should render the list item using the given shader program
         *
         * @param sp the shader program to use for rendering
         */
        void render(ShaderProgram sp);
    }
}
