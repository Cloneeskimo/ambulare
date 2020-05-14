package gameobject.gameworld;

import graphics.*;
import utils.Global;
import utils.Node;
import utils.NodeLoader;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * Constructs the material backdrop by compiling information from a given node. Material backdrops can use (res)from
     * statements. See utils.NodeLoader for more information on (res)from statements. A material backdrop node can have
     * the following children:
     * <p>
     * - texture_paths [optional][default: no texture]: specifies what paths to look for textures at. This node
     * itself should have one or more children nodes formatted as path nodes. If more than one texture path is
     * specified, a random one will be chosen when the material backdrop is created. See utils.Utils.Path for more
     * information on path nodes
     * <p>
     * - color [optional][default: 1f 1f 1f 1f]: specifies what color to use for the material backdrop
     * <p>
     * - blend_mode [optional][default: none]: specifies how to blend color and texture. The options are: (1) 'none' -
     * no blending will occur. The appearance will simple be the texture if there is one, or the color if there is no
     * texture. (2) 'multiplicative' - the components of the color and the components of the texture will be multiplied
     * to create a final color. (3) 'averaged' - the components of the color and the components of the texture will be
     * averaged to create a final color
     * <p>
     * scrolls [optional][default: true]: a flag specifying whether the background should scroll if textured
     * <p>
     * view_scale [optional][default: 1f][0.01f, 1f]: how much of the material's texture's smaller component (width or
     * height) to display if scrolling is enabled (1f if disabled) and the material is textured
     *
     * @param data the node to use to construct the material backdrop
     * @param bmw  the width of the area's block map
     * @param bmh  the height of the area's block map
     */
    public MaterialBackDrop(Node data, int bmw, int bmh) {

        /*
         * Load material backdrop information using node loader
         */
        List<Utils.Path> texturePaths = new ArrayList<>();
        data = NodeLoader.checkForFromStatement("MaterialBackDrop", data);
        Map<String, Object> materialBackDrop = NodeLoader.loadFromNode("MaterialBackDrop", data,
                new NodeLoader.LoadItem[]{
                        new NodeLoader.LoadItem<>("texture_paths", null, Node.class)
                                .useTest((v, sb) -> {
                            boolean issue = false;
                            for (Node child : ((Node) v).getChildren()) {
                                Utils.Path p = new Utils.Path(child);
                                if (!p.exists()) {
                                    sb.append("Texture at path does not exist: '").append(p).append('\n');
                                    issue = true;
                                } else texturePaths.add(p);
                            }
                            return !issue;
                        }),
                        new NodeLoader.LoadItem<>("color", "1f 1f 1f 1f", String.class)
                                .useTest((v, sb) -> {
                            float[] c = Utils.strToColor(v);
                            if (c == null) {
                                sb.append("Must be four valid rgba float values separated by a space");
                                sb.append("\nFor example: '1f 0f 1f 0.5' for a half-transparent purple");
                                return false;
                            }
                            return true;
                        }),
                        new NodeLoader.LoadItem<>("blend_mode", "none", String.class)
                                .setAllowedValues(new String[]{"none", "multiplicative", "averaged"}),
                        new NodeLoader.LoadItem<>("scrolls", true, Boolean.class),
                        new NodeLoader.LoadItem<>("view_scale", 1f, Float.class)
                                .setLowerBound(0.01f).setUpperBound(1f)
                });


        /*
         * Apply loaded information
         */
        String colorData = (String) materialBackDrop.get("color"); // get color data
        // create the color to use for the material
        float[] color = colorData == null ? Global.getThemeColor(Global.ThemeColor.WHITE) : Utils.strToColor(colorData);
        Texture t = texturePaths.size() > 0 ? new Texture(texturePaths.get((int) (Math.random() * texturePaths.size())))
                : null; // create texture to use for the material
        // create the material to use for the backdrop
        this.mat = new Material(t, color, Material.BlendMode.valueOf(((String) materialBackDrop.get("blend_mode"))
                .toUpperCase()));
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
        if (this.cam == null) { // if an attempt to render was made before a camera was given
            Utils.log("Attempted to render material back drop without providing a camera first", this.getClass(),
                    "render", false); // log and ignore
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
