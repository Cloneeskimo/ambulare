package logic;

import gameobject.GameObject;
import gameobject.ROC;
import gameobject.ui.TextButton;
import graphics.*;
import utils.Global;
import utils.MouseInputEngine;
import utils.PhysicsEngine;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

/*
 * MainMenuLogic.java
 * Ambulare
 * Jacob Oaks
 * 4/26/20
 */

/**
 * Dictates the logic the game will follow when the user is at the main menu. The main menu logic is broken up into
 * a series of phases which allow clean animation to occur. Different updates occur based on the current phase. See the
 * update method for more information. The title's physics is simulated without using the physics engine since it is
 * very simple and unique to this situation
 */
public class MainMenuLogic extends GameLogic {

    /**
     * Members
     */
    private Material[] bMats;  // store the main menu button's materials for changing opacity
    private Window window;     // store reference to window to close when exit button is pressed
    private GameObject title;  // title object before added to ROC
    private ShaderProgram sp;  // shader program to render title before adding to ROC
    private float titleV;      // the velocity of the title before added to ROC
    private float time;        // a generic timer used for timing within each phase of the animation
    private int phase;         // the phase of the main menu animation

    /**
     * Initializes the window game object and begins the intro animation if the program was just started up
     *
     * @param window the window
     */
    @Override
    public void initOthers(Window window) {
        this.window = window; // save window reference
        float[] titleModelCoords = new float[]{ // create model coordinates for title game object
                -1.62f, -.305f, // bottom left
                -1.62f, .305f, // top left
                1.62f, .305f, // top right
                1.62f, -.305f  // bottom right
        };
        title = new GameObject(new Model(titleModelCoords, Model.getStdRectTexCoords(), Model.getStdRectIdx()),
                new Material(new Texture("/textures/ui/title.png", true))); // create title object
        title.setScale(0.55f, 0.55f); // scale title down by about one half
        // if the logic has no transfer data, then the program just starting up - so go through startup animation
        if (this.transferData == null) {
            initSP(); // initialize the shader program
            title.setPos(0f, 2f + (1f / Global.ar)); // move title to be up and out of view
            this.renderROC = false; // turn off ROC rendering for now
        } else { // if the program didn't just start up, skip the beginning animation
            // add title to ROC
            this.roc.addStaticObject(title, new ROC.PositionSettings(0f, 0.5f, true, 0f));
            this.phase = 4; // skip to phase 4 -> button creation and fading
            this.roc.fadeIn(new float[]{0f, 0f, 0f, 0f}, 0.5f); // fade in ROC
        }
    }

    /**
     * Initializes the shader program used to render the title before it is added to the ROC
     */
    private void initSP() {
        // create the shader program using hud shaders
        sp = new ShaderProgram("/shaders/hud_vertex.glsl", "/shaders/hud_fragment.glsl");
        sp.registerUniform("ar"); // register aspect ratio uniform
        sp.registerUniform("arAction"); // register aspect ratio action uniform
        sp.registerUniform("x"); // register object x uniform
        sp.registerUniform("y"); // register object y uniform
        sp.registerUniform("isTextured"); // register texture flag uniform
        sp.registerUniform("color"); // register color uniform
        sp.registerUniform("blend"); // register blend uniform
        sp.registerUniform("texSampler"); // register texture sampler uniform
    }

    /**
     * Updates the logic by checking the phase of the animation and reacting appropriately
     *
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval);
        switch (phase) { // switch on the phase
            case 0: // PHASE 0: wait one second before dropping title
                time += interval; // kee track of time
                if (time > 1f) { // if second is over
                    time = 0f; // reset timer
                    phase = 1; // go to phase 1
                }
                break;

            case 1: // PHASE 1: drop title
                // calculate the y value at the bottom of the window
                float bottomY = ((Global.ar < 1.0f) ? -1f / Global.ar : -1f) + title.getHeight() / 2;
                time += interval; // keep track of time
                // update title's velocity using gravity
                titleV = Math.max(titleV - (19.6f * interval), PhysicsEngine.TERMINAL_VELOCITY);
                title.setY(title.getY() + (interval * titleV)); // update the title's y based on velocity
                if (title.getY() < bottomY) { // if title is below the bottom of the screen
                    title.setY(bottomY); // move it back to the bottom
                    titleV = -0.5f * titleV; // and create a bounce
                }
                if (time > 3.6f) { // if 3.6 seconds have passed
                    phase = 2; // go to phase 2
                    time = 0f; // and reset the time
                }
                break;

            case 2: // PHASE 2: move title to proper menu position
                this.renderROC = true; // begin to use the ROC
                // add the title to the ROC at the bottom of the screen
                this.roc.addStaticObject(title, new ROC.PositionSettings(0f, -1f, true, 0f));
                this.roc.moveStaticObject(0, new ROC.PositionSettings(0f, 0.5f, true, 0f),
                        1.5f); // but animate it to be in the proper position
                phase = 3; // go to phase 3
                break;

            case 3: // PHASE 3: wait for title to be in proper menu position
                if (!title.posAnimating()) phase = 4; // one the title is done positionally animating, go to phase 4
                break;

            case 4: // PHASE 4: create buttons and add to ROC
                // create new wgame button
                TextButton newGame = new TextButton(Global.FONT, "New Game");
                newGame.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> {
                    this.phase = 7;
                });
                // create exit button
                TextButton exit = new TextButton(Global.FONT, "Exit");
                exit.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> {
                    window.close();
                });
                // add new game button below title and exit button below new game button
                this.roc.addStaticObject(newGame, new ROC.PositionSettings(null, title, 0f, -2f,
                        0f));
                this.roc.addStaticObject(exit, new ROC.PositionSettings(null, newGame, 0f, -2f,
                        0f));
                bMats = new Material[]{newGame.getMaterial(), exit.getMaterial()}; // get button's materials
                // make the buttons completely transparent to start
                bMats[0].getColor()[3] = 0f;
                bMats[1].getColor()[3] = 0f;
                phase = 5; // go to phase 5
                break;

            case 5: // PHASE 5: fade in buttons
                time += interval; // account for time
                // change transparency based on how much time has passed
                bMats[0].getColor()[3] = (time / 1.5f);
                bMats[1].getColor()[3] = (time / 1.5f);
                if (time > 1.5f) { // if one and a half seconds has passed
                    time = 0f; // reset timer
                    phase = 6; // go to phase 6
                }
                break;

            // PHASE 6 (no update necessary): wait for new game button to be pressed

            case 7: // PHASE 7: fly title up and buttons down
                this.roc.moveStaticObject(0, new ROC.PositionSettings(0f, 2f + (1f / Global.ar), false, 0f),
                        0.5f); // fly the title upwards out of view
                this.roc.moveStaticObject(1, new ROC.PositionSettings(0f, -2f - (1f / Global.ar), false, 0f),
                        0.5f); // fly the buttons downwards out of view
                phase = 8; // go to phase 8
                break;

            case 8: // PHASE 8: wait for title and button to be out of view
                this.roc.ensurePlacement(2); // make sure exit button is following play button
                if (!title.posAnimating()) phase = 9; // if they are out of view, go to phase 9
                break;

            case 9: // PHASE 9: begin fade out
                GameLogic.logicChange = new LogicChange(new NewGameLogic(), 0.5f);
                this.roc.fadeOut(new float[]{0f, 0f, 0f, 0f}, 0.5f); // begin fade out to black
                phase = 10; // go to phase 10
                break;
        }
    }

    /**
     * Responds to mouse input by speeding along the main menu animation process
     *
     * @param x      the normalized and de-aspected x position of the mouse if hover event, 0 otherwise
     * @param y      the normalized and de-aspected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     */
    @Override
    public void mouseInput(float x, float y, int action) {
        super.mouseInput(x, y, action); // handle regular logic mouse inputs
        if (action == GLFW_PRESS) { // if input was a click
            switch (this.phase) { // speed along the animation depending on the phase
                case 0:
                case 1: // if in phase 0 or 1
                    this.phase = 2; // go straight to phase 2
                    break;
                case 3: // if in phase 3
                    this.roc.getStaticGameObject(0).stopPosAnimating(); // stop the title animation
                    this.roc.moveStaticObject(0, new ROC.PositionSettings(0f, 0.5f, true,
                            0f), 0f); // move the title to its proper position
                    phase = 4; // go to phase 4
                    time = 0f; // and reset timer
                    break;
            }
        }
    }

    /**
     * Renders the title during phases 0-2, before the ROC is used
     */
    @Override
    public void renderOthers() {
        if (phase < 3) { // if in phases 0-2
            this.sp.bind(); // bind shader program
            this.sp.setUniform("texSampler", 0); // set texture sampler uniform to use texture unit 0
            this.sp.setUniform("ar", Global.ar); // set aspect ratio uniform
            this.sp.setUniform("arAction", Global.arAction ? 1 : 0); // set aspect ratio action uniform
            this.title.render(this.sp); // render the title
            this.sp.unbind(); // unbind the shader program
        }
    }
}
