
// specify GLSL version
#version 330

// data
in vec4 fColor; // color passed in from vertex shader
out vec4 fragColor; // final color

// main function
void main() {
    fragColor = fColor; // just set to given color for now
}