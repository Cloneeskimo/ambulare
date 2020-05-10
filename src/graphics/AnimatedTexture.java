package graphics;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/*
 * AnimatedTexture.java
 * Ambulare
 * Jacob Oaks
 * 4/27/20
 */

/**
 * Extends textures by implementing animation. Note that in order for animated textures to animate, they must be
 * updated and the rendering system must update the corresponding model's texture coordinate VBO by calling the
 * animated texture's getTexCoordVBO() method. It is a bad idea to have game objects update their animated texture,
 * because if two game objects have the same animated texture, it will get updated twice and the animation will appear
 * twice as fast. Instead, a list of unique animated textures should be kept in the rendering system and update
 * separately
 */
public class AnimatedTexture extends Texture {

    /**
     * Static Data
     */
    public static Map<Integer, int[][]> texCoords = new HashMap<>(); /* this maps from amount of frames of an animation
        to two sets of texture coordinate VBOs to used when rendering a frame, where value[0] is the set of texture
        coordinate VBOs for un-flipped textures for the amount of frames given by the key and texCoords[1] is the set of
        texture coordinate VBOs for flipped textures for the amount of frames given by the key. Since many textures may
        be animated, this saves from having tons of repeat lists/arrays of texture coordinates. The fact that there is a
        different set for each amount of frames also means that less variance in frame count is more space efficient */

    /**
     * Will get the correct texture coordinate vertex buffer object to give to a model given the current frame of the
     * texture, the total amount of frames, and a flag representing whether to flip the texture coordinates or not.
     *
     * @param frame the frame whose texture coordinate VBO to return
     * @param of    the total amount of frames in the considered texture
     * @param flip  whether to flip the texture horizontally. This is useful for animations for entities that may be
     *              facing either left or right at any given time
     * @return the correct texture coordinate VBO as described above
     */
    public static int getTexCoordVBO(int frame, int of, boolean flip) {
        // get the two sets of texture coordinate VBOs for the given amount of frames
        int[][] texCoordVBOs = AnimatedTexture.texCoords.computeIfAbsent(of, k -> new int[2][]);
        int[] vbos = texCoordVBOs[flip ? 1 : 0]; // get the VBOss corresponding to the flip flag
        // if there are no VBOs for the given frame count and flip flag, create them
        if (vbos == null) vbos = texCoordVBOs[flip ? 1 : 0] = Model.calcTexCoordVBOs(of, flip);
        return vbos[frame]; // return the texture coordinate VBO for the current frame
    }

    /**
     * Members
     */
    protected int frames;             // total amount of frames and current frame
    protected int frame;              // current frame of animation
    protected float frameTime;        // amount of time per frame
    protected float frameTimeLeft;    // amount of time left for the current frame
    protected FrameReachCallback frc; // a callback to be called when a new frame in the animation is reached

    /**
     * Constructs the animated texture by creating a texture at the given path and with the given animated properties
     *
     * @param path      the path to the image
     * @param resPath   whether the given path is resource-relative
     * @param frames    the total amount of frames
     * @param frameTime the amount of time (in seconds) to give each frame
     * @param randStart whether to start the animation at a random time
     */
    public AnimatedTexture(String path, boolean resPath, int frames, float frameTime, boolean randStart) {
        super(path, resPath);
        this.frames = frames;
        this.frameTime = frameTime;
        this.frame = randStart ? ((int) (Math.random() * frames)) : 0; // calc starting frame
        this.frameTimeLeft = randStart ? ((float) Math.random() * frameTime) : frameTime; // calc starting time left
    }

    /**
     * Constructs the animated texture with the given openGL texture ID, width and height, and animation properties
     *
     * @param id        the openGL texture ID to use
     * @param w         the width of the texture
     * @param h         the height of the texture
     * @param frames    the total amount of frames
     * @param frameTime the amount of time (in seconds) to give each frame
     * @param randStart whether to start the animation at a random time
     */
    public AnimatedTexture(int id, int w, int h, int frames, float frameTime, boolean randStart) {
        super(id, w, h);
        this.frames = frames;
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
            if (this.frc != null) this.frc.atFrame(this.frame); // invoke the callback if it exists
        }
    }

    /**
     * Sets a frame reach callback to be invoked whenever a new frame is reached in the animation
     *
     * @param frc the frame reach callback to save and invoke
     */
    public void giveFrameReachCallback(FrameReachCallback frc) {
        this.frc = frc; // save new frame reach callback as a member
    }

    /**
     * Grabs the correct texture coordinate VBO to give to a model to represent the current frame of the texture
     *
     * @param flip whether to flip the texture horizontally. This is useful for animations for entities that may be
     *             facing either left or right at any given time
     * @return the id of the correct texture coordinate VBO to use given the current frame of the animated texture
     */
    public int getTexCoordVBO(boolean flip) {
        return AnimatedTexture.getTexCoordVBO(this.frame, this.frames, flip);
    }

    /**
     * @return the amount of frames the animated texture has
     */
    public int getFrameCount() {
        return this.frames;
    }

    /**
     * @return how long each frame in the animated texture should last
     */
    public float getFrameTime() {
        return this.frameTime;
    }

    /**
     * This interface can be used to create a callback whenever a new frame is reached in the animation
     */
    @FunctionalInterface
    public interface FrameReachCallback {

        /**
         * The method that will be called when a new frame is reached
         *
         * @param frame the frame that was reached
         */
        void atFrame(int frame);
    }
}
