package logic;

import gameobject.GameObject;
import gameobject.ROC;
import gameobject.ui.ListObject;
import gameobject.ui.TextButton;
import gameobject.ui.TextObject;
import graphics.*;
import utils.Global;
import utils.MouseInputEngine;

import java.util.ArrayList;
import java.util.List;

public class NewGameLogic extends GameLogic {

    @Override
    public void initOthers(Window window) {

        List<ListObject.ListItem> listItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            listItems.add(new LI(Global.FONT, "Item " + i));
        }
        ListObject lo = new ListObject(listItems, 0.05f, new Material(new float[] {0.5f, 0.5f, 0.5f, 0.5f}));

        lo.givePosAnim(new PositionalAnimation(0.5f, 0.5f, null, 2f));

        this.roc.addStaticObject(lo, new ROC.PositionSettings(0f, 0f, false, 0f));
        this.roc.fadeIn(new float[] { 0f, 0f, 0f, 1f }, 0.5f);
    }

    private class LI extends TextButton implements ListObject.ListItem {

        public LI(Font font, String text) {
            super(font, text);
        }
    }
}
