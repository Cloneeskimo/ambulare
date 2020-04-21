package graphics;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import utils.Pair;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Encapsulates a GLFW window into a single object
 */
public class Window {

    /**
     * Data
     */
    private List<KeyControl> keyControls; // list of key controls (see KeyControl definition below)
    private final String title; // window title
    private int w, h; // window width and height
    private long handle; // the window handle - how GLFW knows this window by
    private boolean resized = false; // whether or not the window has been resized (false by default)
    private boolean vSync; // whether or not to use v-sync

    /**
     * Constructs the window
     * @param title the title to give to the GLFW window
     * @param w the width to make the GLFW window. If -1, will cover 80% of the width of the screen
     * @param h the height to make the GLFW window. If -1, will cover 80% of the height of screen
     * @param vSync whether to enable vertical sync
     */
    public Window(String title, int w, int h, boolean vSync) {
        this.keyControls = new ArrayList<>(); // create KeyControl list
        this.title = title; // set title
        this.w = w; // set width
        this.h = h; // set height
        this.vSync = vSync; // set vertical sync setting
    }

    /**
     * Constructs the window to take 80% of the monitor's width and height
     * @param title the title to give to the window
     * @param vSync whether to enable vertical sync
     */
    public Window(String title, boolean vSync) {
        this(title, -1, -1, vSync); // call other constructor
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
        if (this.w == -1) this.w = (int)(0.8 * vidmode.width()); // if w is -1, use 80% of width of screen
        if (this.h == -1) this.h = (int)(0.8 * vidmode.height()); // if h is -1, use 80% of height of screen

        // create window
        this.handle = glfwCreateWindow(this.w, this.h, this.title, NULL, NULL); // create window with specified config
        if (this.handle == NULL) Utils.handleException(new Exception("Failed to create the GLFW window"),
                "graphics.Window", "init()", true); // throw exception if cannot create window

        // setup resizing callback
        glfwSetFramebufferSizeCallback(this.handle, (window, w, h) -> {
           this.w = w; // update width
           this.h = h; // update height
           this.resized = true; // flag resize - this is what the engine checks for every loop to see if a resize occurs
        });

        // setup key callback to look through registered key controls when a key event occurs (see KeyControls below)
        glfwSetKeyCallback(this.handle, (window, key, scancode, action, mods) -> {
            for (KeyControl kc : this.keyControls) { // for each KeyControl
                if (key == kc.key() && action == kc.action()) kc.reaction(); // if key and action match, react
            }
        });

        // finishing touches on window
        glfwSetWindowPos(this.handle, (vidmode.width() - this.w) / 2,
                (vidmode.height() - this.h) / 2); // set position to be middle of screen
        glfwMakeContextCurrent(this.handle); // set this context to be current
        if (this.vSync) glfwSwapInterval(1); // enable vsync if setting is true

        // show window and finish up gl settings
        glfwShowWindow(this.handle); // show this window
        GL.createCapabilities(); // allows interaction between GLFW and GL. Nothing will work without this
        glEnable(GL_BLEND); // essentially allows transparency
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // defines how the blending will create transparency
        glClearColor(0.4f, 0.7f, 1.0f, 0.0f); // set clear color
    }

    /**
     * Polls for any GLFW window events
     */
    public void pollEvents() { glfwPollEvents(); } // polls for events

    /**
     * Swaps the window buffers
     */
    public void swapBuffers() { glfwSwapBuffers(this.handle); } // swap the buffers

    /**
     * Registers a keyboard control to this window
     * @param keyControl the keyboard control to register to the window (see KeyControl interface defined below)
     */
    public void registerKeyControl(KeyControl keyControl) {
        this.keyControls.add(keyControl); // add key control to list of key controls
    }

    /**
     * Determine if the window was resized
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
    public boolean usesVSync() { return this.vSync; }

    /**
     * @return the width of this window
     */
    public int getWidth() { return this.w; }

    /**
     * @return the height of this window
     */
    public int getHeight() { return this.h; }

    /**
     * @return this window's GLFW handle
     */
    public long getHandle() { return this.handle; }

    /**
     * Determines if a given key is pressed
     * Note that this can create unexpected behavior if this is called for toggle-type settings every loop because this
     * will remain true until the key is lifted caused toggling to occur many times in a row. Use the KeyControl
     * interface to better define and control keyboard reactions
     * @param key the key to check
     * @return whether the given key is pressed
     */
    public boolean isKeyPressed(int key) { return glfwGetKey(this.handle, key) == GLFW_PRESS; }

    /**
     * @return the mouse position as window coordinates
     */
    public Pair getMousePos() {
        double[] x = new double[1]; // create array to hold x
        double[] y = new double[1]; // create array to hold y
        glfwGetCursorPos(this.handle, x, y); // put position in each array
        return new Pair((float)x[0], (float)y[0]); // put into a pair and return
    }

    /**
     * Represents a keyboard control that can be registered to a window
     */
    public interface KeyControl {
        int key(); // the key to trigger this control
        int action(); // the action to trigger this control (key press, release, etc.)
        void reaction(); // what do to when the control is used
    }
}
