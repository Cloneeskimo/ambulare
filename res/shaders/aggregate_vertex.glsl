
/*
 * aggregate_vertex.glsl
 * Ambulare
 * Jacob Oaks
 * 5/11/2020
 */

/*
 * These shaders are used to aggregate many material-based objects into a single texture. This vertex shader just
 * applies a positional offset supplied through uniforms
 */

// specify GLSL version
#version 330

/*
 * Uniforms
 */
uniform float x;    // the object's x position used to offset the model coordinates
uniform float y;    // the object's y position used to offset the model coordinates
uniform float wDiv; /* a value to divide x positions by - especially useful for complex objects whose models cannot
                       easily be normalized */
uniform float hDiv; /* a value to divide y positions by - espeically useful for complex objects whose models cannot
                       easily be normalized */

/*
 * Attributes
 */
layout (location = 0) in vec2 modelCoords; // model coordinate data in VBO at index 0 of VAO
layout (location = 1) in vec2 texCoords;   // texture coordinate data in VBO at index 1 of VAO

/*
 * In/Out Variables
 */
out vec2 fTexCoords;    // texture coordinates are just passed through to fragment shaders
out vec2 normPosCoords; // positional coordinates passed through to fragment shaders for fading, normalized to (0, 1)

/*
 * Main Function
 */
void main() {
    vec2 posCoords = vec2(modelCoords.x + x, -modelCoords.y - y); // apply offset
    if (wDiv != 0 && hDiv != 0) { // if width/height division uniforms set
        // apply width and height division
        posCoords.x /= wDiv;
        posCoords.y /= hDiv;
    }
    gl_Position = vec4(posCoords, 0, 1); // set final position
    normPosCoords = (1 + vec2(posCoords.x, posCoords.y)) / 2; // pas normalized pos within (0, 1) for fragment shader
    fTexCoords = texCoords; // pass texture coordinates through to fragment shader
}
