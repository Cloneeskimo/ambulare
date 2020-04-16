
// specify GLSL version
#version 330

// data
layout (location = 0) in vec3 vPos; // position data in VBO at index 0 of VAO
layout (location = 1) in vec3 vColor; // color data in VBO at index 1 of VAO
out vec3 fColor; // color passed through to fragment shader

// main function
void main() {
    gl_Position = vec4(vPos, 1.0); // simply set position to given position
    fColor = vColor; // pass through color to fragment shader
}
