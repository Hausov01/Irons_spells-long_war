package io.redspace.ironsspellbooks.entity.armor;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.item.armor.ElectromancerArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class ElectromancerArmorModel extends DefaultedItemGeoModel<ElectromancerArmorItem> {

    public ElectromancerArmorModel() {
        super(new ResourceLocation(IronsSpellbooks.MODID, ""));
    }

    @Override
    public ResourceLocation getModelResource(ElectromancerArmorItem object) {
        return new ResourceLocation(IronsSpellbooks.MODID, "geo/electromancer_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ElectromancerArmorItem object) {
        return new ResourceLocation(IronsSpellbooks.MODID, "textures/models/armor/electromancer.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ElectromancerArmorItem animatable) {
        return new ResourceLocation(IronsSpellbooks.MODID, "animations/wizard_armor_animation.json");
    }
}