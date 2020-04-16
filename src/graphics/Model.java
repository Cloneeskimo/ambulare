package graphics;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * Represents a single Model (or model). Currently supports representing objects made out of triangles. Each vertex
 * of the Model can have one property: color
 */
public class Model {

    /**
     * Data
     */
    private final int vaoID; // ID of the vertex array object
    private final int posVboID, colorVboID, idxVboID; // IDs of the vertex buffer objects
    private final int vertexCount; // amount of vertices this Model has

    /**
     * Constructs this Model
     * The assumes input given in sequences of triangles
     * @param positions the positions of the vertices (2-dimensional)
     * @param colors the colors of the vertices (4-dimensional)
     * @param indices the index of the vertices. For example, if you have two triangles to make a square, the two
     *                overlapping points can be given the same index. This helps GL avoid redundant vertex rendering
     */
    public Model(float[] positions, float[] colors, int[] indices) {

        // create buffers, record vertex count, generate VAO
        FloatBuffer posBuffer = null, colorBuffer = null; // buffers for position data and color data
        IntBuffer idxBuffer = null; // buffer for index data
        this.vertexCount = indices.length; // record vertex count
        this.vaoID = glGenVertexArrays(); // generate the vertex array object
        glBindVertexArray(vaoID); // bind the vertex array object

        // process position data
        posBuffer = MemoryUtil.memAllocFloat(positions.length); // allocate buffer space for position data
        posBuffer.put(positions).flip(); // put position data into buffer
        this.posVboID = glGenBuffers(); // generate position vertex buffer object
        glBindBuffer(GL_ARRAY_BUFFER, this.posVboID); // bind position vertex buffer object
        glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW); // put position data into position VBO
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0); // put VBO into VAO

        // process color data
        colorBuffer = MemoryUtil.memAllocFloat(colors.length); // allocate buffer space for color data
        colorBuffer.put(colors).flip(); // put color data into buffer
        this.colorVboID = glGenBuffers(); // generate color vertex buffer object
        glBindBuffer(GL_ARRAY_BUFFER, this.colorVboID); // bind color vertex buffer object
        glBufferData(GL_ARRAY_BUFFER, colorBuffer, GL_STATIC_DRAW); // put color data into color VBO
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0); // put VBO into VAO

        // process index data
        idxBuffer = MemoryUtil.memAllocInt(indices.length); // allocate buffer space for index data
        idxBuffer.put(indices).flip(); // put index data into buffer
        this.idxVboID = glGenBuffers(); // generate index vertex buffer object
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, idxVboID); // bind index vertex buffer object
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuffer, GL_STATIC_DRAW); // put index data into index VBO

        // unbind VBO, VAO
        glBindBuffer(GL_ARRAY_BUFFER, 0); // unbind VBO
        glBindVertexArray(0); // unbind VAO

        // free memory
        MemoryUtil.memFree(posBuffer); // free position buffer memory
        MemoryUtil.memFree(colorBuffer); // free color buffer memory
        MemoryUtil.memFree(idxBuffer); // free index buffer memory
    }

    /**
     * Cleans up this Model by deleting buffers and unbinding any buffer objects or array objects
     */
    public void cleanup() {
        glDisableVertexAttribArray(0); // disable position vbo in vao
        glDisableVertexAttribArray(1); // disable color vbo in vao
        glBindBuffer(GL_ARRAY_BUFFER, 0); // unbind any vbos
        glDeleteBuffers(this.posVboID); // delete position vbo
        glDeleteBuffers(this.colorVboID); // delete color vbo
        glDeleteBuffers(this.idxVboID); // delete index vbo
        glBindVertexArray(0); // unbind vao
        glDeleteVertexArrays(this.vaoID); // delete vao
    }

    /**
     * Renders this Mesh
     */
    public void render() {
        glBindVertexArray(this.vaoID); // bind vao
        glEnableVertexAttribArray(0); // enable position vbo
        glEnableVertexAttribArray(1); // enable color vbo
        glDrawElements(GL_TRIANGLES, this.vertexCount, GL_UNSIGNED_INT, 0); // draw model
        glDisableVertexAttribArray(0); // disable position vbo
        glDisableVertexAttribArray(1); // disable color vbo
        glBindVertexArray(0); // disable vao
    }
}
