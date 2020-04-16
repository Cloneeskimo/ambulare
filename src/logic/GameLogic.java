package logic;

import graphics.Window;

/**
 * Represents a generic Logic for the Engine to follow
 * Different GameLogics should implement this to allow the engine to act differently in different game states
 */
public interface GameLogic {

    /**
     * Here, the GameLogic should initialize its data and register and controls to the window
     * This will be called when the GameLogic is first switched to
     * @param window the window
     */
    void init(Window window);

    /**
     * Here, the logic should respond to any input. graphics.Window's isKeyPressed() method may be particularly useful
     * This will be called every loop
     * @param window the window
     */
    void input(Window window);

    /**
     * Here, the logic should update its data
     * This will be called every loop after input()
     * @param interval the amount of time in seconds since the last update call
     */
    void update(float interval);

    /**
     * Here, the logic should render whatever it needs to render
     * This will be called every loop after update()
     */
    void render();

    /**
     * Here, the logic should react to window resizes
     * This will be called whenever the window resizes
     * @param w the new width of the window
     * @param h the new height of the window
     */
    void resized(int w, int h);

    /**
     * Here, the logic should clean up any components that need cleaned up
     * This will be called when the engine is done with this logic
     */
    void cleanup();
}
