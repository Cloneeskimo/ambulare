package utils;

import graphics.Font;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;

/**
 * A class to hold various global static settings
 */
public class Global {

    /**
     * Data
     */
    public static final boolean V_SYNC = true; // whether to enable vertical sync in the Window
    public static final int FPS_REPORTING_TOGGLE_KEY = GLFW_KEY_F; // the key to toggle FPS reporting in the engine
    public static final int TARGET_FPS = 60; // the target frames per second when vertical sync is off
    public static final int TARGET_UPS = 60; // the target updates per second regardless of vertical sync
    public static final float TIME_BETWEEN_FPS_REPORTS = 1f; // time between FPS reports when reports are enabled
    public static final String WINDOW_TITLE = "Ambulare"; // the window title
    public static Font FONT; // font

    /**
     * Initialize any global members
     */
    public static void init() {
        Global.FONT = new Font("/font.png", "/font_info.txt");
    }
}
