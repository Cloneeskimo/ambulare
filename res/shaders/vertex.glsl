
// specify GLSL version
#version 330

// uniforms
uniform int arAction; // aspect ration action
uniform float ar; // aspect ratio
uniform float x; // object x position
uniform float y; // object y position
uniform float scaleX; // x scaling
uniform float scaleY; // y scaling
uniform float rot; // rotation
uniform float camX; // camera's x position
uniform float camY; // camera's y position
uniform float camZoom; // camera's zoom

// data
layout (location = 0) in vec2 modelCoords; // model coordinate data in VBO at index 0 of VAO
layout (location = 1) in vec2 texCoords; // texture coordinate data in VBO at index 1 of VAO
out vec2 fTexCoords; // texture coordinates are just passed through to fragment shaders

/**
 *  Applies properties of the object such as rotation, scale, and position to the model coordinates
 *  Converts model coordinates into object coordinates
 */
vec2 toObject(vec2 coords) {
    coords.y = coords.y * -1;
    if (rot != 0) { // don't do rotation calculations if there is no rotation
        float cosr = cos(rot); // calculate co-sine of rotation value
        float sinr = sin(rot); // calculate sine of rotation value
        coords = (mat3(cosr, sinr, 0, -sinr, cosr, 0, 0, 0, 1) * // create rotation matrix
                       vec3(coords.x, coords.y, 1)).xy; // and apply it to the model
    }
    return vec2((scaleX * coords.x) + x, (scaleY * coords.y) + y); // apply position and scale
}

/**
 *  Applies camera zoom and position to an object
 *  Converts object coordinates into camera-view
 */
vec2 toCameraView(vec2 coords) {
    return vec2(camZoom * (coords.x - camX), camZoom * (coords.y - camY)); // apply position, then zoom
}

/**
 *  Applies aspect ratio properties to an object
 *  Converts object coordinates or camera-view coordinates into projected coordinates
 */
vec2 toProjected(vec2 coords) {
    if (arAction == 1) coords.y = coords.y * ar; //apply aspect ratio to y
    else coords.x = coords.x / ar; // apply aspect ratio to x
    return coords; // return result
}

/**
 *  Applies all necessary transformations to the model coordinates get them projected correectly
 */
void main() {
    vec2 coords = toObject(modelCoords); // convert model coordinates to object coordinates
    if (camZoom != 0) coords = toCameraView(coords); // convert object coordinates to camera-view if there is a camera
    coords = toProjected(coords); // convert object or camera-view coordinates to project coordinates
    gl_Position = vec4(coords, 0.0, 1.0); // pass through projected coordinates as a vec4
    fTexCoords = texCoords; // pass texture coordinates through to fragment shader
}