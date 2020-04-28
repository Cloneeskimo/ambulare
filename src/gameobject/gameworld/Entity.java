package gameobject.gameworld;

import graphics.Material;
import graphics.Model;
import graphics.Texture;
import utils.PhysicsEngine;

public class Entity extends WorldObject {

    /**
     * Constructor
     *
     * @param model    the model to use
     * @param material the material to use
     */
    public Entity(Model model, EntityMaterial material) {
        super(model, material);
    }

    public void setFacing(boolean right) {
        ((EntityMaterial)this.material).setFacing(right);
    }

    @Override
    public void update(float interval) {
        super.update(interval);
        boolean under = PhysicsEngine.nextTo(this, 0f, -1f);
        ((EntityMaterial)this.material).setAirborne(!under);
    }

    public static class EntityMaterial extends Material {

        Texture[][] textures;
        boolean airborne;
        boolean right;

        public EntityMaterial(Texture leftFacing, Texture rightFacing, Texture leftAirborne, Texture rightAirborne) {
            super(rightFacing);
            this.textures = new Texture[][] { {leftFacing, rightFacing}, { leftAirborne, rightAirborne} };
        }

        public void setFacing(boolean right) {
            this.right = right;
            this.updateTexture();
        }

        public void setAirborne(boolean airborne) {
            this.airborne = airborne;
            this.updateTexture();
        }

        private void updateTexture() {
            this.texture = this.textures[this.airborne ? 1 : 0][right ? 1 : 0];
        }
    }
}
