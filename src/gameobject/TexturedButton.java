package gameobject;

import graphics.Material;
import graphics.Model;
import graphics.Texture;

/**
 * This extends GameObject to implement mouse interactivity and have three different texture animations for each
 * state of mouse interactivity (no interaction, hover, and press) - practically simulating a button
 */
public class TexturedButton extends GameObject implements MIHSB.MouseInteractable {

    /**
     * Members
     */
    private final int MIID; // the mouse interactivity id given in the constructor

    /**
     * Constructs the textured button with animation within any single mouse interactivity state
     *
     * @param model         the model to use for textured button
     * @param texResPath    the resource-relative path to the texture. The texture should have its frames in the
     *                      following order: default frames, then hover frames, then pressed frames
     * @param defaultFrames how many frames in the texture correspond to the default state
     * @param hoverFrames   how many frames in the texture correspond to the hover state
     * @param pressedFrames how many frames in the texture correspond to the pressed state
     * @param frameTime     how much time (in seconds) to show a frame
     * @param MIID          the mouse interactivity ID of the button
     */
    public TexturedButton(Model model, String texResPath, int defaultFrames, int hoverFrames, int pressedFrames,
                          float frameTime, int MIID) {
        super(model, null);
        this.material = new TexturedButtonMaterial(new Texture(texResPath, true), defaultFrames, hoverFrames,
                pressedFrames, frameTime); // create extended textured button material
        this.MIID = MIID;
    }

    /**
     * Constructs the textured button without any animation within mouse interactivity states (the texture will still
     * change when the mouse state changes however). This constructor basically assumes that the texture at the given
     * path has one frame for each mouse interactivity state
     *
     * @param model      the model to use for textured button
     * @param texResPath the resource-relative path to the texture. The texture should have its frames in the
     *                   following order: default frame, then the hover frame, then the pressed frame
     * @param MIID       the mouse interactivity ID of the button
     */
    public TexturedButton(Model model, String texResPath, int MIID) {
        this(model, texResPath, 1, 1, 1, 0, MIID);
    }

    /**
     * Responds to mouse hovering by updating the state of the material
     *
     * @param x the x position of the mouse in either world or camera-view space, depending on whether the
     *          implementing object reacts to a camera
     * @param y the y position of the mouse in either world or camera-view space, depending on whether the
     *          implementing object reacts to a camera
     */
    @Override
    public void onHover(float x, float y) {
        ((TexturedButtonMaterial) this.material).updateMouseState(1);
    }

    /**
     * Responds to mouse hovering ending by updating the state of the material
     */
    @Override
    public void onDoneHovering() {
        ((TexturedButtonMaterial) this.material).updateMouseState(0);
    }

    /**
     * Responds to a mouse press by updating the state of the material
     */
    @Override
    public void onPress() {
        ((TexturedButtonMaterial) this.material).updateMouseState(2);
    }

    /**
     * Responds to a mouse release by updating the state of the material
     */
    @Override
    public void onRelease() {
        ((TexturedButtonMaterial) this.material).updateMouseState(1);
    }

    /**
     * The mouse interaction ID of a text button is the ID given to it when constructing
     *
     * @return the ID given when constructing
     */
    @Override
    public int getID() {
        return this.MIID;
    }

    /**
     * Tailors a material to have different animations depending on a mouse state. Used by textured button for
     * exactly this purpose
     */
    private class TexturedButtonMaterial extends Material {

        /**
         * Members
         */
        private final int[] frameCounts; // the amount of frames for each state of mouse interactivity
        private int mouseState = 0;      // the current mouse state - decides which set of frames to use
        private final boolean animated;  /* flags whether this button is animated within any mouse state's set of
                                            frames. If false, update calculations based on animation can be avoided */
        private int stateFrame = 0;      // the current frame within the subset of frames for the current mouse state

        /**
         * Constructor
         *
         * @param texture       the texture for the material
         * @param defaultFrames the amount of frames for the default mouse state (no hover or press)
         * @param hoverFrames   the amount of frames for the hover mouse state
         * @param pressedFrames the amount of frames for the press mouse state
         * @param frameTime     how much time (in seconds) each frame should receive
         */
        public TexturedButtonMaterial(Texture texture, int defaultFrames, int hoverFrames, int pressedFrames,
                                      float frameTime) {
            super(texture, null, BLEND_MODE.NONE, defaultFrames + hoverFrames + pressedFrames, frameTime,
                    false);
            this.frameCounts = new int[]{defaultFrames, hoverFrames, pressedFrames}; // keep count of frames for each
            // if there is more than one frame for any mouse state then that total frames will be greater than 3
            this.animated = defaultFrames + hoverFrames + pressedFrames > 3;
        }

        /**
         * Updates the textured button material by keeping time for animation if animated
         *
         * @param interval the amount of time to account for
         */
        @Override
        public void update(float interval) {
            if (this.animated) { // if this isn't animated, no need to do timekeeping calculations
                this.frameTimeLeft -= interval; // update frame time left
                if (this.frameTimeLeft <= 0f) { // if frame is over
                    this.frameTimeLeft += this.frameTime; // reset time
                    this.stateFrame++; // move to next frame
                    if (this.stateFrame >= this.frameCounts[mouseState]) // if end of frames for current mouse state
                        this.stateFrame = 0; // return to first frame
                    this.setAppropriateFrame(); // calculate the appropriate frame in the entire texture
                }
            }
        }

        /**
         * Calculates the appropriate frame of the texture to use based on the mouse state and the current frame
         * within that state
         */
        private void setAppropriateFrame() {
            // make sure state frame is actually within the bounds of the corresponding mouse state's frame count
            this.stateFrame = stateFrame % this.frameCounts[mouseState];
            this.frame = this.stateFrame; // start with state frame
            if (this.mouseState > 0) { // if hovering or pressing
                this.frame += this.frameCounts[0]; // we skip default frames
                if (this.mouseState > 1) this.frame += this.frameCounts[1]; // if pressing, skip hovering frames
            }
        }

        /**
         * Updates the mouse state of the textured button material and recalculates the correct frame
         *
         * @param mouseState the new mouse state
         */
        public void updateMouseState(int mouseState) {
            this.mouseState = mouseState; // save new mouse state
            this.setAppropriateFrame(); // update correct frame
        }
    }
}
