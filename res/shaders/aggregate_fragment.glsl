
/*
 * aggregate_fragment.glsl
 * Ambulare
 * Jacob Oaks
 * 5/11/2020
 */

/*
 * These shaders are used to aggregate many materialed objects into a single texture. This fragment shader will take
 * material into account and, optionally, will fade (with or without corners) the texture into transparency based on a
 * give direction
 */

// specify GLSL version
#version 330

/*
 * Uniforms
 */
uniform sampler2D texSampler;     // texture sampler - bound to the material's texture
uniform vec4 color;               // color - bound to the material's color
uniform int isTextured;           // flag representing whether the material is textured or not
uniform int blend;                /* defines how to blend a material's color and texture if there are both: 0 - just use
                                     the texture; 1 - multiply color and texture; 2 - average color and texture */
uniform int fadeDir;              /* which direction to consider for applying a fade: 0 - no fade; 1 - fade out left;
                                     2 - fade out right; 3 - fade out above; 4 - fade out below; */
uniform int corners;              // whether to fade using corners on the caps (1) or not (0)
uniform int w, h;                 // the width and height of the final texture - needed for corners

/*
 * In/Out Variables
 */
in vec2 fTexCoords; // texture coordinates as passed in from vertex shader
in vec2 normPosCoords; // normalized position coordinates as passed in from vertex shader - used for fade
out vec4 fragColor; // final color to assign to the fragment

/*
 * Calculates the base color based on material
 */
vec4 getMaterialColor() {
    if (isTextured == 1) { // if the maetrial is textured
        vec4 texColor = texture(texSampler, fTexCoords); // get texture sampled color
        if (blend == 1) texColor = texColor * color; // if blend mode multiplicative, multiply color and texture
        else if (blend == 2) texColor = (texColor + color) / 2; // if blend mode averaged, average color and texture
        return texColor; // set texture color (blended or not) as final color
    } else return color; // if not textured, use base color of material

}

/*
 * Applies fade to the given color if there should be on, based on the position coordinates and fade uniform values
 */
vec4 applyFade(vec4 c) {
    if (fadeDir < 1 || fadeDir > 4) return c; // if fade direction is out of bounds, do not apply a fade
    float alpha = 0; // calculate the alpha value based on corners flag value and fade direction
    if (corners == 1) { // if corners flag is true
        vec2 p = vec2(normPosCoords.x * w, normPosCoords.y * h); // convert point to pixel space
        float d = 0; // calculate the distance from the edge corresponding to the fade direction
        float thickness = min(w, h); // calculate the thickness of the texture
        switch (fadeDir) { // swwitch on the fade direction and calculate the distance with corners in mind
            case 1:// fade to left
            d = distance(p, vec2(w, min(h - thickness, max(thickness, p.y))));
            break;
            case 2:// fade to right
            d = distance(p, vec2(0, min(h - thickness, max(thickness, p.y))));
            break;
            case 3:// fade above
            d = distance(p, vec2(min(w - thickness, max(thickness, p.x)), h));
            break;
            case 4:// fade below
            d = distance(p, vec2(min(w - thickness, max(thickness, p.x)), 0));
            break;
        }
        alpha = 1 - (d / thickness); // alpha value should be relative to how far along the thickness the point is
    } else { // if corners flag is not true
        switch (fadeDir) { // switch on fade direction and directly calculate alpha value based on normalized coordinate
            case 1: // fade to left
            alpha = normPosCoords.x;
            break;
            case 2: // fade to right
            alpha = 1 - normPosCoords.x;
            break;
            case 3: // fade above
            alpha = normPosCoords.y;
            break;
            case 4: // fade below
            alpha = 1 - normPosCoords.y;
            break;
        }
    }
    return vec4(c.xyz, alpha); // return same color using calculate alpha value
}

/*
 * Main Function
 */
void main() {
    fragColor = applyFade(getMaterialColor()); // get material color, fade (if necessary), and set as final color
}
