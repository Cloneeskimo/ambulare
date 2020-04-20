package utils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Provides various utility methods that are used throughout the program
 */
public class Utils {

    /**
     * Generates a random float within the given range
     * @param min the minimum the random float can be
     * @param max the maximum the random float can be
     * @return the generated random float
     */
    public static float genRandFloat(float min, float max) { return (float)(min + Math.random() * (max - min)); }

    public static Coord rotatePoint(float cx, float cy, float x, float y, float rot) {
        double dx = x - cx, dy = y - cy; // calculate distance from center point (un-translate)
        double rdx = (dx * Math.cos(rot) - dy * Math.sin(rot)); // rotate the x component of the point in question
        double rdy = (dy * Math.cos(rot) + dx * Math.sin(rot)); // rotate the y component of the point in question
        double rx = cx + rdx; // re-translate rotated x
        double ry = cy + rdy; // re-translate rotated y
        return new Coord((float)rx, (float)ry); // put into a coordinate and return
    }

    /**
     * Converts a string to a float array containing color data. A properly formatted string has all four components
     * separated by a space and each component is a proper float value. example: "1f 0.5f 0.1f 1f"
     * @param colorData the string to convert
     * @return the converted float array with four color components ([r, g, b, and a]) or null if the string was not
     * properly formatted
     */
    public static float[] strToColor(String colorData) {
        String[] components = colorData.split(" "); // split by spaces
        if (components.length != 4) return null; // if incorrect length, return null
        float[] color = new float[4]; // create float array
        for (int i = 0; i < color.length; i++) { // loop through each string
            try { color[i] = Float.parseFloat(components[i]); } // parse each as a float
            catch (Exception e) { // if exception
                Utils.handleException(e, "utils.Utils", "strToColor(String)", false); // log exception but don't crash
                return null; // and then return null
            }
        }
        return color; // return color
    }

    /**
     * @return the directory of the folder where all game data should be stored
     */
    public static String getDataDir() {
        return System.getProperty("user.home") + "/Ambulare"; /* user home plus Ambulare folder. usually the user.home
                                                                 ends up being the folder containing the documents
                                                                 folder */
    }

    /**
     * Ensures that a directory exists. This will not consider anything after the last slash to avoid turning
     * what should be normal files into directories themselves (unless there are no slashes)
     * @param directory the directory to ensure
     * @param dataDirRelative whether the given directory is relative to the data directory (see getDataDir())
     */
    public static void ensureDirs(String directory, boolean dataDirRelative) {
        for (int i = directory.length() - 1; i >= 0; i--) { // remove anything after the last slash
            if (directory.charAt(i) == '/') { // starting at the end, if we see a slash
                directory = directory.substring(0, i); // remove everything after the slash
                break; // break from loop - only remove stuff after last slash
            }
        }
        File dir = new File((dataDirRelative ? getDataDir() : "") + directory); // create file object
        boolean outcome = dir.mkdirs(); // attempt to create necessary directories
   }

    /**
     * Loads a resource into a single string
     * @param resPath the resource-relative path of the file
     * @return the loaded resource as a string
     */
    public static String resToString(String resPath) {
        String result = ""; // create empty string
        try (InputStream in = Class.forName(Utils.class.getName()).getResourceAsStream(resPath); // try to open resource
             Scanner scanner = new Scanner(in, "UTF-8")) { // try to then use a Scanner to read it
            result = scanner.useDelimiter("\\A").next(); // read results into single string
        } catch (Exception e) { // if exception
            handleException(e, "utils.Utils", "resToString(String)", true); // handle exception
        }
        return result; // return string read from resource
    }

    /**
     * Loads a resource into a list of strings
     * @param resPath the resource-relative path of the file
     * @return the loaded resource as a list of strings
     */
    public static List<String> resToStringList(String resPath) {
        List<String> file = new ArrayList<>(); // create empty ArrayList
        try (BufferedReader in = new BufferedReader(new InputStreamReader(Class.forName(Utils.class.getName())
                .getResourceAsStream(resPath)))) { // attempt to open resource
            String line; // variable for each line
            while ((line = in.readLine()) != null) file.add(line); // read each line until eof
        } catch (Exception e) { // if exception
            handleException(e, "utils.Utils", "resToStringList(String)", true); // handle exception
        }
        return file; // return list of strings from resource
    }

    /**
     * Generalizes exception handling from anywhere in the program. Logs the exception and exits if fatal
     * This is probably looked down upon amongst Java convention purists, but it essentially does the same thing that
     * a throw would do anyways, but with more customization and while logging the info in an easy to find place and
     * format
     * @param e the exception
     * @param src the source file from which the exception originates
     * @param method the method from which the exception originates
     * @param fatal whether to exit the program after handling the exception
     */
    public static void handleException(Exception e, String src, String method, boolean fatal) {
        StringWriter sw = new StringWriter(); // create StringWriter to store exception stack trace
        e.printStackTrace(new PrintWriter(sw)); // print stack trace to StringWriter
        log(e.getMessage() + "\n" + sw, src, method, fatal); // log exception message and stack trace
        if (fatal) System.exit(-1); // if fatal, exit the program with error code -1
    }

    /**
     * Logs an event
     * @param info the info string to be logged
     * @param src the source code file from which the event originates
     * @param method the method from which the event originates
     * @param fatal whether or not the program is going to exit after handling the event. Note that setting this to
     *              true does not quit the program. The method calling this method needs to quit after logging
     */
    public static void log(String info, String src, String method, boolean fatal) {

        //get and format log line intro
        String lli = getLogLineIntro(fatal, src, method); // get log line intro
        int infoLength = info.length(); // get length of info message
        if (infoLength > 0) { // if it is longer than 0
            if (info.charAt(infoLength - 1) == '\n') { // if there is a newline at the end
                info = info.substring(0, infoLength - 1); // remove it
            }
        }

        //print to console and attempt to print to log file
        System.out.println(lli + info); // print to console
        String fileName = getLogFileName(); // get name of log file
        ensureDirs(fileName, false); // make sure appropriate directories exist
        PrintWriter out; // create PrintWriter to write to log file
        try {
            out = new PrintWriter(new FileOutputStream(new File(fileName), true)); // try to open file
            out.println(lli + info); // print
            out.close(); // close
        } catch (Exception e) { // if exception
            e.printStackTrace(); /* just print corresponding exception and hope for the best because attempting to
                                    handle this exception using Utils.handleException() would likely cause an infinite
                                    loop */
        }
    }

    /**
     * Creates an appropriate log line intro for an event to be logged
     * @param fatal whether the corresponding event is fatal or not
     * @param src the source code file from which the event originates
     * @param method the method from which the event originates
     * @return the created log line into
     */
    private static String getLogLineIntro(boolean fatal, String src, String method) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); // create date/time formatter
        return "[" + dtf.format(LocalDateTime.now()) + "][" + (fatal ? "FATAL" : "INFO") +
                "][" + src + "][" + method + "]: "; // compile important info into one line and return
    }

    /**
     * Creates an appropriate log file name/directory based on the date
     * @return the appropriate file name/directory for a log file at the given date and time
     */
    private static String getLogFileName() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yy"); // format the date
        return getDataDir() + "/logs/ " + dtf.format(LocalDateTime.now()) + ".txt"; // create and return log file name
    }
}