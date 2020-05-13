
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
uniform float x; // the object's x position used to offset the model coordinatess
uniform float y; // the object's y position used to offset the model coordinates

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
    gl_Position = vec4(modelCoords.x + x, -modelCoords.y + y, 0, 1); // apply offset to position
    normPosCoords = (1 + vec2(modelCoords.x + x, -modelCoords.y + y)) / 2; // norm. pos to (0, 1) for fragment shader
    fTexCoords = texCoords; // pass texture coordinates through to fragment shader
}
