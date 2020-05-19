
/*
 * world_fragment.glsl
 * Ambulare
 * Jacob Oaks
 * 4/27/2020
 */

/*
 * These shaders are used to color world-based objects that do react to lighting. These shaders will take into
 * consideration the object's material, the status of a day/night cycle (if useDNC is 1), and a set of lights (if
 * useLights is 1)
 */

// specify GLSL version
#version 330

/*
 * Constants
 */
const int MAX_LIGHTS = 32;       // the maximum amount of lights the shader will accept
const float DARKNESS_FACTOR = 7; // how much to divide color by in order to simulate a lack of light

/*
 * A struct that defines properties of a light to use when shading nearby objects
 */
struct Light {
    vec3 glow;       /* the glow of the color is multiplied onto objects. Components less than 1f should be avoided as
        they actually remove color which is somewhat counter-intuitive to the idea of lighting */
    float reach;     // how far the light reaches
    float intensity; /* how intense the light is. Should be roughly within the range of 0.2f (extremely dim) and 1.8f
        (extremely bright) */
    float x;         // the world x position of the light
    float y;         // the world y position of the light
};

/*
 * Uniforms
 */
uniform sampler2D texSampler;      // texture sampler - bound to the material's texture
uniform vec4 color;                // color - bound to the material's color
uniform int isTextured;            // flag representing whether the material is textured or not
uniform int blend;                 /* defines how to blend a material's color and texture if there are both. 0 - just
                                      use texture; 1 - multiply color and texture; 2 - average color and texture */
uniform int useDNC;                // flag specifying if the day/night cycle lighting should be applied
uniform int useLights;             // flag specifying if individual lights should be applied
uniform float sunPresence;         // how present the sun currently is as a from 0 (not present) to 1 (fully present)
uniform float flicker[MAX_LIGHTS]; // flicker values for lights which are multiplied with reach to create flicker
uniform Light lights[MAX_LIGHTS];  /* the list of lights to consider. To denote that there is no light at index simply
                                      don't set the uniform */

/*
 * In/Out Variables
 */
in vec2 fTexCoords; // texture coordinates as passed in from vertex shader
in vec2 worldPos;   // the world position as passed in from vertex shader
out vec4 fragColor; // final color to assign to the fragment

/*
 * Calculates the base fragment color based on the material (texture and color)
 */
vec4 getBaseColor() {
    if (isTextured == 1) { // if the maetrial is textured
        vec4 texColor = texture(texSampler, fTexCoords); // get texture sampled color
        if (blend == 1) texColor = texColor * color; // if blend mode multiplicative, multiply color and texture
        else if (blend == 2) texColor = (texColor + color) / 2; // if blend mode averaged, average color and texture
        return texColor; // set final color to calculated texture color (blended or not)
    } else return color; // if not textured, use base color of material
}

/**
 * Apples the darkness factor to a color to simulate a lack of light
 * @param c the color to apply the darkness factor to
 */
vec4 applyDarknessFactor(vec4 c) {
    return vec4(c.xyz / DARKNESS_FACTOR, c.w); // apply darkness to rgb values
}

/*
 * Applies lighting based on the day/night cycle to the given color
 * @param color the color to apply lighting to
 */
vec4 applyDayNight(vec4 color) {
    vec3 c = color.xyz * (1 + 3 * sunPresence); // apply brightness from sun
    vec3 sunlight = vec3(1.3, 1.3, 1); // use a orange-ish red color for sunlight
    vec3 moonlight = vec3(1, 1, 1.3); // use a blue-ish gray color for moonlight
    // apply sunlight and/or moonlight depending on sun position and return final color
    return vec4((c * sunlight * (sunPresence) + c * moonlight * (1 - sunPresence)), color.w);
}

/*
 * Applies lights from the lights array to the given color
 * @param color the color to apply the lights to
 */
vec4 applyLights(vec4 color, vec3 baseColor) {
    for (int i = 0; i < MAX_LIGHTS; i++) { // go through each light
        if (lights[i].reach > 0) { // if that light exists
            float actualReach = lights[i].reach * (1 + flicker[i]); // apply flicker to light reach
            float d = distance(worldPos, vec2(lights[i].x, lights[i].y)); // get the distance to the light
            if (d <= actualReach) { // if it is within reach of the light
                // make the base color brighter depending on how present the sun is and how intense the light is
                vec3 c = baseColor * ((1 * sunPresence) + (lights[i].intensity * (1 - sunPresence)));
                c = c * lights[i].glow; // apply the light's glow
                float dist = d / actualReach; // calculate the normalized distance to the light based on reach
                vec3 finalColor = (dist) * color.xyz + (1 - dist) * c; // apply normalized distance
                color = vec4(finalColor, color.w); // update the color
            }
        }
    }
    return color; // return the color after all lights have been applied
}

/*
 * Main Function
 */
void main() {
    vec4 base = getBaseColor(); // get base color based on material
    vec4 lightless = applyDarknessFactor(base); // apply darkness factor to the color
    vec4 c = (useDNC == 1) ? applyDayNight(lightless) : lightless; // apply day/night cycle coloring
    c = (useLights == 1) ? applyLights(c, base.xyz) : c; // apply lighting if lighting flag is true
    fragColor = c; // set final color
}