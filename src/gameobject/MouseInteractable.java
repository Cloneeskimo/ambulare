package gameobject;

import utils.Bounds;
import utils.Coord;

/**
 * An interface to be implemented by objects that want to react to being interacted with by a mouse
 */
public interface MouseInteractable {

    /**
     * The method that should be called when an implementing object is hovered over
     * @param x the normalized and projected x position of the mouse
     * @param y the normalized and projected y position of the mouse
     */
    void onHover(float x, float y);

    /**
     * The method that should be called when an implementing object was being hovered and no longer is
     */
    void onDoneHovering();

    /**
     * The method that should be called whenever the mouse is pressed while hovering over the implementing object
     */
    void onPress();

    /**
     * The method that should be called whenever the mouse is release while hovering over the implementing object
     */
    void onRelease();

    /**
     * @return the appropriate bounds to use to consider whether or not the implementing object is being hovered
     */
    Bounds getHoverBounds();
}
