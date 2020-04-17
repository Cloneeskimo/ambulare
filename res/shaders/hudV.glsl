
// specify GLSL version
#version 330

// uniforms
uniform int arAction; // aspect ration action
uniform float ar; // aspect ratio
uniform float x; // x world position
uniform float y; // y world position

// data
layout (location = 0) in vec2 modelCoords; // model coordinate data in VBO at index 0 of VAO
layout (location = 1) in vec2 texCoords; // texture coordinate data in VBO at index 1 of VAO
out vec2 fTexCoords; // texture coordinates are just passed through to fragment shaders

// main function
void main() {
    vec2 pos = vec2(modelCoords.x + x, modelCoords.y + y); // apply world position and camera position
    if (arAction == 1) pos.y = pos.y * ar; // apply projection to y
    else pos.x = pos.x / ar; // apply projection to x
    gl_Position = vec4(pos, 0.0, 1.0); // pass through calculated position with zoom applied
    fTexCoords = texCoords; // pass through texture coordinates to fragment shader
}
