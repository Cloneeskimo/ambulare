package gameobject;

/**
 * Entity with wandering AI disabled so as to give complete control the player
 */
public class Player extends Entity {

    /**
     * Constructs this player using data from a node-file
     * @param resPath the resource-relative path to the node-file
     * The node-file can contain the following children but does not need to contain any:
     *  - every child that a entity's node-file can contain (see Tile's constructor) - but note that anything related
     *    to wander AI will be ignored
     */
    public Player(String resPath) {
        super(resPath); // call super
        this.wanders = false; // disable wandering
    }

    /**
     * Constructs this player using the default properties
     */
    public Player() {
        super(); // call super
        this.wanders = false; // disable wandering
    }

    /**
     * Override this method to never allow wandering AI for the player
     * @param wanders means nothing for player
     */
    @Override
    public void setWanders(boolean wanders) {}
}
