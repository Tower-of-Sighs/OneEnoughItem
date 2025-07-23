package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Cache;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemStack.class)
public class ItemStackMixin {
    @Mutable
    @Shadow
    @Final
    @Deprecated
    @Nullable
    private Item item;

    @Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;I)V", at = @At("TAIL"))
    private void replace(ItemLike itemLike, int count, CallbackInfo ci) {
        if (this.item == null) {
            return;
        }

        if (isInCreativeModeTabBuilding()) {
            return;
        }

        String originItemId = Utils.getItemRegistryName(this.item);
        Oneenoughitem.LOGGER.debug("Processing item in constructor: {}", originItemId);

        String targetItemId = Cache.matchItem(originItemId);
        if (targetItemId != null) {
            Item newItem = Utils.getItemById(targetItemId);
            if (newItem != null) {
                Oneenoughitem.LOGGER.debug("Replacing item {} with {} in constructor", originItemId, targetItemId);
                this.item = newItem;
            } else {
                Oneenoughitem.LOGGER.warn("Target item not found: {}", targetItemId);
            }
        }
    }

    private boolean isInCreativeModeTabBuilding() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            String methodName = element.getMethodName();

            if (className.contains("CreativeModeTab") ||
                    className.contains("CreativeModeTabs") ||
                    methodName.contains("buildContents") ||
                    methodName.contains("accept")) {
                return true;
            }
        }
        return false;
    }
}