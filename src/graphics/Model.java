package graphics;

import org.lwjgl.system.MemoryUtil;
import utils.BoundingBox;
import utils.Global;
import utils.Pair;
import utils.Utils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * Represents a model with model coordinates, texture coordinates, and indices
 * This model supports scaling and rotating. However, in order for these to work as intended, the center of the model
 * MUST be (0, 0)
 */
public class Model {

    /**
     * Data
     */
    protected final int ids[];           /* integer array to store the various GL object ids: [0] - VAO ID,
                                            [1] - model coordinate VBO ID, [2] - texture coordinate VBO ID,
                                            [4] - index VBO ID */
    private final int idx;               // the amount of vertices this shape has
    private float[] modelCoords;         // the model's model coordinates
    private float sx = 1f, sy = 1f;      // horizontal and vertical scale
    private float r = 0f;                // rotation in radians
    private float w = 0, h = 0;          // width and height of the model in model coordinates
    private float uw = 0, uh = 0;        // width and height of the model when not rotated
    private boolean outdatedSize = true; /* whenever scale or rotation of the model is changed, this flag will be set
                                            to true but the model won't re-calculate width and height until the
                                            corresponding methods are called while this flag is true to save computing
                                            power */

    /**
     * Returns the model coordinates appropriate for a rectangular model with the given width/height in cells
     * @param w the width of the rectangular model in cells
     * @param h the height of the rectangular model in cells
     * @return the standard rectangular model coordinates
     */
    public static float[] getGridRectModelCoords(int w, int h) {
        return new float[] {
                -w * Global.GRID_CELL_SIZE / 2, -h * Global.GRID_CELL_SIZE / 2, // bottom left
                -w * Global.GRID_CELL_SIZE / 2,  h * Global.GRID_CELL_SIZE / 2, // top left
                 w * Global.GRID_CELL_SIZE / 2,  h * Global.GRID_CELL_SIZE / 2, // top right
                 w * Global.GRID_CELL_SIZE / 2, -h * Global.GRID_CELL_SIZE / 2  // bottom right
        };
    }

    /**
     * @return the standard rectangle texture coordinates
     */
    public static float[] getStdRectTexCoords() {
        return new float[] {
                0.0f, 1.0f, // top left
                0.0f, 0.0f, // bottom left
                1.0f, 0.0f, // bottom right
                1.0f, 1.0f  // top right
        };
    }

    /**
     * @return the standard rectangle indices
     */
    public static int[] getStdRectIdx() {
        return new int[] { 0, 1, 3,   // first triangle  - bottom left, top left, bottom right
                           3, 1, 2 }; // second triangle - bottom right, top left, top right
    }

    /**
     * Creates a standard rectangular model with the given width and height in cells
     * @param w the width of the rectangular model in cells
     * @param h the height of the rectangular model in cells
     * @return the standard rectangular model
     */
    public static Model getStdGridRect(int w, int h) {
        return new Model(getGridRectModelCoords(w, h), getStdRectTexCoords(), getStdRectIdx());
    }

    /**
     * Constructs the model
     * The assumes input given in sequences of triangles
     * @param modelCoords the positions of the vertices (2-dimensional)
     * @param texCoords the texture coordinates of the vertices (2-dimensional)
     * @param indices the index of the vertices. For example, if you have two triangles to make a square, the two
     *                overlapping points can be given the same index. This helps GL avoid redundant vertex rendering
     */
    public Model(float[] modelCoords, float[] texCoords, int[] indices) {

        // set and initialize members
        this.modelCoords = modelCoords; // save model coordinates
        this.idx = indices.length; // save index count
        this.ids = new int[4]; // initialize ID array

        // create buffers, generation VAO
        FloatBuffer fb = null; // buffer to use for loading float data into VBOs
        IntBuffer ib = null; // buffer to use for loading integer data into VBOs
        this.ids[0] = glGenVertexArrays(); // generate the vertex array object
        glBindVertexArray(this.ids[0]); // bind the vertex array object

        // process model coordinate data
        this.updateModelCoordsVBO(); // update model coordinates
        glBindVertexArray(this.ids[0]); // bind the vertex array object

        // process texture coordinate data
        fb = MemoryUtil.memAllocFloat(texCoords.length); // allocate buffer space for tex coord data
        fb.put(texCoords).flip(); // put texture coordinate data into buffer
        this.ids[2] = glGenBuffers(); // generate texture coordinate vertex buffer object
        glBindBuffer(GL_ARRAY_BUFFER, this.ids[2]); // bind texture coordinate vertex buffer object
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW); // put tex coord data into tex coord VBO
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0); // put VBO into VAO
        MemoryUtil.memFree(fb); // free buffer

        // process index data
        ib = MemoryUtil.memAllocInt(indices.length); // allocate buffer space for index data
        ib.put(indices).flip(); // put index data into buffer
        this.ids[3] = glGenBuffers(); // generate index vertex buffer object
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.ids[3]); // bind index vertex buffer object
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW); // put index data into index VBO
        MemoryUtil.memFree(ib); // free buffer

        // unbind VBO, VAO
        glBindBuffer(GL_ARRAY_BUFFER, 0); // unbind VBO
        glBindVertexArray(0); // unbind VAO

        // calculate initial size
        this.calculateSize(); // calculate size
        this.uw = this.w; // save width as un-rotated width
        this.uh = this.h; // save height as un-rotated height
    }

    /**
     * Updates the width and height members of the model
     */
    private void calculateSize() {
        if (modelCoords.length == 0) { // if empty Model
            this.w = this.h = 0; // set width and height to 0
        } else { // otherwise
            float minX = modelCoords[0], minY = modelCoords[1]; // initialize minimum x and y to the first x and y
            float maxX = minX, maxY = minY; // initialize max x and y to the first x and y
            for (int i = 2; i < modelCoords.length; i++) { // for the rest of the model coordinates
                if (i % 2 == 0) { // if an x coordinate
                    minX = Math.min(minX, modelCoords[i]); // check for smaller x
                    maxX = Math.max(maxX, modelCoords[i]); // check for larger x
                } else { // if a y coordinate
                    minY = Math.min(minY, modelCoords[i]); // check for smaller y
                    maxY = Math.max(maxY, modelCoords[i]); // check for larger y
                }
            }
            this.w = Math.abs(maxX - minX); // store width of model
            this.h = Math.abs(maxY - minY); // store height of model
        }
        this.outdatedSize = false; // set size update flag to false
    }

    /**
     * Updates the model coordinates VBO with the current model coordinates member of the model
     */
    private void updateModelCoordsVBO() {
        glBindVertexArray(this.ids[0]); // bind the vertex array object
        FloatBuffer b = MemoryUtil.memAllocFloat(this.modelCoords.length); // allocate buffer space for position data
        b.put(this.modelCoords).flip(); // put position data into buffer
        this.ids[1] = glGenBuffers(); // generate position vertex buffer object
        glBindBuffer(GL_ARRAY_BUFFER, this.ids[1]); // bind position vertex buffer object
        glBufferData(GL_ARRAY_BUFFER, b, GL_STATIC_DRAW); // put position data into position VBO
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0); // put VBO into VAO
        glBindBuffer(GL_ARRAY_BUFFER, 0); // unbind VBO
        glBindVertexArray(0); // unbind the vertex array object
    }

    /**
     * Renders the model
     */
    public void render() {
        glBindVertexArray(this.ids[0]); // bind vao
        glEnableVertexAttribArray(0); // enable model coordinate vbo
        glEnableVertexAttribArray(1); // enable texture coordinate vbo
        glDrawElements(GL_TRIANGLES, this.idx, GL_UNSIGNED_INT, 0); // draw model
        glDisableVertexAttribArray(0); // disable model coordinate vbo
        glDisableVertexAttribArray(1); // disable texture coordinate vbo
        glBindVertexArray(0); // disable vao
    }

    /**
     * Sets the scaling factors of the model to the given scaling factors
     * @param x the x scaling factor to use
     * @param y the y scaling factor to use
     */
    public void setScale(float x, float y) {
        float mx = x / this.sx, my = y / this.sy; // get multiplicative factor to use on each model coordinate
        this.sx = x; this.sy = y; // update members
        for (int i = 0; i < this.modelCoords.length; i++) { // for each model coordinate
            if (i % 2 == 0) this.modelCoords[i] *= mx; // if x coordinate, use x factor on it
            else this.modelCoords[i] *= my; // if y coordinate, use y factor on it
        }
        this.updateModelCoordsVBO(); // update VBO
        this.outdatedSize = true; // flag that the size members are outdated
    }

    /**
     * Sets the x scaling factor of the model to the given x scaling factor
     * @param x the x scaling factor to use
     */
    public void setXScale(float x) {
        this.setScale(x, this.sy); // call other method
    }

    /**
     * Sets the y scaling factor of the model to the given y scaling factor
     * @param y the y scaling factor to use
     */
    public void setYScale(float y) {
        this.setScale(this.sx, y); // call other method
    }

    /**
     * Sets the rotation of the model
     * @param r the new rotation in radians
     */
    public void setRotationRad(float r) {
        r = r % (2 * (float)Math.PI); // keep within one full rotation
        float dr = r - this.r; // calculate difference from current rotation
        this.r = r; // save member
        for (int i = 0; i < this.modelCoords.length; i+=2) { // for each coordinate
            Pair rp = Utils.rotatePoint(0f, 0f, modelCoords[i], modelCoords[i + 1], dr); // rotate it
            this.modelCoords[i] = rp.x; // save rotated x
            this.modelCoords[i + 1] = rp.y; // save rotated y
        }
        this.updateModelCoordsVBO(); // update VBO
        this.outdatedSize = true; // flag that the size members are outdated
    }

    /**
     * @return this model's width in model coordinates
     */
    public float getWidth() {
        if (this.outdatedSize) this.calculateSize(); // if outdated width member, recalculate
        return this.w; // return width
    }

    /**
     * @return this model's width when not rotated
     */
    public float getUnrotatedWidth() {
        return this.uw * this.sx; // take scale into account and return
    }

    /**
     * @return this model's horizontal scaling factor
     */
    public float getXScale() { return this.sx; }

    /**
     * @return this model's height in model coordinates
     */
    public float getHeight() {
        if (this.outdatedSize) this.calculateSize(); // if outdated height member, recalculate
        return this.h; // return height
    }

    /**
     * @return this model's height when not rotated
     */
    public float getUnrotatedHeight() {
        return this.uh * this.sy; // take scale into account and return
    }

    /**
     * @return this model's vertical scaling factor
     */
    public float getYScale() { return this.sy; }

    /**
     * @return the model's rotation in radians
     */
    public float getRotationRad() { return this.r; }

    /**
     * Calculates a bounding box for this model. If rotate is set to true, and this is a rectangular model, it will
     * return a rotated bounding box. These rotated bounding boxes cannot be used
     * @return the frame described above
     */
    public BoundingBox getBoundingBox(boolean rotated) {
        if (this.modelCoords.length == 8 && rotated) { // if rectangular
            float[] corners = new float[this.modelCoords.length]; // create corners array
            for (int i = 0; i < corners.length; i+=2) { // fill it with model coords
                corners[i] = modelCoords[i]; // copy x
                corners[i + 1] = modelCoords[i + 1]; // copy negative y
            }
            return new BoundingBox(corners, this.r, 0f, 0f); // return the frame with the appropriate r value
        } else { // if not rectangular
            float w2 = this.getWidth() / 2; // calculate half of width of bounding box
            float h2 = this.getHeight() / 2; // calculate half of height of bounding box
            return new BoundingBox(new float[] {-w2, -h2, -w2, h2, w2, h2, w2, -h2}, 0f, 0f, 0f); // create frame
        }
    }

    /**
     * Cleans up this model by deleting buffers and unbinding any buffer objects or array objects
     */
    public void cleanup() {
        glDisableVertexAttribArray(0); // disable model coordinate vbo in vao
        glDisableVertexAttribArray(1); // disable texture coordinate vbo in vao
        glBindBuffer(GL_ARRAY_BUFFER, 0); // unbind any vbo
        for (int i = 1; i < this.ids.length; i++) glDeleteBuffers(this.ids[i]); // delete VBOs
        glBindVertexArray(0); // unbind vao
        glDeleteVertexArrays(this.ids[0]); // delete vao
    }
}
