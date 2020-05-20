package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * An abstract class to deal with game settings. These settings are saved to and loaded from a settings file stored
 * in the game's data directory
 */
public abstract class Settings {

    /**
     * Enum class listing out all of the settings
     */
    public enum Setting {
        STARTING_WINDOW_WIDTH, STARTING_WINDOW_HEIGHT, V_SYNC
    }

    /**
     * Static Data
     */
    public static final int SCROLL = 99; // a flag used to denote scrolling action as mouse input
    private static final Map<Setting, Object> SETTINGS = new HashMap<>(); // a map form settings to their values

    /**
     * Loads all the game settings from the settings file in the data directory using node loader, with defaults set
     * for all of them for initial startups
     */
    public static void load() {

        /*
         * Load settings information using node loader
         */
        Node data = Node.pathContentsToNode(new Utils.Path("/settings.node", false));
        Map<String, Object> settings = NodeLoader.loadFromNode("settings", data == null ? new Node() : data,
                new NodeLoader.LoadItem[] {
                new NodeLoader.LoadItem<>("starting_window_width", -1, Integer.class)
                    .setLowerBound(600),
                new NodeLoader.LoadItem<>("starting_window_height", -1, Integer.class)
                    .setLowerBound(400),
                new NodeLoader.LoadItem<>("v_sync", true, Boolean.class)
        });

        /*
         * Apply loaded information
         */
        for (String setting : settings.keySet()) // for each setting, put it and its value into the settings map
            Settings.SETTINGS.put(Setting.valueOf(setting.toUpperCase()), settings.get(setting));
        Utils.log("Settings loaded", Settings.class, "load", false); // log
    }

    /**
     * Saves all of the game settings to the settings file in the game's data directory to be loaded again when the game
     * is started up again
     */
    public static void save() {
        // update starting window size for next startup to be the current window size
        Settings.SETTINGS.put(Setting.STARTING_WINDOW_WIDTH, Global.gameWindow.getWidth());
        Settings.SETTINGS.put(Setting.STARTING_WINDOW_HEIGHT, Global.gameWindow.getHeight());
        Node data = new Node("settings"); // create a node to store the settings
        for (Setting setting : Settings.SETTINGS.keySet()) // for each setting, add it as a child to the settings node
            data.addChild(setting.toString().toLowerCase(), Settings.SETTINGS.get(setting).toString());
        Node.nodeToFile(data, new Utils.Path("/settings.node", false)); // save node to settings file
        Utils.log("Settings saved", Settings.class, "save", false); // log
    }

    /**
     * Retrieves a setting's value
     * @param setting the setting whose value to retrieve
     * @return the value of the given setting
     */
    public static Object getSetting(Setting setting) {
        return Settings.SETTINGS.get(setting);
    }

    /**
     * Updates the given setting to the given value
     * @param setting the setting whose value to update
     * @param value the value to update the setting to. If value is not the correct type, the occurrence will be logged
     *              and ignored
     */
    public void updateSetting(Setting setting, Object value) {
        if (value.getClass() != Settings.SETTINGS.get(setting).getClass()) { // check types
            Utils.log("Invalid type '" + value.getClass() + "' for setting '" + setting + "'.", Settings.class,
                    "updateSetting", false); // log if invalid
        } else Settings.SETTINGS.put(setting, value); // update if valid
    }
}
