package graphics;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import utils.Global;
import utils.Pair;
import utils.Utils;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/*
 * Window.java
 * Ambulare
 * Jacob Oaks
 * 4/14/20
 */

/**
 * Encapsulates a GLFW window in a single object
 */
public class Window {

    /**
     * Data
     */
    private final String title;           // window title
    private long handle;                  // the window handle - how GLFW knows this window by
    private int w, h;                     // window width and height
    private int fbw, fbh;                 // frame buffer width and height
    private boolean resized = false;      // whether or not the window has been resized (false by default)
    private boolean vSync;                // whether or not to use v-sync

    /**
     * Constructs the window
     *
     * @param title the title to give to the GLFW window
     * @param w     the width to make the GLFW window. If -1, will cover 80% of the width of the screen when initialized
     * @param h     the height to make the GLFW window. If -1, will cover 80% of the height of screen when initialized
     * @param vSync whether to enable vertical sync
     */
    public Window(String title, int w, int h, boolean vSync) {
        this.title = title; // set title
        this.w = w; // set width
        this.h = h; // set height
        this.vSync = vSync; // set vertical sync setting
    }

    /**
     * Initializes this window by creating and configuring the GLFW window
     */
    public void init() {

        // setup error callback and initialize GLFW
        GLFWErrorCallback.createPrint(System.err).set(); // set error callback. by default, prints errors to System.err
        if (!glfwInit()) Utils.handleException(new Exception("Unable to initialize GLFW"), "graphics.Window", "init()",
                true); // throw exception if cannot initialize GLFW

        // set window hints
        glfwDefaultWindowHints(); // set hints to the defaults
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE); // keep hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE); // window will be resizable
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3); // request GL 3
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2); // request GL 3.2
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); // request core profile
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE); // request forward compatibility

        // check window size
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor()); // get resolution info for main monitor
        if (this.w == -1) this.w = (int) (0.8 * vidmode.width()); // if w is -1, use 80% of width of screen
        if (this.h == -1) this.h = (int) (0.8 * vidmode.height()); // if h is -1, use 80% of height of screen

        // create window
        this.handle = glfwCreateWindow(this.w, this.h, this.title, NULL, NULL); // create window with specified config
        glfwSetWindowSize(this.handle, this.w, this.h); // make sure it is the correct size
        if (this.handle == NULL) Utils.handleException(new Exception("Failed to create the GLFW window"),
                "graphics.Window", "init()", true); // throw exception if cannot create window
        int[] fbw = new int[1]; // create array to get frame buffer width
        int[] fbh = new int[1]; // create array to get frame buffer height
        glfwGetFramebufferSize(this.handle, fbw, fbh); // get frame buffer size
        this.fbw = fbw[0]; // save frame buffer width
        this.fbh = fbh[0]; // save frame buffer height

        // setup frame buffer resizing callback
        glfwSetFramebufferSizeCallback(this.handle, (window, w, h) -> {
            this.fbw = w;
            this.fbh = h;
            this.resized = true; // flag resize - engine checks this every loop to see if a resize occurs
        });

        // setup window resizing callback
        glfwSetWindowSizeCallback(this.handle, (window, w, h) -> {
            this.w = w;
            this.h = h;
        });

        // finishing touches on window
        glfwSetWindowPos(this.handle, (vidmode.width() - this.fbw) / 2,
                (vidmode.height() - this.fbh) / 2); // set position to be middle of screen
        glfwMakeContextCurrent(this.handle); // set this context to be current
        if (this.vSync) glfwSwapInterval(1); // enable vsync if setting is true

        // show window and finish up gl settings
        glfwShowWindow(this.handle); // show this window
        GL.createCapabilities(); // allows interaction between GLFW and GL. Nothing will work without this
        glEnable(GL_BLEND); // essentially allows transparency
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // defines how the blending will create transparency
        glClearColor(0f, 0f, 0f, 0f); // set clear color
    }

    /**
     * Polls for any GLFW window events
     */
    public void pollEvents() {
        glfwPollEvents();
    } // polls for events

    /**
     * Swaps the window buffers
     */
    public void swapBuffers() {
        glfwSwapBuffers(this.handle);
    } // swap the buffers

    /**
     * Closes the window
     */
    public void close() {
        glfwSetWindowShouldClose(this.handle, true); // close the window
    }

    /**
     * Determine if the window was resized
     *
     * @param resetFlag whether to reset the flag after checking (to false)
     * @return whether the window was resized
     */
    public boolean resized(boolean resetFlag) {
        boolean rsz = this.resized; // save resized value
        if (resetFlag) this.resized = false; // reset if reset flag is true
        return rsz; // reset whether resized
    }

    /**
     * @return whether this window should close
     */
    public boolean shouldClose() {
        return glfwWindowShouldClose(this.handle); // determine if this window should close and return the result
    }

    /**
     * @return whether this window has vertical sync enabled
     */
    public boolean usesVSync() {
        return this.vSync;
    }

    /**
     * @return the width of this window
     */
    public int getWidth() {
        return this.w;
    }

    /**
     * @return the height of this window
     */
    public int getHeight() {
        return this.h;
    }

    /**
     * @return the frame buffer width of this window
     */
    public int getFBWidth() {
        return this.fbw;
    }

    /**
     * @return the frame buffer height of this window
     */
    public int getFBHeight() {
        return this.fbh;
    }

    /**
     * @return this window's GLFW handle
     */
    public long getHandle() {
        return this.handle;
    }

    /**
     * Determines if a given key is pressed
     * Note that this can create unexpected behavior if this is called for toggle-type settings every loop because this
     * will remain true until the key is lifted caused toggling to occur many times in a row. Instead, properly react
     * to key events by using keyboardInput() (the engine and all sets of game logic have this method)
     *
     * @param key the key to check
     * @return whether the given key is pressed
     */
    public boolean isKeyPressed(int key) {
        return glfwGetKey(this.handle, key) == GLFW_PRESS;
    }
}
