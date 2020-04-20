package logic;

import gameobject.HUD;
import gameobject.TextObject;
import gameobject.World;
import graphics.Font;
import graphics.Window;
import utils.Coord;
import utils.Global;
import utils.Transformation;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Lays out and abstracts away many lower-level details and capabilities that a game logic should take care of. Notably:
 *  - has a world to render game objects that react to a camera
 *  - has a robust and customizable HUD to render game objects that don't react to a camera over the world
 *  - flags to enable/disable rendering of the world and/or the HUD
 *  - ability to use custom shader programs and render using them
 */
public abstract class GameLogic {

    /**
     * Data
     */
    protected HUD hud; // used for rendering game objects over the world that do not react to a camera
    protected World world; // used for rendering game objects that react to a Camera
    protected boolean renderWorld = true, renderHUD = true; /* flags to be set by extending classes to enable/disable
                                                               rendering for the world and HUD, respectively */
    private boolean FPSItemsAdded = false; /* flag representing whether the FPS HUD items have been added. It is
                                              possible they are not added if extending classes override initOtherItems()
                                              without calling super so this is necessary to ensure that no text updates
                                              are called on TextObjects that do not exist */

    /**
     * Initializes the logic. This method is the only entry point into the logic other then input, update, and render
     * Extending classes cannot override this method. However, this method will call initOthers() which can be
     * overridden by extending classes and should be used for additional initialization
     * @param window the window
     */
    public final void init(Window window) {
        float ar = (float)window.getWidth() / (float)window.getHeight(); // calculate aspect ratio
        boolean arAction = (ar < 1.0f); /* this stores what actions need to be done to compensate for aspect ratio. If
                                            ar < 1.0f (height > width) then we will make objects shorter to compensate
                                            and if ar > 1.0f, the opposite is true */
        this.hud = new HUD(ar, arAction); // create HUD
        this.world = new World(window.getHandle(), ar, arAction); // create world
        this.initOthers(window); // allow extending classes to initialize other members
    }

    /**
     * Extending classes should initialize any game objects to be placed in the World/HUD or any additional members here
     * In order for FPS reporting to still occur in the HUD, extending classes should call super.initOthers()
     * @param window the window
     */
    protected void initOthers(Window window) {
        TextObject FPSStatic = new TextObject(Global.FONT, "FPS: "); /* we separate the part of the text that
                                                                             doesn't change so that we don't reinvent
                                                                             the wheel when updating the FPS text */
        TextObject FPSCount = new TextObject(Global.FONT, ""); // create actual FPS text object
        FPSStatic.setScale(0.1f); // scale FPS counter static text
        FPSCount.setScale(0.1f); // scale actual FPS text
        FPSStatic.setVisibility(false); // invisible to start
        FPSCount.setVisibility(false); // invisible to start
        this.hud.addObject(FPSStatic, new HUD.HUDPositionSettings(-1f, 1f, true,
                0.02f)); // add static FPS text to HUD
        this.hud.addObject(FPSCount, new HUD.HUDPositionSettings(FPSStatic, FPSStatic, 1f, 0f,
                0f)); // add actual FPS text to HUD
        this.FPSItemsAdded = true; // save that these FPS items have been added
    }

    /**
     * Receives mouse input from the engine and notifies the world and the HUD of the input.
     * Extending classes can certainly override this method to change how they react to mouse input. If super is not
     * called, or the world and HUD are not manually notified, they may not respond to mouse input
     * @param x the normalized and projected x position of the mouse if hover event, 0 otherwise
     * @param y the normalized and projected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     */
    public void mouseInput(float x, float y, int action) {
        this.world.mouseInput(x, y, action); // notify world of input
        this.hud.mouseInput(x, y, action); // notify HUD of input
    }

    /**
     * Extending classes should override this and use the window reference to respond to any input they so desire to
     * respond to
     * @param window the window
     */
    public void input(Window window) {}

    /**
     * Updates this logic by updating the world and the HUD
     * Extending classes can certainly override this but unless updated in the overriding method (or super.update()
     * is called), the world and HUD will no longer be updated
     * @param interval the amount of time to account for
     */
    public void update(float interval) {
        this.world.update(interval); // update world
        this.hud.update(interval); // update HUD
    }

    /**
     * Renders this logic's World, then this logic's HUD, and then will render anything else that extending classes wish
     * to render by calling renderOthers()
     * Extending classes cannot override this method. If extending classes wish to NOT render the World, the HUD, or
     * both, they can toggle the renderWorld and renderHUD flags and provide their own rendering process in
     * renderOthers()
     */
    public final void render() {
        if (this.renderWorld) this.world.render(); // render world if flag is set to true
        if (this.renderHUD) this.hud.render(); // render HUD if flag is set to true
        this.renderOthers(); // allow extending class to render
    }

    /**
     * Extending classes should override this method if they desire to render in other procedures beside the default
     * world and HUD rendering
     */
    protected void renderOthers() {}

    /**
     * Reacts to the window resizing by calculating the new aspect ratio and aspect ratio action (see GameLogic.init)
     * and then notifying this logic's world and HUD of the resize
     * Extending classes cannot override this method
     * @param w the new window width
     * @param h the new window height
     */
    public final void resized(int w, int h) {
        float ar = (float)w / (float)h; // calculate aspect ratio
        boolean arAction = (ar < 1.0f); // calculate aspect ratio action (see GameLogic.init)
        this.world.resized(ar, arAction); // notify world of resize
        this.hud.resized(ar, arAction); // notify HUD of resize
    }

    /**
     * This is called by the engine when there is an FPS to report while recording FPS, or when FPS reporting has been
     * toggled off
     * Extending classes can override this to change logic behavior when FPS is reported, but functionality of the
     * default FPS displaying HUD text objects will no longer work correctly if super.reportFPS() is not called
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
     * Cleans up this logic
     * Extending classes should override this to cleanup any additional members they need to do, but they should ALWAYS
     * call super.cleanup() to clean up the base logic members as well
     */
    public void cleanup() {
        this.world.cleanup(); // cleanup World
        this.hud.cleanup(); // cleanup HUD
    }
}
