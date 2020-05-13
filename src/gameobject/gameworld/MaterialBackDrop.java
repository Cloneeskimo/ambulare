package gameobject.gameworld;

import graphics.*;
import utils.Global;
import utils.Node;
import utils.Utils;

/*
 * BlockBackDrop.java
 * Ambulare
 * Jacob Oaks
 * 5/12/2020
 */

/*
 * MaterialBackDrop.java
 * Ambulare
 * Jacob Oaks
 * 5/12/2020
 */

/**
 * Material backdrops render a single material behind the area as a backdrop. If this material is textured, texture
 * coordinates will automatically be updated to make sure its aspect ratio stays correct when display. Additionally,
 * textured material backdrops can optionally scroll. If scrolling is enabled, a specific scale can be applied to how
 * much of the view is showed. Material backdrops are loaded through node-files but will not render until a camera
 * reference is provided to them via useCam(). For more information on how to format a material backdrop node, see the
 * constructor
 */
public class MaterialBackDrop implements Area.BackDrop {

    /**
     * Members
     */
    private Model mod;              // the model to render the material onto, fitting the window perfectly
    private Material mat;           // the material to use for rendering the backdrop
    private Camera cam;             // a reference to the camera whose position will be followed by the backdrop
    private float viewScale = 1f;   /* how much of the material's texture's smaller component (width or height) to
                                       display if scrolling is enabled and the material is textured */
    private float texAr;            // the aspect ratio of the material's texture if it is textured
    private int bmw, bmh;           // the width and height of the area's block map
    private boolean scrolls = true; // whether or not the backdrop scrolls if it is textured
    private boolean textured;       // a flag denoting if the material is textured

    /**
     * Constructs the material backdrop by compiling information from a given node. If the value of the root node starts
     * with the statements 'from' or 'resfrom', the next statement will be assumed to be a different path at which to
     * find the material backdrop node-file. This is useful for reusing the same material backdrop in many settings.
     * 'from' assumes the following path is relative to the Ambulare data folder (in the user's home folder) while
     * 'resfrom' assumes the following path is relative to the Ambulares's resource path. Note that these kinds of
     * statements cannot be chained together. A material backdrop node can have the following children:
     * <p>
     * material [optional][default: white]: a block info node defining the material to use for the backdrop material.
     * This node should be formatted as a proper block info node. This material cannot be animated. See
     * gameobject.gameworld.Block.BlockInfo for more info on how to format a block info node
     * <p>
     * scrolls [optional][default: true]: a flag specifying whether the background should scroll if textured
     * <p>
     * view_scale [optional][default: 1f]: how much of the material's texture's smaller component (width or height) to
     * display if scrolling is enabled (1f if disabled) and the material is textured. This value must be within 0.1f and
     * 1f
     * <p>
     * Note that, if any of the info above is improperly formatted, a message saying as much will be logged. As
     * such, when designing material backdrops to be loaded into the game, the logs should be checked often to make sure
     * the loading process is unfolding correctly
     *
     * @param info the node to use to construct the material backdrop
     * @param bmw  the width of the area's block map
     * @param bmh  the height of the area's block map
     */
    public MaterialBackDrop(Node info, int bmw, int bmh) {

        // load from elsewhere if from or resfrom statement used
        String value = info.getValue(); // get value
        if (value != null) { // if there is a value
            // check for a from statement
            if (value.length() >= 4 && value.substring(0, 4).toUpperCase().equals("FROM"))
                // update info with node at the given path in the from statement
                info = Node.fileToNode(info.getValue().substring(5), true);
                // check for a resfrom statement
            else if (value.length() >= 7 && value.substring(0, 7).toUpperCase().equals("RESFROM"))
                // update info with node at the given path in the from statement
                info = Node.resToNode(info.getValue().substring(8));
            if (info == null) // if the new info is null
                Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("(res)from statement",
                        "MaterialBackDrop", "invalid path in (res)from statement: " + value,
                        false)), "gameobject.gameworld.MaterialBackDrop", "MaterialBackDrop(Node, int, int)",
                        true); // throw an exception stating the path is invalid
        }

        // load information from node
        try {
            for (Node c : info.getChildren()) { // loop through children
                String n = c.getName().toLowerCase(); // get child name in lowercase
                if (n.equals("material")) { // material
                    Block.BlockInfo bi = new Block.BlockInfo(c); // create block info from node to use to make material
                    Texture t = bi.texPaths.size() > 0 ? new Texture(bi.texPaths.get((int) (Math.random() *
                            bi.texPaths.size())), bi.texResPath) : null; // create texture if the material is textured
                    this.mat = new Material(t, bi.color, bi.bm); // create material from block info
                } else if (n.equals("scrolls")) { // scrolls flag
                    this.scrolls = Boolean.parseBoolean(c.getValue()); // convert to boolean flag
                } else if (n.equals("view_scale")) { // view scale
                    try {
                        this.viewScale = Float.parseFloat(c.getValue()); // try to convert to a float
                    } catch (Exception e) { // if conversion was unsuccessful
                        Utils.log(Utils.getImproperFormatErrorLine("view_scale", "MaterialBackDrop",
                                "must be a proper floating pointer number between 0.1f and 1f", true),
                                "gameobject.gameworld.MaterialBackDrop", "MaterialBackDrop(Node, int, int)",
                                false); // log as much
                    }
                    if (this.viewScale < 0.1f || this.viewScale >= 1f) { // if not in correct range, log as much
                        Utils.log(Utils.getImproperFormatErrorLine("view_scale", "MaterialBackDrop",
                                "must be a proper floating pointer number between 0.1f and 1f", true),
                                "gameobject.gameworld.MaterialBackDrop", "MaterialBackDrop(Node, int, int)",
                                false); // log as much
                        this.viewScale = 1f; // and reset to default
                    }
                } else // if an unrecognized child appears
                    Utils.log("Unrecognized child given for material backdrop info:\n" + c + "Ignoring.",
                            "gameobject.gameworld.MaterialBackDrop", "MaterialBackDrop(Node, int, int)",
                            false); // log and ignore
            }
        } catch (Exception e) { // if any strange exceptions occur
            Utils.handleException(new Exception(Utils.getImproperFormatErrorLine("MaterialBackDrop",
                    "MaterialBackDrop", e.getMessage(), false)),
                    "gameobject.gameworld.MaterialBackDrop", "MaterialBackDrop(Node, int, int)", true); // log and crash
        }

        // finalize loading process
        if (this.mat == null) // if no material was specified
            this.mat = new Material(Global.getThemeColor(Global.ThemeColor.WHITE)); // use a white background
        this.bmw = bmw; // save block map width as member
        this.bmh = bmh; // save block map height as member
        this.mod = Model.getStdGridRect(2, 2); // create model
        this.resized(); // fit model to window
        this.textured = this.mat.isTextured(); // save textured flag
        if (this.textured) // if the material is textured, calculate its aspect ratio
            this.texAr = (float) this.mat.getTexture().getWidth() / (float) this.mat.getTexture().getHeight();
    }

    /**
     * Saves a reference to the camera to use. Material backdrops will not render before this is called with a valid
     * camera
     *
     * @param cam the camera used for rendering the area
     */
    public void useCam(Camera cam) {
        this.cam = cam; // save camera reference as member
        this.updateTexCoords(); // calculate starting texture coordinates
    }

    /**
     * Responds to window resizing by resizing the model to fit the window and re-calculating texture coordinates
     */
    public void resized() {
        // resize model to fit the window
        mod.setScale(2f * (Global.ar > 1f ? Global.ar : 1), 2f / (Global.ar < 1f ? Global.ar : 1));
        this.updateTexCoords(); // re-calculate texture coordinate
    }

    /**
     * Renderss the material back drop using the given world shader program
     *
     * @param sp the world shader program
     */
    public void render(ShaderProgram sp) {
        if (this.cam == null) { // if an attempt to render was made before a camera was given, log
            Utils.log("Attempted to render material back drop without providing a camera first",
                    "gameobject.gameworld.MaterialBackDrop", "render(ShaderProgram)", false);
            return; // and return without rendering
        }
        if (this.scrolls) this.updateTexCoords(); // if this scrolls, updating the texture coordinates
        sp.setUniform("camZoom", 1f); // temporarily set camera zoom to 1f to show entire backdrop
        sp.setUniform("useLights", 0); // do not use individual lights for background
        // set position offset to camera's position
        sp.setUniform("x", this.cam.getX());
        sp.setUniform("y", this.cam.getY());
        this.mat.setUniforms(sp); // set material uniforms
        this.mod.render(); // render the model
        sp.setUniform("useLights", 1); // turn light usage back on
        sp.setUniform("camZoom", this.cam.getZoom()); // and reset the zoom to camera's actual zoom
    }

    /**
     * Update's the models texture coordinates if the material backdrop is textured, based on the view scale and the
     * scrolling flag
     */
    private void updateTexCoords() {
        if (!this.textured) return; // if not textured, return
        if (this.cam == null) return; // if no camera has been given, return

        // calculate how far the camera is in proportion to the area width and height
        float xProp = this.scrolls ? Math.max(0f, Math.min(1f, this.cam.getX() / (float) this.bmw)) : 0.5f;
        float yProp = this.scrolls ? Math.max(0f, Math.min(1f, 1f - this.cam.getY() / (float) this.bmh)) : 0.5f;

        // calculate the width and height of the view depending on the ratio of aspect ratios
        float viewWidth = 1f, viewHeight = 1f;
        if (Global.ar > texAr) { // screen is wider than backdrop in proportion to height
            viewHeight = texAr / Global.ar; // apply ratio of ratios on view height
        } else { // backdrop is wider than screen in proportion to height
            viewWidth = Global.ar / texAr; // apply ratio of ratios on view width
        }

        // apply view scale
        viewWidth *= this.viewScale;
        viewHeight *= this.viewScale;

        // calculate final texture coordinates
        float lx = xProp * (1 - viewWidth); // calculate left x tex coordinate
        float ly = yProp * (1 - viewHeight); // calculate lower y tex coordinate
        this.mod.useTexCoords(new float[]{ // compile info into finalized texture coordinate float array
                lx, ly, // top left
                lx, ly + viewHeight, // bottom left
                lx + viewWidth, ly + viewHeight, // bottom right
                lx + viewWidth, ly // top right
        });
    }

    /**
     * Cleans up the material backdrop by cleaning up its material and model
     */
    @Override
    public void cleanup() {
        this.mat.cleanup(); // cleanup material
        this.mod.cleanup(); // cleanup model
    }
}
