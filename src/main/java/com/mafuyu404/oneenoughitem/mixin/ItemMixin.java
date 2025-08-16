package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.init.Config;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Item.class)
public abstract class ItemMixin {
    @Shadow
    public abstract Item asItem();

    @Inject(method = "getFoodProperties", at = @At("HEAD"), cancellable = true)
    private void qq(CallbackInfoReturnable<FoodProperties> cir) {
        if (!Config.CLEAR_FOOD_PROPERTIES.get()) return;

        String itemId = Utils.getItemRegistryName(asItem());
        if (itemId != null) {
            String matched = ReplacementCache.matchItem(itemId);
            if (matched != null) {
                cir.setReturnValue(null);
            }
        }
    }
}
