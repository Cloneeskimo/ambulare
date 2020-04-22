package graphics;

import org.lwjgl.system.MemoryUtil;
import utils.Utils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * A model that can have multiple sets of texture coordinates. This is ideal for animation on a single sprite sheet
 * The model does not take care of the animating, it just stores the different sets of texture coordinates. The class
 * owning the model still needs to tell it when to change frames. Also, this will only work for rectangular models
 */
public class MultiTexCoordModel extends Model {

    /**
     * Data
     */
    private float[][] texCoords; // an array of sets of texture coordinates

    /**
     * Creates a standard rectangular model with support multiple sets of texture coordinates
     * @param w the width of the rectangular model in cells
     * @param h the height of the rectangular model in cells
     * @param frameCount the amount of horizontal frames to calculate texture coordinates for. If this isn't equal to
     *                   the amount of frames in the texture, the frames won't line up
     * @return the standard rectangular model
     */
    public static MultiTexCoordModel getStdMultiTexGridRect(int w, int h, int frameCount) {
        return new MultiTexCoordModel(getGridRectModelCoords(w, h), getStdRectIdx(), frameCount);
    }

    /**
     * Given an amount of desired frames for a corresponding texture, the constructor will calculate the equi-distant
     * texture coordinates for each frame and store them
     * @param modelCoords the positions of the vertices (2-dimensional)
     * @param indices     the indices
     * @param frameCount  the amount of horizontal frames to consider
     */
    public MultiTexCoordModel(float[] modelCoords, int[] indices, int frameCount) {
        super(modelCoords, getTexCoordsForFrame(0, frameCount), indices); // call super
        if (indices.length != 6) Utils.handleException(new Exception("Invalid model: MultiTexCoordModels can only be" +
                "rectangular"), "graphics.MultiTexCoordMode", "MultiTexCoordModel(float[], int[], int)",
                true); // throw exception if not a rectangle
        this.calcTexCoords(frameCount); // calculate texture coordinates
    }

    /**
     * Changes the texture coordinates of the model to match the given frame
     * @param frame the frame
     */
    public void setFrame(int frame) {
        if (frame >= this.texCoords.length || frame < 0) { // if invalid frame
            Utils.handleException(new Exception("invalid frame: " + frame), "graphics.MultiTexCoordModel",
                    "setFrame(int)", true); // throw exception
        }
        glBindVertexArray(this.ids[0]); // bind the vertex array object
        FloatBuffer b = MemoryUtil.memAllocFloat(this.texCoords[frame].length); // allocate buffer space for data
        b.put(this.texCoords[frame]).flip(); // put texture coordinate data into buffer
        this.ids[2] = glGenBuffers(); // generate texture coordinate vertex buffer object
        glBindBuffer(GL_ARRAY_BUFFER, this.ids[2]); // bind texture coordinate vertex buffer object
        glBufferData(GL_ARRAY_BUFFER, b, GL_STATIC_DRAW); // put texture coordinate data into position VBO
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0); // put VBO into VAO
        glBindBuffer(GL_ARRAY_BUFFER, 0); // unbind VBO
        glBindVertexArray(0); // unbind the vertex array object
    }

    /**
     * Calculates the sets of texture coordinates for each frame
     * @param frameCount the amount of horizontal frames in the corresponding texture
     */
    private void calcTexCoords(int frameCount) {
        this.texCoords = new float[frameCount][8]; // 8 texture coordinates per frame
        for (int i = 0; i < frameCount; i++) this.texCoords[i] = getTexCoordsForFrame(i, frameCount); // calc each frame
    }

    /**
     * Calculates the set of texture coordinates for the given horizontal frame
     * @param i the frame to calculate the texture coordinates for
     * @param of the total amount of horizontal frames in the corresponding texture
     * @return the length-eight float array containing the texture coordinates
     */
    private static float[] getTexCoordsForFrame(int i, int of) {
        float frameWidth = (float)1 / (float)of; // calculate width of one frame
        float frac = (float)i / (float)of; // calculates how horizontally far this frame is in texture
        return new float[] { // create texture coordinates array
                frac, 1.0f, // top left
                frac, 0.0f, // bottom left
                frac + frameWidth, 0.0f, // bottom right
                frac + frameWidth, 1.0f // top right
        };
    }
}
