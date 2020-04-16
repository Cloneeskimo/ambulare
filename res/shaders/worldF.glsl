
// specify GLSL version
#version 330

// uniforms
uniform sampler2D texSampler; // texture sampler
uniform vec4 color; // color
uniform int isTextured; // texture flag
uniform int blend; // blend flag

// data
in vec2 fTexCoords; // texture coordinates passed in from vertex shader
out vec4 fragColor; // final color

// main function
void main() {
    if (isTextured == 1) { // if textured
        vec4 texColor = texture2D(texSampler, fTexCoords); // get texture sampled color
        if (blend == 1) texColor = texColor * color; // if blend, blend texture sampled color with base color
        fragColor = texColor; // set final color to calculate texture color (blended or not)
    } else fragColor = color; // if not textured, use base color
}