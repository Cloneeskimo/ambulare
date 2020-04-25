package gameobject.gameworld;

import graphics.Material;
import graphics.MultiTexCoordModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Extends the block by performing texture animation
 */
public class AnimatedBlock extends Block {
    
    /**
     * Static Data
     */
    public static final Map<Integer, MultiTexCoordModel> models = new HashMap<>(); /* this will maps from amount of
        frames to a MTCM with the appropriate set of texture coordinates. These are then used to do the rendering of
        animated blocks. Since many blocks may be animated, this saves from having tons of instantiated models. The
        fact that there is a different model for each amount of frames also means that less variance in frame count
        in animated blocks is more space efficient */

    /**
     * Members
     */
    private int frames;              // how many frames are in the texture
    private int currentFrame;        // the current frame of the animation
    private float frameTime;         // how much time a frame should last
    private float frameTimeLeft;     // how much time is left for the current frame

    /**
     * Constructs the animated block with the texture with the given resource-relative path
     * @param texPath the path to the texture
     * @param resPath whether the given path is resource-relative
     * @param x the x grid position of the block
     * @param y the y grid position of the block
     * @param frames the amount of frames in the texture
     * @param frameTime how long each frame will be
     * @param randStart whether to start at a random location in the animation
     */
    public AnimatedBlock(String texPath, boolean resPath, int x, int y, int frames, float frameTime,
                         boolean randStart) {
        super(texPath, resPath, x, y);
        this.initAnim(frames, frameTime, randStart); // initialize the animation
    }

    /**
     * Constructs the animated block with a custom material
     * @param material the material to use for the block
     * @param x the x grid position of the block
     * @param y the y grid position of the block
     * @param frames the amount of frames in the texture
     * @param frameTime how long each frame will be
     * @param randStart whether to start at a random location in the animation
     */
    public AnimatedBlock(Material material, int x, int y, int frames, float frameTime, boolean randStart) {
        super(material, x, y);
        this.initAnim(frames, frameTime, randStart); // initialize the animation
    }

    /**
     * Initializes the animation
     * @param frames the amount of frames in the texture
     * @param frameTime how long each frame will be
     * @param randStart whether to start at a random location in the animation
     */
    private void initAnim(int frames, float frameTime, boolean randStart) {
        this.frames = frames;
        this.frameTime = frameTime;
        this.currentFrame = randStart ? ((int)(Math.random() * frames)) : 0; // calculate starting frame
        this.frameTimeLeft = randStart ? ((float)Math.random() * frameTime) : frameTime; // calculate starting time left
    }

    /**
     * Updates the animated block by updating its animation
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        this.frameTimeLeft -= interval; // update frame time left
        if (this.frameTimeLeft <= 0f) { // if frame is over
            this.frameTimeLeft += this.frameTime; // reset time
            this.currentFrame++; // iterate frame
            if (this.currentFrame >= this.frames) this.currentFrame = 0; // if end of frames, start over
        }
    }

    /**
     * @return the current frame of the animated block
     */
    public int getCurrentFrame() {
        return this.currentFrame;
    }

    /**
     * @return how many frames the animated block's texture has
     */
    public int getFrames() {
        return this.frames;
    }
}
