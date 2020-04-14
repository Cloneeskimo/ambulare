import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Provides various utility methods to be used throughout the program
 */
public class Utils {

    /**
     * Static Data
     */
    public static final String LOG_DIRECTORY = "data/logs/"; // location where logs will be stored

    /**
     * Ensures that a directory exists
     * @param directory the directory to ensure
     */
    public static void ensureDirectory(String directory) {
        File dir = new File(directory); // create file object
        boolean outcome = dir.mkdirs(); // attempt to create necessary directories
        if (!outcome) Utils.log("Unable to create directories at '" + directory + "', assuming they exist",
                "Utils", "ensureDirectory", false); // log if exists
    }

    /**
     * Loads a resource into a single String
     * @param resourcePath the resource-relative path of the file
     * @return the loaded resource as a string
     */
    public static String loadResourceIntoString(String resourcePath) {
        String result = ""; // create empty string
        try (InputStream in = Class.forName(Utils.class.getName()).getResourceAsStream(resourcePath); // try to open resource
             Scanner scanner = new Scanner(in, "UTF-8")) { // try to then use a Scanner on it
            result = scanner.useDelimiter("\\A").next(); // read results into single string
        } catch (Exception e) { // if exception
            Utils.log(e, "Utils", "loadResourceIntoString"); // log
            e.printStackTrace(); // print
        }
        return result; // return string read from resource
    }

    /**
     * Loads a resource into a list of strings
     * @param resourcePath the resource-relative path of the file
     * @return the loaded resource as a list of strings
     */
    public static List<String> loadResourceIntoStringList(String resourcePath) {
        List<String> file = new ArrayList<>(); // create empty ArrayList
        try (BufferedReader in = new BufferedReader(new InputStreamReader(Class.forName(Utils.class.getName())
                .getResourceAsStream(resourcePath)))) { // attempt to open resource
            String line; // variable for each line
            while ((line = in.readLine()) != null) file.add(line); // read each line until eof
        } catch (Exception e) { // if exception
            Utils.log(e, "Utils", "loadResourceIntoStringList"); // log
            e.printStackTrace(); // display
        }
        return file; // return list of strings from resource
    }

    /**
     * Logs an exception
     * @param e the exception to log
     * @param src the source code file from which the event originates
     * @param method the method from which the event originates
     * @return the exception given as a parameter - this helps make errors a one-liner
     */
    public static Exception log(Exception e, String src, String method) {
        log(e.getMessage(), src, method, true); // log using exception message
        return e;
    }

    /**
     * Logs an event
     * @param info the info string to be logged
     * @param src the source code file from which the event originates
     * @param method the method from which the event originates
     * @param toFile whether or not to log to file
     */
    public static void log(String info, String src, String method, boolean toFile) {

        //get and format log line intro
        String lli = getLogLineIntro(false, src, method); // get log line intro
        int infoLength = info.length(); // get length of info message
        if (infoLength > 0) { // if it is longer than 0
            if (info.charAt(infoLength - 1) == '\n') { // if there is a newline at the end
                info = info.substring(0, infoLength - 1); // remove it
            }
        }

        //print event
        System.out.println(lli + info); // print to console
        if (toFile) { // print to file if set

            //get appropriate log file name and attempt to open corresponding file
            String fileName = getLogFileName(); // get file name
            PrintWriter out; // create PrintWriter
            try {
                out = new PrintWriter(new FileOutputStream(new File(fileName), true)); // try to open file

                // print and close
                out.println(lli + info); // print
                out.close(); // close

            //catch file not found exception
            } catch (Exception e) {
                e.printStackTrace(); // just print exception
            }
        }
    }

    /**
     * Creates an appropriate log file name/directory based on the day
     * @return the appropriate file name/directory for a log file at the given date and time
     */
    private static String getLogFileName() {
        Utils.ensureDirectory(LOG_DIRECTORY); // ensure the log directory exists
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yy"); // format the date
        return LOG_DIRECTORY + "log " + dateFormatter.format(LocalDateTime.now()) + ".txt"; // create and return the appropriate log file name
    }

    /**
     * Creates an appropriate log line intro for something to be logged
     * @param exception whether the corresponding event is an exception or not
     * @param src the source code file from which the event originates
     * @param method the method from which the event originates
     * @return the built log line into
     */
    private static String getLogLineIntro(boolean exception, String src, String method) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); // create date/time formatter
        return "[" + dtf.format(LocalDateTime.now()) + "][" + (exception ? "EXCEPTION" : "INFO") +
                "][" + src + "][" + method + "]: "; // compile important info into one line
    }
}