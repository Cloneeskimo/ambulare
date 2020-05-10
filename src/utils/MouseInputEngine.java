package utils;

import graphics.Camera;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/*
 * MouseInputEngine.java
 * Ambulare
 * Jacob Oaks
 * 4/21/20
 */

/**
 * A mouse input engine handles mouse interactivity in an organized, efficient, and customizable manner. Mouse input
 * engines are given a collection of objects which implement the MouseInteractive interface and expect to be told
 * whenever general mouse input occurs. It will then process the input and tell relevant objects about hovering,
 * clicking, and releasing events
 */
public class MouseInputEngine {

    /**
     * This enum lists all types of mouse input detected for mouse interactive objects
     */
    public enum MouseInputType {HOVER, DONE_HOVERING, PRESS, RELEASE}

    /**
     * Members
     */
    private final List<MouseInteractive> mis; /* list of objects able to be interacted with via mouse. See
                                                 MouseInteractive for more information */
    private final List<Boolean> hoverStates;  /* the hover state of each item in the mouse interactive object list. This
                                                 is kept as a parallel array to store what objects are being hovered.
                                                 This allows for efficient notification of hovering ending */
    private final List<Boolean> useCam;       /* the camera usage status of each item in the mouse interactive object
                                                 list. This is kept as a parallel array to store which objects use a
                                                 camera when being rendered to the window */
    private Camera cam;                       /* the camera to use for transforming mouse input coordinates into
                                                 camera-view coordinates for objects that use a camera */
    private boolean pressed;                  // whether the mouse is currently pressed down
    private Pair<Float> mousePos;             // the current position of the mouse
    private Pair<Float> camMousePos;          // the current position of the mouse in camera-view coordinates

    /**
     * Constructs the mouse input engine
     */
    public MouseInputEngine() {
        this.mis = new ArrayList<>(); // create new list for mouse interactive objects
        this.hoverStates = new ArrayList<>(); // create new list for hover states
        this.useCam = new ArrayList<>(); // create new list for camera usage flags
    }

    /**
     * Reacts to mouse input by processing it and notifying relevant objects of clicks, releases, and hovering within
     * their fitting boxes
     *
     * @param x      the normalized and projected x position of the mouse if hover event, 0 otherwise
     * @param y      the normalized and projected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)s
     */
    public void mouseInput(float x, float y, int action) {
        if (action == GLFW_HOVERED) { // if hover
            this.mousePos = new Pair<>(x, y); // update mouse position
            this.camMousePos = new Pair<>(x, y); // save a separate pair for camera-view coordinates
            // transform mouse position into camera-view pos if there is a camera
            if (cam != null) Transformation.useCam(this.camMousePos, cam);
            for (int i = 0; i < this.mis.size(); i++) { // for each that can be interacted with by a mouse
                // if it uses a camera, use the mouse position in camera-view coordinates. Otherwise, use norm pos
                Pair<Float> pos = this.useCam.get(i) ? this.camMousePos : this.mousePos;
                if (mis.get(i).getFittingBox().contains(pos)) { // if the fitting box contains the mouse
                    // if the mouse wandered in while pressed, notify the object
                    if (this.pressed) mis.get(i).mouseInteraction(MouseInputType.PRESS, pos.x, pos.y);
                        // otherwise, notify the object of the hover
                    else mis.get(i).mouseInteraction(MouseInputType.HOVER, pos.x, pos.y);
                    this.hoverStates.set(i, true); // save new hover state as true
                } else { // otherwise
                    if (hoverStates.get(i)) { // if previously being hovered
                        mis.get(i).mouseInteraction(MouseInputType.DONE_HOVERING, pos.x, pos.y); // notify hovering done
                        hoverStates.set(i, false); // save new hover state as false
                    }
                }
            }
        } else { // if press or release
            this.pressed = (action == GLFW_PRESS); // keep track of if mouse is pressed or not
            for (int i = 0; i < this.mis.size(); i++) { // go through each object able to be interacted with
                if (this.hoverStates.get(i)) { // if the object is being hovered
                    Pair<Float> pos = this.useCam.get(i) ? this.camMousePos : this.mousePos;
                    // tell object it was clicked upon if the event was a click eve t
                    if (action == GLFW_PRESS) this.mis.get(i).mouseInteraction(MouseInputType.PRESS, pos.x, pos.y);
                    else if (action == GLFW_RELEASE) this.mis.get(i).mouseInteraction(MouseInputType.RELEASE,
                            pos.x, pos.y); // tell object it was released upon if the event was a release event
                }
            }
        }
    }

    /**
     * Adds a new object to consider mouse input for
     *
     * @param mi     the object
     * @param useCam whether this object will be rendered using a camera
     */
    public void add(MouseInteractive mi, boolean useCam) {
        this.mis.add(mi); // add to list of mouse interactive objects
        this.hoverStates.add(false); // create new hover states
        this.useCam.add(useCam); // save camera usage flag
    }

    /**
     * Gives the mouse input engine a camera to use to convert mouse input to camera-view coordinates. This is used to
     * check fitting boxes of objects that are rendered to the window using the given camera
     *
     * @param cam the camera described above
     */
    public void useCam(Camera cam) {
        this.cam = cam; // save camera reference as members
    }

    /**
     * An interface to be implemented by objects that want to react to being interacted with by a mouse through a
     * mouse input engine. Callbacks need to be saved and invoked by implement classes or else they will not work.
     * However, the static methods saveCallback() and invokeCallback() make this task very straightforward
     */
    public interface MouseInteractive {

        /**
         * The method that will be called by outside scopes to give the implementing object a mouse input callback.
         * Assuming a length-4 array of mouse callbacks is being kept in the correct order, saveMouseCallback() can be
         * called with the implementing object's array of mouse callbacks as a parameter and the callback will
         * automatically be saved correctly (see saveCallback())
         *
         * @param type the mouse input type to give a callback for
         * @param mc   the callback
         */
        void giveCallback(MouseInputType type, MouseCallback mc);

        /**
         * The method that will be called by the mouse input engine whenever mouse interaction with the implementing
         * object occurs. Assuming a length-4 array of mouse callbacks is being kept in the correct order,
         * invokeCallback() can be called with the implementing object's array of mouse callbacks as a parameter and the
         * correct callback will automatically be called (see invokeCallback())
         *
         * @param type the type of mouse input that occurred
         * @param x    the x position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
         *             input engine's camera usage flag for this particular implementing object
         * @param y    the y position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
         *             input engine's camera usage flag for this particular implementing object
         */
        void mouseInteraction(MouseInputType type, float x, float y);

        /**
         * @return the appropriate fitting box to use to consider whether or not the mouse in within/over the
         * implementing object
         */
        FittingBox getFittingBox();

        /**
         * Automatically handles callback saving. This should be called by implementing objects whenever their
         * giveCallback() method is called in order to correctly save the callback
         *
         * @param type the kind of mouse input callback being saved
         * @param mc   the mouse callback being saved
         * @param mcs  in order for this method to work properly, the implementing object must have a length-4 array of
         *             mouse callbacks saved as a member whichs should be passed in here
         */
        static void saveCallback(MouseInputType type, MouseCallback mc, MouseCallback[] mcs) {
            switch (type) { // switch on the type
                case HOVER: // hover
                    mcs[0] = mc; // hover callback resides in mcs[0]
                    break;
                case DONE_HOVERING: // done hovering
                    mcs[1] = mc; // done hovering callback resides in mcs[1]
                    break;
                case PRESS: // press
                    mcs[2] = mc; // press callback resides in mcs[2]
                    break;
                case RELEASE: // release
                    mcs[3] = mc; // release callback resides in mcs[3]
                    break;
            }
        }

        /**
         * Automatically handles callback invoking. This should be called by implementing objects whenever their
         * mouseInteraction() method is called in order to correctly invoke their callbacks
         *
         * @param type the kind of mouse input callback being saved
         * @param mcs  in order for this method to work properly, the implementing object must have a length-4 array of
         *             mouse callbacks saved as a member which should be passed in here
         * @param x
         * @param y
         */
        static void invokeCallback(MouseInputType type, MouseCallback[] mcs, float x, float y) {
            switch (type) { // switch on the type
                case HOVER: // hover
                    if (mcs[0] != null) mcs[0].callback(x, y); // hover callback resides in mcs[0]
                    break;
                case DONE_HOVERING: // done hovering
                    if (mcs[1] != null) mcs[1].callback(x, y); // done hovering callback resides in mcs[1]
                    break;
                case PRESS: // press
                    if (mcs[2] != null) mcs[2].callback(x, y); // press callback resides in mcs[2]
                    break;
                case RELEASE: // release
                    if (mcs[3] != null) mcs[3].callback(x, y); // release callback resides in mcs[3]
                    break;
            }
        }
    }

    /**
     * Defines a mouse callback that can be represented by a lambda expression for simple mouse input usage. This same
     * mouse callback can represent a mouse callback for hovering, hover termination, pressing, and releasing on the
     * mouse. The mouse input engine does not do anything whenever these events occur other than notify the object.
     * Thus, objects that implement mouse interaction should definitely properly save and invoke callbacks for their
     * mouse events to allow outside scopes to respond to them being interacted with. See MouseInteraction for more
     * details on how these callbacks should be saved and invoked
     */
    @FunctionalInterface
    public interface MouseCallback {
        void callback(float x, float y); // called whenever the corresponding mouse event occurs
    }
}
