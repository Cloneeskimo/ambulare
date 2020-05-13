
/*
 * format_block_fragment.glsl
 * Ambulare
 * Jacob Oaks
 * 5/9/2020
 */

// specify GLSL version
#version 330

/*
 * These shaders are used to format blocks loaded into areas via area node-files. They support cutting of edges and
 * applying up to MAX_OVERLAYS overylay textures. When combined, cuts and overlays provide more aesthetic appeal to
 * areas. Having extensive programming to handle formatting of these textures allows for easier and more efficient area
 * design at the expense of a small increase in area loading time
 */

/*
 * Constants
 */
const int MAX_OVERLAYS = 4; // the maximum amount of overlays that can be added to a block texture

/*
 * Uniforms
 */
uniform sampler2D base;                   // the base block texture
uniform sampler2D[MAX_OVERLAYS] overlays; // the overlays to place over the block texture
uniform int[MAX_OVERLAYS] rotations;      /* the parallel list of rotations to apply to the overlays. If there are less
                                              than MAX_OVERLAYS overlays, setting non-existent overlay's parallel
                                              rotations to -1 will cause that overlay to not be used. For example, if
                                              there are only two overlays, set rotations[2-4] to -1 to disable overlays
                                              in the additional slots. Rotations can have the following values: 0 (no
                                              rotation); 1 (90 degrees of rotation); 2 (180 degrees of rotation); and 3
                                              (270 degrees of rotation); where all rotations are performed counter-
                                              clockwise */
uniform int cutTopLeft;                    // whether to cut the top left corner (1) or not (0)
uniform int cutTopRight;                   // whether to cut the top right corner (1) or not (0)
uniform int cutBottomLeft;                 // whether to cut the bottom left corner (1) or not (0)
uniform int cutBottomRight;                // whether to cut the bottom right corner (1) or not (0)
uniform int frames;                        // how many frames the base block texture has (1 if not animated)
uniform float cutRadius;                   /* the cut radius to use when cutting corners. This should be a value from
                                              0 to 1 (inclusive) where 0 denotes no cutting and 1 denotes cutting
                                              according to a circle whose radius is half of the block size */

/*
 * In/Out Variables
 */
in vec2 texCoordsF;                        // the texture coordinates as passed in from the vertex shader
in vec2 modelCoordsF;                      // the model coordinates as passsed in from the vertex shader
out vec4 fragColor;                        // the final fragment color

/*
 * Determines whether this fragment needs to be cut depending on the cut uniforms, radius, and location of the fragment
 */
bool cut() {

    // create relevant variables
    bool cut = false; // cut flags is false initially
    float cutStart = 1 - cutRadius; // the start of the cut-applicable area is 1 - the radius

    // transform x based on animation, ensureing that cuts apply to all frames instead of just the overall texture
    float x = (modelCoordsF.x + 1); // transform x from (-1, 1) to (0, 2)
    x /= 2; // transform x from (0, 2) to (0, 1)
    x *= frames; // transform x from (0, 1) to (0, frames)
    x = mod(x, 1); // transform x from (0, frames) to (0, 1) based on current frame
    x *= 2; // transform x from (0, 1) based on current frame to (0, 2) based on current frame
    x -= 1; // transform x from (0, 2) back to (-1, 1) based on current frame
    vec2 consider = vec2(x, modelCoordsF.y); // create the final position to consider for cutting

    // detect if the current fragment is within a corner where cutting is applicable
    int cutXInterval = (consider.x < -cutStart ? -1 : consider.x > cutStart ? 1 : 0);
    int cutYInterval = (consider.y < -cutStart ? -1 : consider.y > cutStart ? 1 : 0);

    // determine if cuts are necessary
    if (cutTopLeft == 1 && cutXInterval == -1 && cutYInterval == 1) // top left
        cut = cut || distance(consider, vec2(-cutStart, cutStart)) > cutRadius; // if outside of radius, cut
    else if (cutTopRight == 1 && cutXInterval == 1 && cutYInterval == 1) // top right
        cut = cut || distance(consider, vec2(cutStart, cutStart)) > cutRadius; // if outside of radius, cut
    else if (cutBottomLeft == 1 && cutXInterval == -1 && cutYInterval == -1) // bottom left
        cut = cut || distance(consider, vec2(-cutStart, -cutStart)) > cutRadius; // if outside of radius, cut
    else if (cutBottomRight == 1 && cutXInterval == 1 && cutYInterval == -1) // bottom right
        cut = cut || distance(consider, vec2(cutStart, -cutStart)) > cutRadius; // if outside of radius, cut
    return cut; // return final cut flag
}

/*
 * Transforms a texture coordinate into the correct one to consider with the amount of animation frames in mind
 * @param point the texture coordinate point to convert to the correct point with animation in mind
 */
vec2 applyFrames(vec2 point) {
    point.x *= frames; // since texture coordinates wrap, a simple multiplication of xis all that is needed
    return point; // return point with multiplied x
}

/*
 * Rotates a point around the origin using the given angle. This is used for rotating overlay texture coordinates
 * to their correct rotation before applying them to the base block texture
 * @param pint the point to rotation
 * @param the angle (in degrees) to rotate the given point by (counter-clockwise)
 */
vec2 rotatePoint(vec2 point, float angle) {
    angle = radians(angle); // convert the angle to radians
    float s = sin(angle); // calculate the sine of the angle
    float c = cos(angle); // calculate the cosine of the angle
    point = vec2(point.x * c - point.y * s, point.x * s + point.y * c); // rotate the point
    return point; // return the rotated point
}

/*
 * Applies rotation to texture coordinates the rotation integer flag at the given index within the rotations array
 * @param texCoords the texture coordinates to rotate
 * @param i the index of the rotation flag in the rotations array to follow when rotating (defined above in Uniforms)
 */
vec2 applyRotation(vec2 texCoords, int i) {
    vec2 p = texCoords; // start with the regular texture coordinates
    switch (rotations[i]) { // switch on the rotation flag at the given index
        case 1: // if the flag is one
            p = rotatePoint(p, 90); // rotate the point 90 degrees
            break;
        case 2: // if the flag is two
            p = rotatePoint(p, 180); // rotate the point 180 degrees
            break;
        case 3: // if the flag is threee
            p = rotatePoint(p, 270); // rotate the point 270 degrees
            break;
    }
    return p; // return the point correctly rotated
}

/*
 * Main Function
 */
void main() {
    if (cut()) fragColor = vec4(0, 0, 0, 0); // if this point should be cut, return transparency as the final color
    else { // if this point shouldn't be cut
        vec4 color = texture(base, texCoordsF); // get the base texture's color
        for (int i = 0; i < MAX_OVERLAYS; i++) { // go through each possible overlay
            if (rotations[i] > -1) { // if there is actually an overlay at index i
                vec2 overlayTexCoords = applyFrames(texCoordsF); // convert the tex coords into frame-relative ones
                overlayTexCoords = applyRotation(overlayTexCoords, i); // apply rotation to it
                vec4 c2 = texture(overlays[i], overlayTexCoords); // get the color of the overlay at the tex coords
                vec3 mix = color.xyz * (1 - c2.a) + c2.xyz * (c2.a); // mix it with the current texture
                color = vec4(mix, color.a); // update the currenet color
            }
        }
        fragColor = color; // after all overlays have been applied, use the current color as the final one
    }
}
