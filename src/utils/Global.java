package utils;

import graphics.Font;
import graphics.Window;
import org.lwjgl.Version;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.opengl.GL11.*;

/*
 * Global.java
 * Ambulare
 * Jacob Oaks
 * 4/18/20
 */

/**
 * A class to hold various global static settings
 */
public class Global {

    /**
     * Static Data
     */
    public static final String VERSION = "com95"; // the version of the game
    public static final String WINDOW_TITLE = "Ambulare " + VERSION; // the window title
    public static final float TIME_BETWEEN_FPS_REPORTS = 1f; // time between FPS reports when reports are enabled
    public static final int FPS_REPORTING_TOGGLE_KEY = GLFW_KEY_1; // the key to toggle FPS reporting in the engine
    public static final int POLYGON_MODE_TOGGLE_KEY = GLFW_KEY_2; // the key to toggle between fill/line polygon modes
    public static final int TARGET_FPS = 60; // the target frames per second when vertical sync is off
    public static final int TARGET_UPS = 60; // the target updates per second regardless of vertical sync
    public static final boolean V_SYNC = true; // whether to enable vertical sync in the Window
    public static Font FONT; // font used everywhere throughout the program
    public static float ar = 0f; // the current aspect ratio of the game's window
    public static boolean arAction = false; /* a flag representing how to distort coordinates when rendering depending
        on the game window's aspect ratio. If ar < 1.0f (height > width) then we will make objects shorter to compensate
        and if ar > 1.0f, the opposite is true */
    private static boolean polygonMode; // the current polygon mode used by GL (lines or fill)

    /**
     * Initialize any global members
     */
    public static void init() {
        // log game version
        Utils.log("Ambulare Version: " + VERSION, "utils.Global", "init()", false);
        // initialize the global font
        Global.FONT = new Font("/textures/ui/font.png", Node.resToNode("/misc/font.node"));
        Utils.log("Global font initialized", "utils.Global", "init()", false); // log
    }

    /**
     * Updates the global aspect ratio and aspect ratio variables
     *
     * @param w the window to use for the calculations
     */
    public static void updateAr(Window w) {
        Global.ar = (float) w.getFBWidth() / (float) w.getFBHeight(); // calculate aspect ratio
        Global.arAction = (Global.ar < 1.0f); // calculate the aspect ratio action. See static data for more information
    }

    /**
     * Toggles GL's polygon mode between fill and lines
     */
    public static void togglePolygonMode() {
        Global.polygonMode = !Global.polygonMode; // update flag
        glPolygonMode(GL_FRONT_AND_BACK, Global.polygonMode ? GL_LINE : GL_FILL); // if true -> lines; false -> fill
    }

    /**
     * Grabs the exact rgba color corresponding to the given theme color. The point of this method is to have important
     * colors match a general theme and to originate from the same place
     *
     * @param tc the theme color whose exact rgba color to return
     * @return the exact rgba color corresponding to the given theme color
     */
    public static float[] getThemeColor(ThemeColor tc) {
        float[] c = null;
        switch (tc) { // switch on the theme color
            case GRAY: // gray
                c = new float[]{0.522f, 0.522f, 0.522f, 1f};
                break;
            case WHITE: // white
                c = new float[]{0.97f, 0.97f, 0.97f, 1f};
                break;
            case DARK_GREEN: // dark green
                c = new float[]{0.082f, 0.349f, 0.098f, 1f};
                break;
            case GREEN: // green
                c = new float[]{0.333f, 0.584f, 0.318f, 1f};
                break;
            case SKY_BLUE: // sky blues
                c = new float[]{0.49f, 0.808f, 0.922f, 1f};
                break;
        }
        return c; // return the corresponding color
    }

    /**
     * Defines a set of consolidated theme colors whose exact RGBA equivalents are outlined in getThemeColor()
     */
    public enum ThemeColor {
        GRAY, WHITE, DARK_GREEN, GREEN, SKY_BLUE
    }
}
