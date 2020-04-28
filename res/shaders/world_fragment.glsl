
// specify GLSL version
#version 330

// uniforms
uniform sampler2D texSampler; // texture sampler
uniform vec4 color; // color
uniform int isTextured; // texture flag
uniform int blend; // blend flag

// lighting uniforms;
uniform float sunPresence; // how present the sun currently is

// data
in vec2 fTexCoords; // texture coordinates passed in from vertex shader
out vec4 fragColor; // final color

// calculates the base color based on material
vec4 getBaseColor() {
    if (isTextured == 1) { // if textured
        vec4 texColor = texture(texSampler, fTexCoords); // get texture sampled color
        if (blend == 1) texColor = texColor * color; // if blend mode multiplicative, multiply color and texture
        else if (blend == 2) texColor = (texColor + color) / 2; // if blend mode averaged, average color and texture
        return texColor; // set final color to calculate texture color (blended or not)
    } else return color; // if not textured, use base color
}

// applies lighting based on the day/night cycle
vec4 applyDayNight(vec4 baseColor) {
    vec3 c = vec3(baseColor.xyz * (0.5 + sunPresence)); // more sun presence -> lighter color
    vec3 sunlight = c * vec3(1.1, 0.9, 0.75); // orange-ish red color for sunlight
    vec3 moonlight = c * vec3(0.6, 0.6, 1.1); // blue-ish gray color for moonlight
    // depending on how present the sun is, apply sunlight and/or moonlight
    return vec4((sunlight * (sunPresence) + moonlight * (1 - sunPresence)), baseColor.w);
}

// main function
void main() {
    vec4 baseColor = getBaseColor(); // get base color based on material
    fragColor = applyDayNight(baseColor); // apply day/night cycle coloring
}