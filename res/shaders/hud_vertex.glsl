
/*
 * hud_vertex.glsl
 * Ambulare
 * Jacob Oaks
 * 4/27/2020
 */

/*
 * These shaders are used to position HUD-based elements that do not react to a camera. This shader will only take into
 * consideration the object's position and the window's aspect ratio
 */

// specify GLSL version
#version 330

/*
 * Uniforms
 */
uniform int arAction; // aspect ratio action flag where 0 denotes multiplying y and 1 denotes dividing x
uniform float ar;     // the aspect ratio of the window
uniform float x;      // the object's x position used to offset the model coordinatess
uniform float y;      // the object's y position used to offset the model coordinates

/*
 * Attributes
 */
layout (location = 0) in vec2 modelCoords; // model coordinate data in VBO at index 0 of VAO
layout (location = 1) in vec2 texCoords;   // texture coordinate data in VBO at index 1 of VAO

/*
 * In/Out Variables
 */
out vec2 fTexCoords; // texture coordinates to be passed through to fragment shaders

/*
 * Applies aspect ratio properties to a point, converting world coordinates or camera-view coordinates into aspect
 * coordinates
 * @param coords the point to apply aspect ratio properties to
 */
vec2 aspect(vec2 coords) {
    if (arAction == 1) coords.y = coords.y * ar; // apply aspect ratio to y if ar action is 1
    else coords.x = coords.x / ar; // apply aspect ratio to x if ar action is 0
    return coords; // return result
}

/*
 * Main Function
 */
void main() {
    vec2 pos = vec2(modelCoords.x + x, modelCoords.y + y); // convert model coordinates to world coordinates
    pos = aspect(pos); // convert world coordinates to aspect coordinates
    gl_Position = vec4(pos, 0.0, 1.0); // pass through aspect coordinates as a vec4
    fTexCoords = texCoords; // pass through texture coordinates to fragment shader
}