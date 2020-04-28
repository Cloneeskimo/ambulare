
// specify GLSL version
#version 330

// uniforms
uniform int arAction; // aspect ration action
uniform float ar; // aspect ratio
uniform float x; // object x position
uniform float y; // object y position

// data
layout (location = 0) in vec2 modelCoords; // model coordinate data in VBO at index 0 of VAO
layout (location = 1) in vec2 texCoords; // texture coordinate data in VBO at index 1 of VAO
out vec2 fTexCoords; // texture coordinates are just passed through to fragment shaders

/**
 *  Applies aspect ratio properties to an object
 *  Converts world coordinates or camera-view coordinates into aspect coordinates
 */
vec2 aspect(vec2 coords) {
    if (arAction == 1) coords.y = coords.y * ar; //apply aspect ratio to y
    else coords.x = coords.x / ar; // apply aspect ratio to x
    return coords; // return result
}

/**
 *  Applies all necessary transformations to the model coordinates get them projected correectly
 */
void main() {
    vec2 pos = vec2(modelCoords.x + x, modelCoords.y + y); // convert model coordinates to world coordinates
    pos = aspect(pos); // convert world or camera-view coordinates to aspect coordinates
    gl_Position = vec4(pos, 0.0, 1.0); // pass through aspect coordinates as a vec4
    fTexCoords = texCoords; // pass texture coordinates through to fragment shader
}