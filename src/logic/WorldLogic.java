package logic;

import gameobject.ROC;
import gameobject.ui.TextButton;
import gameobject.ui.TextObject;
import gameobject.gameworld.Area;
import gameobject.gameworld.Entity;
import gameobject.gameworld.WorldObject;
import graphics.*;
import utils.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Dictates the logic the game will follow when the user is out in the world
 */
public class WorldLogic extends GameLogic {

    /**
     * Members
     */
    Window window;                      // reference to the window for exit button
    Entity player;                      // reference to player

    /**
     * Initializes any members
     *
     * @param window the window
     */
    @Override
    protected void initOthers(Window window) {
        super.initOthers(window); // call super so that FPS displaying objects are added to HUD
        this.window = window; // save reference to window
        // create game world with starting area
        this.roc.useGameWorld(window.getHandle(), new Area(Node.resToNode("/mainstory/areas/area.node")));

        // create and add player
        player = new Entity(Model.getStdGridRect(1, 2),
                new LightSourceMaterial(new MSAT("/textures/entity/player.png", true, new MSAT.MSATState[]{
                        new MSAT.MSATState(1, 1f),
                        new MSAT.MSATState(1, 1f),
                        new MSAT.MSATState(1, 1f),
                        new MSAT.MSATState(1, 1f),
                        new MSAT.MSATState(5, 0.05f),
                        new MSAT.MSATState(5, 0.05f)
                }), new LightSource(new float[]{1f, 1f, 1f, 1f}, 5f, 1.5f)));
        player.setBoundingWidth(0.9f);
        player.setBoundingHeight(0.9f);
        player.setPos(Transformation.getCenterOfCell(new Pair<>(1, 9))); // move to grid cell 3, 5
        player.getPhysicsProperties().rigid = true; // male player rigid
        this.roc.addToWorld(player); // add player to world
        this.roc.getGameWorld().getCam().follow(player); // make camera follow player

        // create a random object
        WorldObject o = new WorldObject(Model.getStdGridRect(1, 1), new Material(
                new float[]{1.0f, 0.0f, 0.0f, 1.0f})); // as a pink square
        o.setScale(0.3f, 0.3f); // scale down a lot
        o.getPhysicsProperties().bounciness = 0.9f; // make very bouncy
        o.getPhysicsProperties().mass = 0.6f; // make it light
        o.setPos(Transformation.getCenterOfCell(new Pair<>(2, 9))); // move to grid cell 2, 7
        this.roc.addToWorld(o); // add to ROC

        // create another random object
        o = new WorldObject(Model.getStdGridRect(1, 1), new Material(
                new float[]{1.0f, 0.0f, 0.0f, 1.0f})); // as a pink square
        o.setScale(0.3f, 0.3f); // scale down a lot
        o.getPhysicsProperties().bounciness = 0.9f; // make very bouncy
        o.getPhysicsProperties().mass = 0.6f; // make it light
        o.setPos(Transformation.getCenterOfCell(new Pair<>(0, 9))); // move to grid cell 5, 7
        this.roc.addToWorld(o); // add to ROC

        // create and add player position text
        this.roc.addStaticObject(new TextObject(Global.FONT, "(0, 0)"), // player pos text
                new ROC.PositionSettings(-1f, -1f, true, 0.02f));
        this.roc.getStaticGameObject(2).setScale(0.6f, 0.6f); // scale text down

        // create and add exit button
        TextButton exit = new TextButton(Global.FONT, "Exit"); // create exit button
        exit.setScale(0.6f, 0.6f);
        exit.giveCallback(MouseInputEngine.MouseInputType.RELEASE, (x, y) -> { window.close(); });

        // add exit button to ROC
        this.roc.addStaticObject(exit, new ROC.PositionSettings(1f, -1f, true, 0.02f));
        this.roc.ensureAllPlacements(); // ensure ROC static object placements

        // create and add area name
        this.roc.addStaticObject(new TextObject(Global.FONT, this.roc.getGameWorld().getArea().getName()),
                new ROC.PositionSettings(0f, 1f, true, 0.1f)); // create area name
        this.roc.getStaticGameObject(4).setScale(0.8f, 0.8f); // scale area name

        // create and add reset info
        this.roc.addStaticObject(new TextObject(Global.FONT, "Press enter to reset"),
                new ROC.PositionSettings(0f, -1f, true, 0.1f)); // create reset info
        this.roc.getStaticGameObject(5).setScale(0.7f, 0.7f); // scale area name

        // fade in
        this.roc.fadeIn(new float[]{0.0f, 0.0f, 0.0f, 1.0f}, 3f);
    }

    /**
     * Responds to keyboard input by moving the player
     *
     * @param key    the key in question
     * @param action the action of the key (GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT)
     */
    @Override
    public void keyboardInput(int key, int action) {
        if (key == GLFW_KEY_SPACE && action == GLFW_PRESS) { // if space is pressed
            if (PhysicsEngine.nextTo(player, 0f, -1f)) player.setVY(10f); // jump if there is something under
        } else if (key == GLFW_KEY_ENTER && action == GLFW_RELEASE) { // if enter is pressed
            player.givePosAnim(new PositionalAnimation(1.5f, 9.5f, null, 1.0f)); // reset player
            // reset first random object
            roc.getGameWorld().getWorldObject(1).givePosAnim(new PositionalAnimation(0.5f, 9.5f, null,
                    1.0f));
            // reset second random object
            roc.getGameWorld().getWorldObject(2).givePosAnim(new PositionalAnimation(2.5f, 9.5f, null,
                    1.0f));
        }
    }

    /**
     * Reacts to mouse input
     *
     * @param x      the normalized and de-aspected x position of the mouse if hover event, 0 otherwise
     * @param y      the normalized and de-aspected y position of the mouse if hover event, 0 otherwise
     * @param action the nature of the mouse input (GLFW_PRESS, GLFW_RELEASE, or GLFW_HOVERED)
     */
    @Override
    public void mouseInput(float x, float y, int action) {
        super.mouseInput(x, y, action); // call super mouse input
    }

    /**
     * Updates the world logic
     *
     * @param interval the amount of time to account for
     */
    @Override
    public void update(float interval) {
        super.update(interval);
        Pair pos = new Pair(player.getX(), player.getY()); // pair player position
        Transformation.getGridCell(pos);
        if (((TextObject) this.roc.getStaticGameObject(2)).setText(pos.x + ", " + pos.y + ")")) // change player pos text
            this.roc.ensurePlacement(2); // update placement if changed
        int vx = 0;
        if (window.isKeyPressed(GLFW_KEY_D)) vx += 4;
        if (window.isKeyPressed(GLFW_KEY_A)) vx -= 4;
        player.setVX(vx);
        if (vx == 0) {
            player.setIsMoving(false);
        } else {
            player.setIsMoving(true);
            player.setFacing(vx > 0);
        }
    }
}
