package utils;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;

/**
 * A class to hold global static settings
 */
public class Global {
    public static final int FPS_REPORTING_TOGGLE_KEY = GLFW_KEY_F; // the key to toggle FPS reporting in a GameEngine
    public static final int TARGET_FPS = 60; // the target frames per second when vertical sync is off
    public static final int TARGET_UPS = 60; // the target updates per second regardless of vertical sync
    public static final boolean V_SYNC = true; // whether to enable vertical sync in the Window
}
