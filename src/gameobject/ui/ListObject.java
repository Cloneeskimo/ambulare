package gameobject.ui;

import gameobject.GameObject;
import graphics.Material;
import graphics.Model;
import utils.MouseInputEngine;

public class ListObject extends GameObject implements MouseInputEngine.MouseInteractive {

    public ListObject(Model model, Material material) {
        super(model, material);
    }

    @Override
    public void giveCallback(MouseInputEngine.MouseInputType type, MouseInputEngine.MouseCallback mc) {

    }

    @Override
    public void mouseInteraction(MouseInputEngine.MouseInputType type, float x, float y) {

    }

    public interface ListItem {

        float getWidth();

        float getHeight();

        int method(int param);

    }
}
