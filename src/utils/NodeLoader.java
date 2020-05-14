package utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * NodeLoader.java
 * Ambulare
 * Jacob Oaks
 * 5/13/20
 */

/**
 * This abstract class provides methods for loading data from nodes in a controlled, standard, configurable, and easily
 * debuggable manner. The purpose of this class is to facilitate loading much of the game's data from nodes and provide
 * extensive stability and standardized feedback through logging whenever issues are encountered.
 */
public abstract class NodeLoader {

    /**
     * Returns a standardized message describing an issue that was encountered when loading a node
     *
     * @param name     the name of the load item or object in general where the issues lies
     * @param object   the broader object that is trying to be loaded (or null if non-applicable)
     * @param issue    a description of issue that was encountered
     * @param ignoring whether or not this issue will be ignored
     * @return a compiled standardized message that should then be logged or turned into an exception
     */
    public static String getMessage(String name, String object, String issue, boolean ignoring) {
        return "Encountered issue while attempting to load '" + name + (object != null ?
                ("' in '" + object + "' node: ") : ("': ")) + issue +
                (ignoring ? ". Ignoring." : "."); // compile and return message
    }

    /**
     * Given a data node and a list of load items, this method will go through each child of the data node and attempt
     * to load each using a corresponding load item. Load items provide extensive configurability in terms of what
     * values are allowed and what data type to convert them to. See utils.NodeLoader.LoadItem for more info. If a child
     * is encountered that does not have a matching load item the occurrence will be logged. As such, it is smart to
     * include load items for children that may not be used right away but are still valid children so as not to
     * confuse readers of the logs into thinking they are providing useless information
     *
     * @param object the name of the object attempting to be loaded from this method, to be used for logging and error-
     *               reporting
     * @param data   the data node to load from. If childreen with the same name appear multiple times, they will all be
     *               parsed but only the one appearing last in the node's children will appear in the final mapping
     * @param li     a list of load items describing how to load each child. If multiple load items with the same name are
     *               in this array, only the first one will ever be used
     * @return a mapping of child names to their data loaded from the node and parsed according to the corresponding
     * load item's restrictions and configuration
     */
    public static Map<String, Object> loadFromNode(String object, Node data, LoadItem[] li) {
        Map<String, Object> loadedNode = new HashMap<>(); // initialize map
        if (data == null) { // if no data was provided
            Utils.log(getMessage(object, null,
                    "Null data node supplied while loading, returning empty map",
                    true), NodeLoader.class, "loadFromNode", false); // log as much
            return loadedNode; // return the empty map
        }
        if (li == null) { // if no load items were provided
            Utils.log(getMessage(object, null,
                    "Null load item list supplied while loading, returning empty map",
                    true), NodeLoader.class, "loadFromNode", false); // log as much
            return loadedNode; // return the empty map
        }
        // tell each load item what the object being loaded is so that they can use it in error reporting and logging
        for (LoadItem item : li) item.setObject(object);
        for (Node child : data.getChildren()) { // for each child of the data node
            boolean matched = false; // maintain a flag representing whether or not it was matched with a load item
            for (LoadItem item : li) { // for each load item
                if (item.matches(child.getName())) { // if the load item matches the child
                    item.parseValue(child); // attempt to parse the data via the load item's configuration
                    matched = true; // flag that a match has been made
                    break; // break from the loop
                }
            }
            // if no match was made, log that an unrecognized child was found
            if (!matched) Utils.log(getMessage(object, null, "Unrecognized child found while loading '" +
                            object + "':\n" + child + "Unexpected", true), NodeLoader.class,
                    "loadFromNode", false);
        }
        // compile the map items by finishing up each load item
        for (LoadItem item : li) loadedNode.put(item.getName(), item.finish());
        return loadedNode; // return the final mapping
    }

    /**
     * This method checks the given node for a (res)from statement in its value and will, if one exists, create a new
     * node at the corresponding path and return it if it exists. If it does not exist, the program will crash. Note
     * that resfrom statements refer to paths that are resource-relative and from statements refer to paths that are
     * relative to the Ambulare data directory. Node also that this method will not check for chains of these statements
     *
     * @param object the name of the object attempting to be loaded from this method, to be used for logging and error-
     *               reporting
     * @param data   the node to check for a (res)from statement in
     * @return the node at the path of the (res)from statement, or the original node if no (res)from statement was found
     */
    public static Node checkForFromStatement(String object, Node data) {
        String value = data.getValue(); // get value
        if (value != null) { // if there is a value
            // check for a from statement
            if (value.length() >= 4 && value.substring(0, 4).toUpperCase().equals("FROM"))
                // update info with node at the given path in the from statement
                data = Node.pathContentsToNode(new Utils.Path(data.getValue().substring(5), false));
                // check for a resfrom statement
            else if (value.length() >= 7 && value.substring(0, 7).toUpperCase().equals("RESFROM"))
                // update info with node at the given path in the from statement
                data = Node.pathContentsToNode(new Utils.Path(data.getValue().substring(8), true));

            if (data == null) // if the new info is null, then throw an exception stating the path is invalid
                Utils.handleException(new Exception(getMessage(object, null, "Invalid (res)from path: '" +
                        value + "'", false)), NodeLoader.class, "checkForFromStatement", true);
        }
        return data; // return final node
    }

    /**
     * Defines settings on how to parse and load a child from a node. See the methods below for descriptions on how each
     * setting is applied. Each method returns the instance of the load item so that these calls can be easily chained
     * together for code space efficiency
     *
     * @param <T> the data type that should result, where the following are acceptable types: Integer, Float, Double,
     *            Boolean, String, and Node
     */
    public static class LoadItem<T> {

        /**
         * Members
         */
        private String name;                // the name of the child node
        private String lname;               // the name of the child node in all lowercase
        private String object;              // the larger object being loaded, used for error-reporting and logging
        private T result;                   // the resulting parsed and tested data
        private T defaultVal;               // the default value of data if none is specified
        private T lb, ub;                   // a lower and upper bound on the node's value (inclusive)
        private T[] allowedValues;          // a list of values that the node's value may take
        private Test<T> test;               // a functional interface test to use on the value to ensure validity
        private Class cl;                   // the resulting data type class. See class definition for acceptable types
        private boolean required;           // whether this child represented by this load item must be provided
        private boolean caseSensitiveName;  // whether the name of the node should be considered case-sensitive
        private boolean caseSensitiveValue; // whether the value of the node should be considered case-sensitive

        /**
         * Constructor
         *
         * @param name       the name of the child node to match with
         * @param defaultVal the default value the resulting data type should take on (may be null)
         * @param cl         the class that described the data, where acceptable types are listed in the class definition
         */
        public LoadItem(String name, T defaultVal, Class cl) {
            this.name = name; // save name as member
            this.lname = name.toLowerCase(); // save a lowercase version of the name for non-case sensitive settings
            this.defaultVal = defaultVal; // save the default data value as member
            this.cl = cl; // save the class as member
        }

        /**
         * Makes the name of the load item case sensitive. In other words, until this is called, this load item will
         * not consider the casing of the names of child nodes when determining if they match with the load item
         *
         * @return this load item
         */
        public LoadItem<T> makeNameSensitive() {
            this.caseSensitiveName = true; // flag that names should be case sensitive
            return this; // return this load item
        }

        /**
         * Makes the value of the load item case sensitive. In other words, until this is called, this load item will
         * convert any values it receives to lower case before attempting to parse them
         *
         * @return this load item
         */
        public LoadItem<T> makeValueSensitive() {
            this.caseSensitiveValue = true; // flag that values should be case sensitive
            return this; // return this load item
        }

        /**
         * Makes this load item's corresponding child node required. If the load item has no resulting data by the time
         * finish() is called and the load item has its required flag set to true, the program will crash
         *
         * @return this load item
         */
        public LoadItem<T> makeRequired() {
            this.required = true; // flag that this load item is required
            return this; // return this load item
        }

        /**
         * Specifies a test, through the form of the functional interface Test, that any values should be passed through
         * before being considered valid. See utils.NodeLoader.LoadItem.Test for more information on how to configure
         * such a test
         *
         * @param test the test to perform
         * @return this load item
         */
        public LoadItem<T> useTest(Test<T> test) {
            this.test = test; // save the test as member
            return this; // return this load item
        }

        /**
         * Sets an inclusive upper bound for allowed values. If the load item's data type is not comparable, the program
         * will crash
         *
         * @param ub the upper bound above which no values should be considered valid
         * @return this load item
         */
        public LoadItem<T> setUpperBound(T ub) {
            if (!(ub instanceof Comparable)) // if the data type is not comparable
                Utils.handleException(new Exception(getMessage(this.name, this.object,
                        "Attempted to set an upper bound on a non-comparable data type", false)),
                        this.getClass(), "setUpperBound", true); // crash the program
            this.ub = ub; // save upper bound as member
            return this; // return this load item
        }

        /**
         * Sets an inclusive lower bound for allowed values. If the load item's data type is not comparable, the program
         * will crash
         *
         * @param lb the lower bound below which no values should be considered valid
         * @return this load item
         */
        public LoadItem<T> setLowerBound(T lb) {
            if (!(lb instanceof Comparable)) // if the data type is not comparable
                Utils.handleException(new Exception(getMessage(this.name, this.object,
                        "Attempted to set an lower bound on a non-comparable data type", false)),
                        this.getClass(), "setLowerBound", true); // crash the program
            this.lb = lb; // save lower bound as member
            return this; // return this load item
        }

        /**
         * Sets inclusive bounds for allow values. If the load item's data type is not comparable, the program will
         * crash
         *
         * @param ub the upper bound above which no values should be considered valid
         * @param lb the lower bound below which no values should be considered valid
         * @return this load item
         */
        public LoadItem<T> setBounds(T ub, T lb) {
            return this.setLowerBound(ub).setLowerBound(lb); // set lower and upper bound separately
        }

        /**
         * Defines a set of allowed values that a value may be when parsed
         *
         * @param allowedValues an array of allowed values whose .equals() will be performed with resulting data before
         *                      being considered valid. If null, occurrence will be logged and ignored
         * @return this load item
         */
        public LoadItem<T> setAllowedValues(T[] allowedValues) {
            if (allowedValues == null) // if the list is null
                Utils.log(getMessage(this.name, this.object, "Null list passed as allowed values list",
                        true), this.getClass(), "setAllowedValues", false); // log and ignore
            this.allowedValues = allowedValues; // save allowed values as member
            return this; // return this load item
        }

        /**
         * Tells the load item which larger object that is being loaded that it is a part of. This is called by
         * loadFromNode() automatically
         *
         * @param object the name of the object attempting to be loaded from this method, to be used for logging and error-
         *               reporting
         */
        public void setObject(String object) {
            this.object = object; // save object as member
        }

        /**
         * Attempts to parse the value of a given node into the load item's resulting data type, using the configuration
         * and restrictions previously set on the load item
         *
         * @param n the node to attempt to parse into the load item
         */
        public void parseValue(Node n) {

            /*
             * Convert data to the load item's data type
             */
            T conv = null; // create a reference to the data converted to the load item's data type
            String val = n.getValue(); // get the value of the node
            if (!this.caseSensitiveValue) val = val.toLowerCase(); // if value not case-sensitive, convert to lowercase
            String type = "value"; // create a string to describe the data type for error-reporting/logging
            try { // try to convert to the correct type
                if (Integer.class == this.cl) { // if the load item represents an integer
                    type = "integer"; // update type to integer
                    conv = (T) (Integer) Integer.parseInt(val); // and try to parse an integer from given node
                } else if (Float.class == this.cl) { // if the load item represents a floating point number
                    type = "floating point number"; // update type to floating point number
                    conv = (T) (Float) Float.parseFloat(val); // and try to parse a floating point number from given node
                } else if (Double.class == this.cl) { // if the load item represents a double
                    type = "double"; // update type to double
                    conv = (T) (Double) Double.parseDouble(val); // and try to parse a double from given node
                } else if (Boolean.class == this.cl) { // if the load item represents a boolean
                    type = "boolean"; // update type to boolean
                    conv = (T) (Boolean) Boolean.parseBoolean(val); // and try to parse a boolean from given node
                } else if (String.class == this.cl) { // if the load item represents a string
                    type = "string"; // update type to string
                    conv = (T) val; // and simply save the node's value
                } else if (Node.class == this.cl) { // if the load item represents a node
                    type = "node"; // update type to node
                    conv = (T) n; // and simply save entire node
                } else // if the data type is none of the above
                    Utils.handleException(new Exception(getMessage(this.name, this.object,
                            "Unsupported data type trying to be parsed into", false)), this.getClass(),
                            "parseValue", true); // crash the program
            } catch (Exception e) { // if an exception occurs during conversion, crash the program
                Utils.handleException(new Exception(getMessage(this.name, this.object, val + " is not a valid " +
                        type, false)), this.getClass(), "parseValue", true);
            }

            /*
             * Check data against allowed values
             */
            if (this.allowedValues != null) { // if a set of allowed values was specified
                boolean allowed = false; // maintain a flag representing whether the value is allowed
                for (int i = 0; i < this.allowedValues.length && !allowed; i++) // loop through allowed values
                    if (conv.equals(this.allowedValues[i])) allowed = true; // if this value is found, its allowed
                if (!allowed) { // if the value is not allowed
                    StringBuilder sb = new StringBuilder(); // create a string builder to list allowed values
                    for (T obj : this.allowedValues) sb.append(obj.toString() + ", "); // add each allowed value
                    sb.delete(sb.length() - 2, sb.length()); // remove last comma and space
                    String msg = getMessage(this.name, this.object, "Non-allowed value: '" + conv +
                            "'. Allowed values are: " + sb.toString(), !this.required); // create error message
                    if (this.required) // if this load item is required
                        Utils.handleException(new Exception(msg), this.getClass(), "parseValue", true); // crash
                        // otherwise just log
                    else Utils.log(msg, this.getClass(), "parseValue", false);
                    return; // return
                }
            }

            /*
             * Check data against upper and lower bounds
             */
            boolean withinBounds = true; // maintain a flag representing whether the data is within bounds
            if (this.lb != null) // if there is a lower bound
                if (((Comparable) conv).compareTo(this.lb) < 0) withinBounds = false; // make sure value is >= to it
            if (this.ub != null && withinBounds) // if there is an upper bound
                if (((Comparable) conv).compareTo(this.ub) > 0) withinBounds = false; // make sure value is <= to it
            if (!withinBounds) { // if value is out of bounds
                String msg = "Value out of bounds: " + conv + ". Must be a " + type + " "; // create error message
                if (this.lb != null) msg += "no less than '" + lb + "'"; // if lower bound, include in message
                if (this.ub != null) { // if there is an upper bound
                    if (this.lb != null) msg += " and"; // and a lower bound, add and "and"
                    msg += " no greater than '" + this.ub + "'"; // include upper bound in message
                }
                msg = getMessage(this.name, this.object, msg, !this.required); // get standardized error message
                // if this load item is required, crash
                if (this.required) Utils.handleException(new Exception(msg), this.getClass(), "parseValue", true);
                    // otherwise just log
                else Utils.log(msg, this.getClass(), "parseValue", false);
                return; // and return
            }

            /*
             * Check data against provided test
             */
            if (this.test != null) { // if there is a provided test
                StringBuilder msg = new StringBuilder(); // create a string builder for error message
                if (!this.test.test(conv, msg)) { // if the test returns false
                    // get an error message based on the string builder
                    String m = getMessage(this.name, this.object, msg.toString(), !this.required);
                    if (this.required) // if this load item is required,
                        Utils.handleException(new Exception(m), this.getClass(), "parseValue", true); // crash
                        // otherwise just log
                    else Utils.log(m, this.getClass(), "parseValue", false);
                    return; // and return
                }
            }
            this.result = conv; // if all checks were passed, save result
        }

        /**
         * Checks if the load item's name matches with the given name, taking case sensitivity into account
         *
         * @param name the name to check with
         * @return whether the load item's name matches with the given name
         */
        public boolean matches(String name) {
            if (this.caseSensitiveName) return this.name.equals(name); // if case sensitive, directly check
            else return this.lname.equals(name.toLowerCase()); // otherwise check in all lower case
        }

        /**
         * @return the name of the load item taking case sensitivity into account
         */
        public String getName() {
            if (this.caseSensitiveName) return this.name; // if case sensitive, return original name
            else return this.lname; // otherwise return lowercase name
        }

        /**
         * Performs a check when loading has finished to see if the load item is required and, if so, was given a value.
         * If required and no value was given, the program will crash
         *
         * @return the final result of the load item, or the default value if no valid result was loaded
         */
        public T finish() {
            if (this.result == null) { // if no result exists
                if (this.required) // if this load item is required
                    Utils.handleException(new Exception(getMessage(this.name, this.object, "Must be provided",
                            false)), this.getClass(), "finish", true); // crash
                // use default value if there is one
                if (this.defaultVal != null) this.result = this.defaultVal;
            }
            return this.result; // return result
        }

        /**
         * An interface defining an additional specific test to make against a value given to a load item to check for
         * its validity
         *
         * @param <T> the data type of the corresponding load item
         */
        @FunctionalInterface
        public interface Test<T> {

            /**
             * Performs the test
             *
             * @param v   the value to test
             * @param msg if the test fails, this string builder should be populated, using .append(), with a message
             *            describing why the test was failed
             * @return whether the test was passed
             */
            boolean test(T v, StringBuilder msg);
        }
    }
}