package gameobject.ui;

import gameobject.GameObject;
import graphics.*;
import utils.Global;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static gameobject.ui.TextObject.DEFAULT_SIZE;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Enhances the normal functionality of a text object by rendering a background material and having multiple lines
 * with the ability to have different configurations for each. See the Line class for more info on configuring a line.
 * Enhanced text objects are meant to be used for scenarios when a lot of text needs to be displayed. However, this can
 * get costly very quick with the increase in vertices. Thus, whenever the text won't change, it is imperative that the
 * solidify() be called to retrieve a single quad version of the enhanced text object which will render much quicker.
 * Enhanced text objects cannot be rotated
 */
public class EnhancedTextObject extends GameObject {

    /**
     * Members
     */
    protected List<Line> lines;         // the lines being displayed
    private List<TextObject> tos;       // the text objects displaying the lines
    private final float borderPadding;  // the padding around the edges of the contents
    private float overallXScale = 1f;   // overall x scale applied to contents (not including line scale)
    private float overallYScale = 1f;   // overall y scale applied to contents (not including line scale)

    /**
     * Constructs the enhanced text object with the given string
     * @param backgroundMaterial the material to use to render the background. If null, no background will be
     *                           rendered
     * @param font the font to use for the text
     * @param text the text to display. Multiple lines can be denoted by used of the newline character ('\n'). The
     *             resulting lines will have the default line properties
     * @param borderPadding the padding to place around the edges of the lines
     */
    public EnhancedTextObject(Material backgroundMaterial, Font font, String text, float borderPadding) {
        this(backgroundMaterial, font, text.split("\\r?\\n"), borderPadding); // split text and call other constructor
    }

    /**
     * Constructs the enhanced text object with the given array of strings
     * @param backgroundMaterial the material to use to render the background. If null, no background will be
     *                           rendered
     * @param font the font to use for the text
     * @param text the text to display where each element in the array represents a line. The resulting lines will have
     *             the default line properties
     * @param borderPadding the padding to place around the edges of the lines
     */
    public EnhancedTextObject(Material backgroundMaterial, Font font, String[] text, float borderPadding) {
        super(Model.getStdGridRect(1, 1), backgroundMaterial != null // call super with rectangle model
                ? backgroundMaterial // use background material if exists
                : new Material(new float[] { 1f, 1f, 1f, 0f })); // otherwise use a transparent material
        this.borderPadding = borderPadding; // save border padding as member
        List<Line> l = new ArrayList<>(); // create a list of lines
        for (int i = 0; i < text.length; i++) l.add(new Line(text[i])); // populate the lines using given text
        this.createTextObjects(font, l); // create the corresponding text objects
    }

    /**
     * Constructs the enhanced text object with the given list of configured lines
     * @param backgroundMaterial the material to use to render the background. If null, no background will be
     *                           rendered
     * @param font the font to use for the text
     * @param lines the configured lines to display (see Line class)
     * @param borderPadding the padding to place around the edges of the lines
     */
    public EnhancedTextObject(Material backgroundMaterial, Font font, List<Line> lines, float borderPadding) {
        super(Model.getStdGridRect(1, 1), backgroundMaterial != null // call super with rectangle model
                ? backgroundMaterial // use background material if exists
                : new Material(new float[] { 1f, 1f, 1f, 0f })); // otherwise use a transparent material
        this.borderPadding = borderPadding; // save border padding as member
        this.createTextObjects(font, lines); // create corresponding text objects
    }

    /**
     * Creates the enhanced text object's text objects based off of the given list of configured lines
     * @param font the font to use for the text objects
     * @param lines the list of configured lines
     */
    private void createTextObjects(Font font, List<Line> lines) {
        this.lines = lines; // save lines as member
        this.tos = new ArrayList<>(); // initialize text object list
        for (Line line : lines) this.tos.add(createTextObject(font, line)); // create text objects
        this.position(); // scale the object and the contents
    }

    /**
     * Creates a text object corresponding to a given line
     * @param font the font to used for the text
     * @param line the line whose configuration to use for the text object
     * @return the corresponding text object
     */
    private TextObject createTextObject(Font font, Line line) {
        TextObject to = new TextObject(font, line.text); // create the corresponding text object
        // scale according to line configuration and overall scale
        to.setScale(line.scale * this.overallXScale, line.scale * this.overallYScale);
        if (line.color != null) to.getMaterial().setColor(line.color); // color it according to line configuration
        return to; // return final product
    }

    /**
     * Scales the background to fit the contents and positions the contents accordingly
     */
    private void position() {
        float w = 0f; // a maximum width will be determined
        float h = this.borderPadding * 2f; // a running sum of text object heights and padding valuess will be kept
        for (int i = 0; i < this.lines.size(); i++) { // for each sum
            // if it is longer than the current recorded width, record it as the new width
            w = Math.max(w, this.tos.get(i).getWidth() + (this.borderPadding * 2f));
            float pads = 2; // normally, a padding above and below would be needed
            if (i == 0) pads --; // if first element, one less padding needed
            if (i == this.lines.size() - 1) pads--; // if last element, one less padding needed
            h += this.tos.get(i).getHeight() + pads * this.lines.get(i).padding; // add height, padding to total height
        }
        this.model.setScale(w, h); // scale the object to fit all lines and padding
        float y = this.getY() + (this.getHeight() / 2) - borderPadding; // start from the top of the object
        float lx = this.getX() - (this.getWidth() / 2) + borderPadding; // left-most x of the object
        float rx = this.getX() + (this.getWidth() / 2) - borderPadding; // right-most x of the object
        for (int i = 0; i < this.lines.size(); i++) { // go through each line
            if (i != 0) y -= this.lines.get(i).padding; // besides the first line, use additional padding
            float ih2 = this.tos.get(i).getHeight() / 2; // calculate the text object's half width
            y -= ih2; // iterate y by the text object's half-width
            this.tos.get(i).setPos( // set the position
                    this.lines.get(i).alignment == Line.Alignment.LEFT // if left aligned
                            ? lx + this.tos.get(i).getWidth() / 2 // set x to the left
                            : this.lines.get(i).alignment == Line.Alignment.CENTER // if center aligned
                            ? this.getX() // set x to center
                            : rx - this.tos.get(i).getWidth() / 2 // if right aligned, set x to the right
                    , y); // then place the item there
            y -= ih2; // iterate y again by the current text object's half-width
            y -= this.lines.get(i).padding; // add padding before next text object
        }
    }

    /**
     * Responds to movement by re-positioning contained text objects and re-scaling the background model
     */
    @Override
    protected void onMove() {
        super.onMove(); // call super's onMove()
        this.position(); // re-position
    }

    /**
     * Renders the background and each line of text
     * @param sp the shader program to use to render the game object
     */
    @Override
    public void render(ShaderProgram sp) {
        if (this.visible) {
            super.render(sp); // render background
            for (TextObject to : this.tos) to.render(sp); // render each line of text
        }
    }

    /**
     * Solidifies the enhanced text object into a single-quad modeled game object. For enhanced text objects whose text
     * won't change often, this is imperative as it results in much more efficient rendering. Note that the resulting
     * game object will not be able to have its text changed
     * @return a game object representing the enhanced text object without changeable text. Note that, regardless of the
     * scaling of this enhanced text object, the returned game object will be the same size but will restart at a scale
     * of 1f
     */
    public GameObject solidify() {
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
        // register width/height division uniforms
        sp.registerUniform("wDiv");
        sp.registerUniform("hDiv");

        // figure out a horizontal resolution to use to size the resulting texture
        float resolutionX = 0;
        for (TextObject to : this.tos) { // for each text object
            float rX = (float) to.getPixelWidth(); // get its resolution relative to the pixel width of the text object
            if (rX > resolutionX) resolutionX = rX; // if greater than current greatest, update
        }
        // add border to resolution
        float bpfactor = 2 * this.borderPadding / (this.getWidth() - 2 * this.borderPadding);
        resolutionX += bpfactor * resolutionX;
        float resolutionY = (this.getHeight() / this.getWidth()) * resolutionX;
        // use the item's aspect ratio to figure out the vertical resolution from the horizontal resolution
        // convert to integers rounded up to not lose any resolution
        int w = (int)resolutionX;
        int h = (int)resolutionY;
        // create FBO w/ texture attached, large enough to support our resolution
        int[] IDs = Utils.createFBOWithTextureAttachment(w, h);

        // pre-render
        glBindFramebuffer(GL_FRAMEBUFFER, IDs[0]); // bind the frame buffer object
        glViewport(0, 0, w, h); // set the viewport to the size of the texture
        glClear(GL_COLOR_BUFFER_BIT); // clear color
        sp.bind(); // bind the shader program
        sp.setUniform("texSampler", 0); // tell the texture sampler to look in texture bank 0
        // set texture width/height uniforms
        sp.setUniform("w", w);
        sp.setUniform("h", h);
        // set width/height division uniforms to normalize model coordinates
        sp.setUniform("wDiv", this.model.getWidth() / 2f);
        sp.setUniform("hDiv", this.model.getHeight() / 2f);

        // render
        float x = this.getX();
        float y = this.getY();
        this.setPos(0, 0);
        this.render(sp); // render the enhanced text object
        this.setPos(x, y);

        // post-render
        sp.unbind(); // unbind shader program
        sp.cleanup(); // cleanup shader program
        Texture t = new Texture(IDs[1], w, h); // create final texture
        glBindFramebuffer(GL_FRAMEBUFFER, 0); // unbind the frame buffer object
        glDeleteFramebuffers(IDs[0]); // delete the frame buffer object
        // reset GL viewport to window's framebuffer size
        glViewport(0, 0, Global.gameWindow.getFBWidth(), Global.gameWindow.getFBHeight());

        // create and return game object
        Model m = new Model(t.getModelCoords(resolutionY / this.getHeight()),
                Model.getStdRectTexCoords(), Model.getStdRectIdx()); // create the model
        return new GameObject(this.getX(), this.getY(), m, new Material(t)); // create and return game object
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
     * Sets the alignment for all lines
     * @param alignment the alignment to set all lines to. See Line class below for more info
     */
    public void alignAll(Line.Alignment alignment) {
        for (Line line : this.lines) line.alignment = alignment; // update alignments
        this.position(); // reposition everything
    }

    /**
     * Scales all text objects by the given horizontal (x) factor (as multiplied with the line configurations' scales)
     *
     * @param x the x scaling factor to use
     */
    @Override
    public void setXScale(float x) {
        this.overallXScale = x; // save as member
        for (int i = 0; i < this.lines.size(); i++) // for each list item in the list
            this.tos.get(i).setXScale(this.lines.get(i).scale * x); // set its horizontal scale
        this.position(); // reposition
    }

    /**
     * Scales all text objects by the given vertical (y) factor (as multiplied with the line configurations' scales)
     *
     * @param y the y scaling factor to use
     */
    @Override
    public void setYScale(float y) {
        this.overallYScale = y; // save as member
        for (int i = 0; i < this.lines.size(); i++) // for each list item in the list
            this.tos.get(i).setYScale(this.lines.get(i).scale * y); // set its vertical scale
        this.position(); // reposition
    }

    /**
     * Scales all text objects by the given horizontal (x) and vertical (y) factors (as multiplied with the line
     * configurations' scales)
     *
     * @param x the x scaling factor to use
     * @param y the y scaling factor to use
     */
    @Override
    public void setScale(float x, float y) {
        // save as members
        this.overallXScale = x;
        this.overallYScale = y;
        for (int i = 0; i < this.lines.size(); i++) {// for each list item in the list
            this.tos.get(i).setXScale(this.lines.get(i).scale * x); // set its horizontal scale
            this.tos.get(i).setYScale(this.lines.get(i).scale * y); // set its vertical scale
        }
        this.position(); // reposition
    }

    /**
     * Reacts to attempts to rotate by logging and ignoring the attempt
     *
     * @param r the attempted new rotation value
     */
    @Override
    public void setRotRad(float r) {
        Utils.log("Attempted to rotate an enhanced text object. Ignoring.", this.getClass(), "setRotRad",
                false); // log and ignore the attempt to rotate
    }

    /**
     * Updates the text at the given line
     * @param line the index of the line whose text should be updated, where index 0 refers to the top line. If out
     *             of bounds, the occurrence will be logged and ignored
     * @param text the new text to display
     */
    public void setLineText(int line, String text) {
        if (line < 0 || line >= this.lines.size()) // if the index is out of bounds
            Utils.log("Invalid line index '" + line + "' when there are " + lines.size() + " lines.",
                    this.getClass(), "setLineText", false); // log and ignore
        else if (!this.lines.get(line).getText().equals(text)) { // if the text is actually new
            this.lines.get(line).setText(text); // update the line's text
            this.tos.get(line).setText(text); // update the corresponding text object
            this.position(); // and re-position
        }
    }

    /**
     * Adds a line with the default configuration to the bottom of the enhanced text object
     * @param font the font to use for the new line
     * @param text the text to display in the line
     * @return the index of the new line in the enhanced text object
     */
    public int addLine(Font font, String text) {
        return this.addLine(font, new Line(text)); // create default line using given text, add, and return index
    }

    /**
     * Adds the given line to the bottom of the enhanced text object
     * @param font the font to use for the new line
     * @param line the line to add
     * @return the index of the new line in the enhanced text object
     */
    public int addLine(Font font, Line line) {
        this.lines.add(line); // add the line
        this.tos.add(createTextObject(font, line)); // add the corresponding text object
        this.position(); // position
        return this.lines.size() - 1; // return the new line's index
    }

    /**
     * Removes the line with the corresponding index from the enhanced text object
     * @param line the index of the line to remove. If invalid, the occurrence will be logged and ignored
     */
    public void removeLine(int line) {
        if (line >= 0 && line < this.lines.size()) { // if valid index
            this.lines.remove(line); // remove the line
            this.tos.remove(line); // remove the corresponding text object
            this.position(); // and re-position
        } else Utils.log("Invalid line index '" + line + "' when there are " + lines.size() + " lines.",
                    this.getClass(), "removeLine", false); // log and ignore
    }

    /**
     * Cleans up the enhanced text object's background and contained text objected
     */
    @Override
    public void cleanup() {
        super.cleanup(); // cleanup background
        for (TextObject to : this.tos) to.cleanup(); // cleanup text objects
    }

    /**
     * Represents a single line in an enhanced text object. This class provides various configuration options for
     * customizing each individual line
     */
    public static class Line {

        /**
         * Represents alignment of a line
         */
        public enum Alignment { LEFT, CENTER, RIGHT }

        /**
         * Static Data
         */
        public static final Alignment DEFAULT_ALIGNMENT = Alignment.LEFT; // default alignment value
        public static final float DEFAULT_SCALE = 0.5f;                   // default scale value
        public static final float DEFAULT_PADDING = 0.005f;                // default padding value

        /**
         * Members
         */
        private final float[] color; // the line's color
        private final float scale;   // how the text should scale
        private final float padding; // how much padding should be above/below the line
        private Alignment alignment; // how the text should be aligneds
        private String text;         // the text the line should contain

        /**
         * Constructs the line with completely custom settings
         * @param text the text the line should contain
         * @param color the color of the line's text
         * @param alignment how the line's text should be aligned
         * @param scale the scale of the text
         * @param padding how much padding should be above/below the line
         */
        public Line(String text, float[] color, Alignment alignment, float scale, float padding) {
            this.text = text; // save text as member
            this.color = color; // save color as member
            this.alignment = alignment; // save alignment as member
            this.scale = scale; // save scale as member
            this.padding = padding; // save padding as member
        }

        /**
         * Constructs the with the given text and with default configuration
         * @param text the text the line should contain
         */
        public Line(String text) {
            // call other constructor with default configuration values
            this(text, null, DEFAULT_ALIGNMENT, DEFAULT_SCALE, DEFAULT_PADDING);
        }

        /**
         * Updates the text the line should contain
         * @param text the new text the line should contain
         */
        public void setText(String text) {
            this.text = text; // update text member
        }

        /**
         * @return the text the line should contain
         */
        public String getText() {
            return this.text;
        }
    }
}
