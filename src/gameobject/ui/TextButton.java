package gameobject.ui;

import gameobject.GameObject;
import graphics.*;
import utils.Global;
import utils.MouseInputEngine;
import utils.Utils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.*;

/*
 * TextButton.java
 * Ambulare
 * Jacob Oaks
 * 4/21/2020
 */

/**
 * This extends TextObjects by implementing mouse interaction and having three separate colors to denote different
 * levels of mouse interaction - practically simulating a button
 */
public class TextButton extends TextObject implements MouseInputEngine.MouseInteractive {

    /**
     * Static Data
     */
    private static final float[] DEFAULT_NORMAL_COLOR = Global.getThemeColor(Global.ThemeColor.GRAY); // default normal
    private static final float[] DEFAULT_HOVER_COLOR = Global.getThemeColor(Global.ThemeColor.WHITE); // default hover
    private static final float[] DEFAULT_PRESS_COLOR = Global.getThemeColor(Global.ThemeColor.GREEN); // default press

    /**
     * Members
     */
    private final float[] defaultC, hoverC, pressC;     // the colors for no interaction, hovering, and pressing
    private final MouseInputEngine.MouseCallback[] mcs; // array for mouse callbacks

    /**
     * Constructs the text button with the given custom colors
     *
     * @param font     the font to use for the text
     * @param text     the text to display
     * @param defaultC the color to use when no interaction is occurring
     * @param hoverC   the color to use when the button is being hovered
     * @param pressC   the color to use when the mouse is pressed down over the button
     */
    public TextButton(Font font, String text, float[] defaultC, float[] hoverC, float[] pressC) {
        super(font, text, defaultC); // call super
        this.defaultC = defaultC; // save default color as member
        this.hoverC = hoverC; // save hover color as member
        this.pressC = pressC; // save press color as members
        this.mcs = new MouseInputEngine.MouseCallback[4]; // create array for callbacks
    }

    /**
     * Constructs the text button with the default colors. The defaults for hover and press are defined above, while
     * the default for no interaction is the same default as a normal text object
     *
     * @param font the font to use for the text
     * @param text the text to display
     */
    public TextButton(Font font, String text) {
        this(font, text, // call other constructor using the given font and text
                new float[]{DEFAULT_NORMAL_COLOR[0], DEFAULT_NORMAL_COLOR[1], DEFAULT_NORMAL_COLOR[2],
                        DEFAULT_NORMAL_COLOR[3]}, // with the default normal color
                new float[]{DEFAULT_HOVER_COLOR[0], DEFAULT_HOVER_COLOR[1], DEFAULT_HOVER_COLOR[2],
                        DEFAULT_HOVER_COLOR[3]}, // the default hover color
                new float[]{DEFAULT_PRESS_COLOR[0], DEFAULT_PRESS_COLOR[1], DEFAULT_PRESS_COLOR[2],
                        DEFAULT_PRESS_COLOR[3]} // and the default press color
        );
    }

    /**
     * Will convert a text button into a textured button with a normal quad model and a regular animation texture. For
     * text buttons whose text will not be changing often, this is a good idea to call. Note that the resulting textured
     * button will not be able to change its text, but it will be much more efficiently rendered
     *
     * @return a textured button representing the text object without changeable text
     */
    public TexturedButton solidify() {

        // calculate width and height of resulting texture
        int w = this.getPixelWidth() * 3; // width is the pixel width of the text for each mouse state
        int h = (int)this.font.getCharHeight(); // height is the font's character height

        // create the aggregation shader program
        ShaderProgram sp = new ShaderProgram(new Utils.Path("/shaders/aggregate_vertex.glsl", true),
                new Utils.Path("/shaders/aggregate_fragment.glsl", true));
        sp.registerUniform("texSampler"); // register the texture sampler uniform
        sp.registerUniform("color"); // register the material color uniform
        sp.registerUniform("isTextured"); // register the texture flag uniform
        sp.registerUniform("blend"); // register the material blend uniform
        sp.registerUniform("w"); // register texture width uniform
        sp.registerUniform("h"); // register texture height uniform
        // register offset uniforms
        sp.registerUniform("x");
        sp.registerUniform("y");

        // create other necessary items
        int[] IDs = Utils.createFBOWithTextureAttachment(w, h); // create FBO w/ texture attached
        // save model's original scale
        float osx = this.model.getXScale();
        float osy = this.model.getYScale();
        // get the model's half-width and half-height
        float w2 = this.model.getWidth() / 2f;
        float h2 = this.model.getHeight() / 2f;
        // scale model to normalized coordinates
        this.model.scaleScale(1 / w2, 1 / h2);
        // scale model to fit one grid cells
        this.model.scaleScale(1f / 3f, 1);

        // pre-render
        glBindFramebuffer(GL_FRAMEBUFFER, IDs[0]); // bind the frame buffer object
        glViewport(0, 0, w, h); // set the viewport to the size of the texture
        glClear(GL_COLOR_BUFFER_BIT); // clear color
        sp.bind(); // bind the shader program
        sp.setUniform("texSampler", 0); // tell the texture sampler to look in texture bank 0
        // set the texture width/height uniforms
        sp.setUniform("w", w);
        sp.setUniform("h", h);
        sp.setUniform("y", 0f); // y is always the center in an animation texture

        // render
        this.material.setColor(defaultC); // default color first
        this.material.setUniforms(sp); // set material uniforms
        sp.setUniform("x", -2f / 3f); // set x to first third of texture
        this.model.render(); // render model
        this.material.setColor(hoverC); // hover color second
        this.material.setUniforms(sp); // set material uniforms
        sp.setUniform("x", 0f); // set x to second third of texture
        this.model.render(); // render model
        this.material.setColor(pressC); // press color third
        this.material.setUniforms(sp); // set material uniforms
        sp.setUniform("x", 2f / 3f); // set x to final third of texture
        this.model.render(); // render model

        // post-render
        sp.unbind(); // unbind shader program
        sp.cleanup(); // cleanup shader program
        Texture t = new Texture(IDs[1], w, h); // create final texture
        glBindFramebuffer(GL_FRAMEBUFFER, 0); // unbind the frame buffer object
        glDeleteFramebuffers(IDs[0]); // delete the frame buffer object
        // reset GL viewport to window's framebuffer size
        glViewport(0, 0, Global.GAME_WINDOW.getFBWidth(), Global.GAME_WINDOW.getFBHeight());

        // scale model to original scale and return material to original color
        this.model.setXScale(osx);
        this.model.setYScale(osy);
        this.material.setColor(defaultC);

        // create corresponding texture button and return result
        return new TexturedButton(new Model(
                super.solidify().getMaterial().getTexture()
                        .getModelCoords(font.getCharHeight() / DEFAULT_SIZE),
                        Model.getStdRectTexCoords(),
                        Model.getStdRectIdx()),
                        new MSAT(t.getID(), w, h, new MSAT.MSATState[] {
                        new MSAT.MSATState(1, 1f),
                        new MSAT.MSATState(1, 1f),
                        new MSAT.MSATState(1, 1f)
        }));
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
     * Responds to mouse interaction by invoking any corresponding callbacks and updating the text colors
     *
     * @param type the type of mouse input that occurred
     * @param x    the x position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
     *             input engine's camera usage flag for this particular implementing object
     * @param y    the y position of the mouse in world coordinate or camera-view coordinates, depending on the mouse
     */
    @Override
    public void mouseInteraction(MouseInputEngine.MouseInputType type, float x, float y) {
        switch (type) { // switch on type of input
            case HOVER: // on hover
            case RELEASE: // or release
                this.material.setColor(this.hoverC); // change to hover color (mouse hasn't left yet)
                break;
            case DONE_HOVERING: // on done hovering
                this.material.setColor(this.defaultC); // change to default color
                break;
            case PRESS: // on press
                this.material.setColor(this.pressC); // change to pressed color
                break;
        }
        MouseInputEngine.MouseInteractive.invokeCallback(type, this.mcs, x, y); // invoke callback
    }

    /**
     * Updates the opacity of the text button
     *
     * @param opacity the new opacity from 0f to 1f
     */
    @Override
    public void setOpacity(float opacity) {
        super.setOpacity(opacity); // update current color
        this.defaultC[3] = opacity; // update default color
        this.hoverC[3] = opacity; // update hover color
        this.pressC[3] = opacity; // update press colors
    }
}
