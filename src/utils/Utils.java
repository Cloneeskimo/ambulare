package utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL32;

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
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

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
     * Describes a path in the file system using a string representing the path and a flag representing its relativity.
     * For the most part, all paths that are passed along through the program are either 1) resource-relative, meaning
     * they are relative to the java program's resources directory, or 2) relative to the data directory which is a
     * directory outside of the program that is established in getDataDir()
     */
    public static class Path {

        /**
         * Members
         */
        private String path;         // the string representing the path
        private boolean resRelative; // whether the path is resource-relative (true) or relative to the data dir (false)

        /**
         * Constructs the path using the given path and resource-relativity flag
         *
         * @param path        the string representing the path
         * @param resRelative whether the path is resource-relative (true) or relative to the data dir (false)
         */
        public Path(String path, boolean resRelative) {
            this.path = path; // save path as member
            this.resRelative = resRelative; // save resource-relativity flag as member
        }

        /**
         * Constructs the path using the given node
         *
         * @param n the node to construct the path from where the name of the node represents the relativity and the
         *          value is the relative path. If the name contains the string (non-case sensitive) "res", the path
         *          will be considered resource-relative. Otherwise, it will be considered to be relative to the data
         *          directory
         */
        public Path(Node n) {
            this.path = n.getValue(); // save path as value of node
            this.resRelative = n.getName().toLowerCase().contains("res"); // determine relativity as described above
        }

        /**
         * Adds the given string to the path's string representation and returns the result as a new path
         *
         * @param s the string to add to the path's string representation
         * @return the resulting path
         */
        public Path add(String s) {
            return new Path(this.path + s, this.resRelative); // add given string and return as new path
        }

        /**
         * @return the file corresponding to the path or null if the path is resource-relative
         */
        public File getFile() {
            if (this.resRelative) return null; // if resource-relative, return null
            return new File(Utils.getDataDir() + this.path); // otherwise create and return file
        }

        /**
         * @return the input stream corresponding to the path
         */
        public InputStream getStream() {
            // if relative to the data directory, return null
            try { // try to get the resource as a stream and return it
                if (!this.resRelative) return new FileInputStream(this.getFile());
                return Class.forName(Utils.class.getName()).getResourceAsStream(this.path);
            } catch (Exception e) { // if an exception occurs
                Utils.handleException(new Exception("Unable to get path '" + this.path + "' input stream for reason: "
                        + e.getMessage()), this.getClass(), "getStream()", true); // crash
            }
            return null;
        }

        /**
         * @return whether the file at the path exists
         */
        public boolean exists() {
            // if resource relative, try to create a stream and return whether successful
            if (this.resRelative) return (this.getStream() != null);
                // if relative to the data directory, create file and return whether it exists
            else return (this.getFile().exists());
        }

        /**
         * @return this path's path string
         */
        public String getPath() {
            return this.path;
        }

        /**
         * @return whether the path is resource-relative
         */
        public boolean isResRelative() {
            return this.resRelative;
        }

        /**
         * Converts the path to a node by formatting it the same way that the path constructor that takes a node would
         * except
         *
         * @return the path converted to a node
         */
        public Node toNode() {
            return new Node(this.resRelative ? "resource_path" : "path", this.path); // convert to node and return
        }

        /**
         * Converts the path to a string representing the path's absolute path
         *
         * @return the string representing the path's absolute path
         */
        @Override
        public String toString() {
            String base = this.resRelative ? "<res>" : getDataDir(); // get base
            return base + this.path; // add path and return
        }

        /**
         * Checks the path for equality with another path by checking the relativity and string path representations
         * of each
         *
         * @param obj the other object to check for equality with
         * @return whether the given path is considered equal to the one
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Path) { // if the given object is a path, check its relativity and string path rep.
                return (this.resRelative == ((Path) obj).resRelative && this.path.equals(((Path) obj).path));
            } else return false; // otherwise return inequal
        }
    }

    /**
     * Opens the given path in the native operating system's file explorer if it exists. This will not work for
     * resource-relative paths and any attempts will be logged
     *
     * @param path the path to open
     */
    public static void openNativeFileExplorer(Path path) {
        // if the path is resource-relative, log
        if (path.isResRelative()) Utils.log("Attempted to open native file explorer at a resource-relative path: "
                + path, Utils.class, "openNativeFileExplorer", true);
        File file = path.getFile(); // get the file at the path
        if (file.exists()) { // if the path exists
            try { // try to open a native file explorer at that path
                String os = System.getProperty("os.name").toLowerCase(); // get the operating system
                if (os.contains("win")) { // if the operating system is windows
                    Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler",
                            file.getAbsolutePath()}); // open using windows
                } else if (os.contains("nux") || os.contains("nix") || os.contains("mac")) // if linux or mac
                    Runtime.getRuntime().exec(new String[]{"/usr/bin/open", file.getAbsolutePath()}); // open with linux
            } catch (Exception e) { // if an exception occurs, log and ignore
                Utils.log("Unable to open path: " + path + " in native file explorer for reason: " +
                        e.getMessage(), Utils.class, "openNativeFileExplorer", false);
            }
        } else // if the file doesn't exist
            Utils.log("Unable to open path: " + path + " in native file explorer for reason: non-existent",
                    Utils.class, "openNativeFileExplorer", false); // log and ignore
    }

    /**
     * @return the directory of the folder where all game data should be stored as an absolute path. This won't include
     * a slash at the end
     */
    public static String getDataDir() {
        // use user.home plus Ambulare folder. Usually user.home is the folder containing the documents folder
        return System.getProperty("user.home") + "/Ambulare";
    }

    /**
     * Ensures that a directory exists. This will not consider anything after the last slash to avoid turning
     * what should be normal files into directories themselves (unless there are no slashes)
     *
     * @param path the path to ensure directories at. If the path is resource-relative, the program will crash
     */
    public static void ensureDirs(Path path) {
        if (path.resRelative) Utils.handleException(new Exception("Attempted to ensure directories in a resource-" +
                "relative path: " + path), Utils.class, "ensureDirs", true); // if resource-relative path, crash
        String p = path.getPath(); // get the path
        for (int i = p.length() - 1; i >= 0; i--) { // loop through directory starting at the end
            if (p.charAt(i) == '/') { // if we see a slash
                p = p.substring(0, i); // remove everything after the slash
                break; // break from loop - only remove stuff after last slash
            }
        }
        File dir = new File(getDataDir() + p); // make a file at the directory
        dir.mkdirs(); // attempt to create necessary directories
    }

    /**
     * Converts the contents of a file at the given path to a string
     *
     * @param path the path
     * @return the file contents converted to a string
     */
    public static String pathContentsToString(Path path) {
        Scanner s = new Scanner(path.getStream(), "UTF-8"); // create a stream
        return s.useDelimiter("\\A").next(); // compile contents to a string and return
    }

    /**
     * Converts the contents of a file at the given path to a string list
     *
     * @param path the path
     * @return the file contents converted to a string list
     */
    public static List<String> pathContentsToStringList(Path path) {
        List<String> file = new ArrayList<>(); // create empty ArrayList
        try {
            // create buffered reader from path stream
            BufferedReader in = new BufferedReader(new InputStreamReader(path.getStream()));
            String line; // maintain a variable holding each line
            while ((line = in.readLine()) != null) file.add(line); // read each line until eof
        } catch (Exception e) { // if an exception occurs
            Utils.handleException(new Exception("Encountered a problem while reading contents of file at path: " +
                    path + ": " + e.getMessage()), Utils.class, "pathContentsToStringList", true); // crash
        }
        return file; // return resulting list of stings
    }

    /**
     * Converts the contents of a file at the given path to a byte buffer
     *
     * @param path       the path
     * @param bufferSize the initial size of the buffer (will be increased if necessary)
     * @return the file contents converted to a byte buffer
     */
    public static ByteBuffer pathContentsToByteBuffer(Path path, int bufferSize) {
        InputStream is = path.getStream(); // get stream
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
                    path + "' for reason: " + e.getMessage()), Utils.class, "fileToByteBuffer", true); // crash
        }
        buffer.flip(); // flip the result
        return buffer; // and return the final buffer
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
     * Creates a frame buffer object and a texture attachment with the given width and height
     *
     * @param w the width to give the texture attachment
     * @param h the height to give the texture attachment
     * @return a length two integer array where [0] is the FBO ID and [1] is the texture attachment's texture ID
     */
    public static int[] createFBOWithTextureAttachment(int w, int h) {

        // create frame buffer object to draw textures to
        int fboID = glGenFramebuffers(); // generate frame buffer object
        glBindFramebuffer(GL_FRAMEBUFFER, fboID); // bind the frame buffer object
        glDrawBuffer(GL_COLOR_ATTACHMENT0); // enable drawing in color attachment zero

        // create texture attachment for the frame buffer object
        int texID = glGenTextures(); // generate texture
        glBindTexture(GL_TEXTURE_2D, texID); // bind texture
        // create an empty texture with the given size
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        // these parameters make the pixels of the texture crystal clear
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        GL32.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, texID, 0); // attach texture to FBO
        glBindTexture(GL_TEXTURE_2D, 0); // unbind texture

        // return the fbo and the texture attachment
        return new int[]{fboID, texID};
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
                Utils.handleException(e, Utils.class, "strToColor", false); // log exception but don't crash
                return null; // and return null
            }
        }
        return color; // return color
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
    public static void handleException(Exception e, Class src, String method, boolean fatal) {
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
    public static void log(String info, Class src, String method, boolean fatal) {

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
        Path lfp = getLogFilePath(); // get path to the log file to use
        ensureDirs(lfp); // make sure appropriate directories exist
        PrintWriter out;
        try {
            out = new PrintWriter(new FileOutputStream(lfp.getFile(), true)); // try to open file
            out.println(lli + info);
            out.close();
        } catch (Exception e) {
            e.printStackTrace(); /* just print corresponding exception and hope for the best because attempting to
                handle this exception using Utils.handleException() would likely cause an infinite loop */
        }
    }

    /**
     * Creates an appropriate log line intro for an event to be logged
     *
     * @param fatal  whether the corresponding event is fatal or not
     * @param src    the source code file from which the event originates
     * @param method the method from which the event originates
     * @return the created log line into
     */
    private static String getLogLineIntro(boolean fatal, Class src, String method) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); // create date/time formatter
        return "[" + dtf.format(LocalDateTime.now()) + "][" + (fatal ? "FATAL" : "INFO") +
                "][" + src.toString().substring(6) + "][" + method + "]: "; // compile important info and return
    }

    /**
     * @return an appropriate life file path based on the date
     */
    private static Path getLogFilePath() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yy"); // create date/time formatter
        // create and return path using date
        return new Path("/logs/ " + dtf.format(LocalDateTime.now()) + ".txt", false);
    }
}