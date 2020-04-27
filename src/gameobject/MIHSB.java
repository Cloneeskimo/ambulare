package gameobject;

import graphics.Camera;
import utils.FittingBox;
import utils.Pair;
import utils.Transformation;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * MIHSB stands for Mouse Interaction Hover State Bundle
 * This class stores a collection of objects that can be interacted with by a mouse and, given constant mouse
 * updates, abstract away constantly updating the objects' reactions to mouse input. See MIHSB.MouseInteractable for
 * details on how an appropriate object would be interacted with by this class
 */
public class MIHSB {

    /**
     * Members
     */
    private List<MouseInteractable> mis; // list of objects able to be interacted with via mouse
    private List<Boolean> hoverStates;   // the hover states for each object in mis (kept as a parallel list)
    private List<Boolean> useCam;        // whether to transform the mouse input into camera-view coordinates for each
    private Camera cam;
    private boolean pressed;             // whether the mouse is currently pressed down

    /**
     * Constructs the MIHSB
     */
    public MIHSB() {
        this.mis = new ArrayList<>(); // create new list for mouse interactables
        this.hoverStates = new ArrayList<>(); // create new list for hover states
        this.useCam = new ArrayList<>(); // create new list for camera usage flags
    }

    /**
     * Reacts to mouse input
     *
     * @param x      the normalized and projected x position of the mouse if hover event, 0 otherwise
     * @param y      the normalized and projected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     * @return an array containing the IDs of all items that were clicked (mouse released)
     */
    public int[] mouseInput(float x, float y, int action) {
        List<Integer> clickedIDs = new ArrayList<>(); // create a list to store the IDs of the objects that were clicked
        if (action == GLFW_HOVERED) { // if hover
            Pair<Float> pos = new Pair<Float>(x, y); // bundle into a pair
            Pair<Float> camPos = new Pair<Float>(x, y); // create a separate pair for camera-view coordinates
            if (cam != null) Transformation.useCam(camPos, cam); // transform camera position into camera-view pos
            for (int i = 0; i < this.mis.size(); i++) { // for each that can be interacted with by a mouse
                // if it uses a camera, use the mouse position in camera-view coordinates. Otherwise, use world pos
                Pair<Float> appropriatePos = this.useCam.get(i) ? camPos : pos;
                if (mis.get(i).getFittingBox().contains(appropriatePos)) { // if the fitting box contains the mouse
                    if (this.pressed) mis.get(i).onPress(); // if the mouse wandered in while pressed, call onPress
                    else mis.get(i).onHover(appropriatePos.x, appropriatePos.y); // otherwise, call onHover
                    this.hoverStates.set(i, true); // save new hover state
                } else { // otherwise
                    if (hoverStates.get(i)) { // if previously being hovered
                        mis.get(i).onDoneHovering(); // notify it that hovering has stopped
                        hoverStates.set(i, false); // save new hover state
                    }
                }
            }
        } else { // if press or release
            this.pressed = (action == GLFW_PRESS); // keep track of if mouse is pressed or not
            for (int i = 0; i < this.mis.size(); i++) { // go through each object able to be interacted with
                if (this.hoverStates.get(i)) { // if the object is being hovered
                    if (action == GLFW_PRESS) this.mis.get(i).onPress(); // if press, tell object it was clicked
                    else if (action == GLFW_RELEASE) { // if release
                        this.mis.get(i).onRelease(); // tell object it was released upon
                        clickedIDs.add(this.mis.get(i).getID()); // save ID of clicked object
                    }
                }
            }
        }
        int[] clicked = new int[clickedIDs.size()]; // create array to put the clicked IDs into
        for (int i = 0; i < clicked.length; i++) clicked[i] = clickedIDs.get(i); // copy over IDs
        return clicked; // return the array of IDs of items that were clicked
    }

    /**
     * Adds a new object to consider mouse input for
     *
     * @param mi     the object
     * @param useCam whether this object will be rendered using a camera
     */
    public void add(MouseInteractable mi, boolean useCam) {
        this.mis.add(mi); // add to list of MI objects
        this.hoverStates.add(false); // create new hover states
        this.useCam.add(useCam); // save camera usage flag
    }

    public void useCam(Camera cam) {
        this.cam = cam;
    }

    /**
     * An interface to be implemented by objects that want to react to being interacted with by a mouse through a MIHSB
     */
    public interface MouseInteractable {

        /**
         * The method that will be called when an implementing object is hovered over
         *
         * @param x the x position of the mouse in either world or camera-view space, depending on whether the
         *          implementing object reacts to a camera
         * @param y the y position of the mouse in either world or camera-view space, depending on whether the
         *          implementing object reacts to a camera
         */
        void onHover(float x, float y);

        /**
         * The method that will be called when an implementing object was being hovered and no longer is
         */
        void onDoneHovering();

        /**
         * The method that will be called whenever the mouse is pressed down while hovering over the implementing object
         */
        void onPress();

        /**
         * The method that will be called whenever the mouse is released while hovering over the implementing object
         */
        void onRelease();

        /**
         * This will be called by the MIHSB to get an ID from the item to return to the object that owns the
         * MIHSB. IDs will not be checked for uniqueness in case the implementor wants multiple objects to have the
         * same reaction
         *
         * @return the ID described above
         */
        int getID();

        /**
         * @return the appropriate fitting box to use to consider whether or not the implementing object is being
         * hovered by the mouse
         */
        FittingBox getFittingBox();
    }

}
