package graphics;

import utils.Utils;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/**
 * Represents a GLSL shader program. All uniforms must be registered and set before rendering. The shader program class
 * provides essy ways to register and set an array of lights uniform of length MAX_LIGHTS and of name LIGHT_ARRAY_NAME
 * for easy light integration
 */
public class ShaderProgram {

    /**
     * Static Data
     */
    private static final int MAX_LIGHTS = 32;                /* the largest number of lights that can be rendered per
                                                                binding of one shader program*/
    private static final String LIGHT_ARRAY_NAME = "lights"; /* the name to assume light array uniforms to be in shader
                                                                program source codee*/

    /**
     * Members
     */
    private final Map<String, Integer> uniforms; // map of uniform names to locations
    private final int progID;                    // program id of the shader program
    private int vShaderID;                       // program id of the vertex shader
    private int fShaderID;                       // program id of the fragment shader
    private int lightNo;                         // how many light uniforms have been set since the last unbind/bind

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
     * Registers a light array uniform with the length MAX_LENGTH and with the name LIGHT_ARRAY_NAME
     */
    public void registerLightArrayUniform() {
        for (int i = 0; i < MAX_LIGHTS; i++) {
            this.registerUniform(LIGHT_ARRAY_NAME + "[" + i + "].glow");
            this.registerUniform(LIGHT_ARRAY_NAME + "[" + i + "].reach");
            this.registerUniform(LIGHT_ARRAY_NAME + "[" + i + "].intensity");
            this.registerUniform(LIGHT_ARRAY_NAME + "[" + i + "].x");
            this.registerUniform(LIGHT_ARRAY_NAME + "[" + i + "].y");
        }
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
     * Inserts the light corresponding to the given light source and position into the shader program's lights array
     * uniform. This will only accept up to MAX_LIGHTS amount of lights per binding of the shader program. This assumes
     * that the light uniform is named LIGHT_ARRAY_NAME in the source code
     *
     * @param light the light source whose light properties to use
     * @param x     the x position of the light
     * @param y     the y position of the light
     */
    public void putInLightArrayUniform(LightSource light, float x, float y) {
        if (this.lightNo >= MAX_LIGHTS) // if too many lights are being rendered, throw an exception
            Utils.handleException(new Exception("Maximum amount of renderable lights exceeded: " + MAX_LIGHTS),
                    "graphics.ShaderProgram", "setLightUniform(String, LightSource, float, float)", true);
        try {
            String name = "lights[" + this.lightNo + "]"; // get the proper name for the light in the lights array
            this.lightNo++; // iterate the lights array iterator
            float[] glow = light.getGlow(); // get the light's glow
            glUniform3f(this.uniforms.get(name + ".glow"), glow[0], glow[1], glow[2]); // set the light's glow
            glUniform1f(this.uniforms.get(name + ".reach"), light.getReach()); // set the light's reach
            glUniform1f(this.uniforms.get(name + ".intensity"), light.getIntensity()); // set the light's intensity
            glUniform1f(this.uniforms.get(name + ".x"), x); // set the light's x position
            glUniform1f(this.uniforms.get(name + ".y"), y); // set the light's y position
        } catch (Exception e) { // if exception
            // handle exception
            Utils.handleException(e, "graphics.ShaderProgram", "setUniform(String, LightSource, float,float)", true);
        }
    }

    /**
     * Binds the shader program
     */
    public void bind() {
        glUseProgram(this.progID);
    }

    /**
     * Unbinds the shader program and resets the lights array iterator
     */
    public void unbind() {
        glUseProgram(0); // unbind program
        this.lightNo = 0; // reset lights array iterator
    }

    /**
     * Cleans up the shader program
     */
    public void cleanup() {
        this.unbind(); // make sure isn't bound
        if (this.progID != 0) glDeleteProgram(this.progID); // delete program
    }
}
