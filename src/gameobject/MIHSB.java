package gameobject;

import graphics.Camera;
import utils.Pair;
import utils.Transformation;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * MouseInteractable Hover State Bundle
 * This class stores a collection of MouseInteractable objects and parallel hover states for each one so as to
 * abstract and generalize that away from world and HUD
 */
public class MIHSB {

    /**
     * Data
     */
    private List<MouseInteractable> mis; // list of objects able to be interacted with via mouse
    private List<Boolean> hoverStates; // the hover states for each object in mis (kept as a parallel list)

    /**
     * Constructs the MIHSB
     */
    public MIHSB() {
        this.mis = new ArrayList<>(); // create new list for mouse interactables
        this.hoverStates = new ArrayList<>(); // create new list for hover states
    }

    /**
     * Reacts to mouse input
     * @param x the normalized and projected x position of the mouse if hover event, 0 otherwise
     * @param y the normalized and projected y position of the mouse if hover event, 0 otherwise
     * @param cam a reference to the camera to use to calculate hovers or null if no camera should be used
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     */
    public void mouseInput(float x, float y, Camera cam, int action) {
        if (action == GLFW_HOVERED) { // if hover
            if (cam != null) { // if using a camera
                Pair pos = new Pair(x, y); // combine into a coordinate
                Transformation.useCam(pos, cam); // convert to camera-view coordinates
                x = pos.x; y = pos.y; // extract x and y
                System.out.println("(" + x + ", " + y + ")");
                if (this.mis.size() > 0) System.out.println(((GameObject)this.mis.get(0)).getX() + ", " + ((GameObject)this.mis.get(0)).getY());
            }
            for (int i = 0; i < this.mis.size(); i++) { // for each that can be interacted with by a mouse
                if (mis.get(i).getFrame().contains(x, y)) { // if it is being hovered
                    mis.get(i).onHover(x, y); // notify it of hover
                    this.hoverStates.set(i, true); // save new hover state
                    System.out.println("hover");
                } else { // otherwise
                    if (hoverStates.get(i)) { // if previously being hovered
                        mis.get(i).onDoneHovering(); // notify it that hovering has stopped
                        hoverStates.set(i, false); // save new hover state
                    }
                }
            }
        } else { // if press or release
            for (int i = 0; i < this.mis.size(); i++) { // go through each object able to be interacted with
                if (this.hoverStates.get(i)) { // if the object is being hovered
                    if (action == GLFW_PRESS) this.mis.get(i).onPress(); // if press, tell object it was clicked
                    else if (action == GLFW_RELEASE) this.mis.get(i).onRelease(); // if release, tell object
                }
            }
        }
    }

    /**
     * Adds a new object to consider mouse input for
     * @param mi the object
     */
    public void add(MouseInteractable mi) {
        this.mis.add(mi); // add to list of MI objects
        this.hoverStates.add(false); // create new hover states
    }
}
