
/*
 * world_fragment.glsl
 * Ambulare
 * Jacob Oaks
 * 4/27/2020
 */

/*
 * These shaders are used to color world-based objects that do react to lighting. These shaders will take into
 * consideration the object's material, the status of a day/night cycle, and a set of lights (if useLights is 1)
 */

// specify GLSL version
#version 330

/*
 * Constants
 */
const int MAX_LIGHTS = 32; // the maximum amount of lights the shader will accept

/*
 * A struct that defines properties of a light to use when shading nearby objects
 */
struct Light {
    vec3 glow; /* the glow of the color is multiplied onto objects. Components less than 1f should be avoided as they
        actually remove color which is somewhat counter-intuitive to the idea of lighting */
    float reach; // how far the light reaches
    float intensity; /* how intense the light is. Should be roughly within the range of 0.2f (extremely dim) and 1.8f
        (extremely bright) */
    float x; // the world x position of the light
    float y; // the world y position of the light
};

/*
 * Uniformss
 */
uniform sampler2D texSampler;     // texture sampler - bound to the material's texture
uniform vec4 color;               // color - bound to the material's color
uniform int isTextured;           // flag representing whether the material is textured or not
uniform int blend;                /* defines how to blend a material's color and texture if there are both. 0 - just use
                                     the texture; 1 - multiply color and texture; 2 - average color and texture */
uniform int useLights;            /* flag for applying lights - this becomes false sometimes when rendering foreground
                                     objects or other objects that lighting should not be applied to where 1 denotes
                                     that lights should be used and 0 denotes that lights should not be used */
uniform float sunPresence;        // how present the sun currently is as a from 0 (not present) to 1 (fully present)
uniform Light lights[MAX_LIGHTS]; /* the list of lights to consider. To denote that there is no light at index, simply
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
        return vec4(texColor.xyz / 4, texColor.w); // set final color to calculated texture color (blended or not)
    } else return vec4(color.xyz / 4, color.w); // if not textured, use base color of material
}

/*
 * Applies lighting based on the day/night cycle to the given color
 * @param color the color to apply lighting to
 */
vec4 applyDayNight(vec4 color) {
    vec3 c = color.xyz * (1 + 3 * sunPresence); // apply brightness from sun
    vec3 sunlight = vec3(1.1, 0.9, 0.75); // use a orange-ish red color for sunlight
    vec3 moonlight = vec3(0.6, 0.6, 1.1); // use a blue-ish gray color for moonlight
    // apply sunlight and/or moonlight depending on sun position and return final color
    return vec4((c * sunlight * (sunPresence) + c * moonlight * (1 - sunPresence)), color.w);
}

/*
 * Applies lights from the lights array to the given color
 * @param color the color to apply the lights to
 */
vec4 applyLights(vec4 color) {
    vec3 baseColor = (4 * color.xyz / (1 + 3 * sunPresence)); // get base color before sun was added
    for (int i = 0; i < MAX_LIGHTS; i++) { // go through each light
        if (lights[i].reach > 0) { // if that light exists
            float d = distance(worldPos, vec2(lights[i].x, lights[i].y)); // get the distance to the light
            if (d <= lights[i].reach) { // if it is within reach of the light
                // make the base color brighter depending on how present the sun is and how intense the light is
                vec3 c = baseColor * ((1 * sunPresence) + (lights[i].intensity * (1 - sunPresence)));
                c = c * lights[i].glow; // apply the light's glow
                float dist = d / lights[i].reach; // calculate the normalized distance to the light based on reach
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
    vec4 color = getBaseColor(); // get base color based on material
    color = applyDayNight(color); // apply day/night cycle coloring
    fragColor = (useLights == 1) ? applyLights(color) : color; // apply lighting if lighting flag is true
}