package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
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
    @Shadow
    @Final
    @Deprecated
    @Nullable
    private Item item;

    @Mutable
    @Shadow
    @Final
    @org.jetbrains.annotations.Nullable
    private Holder.Reference<Item> delegate;

    @Inject(method = "forgeInit", at = @At("HEAD"), remap = false)
    private void replace(CallbackInfo ci) {
        if (item == null) {
            return;
        }

        try {
            String originItemId = Utils.getItemRegistryName(item);
            if (!originItemId.contains("book")) {
                item = Utils.getItemById("minecraft:egg");
                delegate = ForgeRegistries.ITEMS.getDelegateOrThrow(item);
            }
        } catch (Exception e) {
            String itemInfo = item != null ? Utils.getItemRegistryName(item) : "null";
            Oneenoughitem.LOGGER.error("ItemStackMixin: Failed to replace item: {}", itemInfo, e);
        }
    }
}