package io.redspace.ironsspellbooks.entity.armor.netherite;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.item.armor.NetheriteMageArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class NetheriteMageArmorModel extends DefaultedItemGeoModel<NetheriteMageArmorItem> {

    public NetheriteMageArmorModel(){
        super(new ResourceLocation(IronsSpellbooks.MODID, ""));
    }

    @Override
    public ResourceLocation getModelResource(NetheriteMageArmorItem object) {
        return new ResourceLocation(IronsSpellbooks.MODID, "geo/netherite_armor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NetheriteMageArmorItem object) {
        return new ResourceLocation(IronsSpellbooks.MODID, "textures/models/armor/netherite.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NetheriteMageArmorItem animatable) {
        return new ResourceLocation(IronsSpellbooks.MODID, "animations/wizard_armor_animation.json");
    }
}