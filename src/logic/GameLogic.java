package logic;

import gameobject.HUD;
import gameobject.TextObject;
import gameobject.World;
import graphics.Font;
import graphics.Window;

/**
 * Lays out and abstracts away many lower-level details and capabilities that a game logic should have. Notably:
 *  - has a World to render GameObjects that react to a Camera
 *  - has a HUD to rendered over the World to render GameObjects that do not react to a Camera
 *  - flags to enable/disable rendering the World and/or the HUD
 *  - ability to use custom ShaderPrograms and render according to them
 */
public abstract class GameLogic {

    /**
     * Data
     */
    protected Font font; // font that can be used for TextObjects
    protected HUD hud; // used for rendering GameObjects over the World which do not react to a Camera
    protected World world; // used for rendering GameObjects which react to a Camera
    protected boolean renderWorld = true, renderHUD = true; // flags to be set by extending classes to enable/disable rendering for the world and HUD, respectively
    private boolean FPSItemsAdded = false; // flag representing whether the FPS HUD items have been added. It is possible
                                           // they are not added if extending classes override initOtherItems() without calling super
                                           // so this is necessary to ensure that no text updates are called on TextObjects
                                           // that do not exist

    /**
     * Initializes this GameLogic. This method is the only entry point into the GameLogic and it is not able to be
     * Extending classes cannot override this method. However, this method will call initOthers() which can be
     * overridden by extending classes and should be used for additional initialization
     * @param window the window
     */
    public final void init(Window window) {
        float ar = (float)window.getWidth() / (float)window.getHeight(); // calculate aspect ratio
        boolean arAction = (ar < 1.0f); // if ar < 1.0f (height > width) then we will make objects shorter to compensate
        this.font = new Font("/font.png", "/font_info.txt"); // create Font
        this.hud = new HUD(ar, arAction); // create HUD
        this.world = new World(window.getHandle(), ar, arAction); // create World
        this.initOthers(window); // allow extending classes to add GameObjects
    }

    /**
     * Extending classes should initialize any GameObjects to be placed in the World/HUD or any other members here
     * In order for FPS reporting to still occur in the HUD, extending classes should call super.initOthers()
     * @param window the Window in use
     */
    protected void initOthers(Window window) {
        TextObject FPSStatic = new TextObject(font, "FPS: "); // create FPS counter static text
        TextObject FPSCount = new TextObject(font, "N/A"); // create actual FPS text
        FPSStatic.setScale(0.1f); // scale FPS counter static text
        FPSCount.setScale(0.1f); // scale actual FPS text
        FPSStatic.setVisibility(false); // invisible to start
        FPSCount.setVisibility(false); // invisible to start
        this.hud.addObject(FPSStatic, new HUD.HUDPositionSettings(-1f, 1f, true, 0.02f)); // add static FPS text to HUD
        this.hud.addObject(FPSCount, new HUD.HUDPositionSettings(FPSStatic, FPSStatic, 1f, 0f, 0f)); // add actual FPS text to HUD
        this.FPSItemsAdded = true; // save that these FPS items have been added
    }

    /**
     * Extending classes should override this to use the window reference to respond to any input they so desire to
     * respond to
     * @param window the Window in use
     */
    public void input(Window window) {}

    /**
     * Updates this GameLogic by updating the World and the HUD
     * Extending classes can certainly override this but unless updated in the overriding method (or super.update()
     * is called), this GameLogic's World and HUD will no longer be updated
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        this.world.update(interval); // update World
        this.hud.update(interval); // update HUD
    }

    /**
     * Renders this GameLogic's World, then this GameObject's HUD, and then will render anything else that extending
     * classes wish to render by calling renderOthers()
     * Extending classes cannot override this method. If extending classes wish to NOT render the World, the HUD, or
     * both, they can toggle the renderWorld and renderHUD flags and provide their own rendering process in
     * renderOthers()
     */
    public final void render() {
        if (this.renderWorld) this.world.render(); // render World if flag is set to true
        if (this.renderHUD) this.hud.render(); // render HUD if flag is set to true
        this.renderOthers(); // allow extending class to render
    }

    /**
     * Extending classes should override this method if they desire to render in other procedures beside the default
     * World and HUD rendering
     */
    protected void renderOthers() {}

    /**
     * Reacts to the window resizing by calculating the new aspect ratio and aspect ratio action and then notifying this
     * GameLogic's World and HUD
     * Extending classes cannot override this method
     * @param w the new window width
     * @param h the new window height
     */
    public final void resized(int w, int h) {
        float ar = (float)w / (float)h; // calculate aspect ratio
        boolean arAction = (ar < 1.0f); // if ar < 1.0f (height > width) then we will make objects shorter to compensate
        this.world.resized(ar, arAction); // notify World of resize
        this.hud.resized(ar, arAction); // notify HUD of resize
    }

    /**
     * This is called by the GameEngine when there is an FPS to report while recording FPS, or when FPS reporting has
     * been toggled off
     * Extending classes can override this to change GameLogic behavior when FPS is reported, but functionality of the
     * default FPS displaying HUD TextObjects will no longer work correctly if super.reportFPS is not called
     * @param FPS the FPS to report, or null if FPS reporting is toggled off
     */
    public void reportFPS(Float FPS) {
        if (this.FPSItemsAdded) { // only modify the FPS displaying items if they were actually added
            if (FPS == null) { // if FPS reporting toggled off
                this.hud.getObject(0).setVisibility(false); // hide FPS static text
                this.hud.getObject(1).setVisibility(false); // hide FPS counter text
            } else { // otherwise
                this.hud.getObject(0).setVisibility(true); // show FPS static text
                this.hud.getObject(1).setVisibility(true); // show FPS counter text
                ((TextObject)this.hud.getObject(1)).setText(Float.toString(FPS)); // update text with new FPS
                this.hud.ensurePlacement(1); // ensure text placement
            }
        }
    }

    /**
     * Cleans up this GameLogic
     * Extending classes should override this to cleanup any additional members they need to do, but they should
     * always call super.cleanup() to clean up the base GameLogic members as well
     */
    public void cleanup() {
        this.world.cleanup(); // cleanup World
        this.hud.cleanup(); // cleanup HUD
    }
}
