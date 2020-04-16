
// specify GLSL version
#version 330

// data
in vec3 fColor; // color passed in from vertex shader
out vec4 fragColor; // final color

// main function
void main() {
    fragColor = vec4(fColor, 1.0); // just set to given color for now
}