package graphics;

/*
 * MSAT.java
 * Ambulare
 * Jacob Oaks
 * 4/27/20
 */

/**
 * MSATs (Multi-State Animated Textures) extend animated textures by providing support for multiple states of animation
 * where each state represents a different set of frames within the texture to use
 */
public class MSAT extends AnimatedTexture {

    /**
     * Counts the total amount of frames in an sprite sheet given the MSAT states
     *
     * @param states the MSAT states to go through to count frames
     * @return the total amount of framese
     */
    private static int countFrames(MSATState[] states) {
        int frames = 0; // start at zero
        for (MSATState state : states) frames += state.frames; // add each state's frame count
        return frames;
    }

    /**
     * Members
     */
    private final MSATState[] states; // the different animation states to use
    private int stateFrame = 0;       // the current frame within the current animate state
    private int state = 0;            // the current animation state

    /**
     * Constructor
     *
     * @param path    the path to the image
     * @param resPath whether the given path is resource-relative
     * @param states  a list of states to guide the MSAT in updating its animation. See MSATState for more info. Note
     *                that the sum of the frames of each state must equal the total amount of frames in the actual
     *                image. These should be given in thee order that they appear in the image
     */
    public MSAT(String path, boolean resPath, MSATState[] states) {
        super(path, resPath, countFrames(states), states[0].frameTime, false);
        this.states = states;
        this.state = 0;
    }

    /**
     * Updates the MSAT
     */
    @Override
    public void update(float interval) {
        this.frameTimeLeft -= interval; // update frame time left
        if (this.frameTimeLeft <= 0f) { // if frame is over
            this.frameTimeLeft += this.states[this.state].frameTime; // reset time
            this.stateFrame++; // move to next frame
            if (this.stateFrame >= this.states[this.state].frames) // if end of frames for current state
                this.stateFrame = 0; // return to first frame
            this.setAppropriateFrame(); // calculate the appropriate overall frame
        }
    }

    /**
     * Calculates and saves the appropriate overall frame of the texture given the current state and frame within the
     * current state
     */
    private void setAppropriateFrame() {
        // make sure state frame is actually within the bounds of the corresponding state's frame count
        this.stateFrame = stateFrame % this.states[this.state].frames;
        this.frame = this.stateFrame; // start with state frame
        // for each previous state, skip past those frames in the image
        for (int i = 1; i <= this.state; i++) this.frame += this.states[i - 1].frames;
    }

    /**
     * Sets the animation state of the MSAT
     *
     * @param state the new animation state (where the first one would be state 0)
     */
    public void setState(int state) {
        // re-calculate frame time left for new state
        this.frameTimeLeft = this.states[state].frameTime;
        this.state = state; // update state
        this.setAppropriateFrame(); // calculate overall frame
    }

    /**
     * Represents a single state of a MSAT
     */
    public static class MSATState {

        /**
         * Members
         */
        int frames;      // how many frames are in the state
        float frameTime; // how much time (in seconds) each frame states in the state

        /**
         * Constructor
         *
         * @param frames    the amount of frames to assign to the state
         * @param frameTime the amount of time each frame should last in the state
         */
        public MSATState(int frames, float frameTime) {
            this.frames = frames;
            this.frameTime = frameTime;
        }
    }
}
