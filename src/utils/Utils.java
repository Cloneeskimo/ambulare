package utils;

import org.lwjgl.BufferUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.lwjgl.BufferUtils.createByteBuffer;

/*
 * Utils.java
 * Ambulare
 * Jacob Oaks
 * 4/15/20
 */

/**
 * Provides various utility methods that are used throughout the program, mostly relating to logging/error-handling and
 * file I/O
 */
public class Utils {

    /**
     * Opens the given path in the native operating system's file explorer if it exists. This will not work for
     * resource-relative paths
     *
     * @param filePath the path to open
     */
    public static void openNativeFileExplorer(String filePath) {
        File file = new File(filePath); // create a file at that path
        if (file.exists()) { // if the path exists
            try { // try to open a native file explorer at that path
                String os = System.getProperty("os.name").toLowerCase(); // get the operating system
                if (os.contains("win")) { // if the operating system is windows
                    Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler",
                            file.getAbsolutePath()}); // open using windows
                } else if (os.contains("nux") || os.contains("nix") || os.contains("mac")) // if linux or mac
                    Runtime.getRuntime().exec(new String[]{"/usr/bin/open", file.getAbsolutePath()}); // open with linux
            } catch (Exception e) { // if an exception occurs, log and ignoree
                Utils.log("Unable to open path: " + filePath + " in native file explorer for reason: " +
                        e.getMessage(), "utils.Utils", "openNativeFileExplorer(String)", false);
            }
        } else // if the file doesn't exist
            Utils.log("Unable to open path: " + file + " in native file exploreer for reason: non-existent",
                    "utils.Utils", "openNativeFileExplorer(String)", false); // log and ignore
    }

    /**
     * Determines if a file with the given path exists
     *
     * @param path    the path to check
     * @param resPath whether the path is resource-relative or not. If not, the data directory will not get
     *                automatically prepended to the path
     * @return whether the path exists or not
     */
    public static boolean fileExists(String path, boolean resPath) {
        if (resPath) { // if resource-relative path
            try { // try to open the resource as a stream. If it's null, the file doesn't exist
                return (Class.forName(Utils.class.getName()).getResourceAsStream(path) != null);
            } catch (Exception e) {
                Utils.handleException(e, "utils.Utils", "fileXists(String, boolean)", true);
            }
        } else { // if not resource-relative
            File file = new File(path); // create the corresponding file
            return file.exists(); // and check if it exists
        }
        return false;
    }

    /**
     * Parses the the given path to separate its directory, name, and extension
     *
     * @param resPath whether the file is resource-relative or not
     * @param file    the path of the file
     * @return a length three array containing [0] the directory (including the last '/') (or null if not in a
     * directory), [1] the name of the file excluding the slash and extension, [2] the extension of the file
     * including the '.' (or null if no extension), [3] 'true' if the file is resource-relative or 'false' otherwise
     */
    public static String[] getFileInfoForPath(boolean resPath, String file) {
        String dir = null, name = file, ext = null;
        for (int i = file.length() - 1; i >= 0; i--) { // loop through file starting at the end
            if (file.charAt(i) == '.') { // if we see a dot
                ext = file.substring(i); // save extension
                name = file = file.substring(0, i);
            } else if (file.charAt(i) == '/') { // if we see a slash
                dir = file.substring(0, i + 1); // save directory
                name = file.substring(i + 1); // save name
                break; // break from loop - don't need to continue anymore
            }
        }
        return new String[]{dir, name, ext, resPath ? "true" : "false"};
    }

    /**
     * @return the directory of the folder where all game data should be stored. This won't include a slash at the end
     */
    public static String getDataDir() {
        // use user.home plus Ambulare folder. Usually user.home is the folder containing the documents folder
        return System.getProperty("user.home") + "/Ambulare";
    }

    /**
     * Ensures that a directory exists. This will not consider anything after the last slash to avoid turning
     * what should be normal files into directories themselves (unless there are no slashes)
     *
     * @param directory       the directory to ensure
     * @param dataDirRelative whether the given directory is relative to the data directory (see getDataDir())
     */
    public static void ensureDirs(String directory, boolean dataDirRelative) {
        for (int i = directory.length() - 1; i >= 0; i--) { // loop through directory starting at the end
            if (directory.charAt(i) == '/') { // if we see a slash
                directory = directory.substring(0, i); // remove everything after the slash
                break; // break from loop - only remove stuff after last slash
            }
        }
        File dir = new File((dataDirRelative ? getDataDir() : "") + directory); // create file object
        dir.mkdirs(); // attempt to create necessary directories
    }

    /**
     * Loads a resource into a single string
     *
     * @param resPath the resource-relative path of the file
     * @return the loaded resource as a string
     */
    public static String resToString(String resPath) {
        String result = "";
        try (InputStream in = Class.forName(Utils.class.getName()).getResourceAsStream(resPath); // try to open resource
             Scanner scanner = new Scanner(in, "UTF-8")) { // try to then use a scanner to read it
            result = scanner.useDelimiter("\\A").next(); // read results into single string
        } catch (Exception e) {
            handleException(e, "utils.Utils", "resToString(String)", true);
        }
        return result;
    }

    /**
     * Loads a resource into a list of strings
     *
     * @param resPath the resource-relative path of the file
     * @return the loaded resource as a list of strings
     */
    public static List<String> resToStringList(String resPath) {
        List<String> file = new ArrayList<>(); // create empty ArrayList
        try (BufferedReader in = new BufferedReader(new InputStreamReader(Class.forName(Utils.class.getName())
                .getResourceAsStream(resPath)))) { // attempt to open resource
            String line; // variable to store a single line
            while ((line = in.readLine()) != null) file.add(line); // read each line until eof
        } catch (Exception e) { // if there is an exception
            handleException(new Exception("Could not load resource: " + resPath + " for reason: " + e.getMessage()),
                    "utils.Utils", "resToStringList(String)", true); // handle it
        }
        return file;
    }

    /**
     * Converts a file to a byte buffer
     *
     * @param path        the path to the file to convert
     * @param resRelative whether the path is resource-relative or not
     * @param bufferSize  the initial buffer size to use
     * @return the byte buffer containing the file data
     */
    public static ByteBuffer fileToByteBuffer(String path, boolean resRelative, int bufferSize) {
        InputStream is = null; // create null input stream
        if (resRelative) is = Utils.class.getResourceAsStream(path); // for resources, get resource as stream
        else { // for non-resources
            try { // try to convert the corresponding file to an input stream
                is = new FileInputStream(new File(path));
            } catch (Exception e) { // if an exception occurs while trying to convert file to an input stream
                Utils.handleException(new Exception("Could not convert file at path: '" + path + "' to a file input" +
                                "stream for reason: " + e.getMessage()), "utils.Utils",
                        "fileToByteBuffer(String, boolean, int)", true); // crash the program
            }
        }
        if (is == null) { // if the input stream is null
            Utils.handleException(new Exception("Could not convert load file at " + (resRelative ? "resource-relative "
                            : "") + "path: '" + path + " - no such file exists."), "utils.Utils",
                    "fileToByteBuffer(String, boolean, int)", true); // crash the program

        }
        ReadableByteChannel rbc = Channels.newChannel(is); // create a readable byte channel using the input stream
        ByteBuffer buffer = createByteBuffer(bufferSize); // create a buffer with the given starting size
        try { // try to read bytes from the RBC into the byte buffer
            while (true) { // look until break
                int bytes = rbc.read(buffer); // read bytes
                if (bytes == -1) break; // if EOF, break from loop
                // if the buffer is full, resize it
                if (buffer.remaining() == 0) buffer = resizeBuffer(buffer, buffer.capacity() * 2);
            }
        } catch (Exception e) { // if an exception occurs
            Utils.handleException(new Exception("Unable to read from readable byte channel from file at path: '" +
                            path + "' for reason: " + e.getMessage()), "utils.Utils", "fileToByteBuffer(String, boolean, int)",
                    true); // crash the program
        }
        buffer.flip(); // flip the result
        return buffer; // and return the final buffer
    }

    /**
     * Generates a random float within the given range
     *
     * @param min the minimum the random float can be
     * @param max the maximum the random float can be
     * @return the generated random float
     */
    public static float genRandFloat(float min, float max) {
        return (float) (min + Math.random() * (max - min));
    }

    /**
     * Rotates a given point around a given center point
     *
     * @param cx the center point's x
     * @param cy the center point's y
     * @param x  the x of the point to rotate
     * @param y  the y of the point to rotate
     * @param r  the amount to rotate (in radians)
     * @return the rotated point as a pair
     */
    public static Pair rotatePoint(float cx, float cy, float x, float y, float r) {
        double dx = x - cx, dy = y - cy; // calculate distance from center point (un-translate)
        double rdx = (dx * Math.cos(r) - dy * Math.sin(r)); // rotate the x component of the point in question
        double rdy = (dy * Math.cos(r) + dx * Math.sin(r)); // rotate the y component of the point in question
        double rx = cx + rdx; // re-translate rotated x
        double ry = cy + rdy; // re-translate rotated y
        return new Pair((float) rx, (float) ry); // put into a coordinate and return
    }

    /**
     * Converts a string to a float array containing color data. A properly formatted string has all four components
     * separated by a space and each component is a proper float value. Example: "1f 0.5f 0.1f 1f"
     *
     * @param colorData the string to convert
     * @return the converted float array with four color components ([r, g, b, and a]) or null if the string was not
     * properly formatted
     */
    public static float[] strToColor(String colorData) {
        String[] components = colorData.split(" "); // split by spaces
        if (components.length != 4) return null; // if incorrect length, return null
        float[] color = new float[4]; // create array for color components
        for (int i = 0; i < color.length; i++) { // loop through each string
            try {
                color[i] = Float.parseFloat(components[i]);
            } // parse each as a float
            catch (Exception e) {
                Utils.handleException(e, "utils.Utils", "strToColor(String)", false); // log exception but don't crash
                return null; // and return null
            }
        }
        return color; // return color
    }

    /**
     * Resizes a byte buffer to the new given capacity
     *
     * @param buffer      the buffer to resize
     * @param newCapacity the new size to give the buffer
     * @return the resized buffer
     */
    public static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity); // create new byte buffer with given capacity
        buffer.flip(); // flip original buffer
        newBuffer.put(buffer); // put original buffer data into new buffer data
        return newBuffer; // return new buffer
    }

    /**
     * Generalizes exception handling from anywhere in the program. Logs the exception and exits if fatal
     * This is probably looked down upon amongst Java convention purists, but it essentially does the same thing that
     * a throw would do anyways, but with more customization and while logging the info in an easy to find place and
     * format
     *
     * @param e      the exception
     * @param src    the source file from which the exception originates
     * @param method the method from which the exception originates
     * @param fatal  whether to exit the program after handling the exception
     */
    public static void handleException(Exception e, String src, String method, boolean fatal) {
        StringWriter sw = new StringWriter(); // create StringWriter to store exception stack trace
        e.printStackTrace(new PrintWriter(sw)); // print stack trace to StringWriter
        log(e.getMessage() + "\n" + sw, src, method, fatal); // log exception message and stack trace
        if (fatal) System.exit(-1); // if fatal, exit the program with error code -1
    }

    /**
     * Logs an event
     *
     * @param info   the info string to be logged
     * @param src    the source code file from which the event originates
     * @param method the method from which the event originates
     * @param fatal  whether or not the program is going to exit after handling the event. Note that setting this to
     *               true does not quit the program. The method calling this method needs to quit after logging
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
        System.out.println(lli + info);
        String fileName = getLogFileName(); // get name of log file
        ensureDirs(fileName, false); // make sure appropriate directories exist
        PrintWriter out;
        try {
            out = new PrintWriter(new FileOutputStream(new File(fileName), true)); // try to open file
            out.println(lli + info);
            out.close();
        } catch (Exception e) {
            e.printStackTrace(); /* just print corresponding exception and hope for the best because attempting to
                handle this exception using Utils.handleException() would likely cause an infinite loop */
        }
    }

    /**
     * Returns an error line to describe improper formatting of something
     *
     * @param object   the improperly formatted object
     * @param loading  what is trying to be created/loaded using said formatting
     * @param message  what is wrong with the formatting
     * @param ignoring whether the error will be ignored
     * @return a compiled and standardized string describing the issue
     */
    public static String getImproperFormatErrorLine(String object, String loading, String message, boolean ignoring) {
        String msg = "Improperly formatted " + object + " found while loading " + loading + ": " + message
                + (ignoring ? ". Ignoring" : ""); // compile string
        return msg; // and return
    }

    /**
     * Creates an appropriate log line intro for an event to be logged
     *
     * @param fatal  whether the corresponding event is fatal or not
     * @param src    the source code file from which the event originates
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
     *
     * @return the appropriate file name/directory for a log file at the given date and time
     */
    private static String getLogFileName() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yy"); // create date/time formatter
        return getDataDir() + "/logs/ " + dtf.format(LocalDateTime.now()) + ".txt"; // compile file name and return
    }
}