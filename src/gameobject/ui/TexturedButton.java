package gameobject.ui;

import gameobject.GameObject;
import graphics.MSAT;
import graphics.Material;
import graphics.Model;
import utils.MouseInputEngine;

/*
 * TexturedButton.java
 * Ambulare
 * Jacob Oaks
 * 4/21/20
 */

/**
 * This extends GameObject to implement mouse interaction by using a multi-state animated texture with separate states
 * for no mouse interaction, mouse hovering, and mouse pressing - practically simulating a button
 */
public class TexturedButton extends GameObject implements MouseInputEngine.MouseInteractive {

    /**
     * Members
     */
    private final MouseInputEngine.MouseCallback[] mcs; // array of callbacks as outlined by mouse interaction interface

    /**
     * Constructs the textured button with animation within texture states state
     *
     * @param model         the model to use for textured button
     * @param texResPath    the resource-relative path to the texture. The texture should have its frames in the
     *                      following order: default frames, then hover frames, then pressed frames
     * @param defaultFrames how many frames in the texture correspond to the default state
     * @param hoverFrames   how many frames in the texture correspond to the hover state
     * @param pressedFrames how many frames in the texture correspond to the pressed state
     * @param frameTime     how much time (in seconds) to show a frame
     */
    public TexturedButton(Model model, String texResPath, int defaultFrames, int hoverFrames, int pressedFrames,
                          float frameTime) {
        super(model, null);
        // create material using a multi-state animated texture
        this.material = new Material(new MSAT(texResPath, true, new MSAT.MSATState[]{
                new MSAT.MSATState(defaultFrames, frameTime),
                new MSAT.MSATState(hoverFrames, frameTime),
                new MSAT.MSATState(pressedFrames, frameTime)}));
        mcs = new MouseInputEngine.MouseCallback[4]; // create array for callbacks
    }

    /**
     * Constructs the textured button without any animation within mouse interactivity states (the texture will still
     * change when the mouse state changes however). This constructor basically assumes that the texture at the given
     * path has one frame for each mouse interactivity state
     *
     * @param model      the model to use for textured button
     * @param texResPath the resource-relative path to the texture. The texture should have its frames in the
     *                   following order: default frame, then the hover frame, then the pressed frame
     */
    public TexturedButton(Model model, String texResPath) {
        this(model, texResPath, 1, 1, 1, 0);
    }

    /**
     * Saves the given mouse callback to be called when the given kind of input occurs
     *
     * @param type the mouse input type to give a callback for
     * @param mc   the callback
     */
    @Override
    public void giveCallback(MouseInputEngine.MouseInputType type, MouseInputEngine.MouseCallback mc) {
        MouseInputEngine.MouseInteractive.saveCallback(type, mc, this.mcs); // save callback
    }

    /**
     * Responds to mouse interaction by invoking any corresponding callbacks and updating the texture state
     *
     * @param type the type of mouse input that occurred
     * @param x    the x position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
     *             input engine's camera usage flag for this particular implementing object
     * @param y    the y position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
     */
    @Override
    public void mouseInteraction(MouseInputEngine.MouseInputType type, float x, float y) {
        switch (type) { // switch on type of interaction
            case HOVER: // on hover
            case RELEASE: // or release
                ((MSAT) this.material.getTexture()).setState(1); // use hovered state of texture
                break;
            case DONE_HOVERING: // on done hovering
                ((MSAT) this.material.getTexture()).setState(0); // use default state of texture
                break;
            case PRESS: // on press
                ((MSAT) this.material.getTexture()).setState(2); // use press state of texture
                break;
        }
        MouseInputEngine.MouseInteractive.invokeCallback(type, mcs, x, y); // invoke callback
    }
}
