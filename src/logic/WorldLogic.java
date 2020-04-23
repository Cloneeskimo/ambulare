package logic;

import gameobject.gameworld.PhysicsObject;
import gameobject.ROC;
import gameobject.TextButton;
import gameobject.TextObject;
import graphics.Material;
import graphics.Model;
import graphics.Texture;
import graphics.Window;
import utils.Global;
import utils.Pair;
import utils.Transformation;

import javax.xml.crypto.dsig.Transform;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Dictates the logic the game will follow when the user is out in the world
 */
public class WorldLogic extends GameLogic {

    /**
     * Data
     */
    Window window; // reference to the window for exit button
    PhysicsObject player, ball, ball2; // some physics objects
    boolean exitButtonPressed = false; // whether exit has been pressed

    /**
     * Initializes any members
     * @param window the window
     */
    @Override
    protected void initOthers(Window window) {
        super.initOthers(window); // call super so that FPS displaying objects are added to HUD
        this.window = window; // save reference to window

        // create and add player
        player = new PhysicsObject(Model.getStdGridRect(1, 2),
                new Material(new float[] {0.2f, 0.2f, 1.0f, 1.0f})); // create player
        player.setPos(5 * Global.GRID_CELL_SIZE, 3f); // set player's position
        player.getPhysicsSettings().bounciness = 0f; // disable bounce on player
        this.roc.getGameWorld().addObject(player); // add to ROC
        this.roc.getGameWorld().getCam().follow(player); // tell camera to follow it

        // create and add "ball"
        ball = new PhysicsObject(Model.getStdGridRect(1, 1), new Material(new float[] {0f, 1f, 1f, 1f})); // create
        // ball object
        ball.setScale(0.4f, 0.4f); // scale it down
        ball.getPhysicsSettings().bounciness = 0.9f; // make ball bouncy
        ball.setPos(2f, 3f); // move it to the left of the player
        ball.getPhysicsSettings().mass = 0.5f; // make it half the player's mass
        this.roc.getGameWorld().addObject(ball); // and add to ROC

        // create and add "ball"s
        ball2 = new PhysicsObject(Model.getStdGridRect(1, 1), new Material(new float[] {0f, 1f, 1f, 1f})); // create
        // ball object
        ball2.setScale(0.4f, 0.4f); // scale it down
        ball2.getPhysicsSettings().bounciness = 0.9f; // make ball bouncy
        ball2.setPos(3f, 3f); // move it to the left of the player
        ball2.getPhysicsSettings().mass = 2; // make it half the player's mass
        this.roc.getGameWorld().addObject(ball2); // and add to ROC

        // create and add dirt floor and walls
        Material dirt = new Material(new Texture("/textures/dirt.png")); // dirt material
        for (int i = 0; i < 10; i++) { // this loop builds the dirt floor and walls
            PhysicsObject po = new PhysicsObject(Model.getStdGridRect(1, 1), dirt); // create floor dirt
            po.getPhysicsSettings().gravity = 0f; // disable gravity on dirt
            po.setX(i * Global.GRID_CELL_SIZE); // calculate x
            po.getPhysicsSettings().rigid = true; // make dirt rigid
            this.roc.getGameWorld().addObject(po); // add to ROC
            po = new PhysicsObject(Model.getStdGridRect(1, 1), dirt); // create wall dirt
            po.getPhysicsSettings().gravity = 0f; // disable gravity on dirt
            po.setX(i > 4 ? 9 * Global.GRID_CELL_SIZE : 0 * Global.GRID_CELL_SIZE); // calculate x
            po.setY(1 + (i > 4 ? ((i - 4) * Global.GRID_CELL_SIZE) : i * Global.GRID_CELL_SIZE)); // calculate y
            po.getPhysicsSettings().rigid = true; // make dirt rigid
            this.roc.getGameWorld().addObject(po); // add to ROC
        }

        // create and add player position text
        this.roc.addStaticObject(new TextObject(Global.FONT, "(0, 0)"), // player pos text
                new ROC.PositionSettings(-1f, -1f, true, 0.02f));
        this.roc.getStaticGameObject(2).setScale(0.075f, 0.075f); // scale text down

        // create and add controls text
        this.roc.addStaticObject(new TextObject(Global.FONT, "SPACE - jump  A/D - move  ENTER - reset ball"),
                new ROC.PositionSettings(0f, -1f, true,
                        0.02f)); // create controls text
        this.roc.getStaticGameObject(3).setScale(0.05f, 0.05f); // scale text down

        // create and add text button
        this.roc.addStaticObject(new TextButton(Global.FONT, "Exit", 1),
                new ROC.PositionSettings(1f, -1f, true,
                0.02f)); // create text button
        this.roc.getStaticGameObject(4).setScale(0.05f, 0.05f); // scale button down
        this.roc.ensureAllPlacements();
    }

    /**
     * Responds to keyboard input by moving the player
     * @param key the key in question
     * @param action the action of the key (GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT)
     */
    @Override
    public void keyboardInput(int key, int action) {
        if (key == GLFW_KEY_SPACE && action == GLFW_PRESS) {
            if (player.somethingUnder(0.1f)) player.setVY(10f); // jum
        }
        else if (key == GLFW_KEY_ENTER && action == GLFW_PRESS) { // if enter is release
            ball.setPos(1f, 3f); // reset ball position
            ball.setVY(0f); // reset its vertical velocity
            ball.setVX((float)Math.random() * 3f - 1.5f); // give it a random horizontal velocity from -1.5f to 1.5f
            ball2.setPos(2f, 3f); // reset ball position
            ball2.setVY(0f); // reset its vertical velocity
            ball2.setVX((float)Math.random() * 3f - 1.5f); // give it a random horizontal velocity from -1.5f to 1.5f
        }
    }

    /**
     * Reacts to mouse input
     * @param x the normalized and de-aspected x position of the mouse if hover event, 0 otherwise
     * @param y the normalized and de-aspected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     */
    @Override
    public void mouseInput(float x, float y, int action) {
        super.mouseInput(x, y, action); // call super mouse input
        if (action == GLFW_HOVERED) { // if hover event
            Pair pos = new Pair(x, y); // create pair of mouse position
            //System.out.println(pos);
            Transformation.useCam(pos, this.roc.getGameWorld().getCam()); // transform to camera-view

            if (player.getFrame().contains(pos.x, pos.y)) { // if mouse is hovering player
                player.setMaterial(new Material(new float[]{(float)Math.random(), (float)Math.random(),
                        (float)Math.random(), 1.0f})); // change player color when mouse hovers
            }
        }
    }

    /**
     * Responds to button clicks by printing out the MIID of the button
     * @param MIID the ID of the object that was clicked
     */
    @Override
    public void clicked(int MIID) {
        if (MIID == 1) exitButtonPressed = true;
    }

    /**
     * Updates the world logic
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval); // call super update
        Pair pos = new Pair(player.getX(), player.getY());
        Transformation.alignToGrid(pos);
        if (((TextObject)this.roc.getStaticGameObject(2)).setText("(" + String.format("%.2f", pos.x) +
                ", " + String.format("%.2f", pos.y) + ")")) // change player pos text
            this.roc.ensurePlacement(2); // update placement if changed
        player.setVX(0);
        if (exitButtonPressed) window.close();
        if (window.isKeyPressed(GLFW_KEY_D)) player.incrementVX(4); // rightwards movement
        if (window.isKeyPressed(GLFW_KEY_A)) player.incrementVX(-4); // leftwards movement
    }
}
