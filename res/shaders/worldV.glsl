
// specify GLSL version
#version 330

// uniforms
uniform float ar; // aspect ratio
uniform int arAction; // aspect ration action
uniform float x; // x offset
uniform float y; // y offset

// data
layout (location = 0) in vec2 vPos; // position data in VBO at index 0 of VAO
layout (location = 1) in vec4 vColor; // color data in VBO at index 1 of VAO
out vec4 fColor; // color passed through to fragment shader

// main function
void main() {
    vec2 pos = vec2(vPos.x + x, vPos.y + y); // apply x and y offset (object position)
    if (arAction == 1) pos.y = pos.y * ar; // apply projection to y
    else pos.x = pos.x / ar; // apply projection to x
    gl_Position = vec4(pos, 0.0, 1.0); // pass through calculated position
    fColor = vColor; // pass through color to fragment shader
}
