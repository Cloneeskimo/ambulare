package utils;

import gameobject.ui.EnhancedTextObject;
import graphics.Font;
import graphics.Material;
import graphics.ShaderProgram;
import graphics.Window;
import org.lwjgl.system.CallbackI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public abstract class Global {

    /**
     * Static Data
     */
    public static final String VERSION = "com117"; // the version of the game
    public static final String WINDOW_TITLE = "Ambulare " + VERSION; // the window title
    public static final int POLYGON_MODE_TOGGLE_KEY = GLFW_KEY_2; // the key to toggle between fill/line polygon modes
    public static final int DEBUG_TOGGLE_KEY = GLFW_KEY_1;  // key to toggle debug reporting
    public static final int TARGET_FPS = 60; // the target frames per second when vertical sync is off
    public static final int TARGET_UPS = 60; // the target updates per second regardless of vertical sync
    public static DebugInfo debugInfo; // an extended enhanced text object to display debug info
    public static Window gameWindow; // GLFW window hosting the game
    public static Font font; // font used everywhere throughout the program
    public static float ar = 0f; // the current aspect ratio of the game's window
    public static boolean arAction = false; /* a flag representing how to distort coordinates when rendering depending
        on the game window's aspect ratio. If ar < 1.0f (height > width) then we will make objects shorter to compensate
        and if ar > 1.0f, the opposite is true */
    public static boolean resetAccumulator; /* when true, the game engine will reset its loop time accumulator. This is
        particularly useful after operations that will take a lot of time and that time may not need to be accounted
        for, such as loading before an ROC fade */
    private static boolean polygonMode; // the current polygon mode used by GL (lines or fill)

    /**
     * Initializes global members
     */
    public static void init() {
        // log game version
        Utils.log("Ambulare Version: " + VERSION, Global.class, "init", false);
        // initialize the global font
        Global.font = new Font(new Utils.Path("/textures/ui/font.png", true),
                Node.pathContentsToNode(new Utils.Path("/misc/font.node", true)));
        Global.debugInfo = new DebugInfo(); // initialize debug info
        Global.debugInfo.setVisibility(false); // set debug info visibility to false initially
        Utils.log("Global members initialized", Global.class, "init", false); // log
    }

    /**
     * Cleans up global members
     */
    public static void cleanup() {
        Global.font.getSheet().cleanup(); // cleanup global font sheet texture
        Global.debugInfo.cleanup(); // cleanup debug info
        Utils.log("Global members cleaned up", Global.class, "cleanup", false); // log
    }

    /**
     * Updates the global aspect ratio and aspect ratio variables
     */
    public static void updateAr() {
        Global.ar = (float) gameWindow.getFBWidth() / (float) gameWindow.getFBHeight(); // calculate aspect ratio
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
                c = new float[]{0.7f, 0.7f, 0.7f, 1f};
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

    /**
     * A generic callback functional interface
     */
    @FunctionalInterface
    public interface Callback {
        void invoke(); // called when the generic event occurs

    }

    /**
     * An extension of enhanced text objects which allows for easy reporting of debug information by allowing for
     * convenient editing of specific debug fields dynamically
     */
    public static class DebugInfo extends EnhancedTextObject {

        /**
         * Members
         */
        private final Map<String, Integer> fields = new HashMap<>(); // map from debug fields to line indices
        private Callback sizeChangeCallback;                         // a callback to invoke when the size changes

        /**
         * Constructor
         */
        public DebugInfo() {
            // call super with pre-defined settings for debug information
            super(new Material(new float[] { 0f, 0f, 0f, 0.33f }), Global.font, "version: " + Global.VERSION,
                    Line.DEFAULT_PADDING * 2f);
            this.setScale(0.75f, 0.75f); // scale down slightly
        }

        /**
         * Updates a specific debug field to the given value which will then be displayed in the debug info. Note that
         * any calls to this will be ignored if the debug info is invisible
         * @param field the field to update; for example, "FPS" or "Render"
         * @param value the new value; for example, "60" or "13 ms"
         */
        public void setField(String field, String value) {
            if (!this.visible) return; // do not update anything if not even visible
            // save width and height before field is set
            float w = this.getWidth();
            float h = this.getHeight();
            if (this.visible) { // only update if visible
                Integer index = this.fields.get(field); // get the line index of the field
                // if new field, add and save index to map
                if (index == null) this.fields.put(field, this.addLine(Global.font, field + ": " + value));
                else this.setLineText(index, field + ": " + value); // otherwise, update text at line
            }
            // if the size has changed and there is a size change callback
            if (this.sizeChangeCallback != null && (this.getWidth() != w || this.getHeight() != h))
                this.sizeChangeCallback.invoke(); // invoke the size change callback
        }

        /**
         * Removes the line corresponding to the given field from the debug info
         * @param field the field whose line should be removed. If no line with the given field exists, the occurrence
         *              will be logged and ignored
         */
        public void removeField(String field) {
            Integer index = this.fields.remove(field); // get the index of the line corresponding to the given field
            if (index == null) // if there is no corresponding line
                Utils.log("Field '" + field + "' cannot be removed as it does not exist", this.getClass(),
                        "removeField", false); // log and ignore
            else { // otherwise
                this.removeLine(index); // remove the corresponding line
                for (String s : this.fields.keySet()) // and for all fields, if their index was greater than given one
                    if (this.fields.get(s) > index) this.fields.put(s, this.fields.get(s) - 1); // lower its index
            }
        }

        /**
         * Specifies a callback to invoke when the size of the debug info changes
         */
        public void useSizeChangeCallback(Callback cb) {
            this.sizeChangeCallback = cb; // save callback as member
        }
    }
}
