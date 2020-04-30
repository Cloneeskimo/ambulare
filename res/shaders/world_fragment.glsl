
// specify GLSL version
#version 330

// constants
const int MAX_LIGHTS = 32; // the maximum amount of lights the shader will accept

// a struct that describe the properties of a light that can be used in shading calculationss
struct Light {
    // the glow of the color is multiplied onto objects. Components less than 1f should be avoided as they remove color
    vec3 glow;
    float reach; // how far the light reaches
    // how intense the light is. Should be roughly within the range of 0.2f (extremely dim) and 1.8f (extremely bright)
    float intensity;
    // the position of the light
    float x;
    float y;
};

// uniforms
uniform sampler2D texSampler; // texture sampler
uniform vec4 color; // color
uniform int isTextured; // texture flag
uniform int blend; // blend flag

// lighting uniforms
uniform float sunPresence; // how present the sun currently is
uniform Light lights[MAX_LIGHTS]; // the list of lights to considers

// data
in vec2 fTexCoords; // texture coordinates passed in from vertex shader
in vec2 worldPos; // the world position passed in from vertex shader
out vec4 fragColor; // final color

// calculates the base color based on material
vec4 getBaseColor() {
    if (isTextured == 1) { // if textured
        vec4 texColor = texture(texSampler, fTexCoords); // get texture sampled color
        if (blend == 1) texColor = texColor * color; // if blend mode multiplicative, multiply color and texture
        else if (blend == 2) texColor = (texColor + color) / 2; // if blend mode averaged, average color and texture
        return vec4(texColor.xyz / 4, texColor.w); // set final color to calculate texture color (blended or not)
    } else return vec4(color.xyz / 4, color.w); // if not textured, use base color
}

// applies lighting based on the day/night cycle
vec4 applyDayNight(vec4 color) {
    vec3 c = color.xyz * (1 + 3 * sunPresence); // apply brightness from sun
    vec3 sunlight = vec3(1.1, 0.9, 0.75); // orange-ish red color for sunlight
    vec3 moonlight = vec3(0.6, 0.6, 1.1); // blue-ish gray color for moonlight
    // apply sunlight and/or moonlight depending on sun position
    return vec4((c * sunlight * (sunPresence) + c * moonlight * (1 - sunPresence)), color.w);
}

// applies nearby lights
vec4 applyLights(vec4 color) {
    vec3 baseColor = (4 * color.xyz / (1 + 3 * sunPresence)); // get base color before sun presence added
    for (int i = 0; i < MAX_LIGHTS; i++) { // go through each light
        if (lights[i].reach > 0) { // if that light exists
            float d = distance(worldPos, vec2(lights[i].x, lights[i].y)); // get the distance to the light
            if (d <= lights[i].reach) { // if it is within reach of the light
                // make the base color brighter depending on how present the sun is and how intense the light is
                vec3 c = baseColor * ((1 * sunPresence) + (lights[i].intensity * (1 - sunPresence)));
                c = c * lights[i].glow; // apply the light's glow
                float dist = d / lights[i].reach; // calculate the normalized distance
                vec3 finalColor = (dist) * color.xyz + (1 - dist) * c; // apply distance
                color = vec4(finalColor, color.w); // update the color
            }
        }
    }
    return color; // return the color after all lights have been applied
}

// main function
void main() {
    vec4 color = getBaseColor(); // get base color based on material
    color = applyDayNight(color); // apply day/night cycle coloring
    fragColor = applyLights(color); // apply lighting
}