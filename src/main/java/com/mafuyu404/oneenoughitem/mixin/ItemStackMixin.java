package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.init.Cache;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = ItemStack.class)
public class ItemStackMixin {
    @Mutable
    @Shadow @Final @Deprecated @Nullable private Item item;

    @Mutable
    @Shadow @Final @org.jetbrains.annotations.Nullable private Holder.Reference<Item> delegate;

    @Inject(method = "forgeInit", at = @At("HEAD"), remap = false)
    private void replace(CallbackInfo ci) {
        String originItemId = Utils.toPathString(item.getDescriptionId());
        String targetItemId = Cache.matchItem(originItemId);
        if (targetItemId != null) {
            item = Utils.getItemById(targetItemId);
            delegate = ForgeRegistries.ITEMS.getDelegateOrThrow(item);
        }
    }
}
