package utils;

import graphics.Font;
import graphics.Window;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;

/**
 * A class to hold various global static settings
 */
public class Global {

    /**
     * Static Data
     */
    public static final String WINDOW_TITLE = "Ambulare"; // the window title
    public static final float TIME_BETWEEN_FPS_REPORTS = 1f; // time between FPS reports when reports are enabled
    public static final int FPS_REPORTING_TOGGLE_KEY = GLFW_KEY_F; // the key to toggle FPS reporting in the engine
    public static final int TARGET_FPS = 60; // the target frames per second when vertical sync is off
    public static final int TARGET_UPS = 60; // the target updates per second regardless of vertical sync
    public static final boolean V_SYNC = true; // whether to enable vertical sync in the Window
    public static Font FONT; // font used everywhere throughout the program
    public static float ar = 0f;
    public static boolean arAction = false;

    /**
     * Initialize any global members
     */
    public static void init() {
        Global.FONT = new Font("/textures/ui/font.png", "/font_info.txt");
    }

    /**
     * Updates the global aspect ratio and aspect ratio variables
     *
     * @param w the window to use for the calculationsss
     */
    public static void updateAr(Window w) {
        Global.ar = (float) w.getFBWidth() / (float) w.getFBHeight(); // calculate aspect ratio
        Global.arAction = (Global.ar < 1.0f); /* this stores what actions need to be done to compensate for aspect
            ratio. If ar < 1.0f (height > width) then we will make objects shorter to compensate and if ar > 1.0f,
            the opposite is true */
    }
}
