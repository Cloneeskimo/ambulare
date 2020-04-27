package logic;

import gameobject.GameObject;
import gameobject.ROC;
import gameobject.TextButton;
import graphics.*;
import utils.Global;
import utils.PhysicsEngine;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

/**
 * Dictates the logic the game will follow when the user is at the main menu
 */
public class MainMenuLogic extends GameLogic {

    /**
     * Static data
     */
    private static final int PLAY_MIID = 0; // mouse interaction ID for play button
    private static final int EXIT_MIID = 1; // mouse interaction ID for exit button

    /**
     * Members
     */
    private Material[] bMats;  // store button materials for changing opacity
    private Window window;     // store reference to window to close when exit button is pressed
    private GameObject title;  // title object before added to ROC
    private ShaderProgram sp;  // shader program to render title before adding to ROC
    private float titleV = 0f; // the velocity of the title before added to ROC
    private int phase = 0;     // the phase of the main menu animation
    private float time = 0f;   // a generic timer used for the main menu animation

    /**
     * Initializes the window game object and beings the intro animation
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
        initSP(); // initialize the shader program
        title = new GameObject(new Model(titleModelCoords, Model.getStdRectTexCoords(), Model.getStdRectIdx()),
                new Material(new Texture("/textures/title.png", true))); // create title object
        title.setScale(0.55f, 0.55f); // scale title down by about one half
        title.setPos(0f, 3f); // move title to be up and out of view
        this.renderROC = false; // turn off ROC rendering for now
    }

    /**
     * Initializes the shader program used to render the title before it is added to the ROC
     */
    private void initSP() {
        // create the shader program
        sp = new ShaderProgram("/shaders/vertex.glsl", "/shaders/fragment.glsl");
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
                if (time > 3.6f) { // if 3.6 seconds has passed
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
                float[] defaultC = new float[]{0.5f, 0.5f, 0.5f, 1f}; // default color for the buttons -> gray
                float[] hoverC = new float[]{1f, 1f, 1f, 1f}; // hover color for the buttons -> white
                float[] pressC = new float[]{1f, 1f, 0f, 1f}; // pressed color for the buttons -> yellow
                // create play button
                TextButton play = new TextButton(Global.FONT, "Play", defaultC, hoverC, pressC, PLAY_MIID);
                play.setScale(1.4f, 1.4f); // make it a little larger
                // create exit button
                TextButton exit = new TextButton(Global.FONT, "Exit", defaultC, hoverC, pressC, EXIT_MIID);
                exit.setScale(1.4f, 1.4f); // make it a littler larger
                // add play button below title and exit button below play button
                this.roc.addStaticObject(play, new ROC.PositionSettings(null, title, 0f, -2f, 0f));
                this.roc.addStaticObject(exit, new ROC.PositionSettings(null, play, 0f, -2f, 0f));
                bMats = new Material[]{play.getMaterial(), exit.getMaterial()}; // get button's materials
                float[] color = bMats[0].getColor(); // get the button material's color
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
            // PHASE 6: wait for play button to be pressed
            case 7: // PHASE 7: fly title up and buttons down
                this.roc.moveStaticObject(0, new ROC.PositionSettings(0f, 2f, false, 0f),
                        0.5f); // fly the title upwards out of view
                this.roc.moveStaticObject(1, new ROC.PositionSettings(0f, -2f, false, 0f),
                        0.5f); // fly the buttons downwards out of view
                phase = 8; // go to phase 8
                break;
            case 8: // PHASE 8: wait for title and button to be out of view
                this.roc.ensurePlacement(2); // make sure exit button is following play button
                if (!title.posAnimating()) phase = 9; // if they are out of view, go to phase 9
                break;
            case 9: // PHASE 9: begin fade out
                this.roc.fadeOut(new float[]{0f, 0f, 0f, 0f}, 1f); // begin fade out to black
                time = 0f; // reset timer
                phase = 10; // go to phase 10
                break;
            case 10: // PHASE 10: fading out
                time += interval; // account for time
                // if fade has finished, go to world logic
                if (time > 1f) GameLogic.logicChange = new LogicChange(new WorldLogic());
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
        super.mouseInput(x, y, action);
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
     * Responds to a mouse click on an object able to be interacted with by a mouse
     *
     * @param MIID the ID of the object that was clicked
     */
    @Override
    public void clicked(int MIID) {
        switch (MIID) { // switch on the mouse interaction ID
            case PLAY_MIID: // if play was clicked
                this.phase = 7; // go to phase 7
                break;
            case EXIT_MIID: // if exit was clicked
                this.window.close(); // close the window
                break;
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
