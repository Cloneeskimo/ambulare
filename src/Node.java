import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Node {

    /*
      Static Data
     */
    private static char DIVIDER_CHAR = ':';
    private static char INDENT_CHAR = '\t';

    /*
      Data
     */
    private List<Node> children; // child nodes
    private String name, value; // name and values (before and after dividing char in file format, respectively)

    /**
     * Constructs this Node by giving it all of its properties
     * @param name the name of the Node (the string before the dividing character in file format)
     * @param value the value of the Node (the string after the dividing node in file format)
     * @param children the starting children for the Node
     */
    public Node(String name, String value, List<Node> children) {
        this.name = name; // set name
        this.value = value; // set value
        this.children = children; // set children
    }

    /**
     * Constructs this Node by giving it just a name and a value
     * @param name the name of the Node (the string before the dividing character in file format)
     * @param value the value of the Node (the string after the dividing node in file format)
     */
    public Node(String name, String value) {
        this(name, value, new ArrayList<>());
    }

    /**
     * Constructs this Node by giving it just a name
     * @param name the name of the Node (the string before the dividing character in file format)
     */
    public Node(String name) {
        this(name, "");
    }

    /**
     * Constructs this Node without any properties set
     */
    public Node() {
        this("");
    }

    /**
     * @return the children of this Node
     */
    public List<Node> getChildren() { return this.children; }

    /**
     * @return the amount of children this Node has
     */
    public int getChildCount() { return this.children.size(); }

    /**
     * @return whether or not this Node has children
     */
    public boolean hasChildren() { return (this.children.size() > 0); }

    /**
     * Adds a child to this Node
     * @param child the child to add
     */
    public void addChild(Node child) {
        this.children.add(child); // add child
    }

    /**
     * Adds a child to this Node
     * @param name the name of the child Node to add
     * @param value the value of the child Node to add
     */
    public void addChild(String name, String value) {
        this.addChild(new Node(name, value)); // create and add child
    }

    /**
     * If this Node has no children, the given children will become its children. Otherwise, the given
     * children will be added one-by-one to this Node's children.
     * @param children the children to consider
     */
    public void setAddChildren(List<Node> children) {
        if (this.children.size() == 0) this.children = children; // if empty, just replace children (quicker)
        else for (Node child : children) this.children.add(child); // otherwise, add one-by-one
    }

    /**
     * Retrieves a child of this Node at the given index.
     * @param index the index of the child to retrieve
     * @return the child at the given index
     */
    public Node getChild(int index) {
        if (index > this.children.size() || index < 0) { // check for invalid index
            IllegalArgumentException e = new IllegalArgumentException("Unable to access index " + index + " in child array of size " + this.children.size()); // create exception if invalid
            Utils.handleException(e, "Node", "getChild(int)", true); // handle exception
        }
        return this.children.get(index); // return appropriate child
    }

    /**
     * Searches for a child of this Node with the given name
     * @param name the name to search for
     * @return the first child with the matching name, or null if there are none
     */
    public Node getChild(String name) {
        for (Node child : this.children) if (child.getName().equals(name)) return child; // look for matching name
        Utils.log("Couldn't find child with name'" + name + "', returning null", "Node", "getChild(String)", false); // log failure
        return null; // return null
    }

    /**
     * Searches for a child of this Node with the given name and, as opposed to getChild, will crash if cannot find
     * @param name the name to search for
     * @return the first child with the matching name
     */
    public Node needChild(String name) {
        for (Node child : this.children) if (child.getName().equals(name)) return child; // look for matching name
        IllegalArgumentException e = new IllegalArgumentException("Unable to find child with name " + name); // create exception if fail
        Utils.handleException(e, "Node", "needChild(String)", true); // handle exception
        return null; // just to make compiler be quiet
    }

    /**
     * @return the name of this Node
     */
    public String getName() { return this.name; }

    /**
     * Updates the name of this Node
     * @param name the new name to assign to this Node
     */
    public void setName(String name) { this.name = name; }

    /**
     * @return the value of this Node
     */
    public String getValue() { return this.value; }

    /**
     * Updates the value of this Node
     * @param value the new value to assign to this Node
     */
    public void setValue(String value) { this.value = value; }

    /**
     * Converts this Node to a String (by returning the same representation that would be found in a file of it)
     * @return the String version of this Node
     */
    public String toString() {
        StringWriter sw = new StringWriter();
        layoutNode(sw, this, new StringBuilder());
        return sw.toString();
    }

    /**
     * Converts a resource to a Node (if the resource is properly formatted)
     * @param resPath the resource-relative path to the data
     * @return the created Node
     */
    public static Node resToNode(String resPath) {
        List<String> data = Utils.resToStringList(resPath); // read resource
        Node node = new Node(); // create root Node
        parseNode(node, data, 0, 0); // parse read data into root Node
        return node; // return parsed node
    }

    /**
     * Converts a file at a given path to a Node (if the file is properly formatted)
     * @param path the path the file is at
     * @param dataDirRelative whether the given path is relative to the data directory
     * @return the created Node
     */
    public static Node fileToNode(String path, boolean dataDirRelative) {
        try { // try to open and read file
            BufferedReader in = new BufferedReader(new FileReader((dataDirRelative ? Utils.getDataDir() + "/": "") + path)); // open file
            List<String> data = new ArrayList<>(); // create empty String lists
            while (in.ready()) data.add(in.readLine()); // read and add file line-by-line
            Node node = new Node(); // create root Node
            parseNode(node, data, 0, 0); // parse read data into root Node
            return node; // return read Node
        } catch (Exception e) { // if exception
            Utils.handleException(e, "Node", "fileToNode(String, boolean)", true); // handle exception
        }
        return null; // to make the compiler be quiet
    }

    /**
     * Converts a given Node to a file to be placed at the given path
     * @param node the Node to convert to a file
     * @param path to path to place the file
     * @param dataDirRelative whether the given path is relative to the data directory
     */
    public static void nodeToFile(Node node, String path, boolean dataDirRelative) {
        try { // try to open and read file
            Utils.ensureDirs(path, dataDirRelative); // ensure directories
            PrintWriter out = new PrintWriter(new File((dataDirRelative ? Utils.getDataDir() + "/": "") + path)); // open
            layoutNode(out, node, new StringBuilder()); // recursively layout node at destination file
            out.close(); // close file
        } catch (Exception e) { // if exception
            Utils.handleException(e, "Node", "nodeToFile(Node, String, boolean)", true); // handle exception
        }
    }

    /**
     * Parses given data into a Node recursively
     * @param curr the root Node
     * @param data the data
     * @param i the position to start at in the data
     * @param in how many indents to expect
     * @return the position in the data after parsing this Node
     */
    private static int parseNode(Node curr, List<String> data, int i, int in) {

        // format next line and find dividing point
        String line = data.get(i); //get next line
        line = line.substring(in); //remove indent
        int dividerLocation = -1; //location of the divider in line
        for (int j = 0; j < line.length() && dividerLocation == -1; j++)
            if (line.charAt(j) == Node.DIVIDER_CHAR) dividerLocation = j; //find divider

        // throw error if no divider found
        if (dividerLocation == -1) { // if no divider found
            IllegalStateException e = new IllegalStateException("Unable to find divider in line " + i + " of given Node data"); // create exception
            Utils.handleException(e, "Node", "parseNode(Node, List<String>, int, int", true); // handle exception
        }

        // create node and set name if there is one
        Node node = new Node(); // create empty node
        String possibleName = line.substring(0, dividerLocation); // get the possible name
        if (!possibleName.equals("")) curr.setName(line.substring(0, dividerLocation)); // create node with name if not empty

        // set node value if there is one
        String possibleValue = line.substring(dividerLocation + 1, line.length()); // get possible value
        if (!possibleValue.equals(" ") && !possibleValue.equals("")) { // if possible value has substance
            curr.setValue(possibleValue.substring(1)); // set value (remove first space)
        }

        // check for more file
        if (i + 1 <= data.size()) { //if not eof
            if (data.get(i + 1).contains("{")) { //if the node has children
                i += 2; //iterate twice
                in++; //iterate indent
                while (!data.get(i).contains("}")) { //while there are more children
                    Node child = new Node(); //create child node
                    i = parseNode(child, data, i, in); //recursively read child, keep track of file position
                    curr.addChild(child); //add child
                    if ((i + 1) > data.size()) { // if unexpected file stop
                        IllegalStateException e = new IllegalStateException("Unexpected file stop at line " + i + " of given Node data"); // create exception
                        Utils.handleException(e, "Node", "parseNode(Node, List<String>, int, int", true); // handle exception
                    }
                    i += 1; // iterate i
                }
            }
        }

        //set node data, return
        node.setName(curr.getName()); // set name
        node.setValue(curr.getValue()); // set value
        node.setAddChildren(curr.getChildren()); // add children
        return i; // return position in data list
    }

    /**
     * Lays out a given Node to a given writer recursively
     * @param out the Write attached to print to
     * @param node the Node to layout
     * @param in how many indents to print
     */
    private static void layoutNode(Writer out, Node node, StringBuilder in) {
        try { // attempt to write to output Writer
            String indentString = in.toString(); // convert indents to a string
            out.write(indentString + node.getName() + Node.DIVIDER_CHAR + " " + node.getValue() + "\n"); // print indent, name, dividing character, and value
            if (node.hasChildren()) { // if the node has children
                out.write(indentString + "{\n"); // print child opening brace
                in.append(Node.INDENT_CHAR); // indent for children
                for (Node child : node.getChildren()) layoutNode(out, child, in); // recursively layout children
                in.deleteCharAt(in.length() - 1); // remove indent used for children
                out.write(indentString + "}\n"); // print child ending brace
            }
        } catch (Exception e) { // if exception
            Utils.handleException(e, "Node", "layoutNode(Writer, Node, StringBuilder)", true); // handle exception
        }
    }
}
