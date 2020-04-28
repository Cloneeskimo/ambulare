package graphics;

import java.util.HashMap;
import java.util.Map;

/**
 * Extends textures by implementing animation. Note that in order for animated textures to animate, they must be
 * updates and the rendering system must update the corresponding model's texture coordinate VBO by calling the
 * animated texture's getTexCoordVBO() method. It is a bad idea to have game objects update their animated texture,
 * because if two game objects have the same animated texture, it will get updated twice and the animation will appear
 * twice as fast. Instead, a list of unique animated textures should be kept in the rendering system and update
 * separately
 */
public class AnimatedTexture extends Texture {

    /**
     * Static Data
     */
    public static Map<Integer, int[]> texCoords = new HashMap<>(); /* this maps from amount of frames of an
        animation to set of texture coordinate VBOs to used when rendering a frame. Since many textures may be
        animated, this saves from having tons of repeat lists/arrays of texture coordinates. The fact that there is a
        different set for each amount of frames also means that less variance in frame count is more space efficient */

    /**
     * Will get the correct texture coordinate vertex buffer object to give to a model given the current frame of the
     * texture and the total amount of frames
     *
     * @return the correct texture coordinate VBO as described above
     */
    public static int getTexCoordVBO(int frame, int of) {
        int[] texCoordVBOs = AnimatedTexture.texCoords.get(of); // try to get the set of VBOs
        if (texCoordVBOs == null) { // if this set of texture coordinate VBOs hasn't been calculated yet
            texCoordVBOs = Model.calcTexCoordVBOs(of); // calculate the tex coords for that amount of frames
            AnimatedTexture.texCoords.put(of, texCoordVBOs); // save to map
        }
        return texCoordVBOs[frame]; // get and return the texture coordinates for the current frame
    }

    /**
     * Members
     */
    protected int frames;          // total amount of frames and current frame
    protected int frame;           // current frame of animation
    protected float frameTime;     // amount of time per frame
    protected float frameTimeLeft; // amount of time left for the current frame

    /**
     * Constructor
     *
     * @param path      the path to the image
     * @param resPath   whether the given path is resource-relative
     * @param frames    the total amount of frames
     * @param frame     the starting frame
     * @param frameTime the amount of time (in seconds) to give each frame
     * @param randStart whether to start the animation at a random time
     */
    public AnimatedTexture(String path, boolean resPath, int frames, int frame, float frameTime, boolean randStart) {
        super(path, resPath);
        this.frames = Math.min(frames - 1, Math.max(0, frame)); // set starting frame bounded by 0 and frame count
        this.frameTime = frameTime;
        this.frame = randStart ? ((int) (Math.random() * frames)) : 0; // calc starting frame
        this.frameTimeLeft = randStart ? ((float) Math.random() * frameTime) : frameTime; // calc starting time left
    }

    /**
     * Updates the animated texture
     */
    public void update(float interval) {
        this.frameTimeLeft -= interval; // account for time in animation
        if (this.frameTimeLeft < 0f) { // if frame time for current frame is up
            this.frameTimeLeft = frameTime; // reset frame time counter
            this.frame++; // go to the next frame
            if (this.frame >= this.frames) this.frame = 0; // go back to start after last frame
        }
    }

    /**
     * @return the id of the correct texture coordinate VBO to use given the current frame of the animated texture
     */
    public int getTexCoordVBO() {
        return AnimatedTexture.getTexCoordVBO(this.frame, this.frames);
    }
}
