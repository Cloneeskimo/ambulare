package graphics;

import utils.Utils;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/**
 * Represents a GLSL shader program
 */
public class ShaderProgram {

    /**
     * Members
     */
    private final int progID;                    // program id of the shader program
    private final Map<String, Integer> uniforms; // map of uniform names to locations
    private int vShaderID;                       // program id of the vertex shader
    private int fShaderID;                       // program id of the fragment shader

    /**
     * Constructor
     *
     * @param vShaderPath the resource-relative path to the vertex shader code
     * @param fShaderPath the resource-relative path to the fragment shader code
     */
    public ShaderProgram(String vShaderPath, String fShaderPath) {
        this.progID = glCreateProgram(); // create GLSL program
        this.uniforms = new HashMap<>(); // initialize uniform map
        if (this.progID == 0) Utils.handleException(new Exception("Unable to create GLSL program"),
                "graphics.ShaderProgram", "ShaderProgram(String, String)",
                true); // throw exception if cannot create program
        this.processShaders(vShaderPath, fShaderPath); // process shaders
    }

    /**
     * Processes the GLSL shaders by loading the code, compiling the code, then linking.
     *
     * @param vShaderPath the resource-relative path to the vertex shader
     * @param fShaderPath the resource-relative path to the fragment shader
     */
    private void processShaders(String vShaderPath, String fShaderPath) {
        String vShaderCode = Utils.resToString(vShaderPath); // read vertex shader code
        String fShaderCode = Utils.resToString(fShaderPath); // read fragment shader code
        this.vShaderID = processShader(vShaderCode, GL_VERTEX_SHADER); // process vertex shader
        this.fShaderID = processShader(fShaderCode, GL_FRAGMENT_SHADER); // process fragment shader
        this.link(); // link shaders
    }

    /**
     * Processes a GLSL shader (vertex or fragment) based on the given code by compiling it and attaching it to the
     * main program
     *
     * @param code the code to create the shader from
     * @param type the type of shader
     * @return the ID of the created shader
     */
    private int processShader(String code, int type) {
        int id = glCreateShader(type); // create shader
        if (id == 0) // if fail
            Utils.handleException(new Exception("Unable to create shader of type " + type + " with code: " + code),
                    "graphics.ShaderProgram", "processShader(String, int)", true); // throw exception
        glShaderSource(id, code); // give shader the code
        glCompileShader(id); // compile shader
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0) // if fail
            Utils.handleException(new Exception("Unable to compile shader of type " + type + ": " +
                            glGetShaderInfoLog(id, 1024)), "graphics.ShaderProgram", "processShader(String, int)",
                    true); // throw exception
        glAttachShader(this.progID, id); // attach to main program
        return id; // return id
    }

    /**
     * Links the shader programs's GLSL shaders together
     */
    private void link() {
        glLinkProgram(this.progID); // link program
        if (glGetProgrami(this.progID, GL_LINK_STATUS) == 0) // if fail
            Utils.handleException(new Exception("Unable to link shaders: " + glGetProgramInfoLog(this.progID,
                    1024)), "graphics.ShaderProgram", "link()", true); // throw exception
        glDetachShader(this.progID, this.vShaderID); // detach vertex shader
        glDetachShader(this.progID, this.fShaderID); // detach fragment shader
        glValidateProgram(this.progID); // validates program. Failure does not necessarily mean program is broken
        if (glGetProgrami(this.progID, GL_VALIDATE_STATUS) == 0) // if validation message
            Utils.log("Shader validation gave the following response: " + glGetProgramInfoLog(this.progID,
                    1024), "graphics.ShaderProgram", "link()", false); // log
    }

    /**
     * Register the uniform with the given name by finding its position and saving it
     *
     * @param name the name of the uniform to find
     */
    public void registerUniform(String name) {
        int loc = glGetUniformLocation(this.progID, name); // get location
        if (loc < 0) // if fail
            Utils.handleException(new Exception("Unable to find uniform with name '" + name + "'"),
                    "graphics.ShaderProgram", "registerUniform(String)", true); // throw exception
        this.uniforms.put(name, loc); // save location
    }

    /**
     * Sets the uniform with the given name to the given value (a float)
     *
     * @param name the name of the uniform the set
     * @param v    the value to set it to
     */
    public void setUniform(String name, float v) {
        try {
            glUniform1f(this.uniforms.get(name), v); // try to set uniform
        } catch (Exception e) { // if exception
            Utils.handleException(e, "graphics.ShaderProgram", "setUniform(String, float)", true); // handle exception
        }
    }

    /**
     * Sets the uniform with the given name to the given value (an integer)
     *
     * @param name the name of the uniform to set
     * @param v    the value to set it to
     */
    public void setUniform(String name, int v) {
        try {
            glUniform1i(this.uniforms.get(name), v); // try to set uniform
        } catch (Exception e) { // if exception
            Utils.handleException(e, "graphics.ShaderProgram", "setUniform(String, int)", true); // handle exception
        }
    }

    /**
     * Sets the uniform with the given name to the given value (a 4-dimensional float array)
     *
     * @param name the name of the uniform to set
     * @param x    the first value of the 4-dimensional float array
     * @param y    the second value of the 4-dimensional float array
     * @param z    the third value of the 4-dimensional float array
     * @param a    the fourth value of the 4-dimensional float array
     */
    public void setUniform(String name, float x, float y, float z, float a) {
        try {
            glUniform4f(this.uniforms.get(name), x, y, z, a); // try to set uniform
        } catch (Exception e) { // if exception
            Utils.handleException(e, "graphics.ShaderProgram", "setUniform(String, float, float, float, float)",
                    true); // handle exception
        }
    }

    /**
     * Binds the shader program
     */
    public void bind() {
        glUseProgram(this.progID);
    }

    /**
     * Unbinds the shader program
     */
    public void unbind() {
        glUseProgram(0);
    }

    /**
     * Cleans up the shader program
     */
    public void cleanup() {
        this.unbind(); // make sure isn't bound
        if (this.progID != 0) glDeleteProgram(this.progID); // delete program
    }
}
