
/*
 * format_block_vertex.glsl
 * Ambulare
 * Jacob Oaks
 * 5/9/2020
 */

// specify GLSL version
#version 330

/*
 * These shaders are used to format blocks loaded into areas via area node-files. Most of the work is done in the
 * fragment shader. This shder simply passes through the model coordinates and texture coordinates
 */

/*
 * Attributes
 */
layout (location = 0) in vec2 modelCoords; // model coordinate data in VBO at index 0 of VAO
layout (location = 1) in vec2 texCoords; // texture coordinate data in VBO at index 1 of VAO

/*
 * In/Out Variables
 */
out vec2 texCoordsF;   // texture coordinates to pass through to fragment shader
out vec2 modelCoordsF; // model coordinates to pass through to fragment shader

/*
 * Main Function
 */
void main() {
    texCoordsF = texCoords; // pass through texture coordinates
    modelCoordsF = modelCoords; // pass through model coordinates
    gl_Position = vec4(modelCoords.x, -modelCoords.y, 0, 1.0); // set final position as model coordinates (inverted y)
}
