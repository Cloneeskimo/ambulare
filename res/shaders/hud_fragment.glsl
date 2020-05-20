
/*
 * hud_fragment.glsl
 * Ambulare
 * Jacob Oaks
 * 4/27/2020
 */

/*
 * These shaders are used to color HUD-based elements that do not react to lighting. This shader will only take into
 * consideration the object's material
 */

// specify GLSL version
#version 330

/*
 * Uniforms
 */
uniform sampler2D texSampler; // texture sampler of the material's texture
uniform vec4 color;           // color of the material
uniform float maxY;           // the maximum y a fragment can have before its not displayed
uniform float minY;           // the minimum y a fragment can have before its not displayed
uniform int boundY;           // whether to bound y to the min/max y sspecified in the above uniforms
uniform int isTextured;       // flag denoting whether the material is actually textured
uniform int blend;            /* blend flag denoting how to blend texture and color where the following values are used:
                                 (0) - no blending; (1) - multiplicative blending; (2) - averaged blending */

/*
 * In/Out Variablese
 */
in vec2 fTexCoords; // texture coordinates as passed in from vertex shader
in vec2 posCoords; // position coordinates to be passed in from vertex shader
out vec4 fragColor; // final color to assign to the fragment

/*
 * Main Function
 */
void main() {
    // if the position is out of bounds, do not render anything
    if ((boundY == 1) && (posCoords.y > maxY || posCoords.y < minY)) fragColor = vec4(0, 0, 0, 0);
    else if (isTextured == 1) { // if the material is textured
        vec4 texColor = texture(texSampler, fTexCoords); // get texture sampled color
        if (blend == 1) texColor = texColor * color; // if blend mode multiplicative, multiply color and texture
        else if (blend == 2) texColor = (texColor + color) / 2; // if blend mode averaged, average color and texture
        fragColor = texColor; // set final color to calculated texture color (blended or not)
    } else fragColor = color; // if not textured, use material color by itself
}