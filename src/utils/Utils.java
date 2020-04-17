package utils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;

/**
 * Provides various utility methods to be used throughout the program
 */
public class Utils {

    /**
     * Returns the directory of the data folder where all user data should be stored
     * @return the aforementioned data directory
     */
    public static String getDataDir() {
        return System.getProperty("user.home") + "/Game"; // user home plus game folder
    }

    /**
     * Ensures that a directory exists. This will remove anything after the last slash to avoid turning
     * what should be normal files into directories themselves (unless there are no slashes)
     * @param directory the directory to ensure
     * @param dataDirRelative whether the given directory is relative to the data directory
     */
    public static void ensureDirs(String directory, boolean dataDirRelative) {
        for (int i = directory.length() - 1; i >= 0; i--) { // remove anything after the last slash
            if (directory.charAt(i) == '/') { // starting at the end, if we see a slash
                directory = directory.substring(0, i); // remove everything after the slash
                break; // break from loop
            }
        }
        File dir = new File((dataDirRelative ? getDataDir() : "") + directory); // create file object
        boolean outcome = dir.mkdirs(); // attempt to create necessary directories
   }

    /**
     * Loads a resource into a single String
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
     * @param fatal whether or not the program is going to exit after handling the event
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
        String fileName = getLogFileName(); // get file name
        ensureDirs(fileName, false); // make sure appropriate directories exist
        PrintWriter out; // create PrintWriter
        try {
            out = new PrintWriter(new FileOutputStream(new File(fileName), true)); // try to open file
            out.println(lli + info); // print
            out.close(); // close

        //if can't log to file
        } catch (Exception e) {
            e.printStackTrace(); // just print corresponding exception and hope for the best
        }
    }

    /**
     * Creates an appropriate log line intro for something to be logged
     * @param fatal whether the corresponding event is fatal or not
     * @param src the source code file from which the event originates
     * @param method the method from which the event originates
     * @return the built log line into
     */
    private static String getLogLineIntro(boolean fatal, String src, String method) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); // create date/time formatter
        return "[" + dtf.format(LocalDateTime.now()) + "][" + (fatal ? "FATAL" : "INFO") +
                "][" + src + "][" + method + "]: "; // compile important info into one line
    }

    /**
     * Creates an appropriate log file name/directory based on the day
     * @return the appropriate file name/directory for a log file at the given date and time
     */
    private static String getLogFileName() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yy"); // format the date
        return getDataDir() + "/logs/ " + dtf.format(LocalDateTime.now()) + ".txt"; // create and return the appropriate log file name
    }
}