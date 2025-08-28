package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.api.EditableItem;
import com.mafuyu404.oneenoughitem.init.config.ModConfig;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = Item.class)
@Implements(@Interface(iface = EditableItem.class, prefix = "lazy$"))
public abstract class ItemMixin implements EditableItem {
    @Shadow
    public abstract Item asItem();

    @Mutable
    @Shadow
    @Final
    @Nullable
    private FoodProperties foodProperties;

    //    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
//    private static Item.Properties q(Item.Properties origin) {
//        String itemId = Utils.getItemRegistryName(new Item(origin));
//        Util.makeDescriptionId("item", BuiltInRegistries.ITEM.getKey(this));
//        if (itemId == null) {
//            return origin;
//        }
//
//        if (!itemId.contains("book")) return new Item.Properties();
//
//        String matched = ReplacementCache.matchItem(itemId);
//        if (matched != null) {
//            return new Item.Properties();
//        }
//        return origin;
//    }

    public void setFoodProperties(@Nullable FoodProperties foodProperties) {
        this.foodProperties = foodProperties;
    }

    @Inject(method = "getFoodProperties", at = @At("HEAD"), cancellable = true)
    private void qq(CallbackInfoReturnable<FoodProperties> cir) {
        if (!ModConfig.CLEAR_FOOD_PROPERTIES.getValue()) return;

        String itemId = Utils.getItemRegistryName(asItem());
        if (itemId != null) {
            String matched = ReplacementCache.matchItem(itemId);
            if (matched != null) {
                cir.setReturnValue(null);
            }
        }
    }
}