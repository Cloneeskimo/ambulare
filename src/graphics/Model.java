package graphics;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * Represents a model with model coordinates, texture coordinates, and indices. It renders in triangles
 */
public class Model {

    /**
     * Data
     */
    private final int ids[]; // [0] - VAO ID, [1] - position VBO ID, [2] - texture coordinate VBO ID, [4] - index VBO ID
    private final int vertexCount; // amount of vertices the model has
    private final float w, h; // width and height of this model in model coordinates
    public static final float STD_SQUARE_SIZE = 0.5f; // the standard square model size (width and height)

    /**
     * @return the standard square model coordinates to make creating square models easier
     */
    public static float[] getStdSquareModelCoords() {
        return new float[] { // rectangle positions
                -STD_SQUARE_SIZE / 2, -STD_SQUARE_SIZE / 2, // top left
                -STD_SQUARE_SIZE / 2,  STD_SQUARE_SIZE / 2, // bottom left
                 STD_SQUARE_SIZE / 2,  STD_SQUARE_SIZE / 2, // bottom right
                 STD_SQUARE_SIZE / 2, -STD_SQUARE_SIZE / 2 // top right
        };
    }

    /**
     * @return the standard square texture coordinates to make creating square models easier
     */
    public static float[] getStdSquareTexCoords() {
        return new float[] {
                0.0f, 1.0f, // top left
                0.0f, 0.0f, // bottom left
                1.0f, 0.0f, // bottom right
                1.0f, 1.0f // top right
        };
    }

    /**
     * @return the standard square indices to make create square models easier
     */
    public static int[] getStdSquareIdx() {
        return new int[] { 0, 1, 3, 3, 1, 2 };
    }

    /**
     * All tiles use a single copy of this standard square model to avoid redundant memory usage
     * @return the standard square model
     */
    public static Model getStdSquare() {
        return new Model(getStdSquareModelCoords(), getStdSquareTexCoords(), getStdSquareIdx());
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

        // create buffers, record vertex count, generate VAO
        FloatBuffer modelCoordsBuffer = null, texCoordsBuffer = null; // buffers for model coords and tex coords
        IntBuffer idxBuffer = null; // buffer for index data
        this.vertexCount = indices.length; // record vertex count
        this.ids = new int[4]; // initialize ID array
        this.ids[0] = glGenVertexArrays(); // generate the vertex array object
        glBindVertexArray(this.ids[0]); // bind the vertex array object

        // process model coordinate data
        modelCoordsBuffer = MemoryUtil.memAllocFloat(modelCoords.length); // allocate buffer space for position data
        modelCoordsBuffer.put(modelCoords).flip(); // put position data into buffer
        this.ids[1] = glGenBuffers(); // generate position vertex buffer object
        glBindBuffer(GL_ARRAY_BUFFER, this.ids[1]); // bind position vertex buffer object
        glBufferData(GL_ARRAY_BUFFER, modelCoordsBuffer, GL_STATIC_DRAW); // put position data into position VBO
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0); // put VBO into VAO

        // process texture coordinate data
        texCoordsBuffer = MemoryUtil.memAllocFloat(texCoords.length); // allocate buffer space for tex coord data
        texCoordsBuffer.put(texCoords).flip(); // put texture coordinate data into buffer
        this.ids[2] = glGenBuffers(); // generate texture coordinate vertex buffer object
        glBindBuffer(GL_ARRAY_BUFFER, this.ids[2]); // bind texture coordinate vertex buffer object
        glBufferData(GL_ARRAY_BUFFER, texCoordsBuffer, GL_STATIC_DRAW); // put tex coord data into tex coord VBO
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0); // put VBO into VAO

        // process index data
        idxBuffer = MemoryUtil.memAllocInt(indices.length); // allocate buffer space for index data
        idxBuffer.put(indices).flip(); // put index data into buffer
        this.ids[3] = glGenBuffers(); // generate index vertex buffer object
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.ids[3]); // bind index vertex buffer object
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuffer, GL_STATIC_DRAW); // put index data into index VBO

        // unbind VBO, VAO
        glBindBuffer(GL_ARRAY_BUFFER, 0); // unbind VBO
        glBindVertexArray(0); // unbind VAO

        // free memory
        MemoryUtil.memFree(modelCoordsBuffer); // free model coordinate buffer memory
        MemoryUtil.memFree(texCoordsBuffer); // free texture coordinate buffer memory
        MemoryUtil.memFree(idxBuffer); // free index buffer memory

        // calculate width and height
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
    }

    /**
     * @return this model's width in model coordinates
     */
    public float getWidth() { return this.w; }

    /**
     * @return this model's height in model coordinates
     */
    public float getHeight() { return this.h; }

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

    /**
     * Renders the model
     */
    public void render() {
        glBindVertexArray(this.ids[0]); // bind vao
        glEnableVertexAttribArray(0); // enable model coordinate vbo
        glEnableVertexAttribArray(1); // enable texture coordinate vbo
        glDrawElements(GL_TRIANGLES, this.vertexCount, GL_UNSIGNED_INT, 0); // draw model
        glDisableVertexAttribArray(0); // disable model coordinate vbo
        glDisableVertexAttribArray(1); // disable texture coordinate vbo
        glBindVertexArray(0); // disable vao
    }
}
