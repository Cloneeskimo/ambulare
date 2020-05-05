package gameobject.ui;

import gameobject.GameObject;
import graphics.Material;
import graphics.Model;
import graphics.ShaderProgram;
import utils.MouseInputEngine;
import utils.Pair;

import java.util.List;

public class ListObject extends GameObject implements MouseInputEngine.MouseInteractive {

    private final MouseInputEngine.MouseCallback[] mcs = new MouseInputEngine.MouseCallback[4];
    private List<ListItem> items;
    private float padding;
    private ListItem hovered;

    public ListObject(List<ListItem> items, float padding, Material background) {
        super(Model.getStdGridRect(1, 1), background);
        this.items = items;
        this.padding = padding;
        this.position();
    }

    private void position() {
        float w = 0f;
        float h = 0f;
        for (ListItem li : items) {
            w = Math.max(w, li.getWidth());
            h += li.getHeight();
        }
        w += this.padding * 2;
        h += this.padding * (1 + this.items.size());
        this.model.setScale(w, h);
        float y = this.getY() - (this.getHeight() / 2) + padding;
        for (int i = 0; i < this.items.size(); i++) {
            float ih2 = this.items.get(i).getHeight() / 2;
            y += ih2;
            this.items.get(i).setPos(this.getX(), y);
            y += ih2;
            y += padding;
        }
    }

    @Override
    protected void onMove() {
        this.position();
    }

    @Override
    public void setXScale(float x) {
        for (ListItem li : this.items) {
            if (li instanceof GameObject) {
                ((GameObject)li).setXScale(x);
            }
        }
        this.position();
    }

    @Override
    public void setYScale(float y) {
        for (ListItem li : this.items) {
            if (li instanceof GameObject) {
                ((GameObject)li).setYScale(y);
            }
        }
        this.position();
    }

    @Override
    public void setScale(float x, float y) {
        for (ListItem li : this.items) {
            if (li instanceof GameObject) {
                ((GameObject)li).setXScale(x);
                ((GameObject)li).setYScale(y);
            }
        }
        this.position();
    }

    @Override
    public void render(ShaderProgram sp) {
        super.render(sp);
        for (ListItem li : this.items) li.render(sp);
    }

    @Override
    public void giveCallback(MouseInputEngine.MouseInputType type, MouseInputEngine.MouseCallback mc) {
        MouseInputEngine.MouseInteractive.saveCallback(type, mc, this.mcs);
    }

    @Override
    public void mouseInteraction(MouseInputEngine.MouseInputType type, float x, float y) {
        MouseInputEngine.MouseInteractive.invokeCallback(type, this.mcs, x, y);
        ListItem item = null;
        Pair<Float> pos = new Pair<>(x, y);
        for (ListItem li : this.items) {
            if (li.getFittingBox().contains(pos)) {
                item = li;
                break;
            }
        }
        if (item != null) {
            if (this.hovered != null) {
                if (item != hovered) hovered.mouseInteraction(MouseInputEngine.MouseInputType.DONE_HOVERING, x, y);
            }
            item.mouseInteraction(type, x, y);
            this.hovered = item;
        } else if (this.hovered != null) hovered.mouseInteraction(MouseInputEngine.MouseInputType.DONE_HOVERING, x, y);
    }

    public interface ListItem extends MouseInputEngine.MouseInteractive {

        float getWidth();

        float getHeight();

        void setPos(float x, float y);

        void render(ShaderProgram sp);
    }
}
