package gameobject;

import graphics.MSAT;
import graphics.Material;
import graphics.Model;

/**
 * This extends GameObject to implement mouse interactivity by using a multi-state animated texture with separate states
 * for no mouse interaction, mouse hovering, and mouse pressing
 */
public class TexturedButton extends GameObject implements MIHSB.MouseInteractable {

    /**
     * Members
     */
    private final int MIID; // the mouse interactivity id given in the constructor

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
     * @param MIID          the mouse interactivity ID of the button
     */
    public TexturedButton(Model model, String texResPath, int defaultFrames, int hoverFrames, int pressedFrames,
                          float frameTime, int MIID) {
        super(model, null);
        // create material using a multi-state animated texture
        this.material = new Material(new MSAT(texResPath, true, new MSAT.MSATState[]{
                new MSAT.MSATState(defaultFrames, frameTime),
                new MSAT.MSATState(hoverFrames, frameTime),
                new MSAT.MSATState(pressedFrames, frameTime)}));
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
     * Responds to mouse hovering by updating the state of the MSAT
     *
     * @param x the x position of the mouse in either world or camera-view space, depending on whether the
     *          implementing object reacts to a camera
     * @param y the y position of the mouse in either world or camera-view space, depending on whether the
     *          implementing object reacts to a camera
     */
    @Override
    public void onHover(float x, float y) {
        ((MSAT) this.material.getTexture()).setState(1);
    }

    /**
     * Responds to mouse hovering ending by updating the state of the MSAT
     */
    @Override
    public void onDoneHovering() {
        ((MSAT) this.material.getTexture()).setState(0);
    }

    /**
     * Responds to a mouse press by updating the state of the MSAT
     */
    @Override
    public void onPress() {
        ((MSAT) this.material.getTexture()).setState(2);
    }

    /**
     * Responds to a mouse release by updating the state of the MSAT
     */
    @Override
    public void onRelease() {
        ((MSAT) this.material.getTexture()).setState(1);
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
}
