
/*
 * world_vertex.glsl
 * Ambulare
 * Jacob Oaks
 * 4/27/2020
 */

/*
 * These shaders are used to position world-based objects that do react to a camera. These shaders will take into
 * consideration the object's position, the window's aspect ratio, the camera's position, and the camera's zoom
 */

// specify GLSL version
#version 330

/*
 * Uniforms
 */
uniform int arAction;  // aspect ratio action flag where 0 denotes multiplying y and 1 denotes dividing x
uniform float ar;      // the aspect ratio of the window
uniform float x;       // the object's x position used to offset the model coordinatess
uniform float y;       // the object's y position used to offset the model coordinates
uniform float camX;    // the camera's x position used to offset the model coordinates
uniform float camY;    // the camera's y position used to offset the model coordinatess
uniform float camZoom; // the camera's zoom used to scale the models

/**
 * Attributes
 */
layout (location = 0) in vec2 modelCoords; // model coordinate data in VBO at index 0 of VAO
layout (location = 1) in vec2 texCoords;  // texture coordinate data in VBO at index 1 of VAO

/*
 * In/Out Variables
 */
out vec2 fTexCoords; // texture coordinates are just passed through to fragment shaders
out vec2 worldPos;   // world position is passed through to fragment shaders for lighting calculations

/*
 * Applies camera zoom and position to an object, converting world coordinates into camera-view coordinatess
 * @param the coordinates to convert
 */
vec2 toCameraView(vec2 coords) {
    return vec2(camZoom * (coords.x - camX), camZoom * (coords.y - camY)); // apply position, then zoom of camera
}

/*
 * Applies aspect ratio properties to an object, converting world coordinates or camera-view coordinates into aspect
 * coordinates
 * @param the coordinates to convert
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
    worldPos = pos; // pass world position to fragment shader for lighting calculations
    if (camZoom != 0) pos = toCameraView(pos); // convert world coordinates to camera-view if there is a camera
    pos = aspect(pos); // convert world or camera-view coordinates to aspect coordinates
    gl_Position = vec4(pos, 0.0, 1.0); // pass through aspect coordinates as a vec4 as the final position
    fTexCoords = texCoords; // pass texture coordinates through to fragment shader
}