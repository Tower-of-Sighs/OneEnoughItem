package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.ClientContext;
import com.mafuyu404.oneenoughitem.init.Config;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.ReplacementControl;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(value = ItemStack.class)
public class ItemStackMixin {
    @Mutable
    @Shadow
    @Final
    @Nullable
    private Item item;

    @Mutable
    @Shadow
    @Final
    PatchedDataComponentMap components;

    @Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;I)V", at = @At("TAIL"))
    private void replace(ItemLike itemLike, int count, CallbackInfo ci) {
        performReplacement();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/core/component/PatchedDataComponentMap;)V", at = @At("TAIL"))
    private void replaceWithComponents(ItemLike itemLike, int count, PatchedDataComponentMap components, CallbackInfo ci) {
        performReplacement();
    }

    private void performReplacement() {
        if (this.item == null) {
            return;
        }

        if (ClientContext.isInCreativeInventory()) {
            return;
        }

        // 检查是否应该跳过替换
        if (ReplacementControl.shouldSkipReplacement()) {
            return;
        }

        String originItemId = Utils.getItemRegistryName(this.item);
        String targetItemId = ReplacementCache.matchItem(originItemId);

        if (targetItemId != null) {
            Item newItem = Utils.getItemById(targetItemId);
            if (newItem != null) {
                DataComponentPatch currentPatch = this.components.asPatch();

                this.item = newItem;

                this.components = PatchedDataComponentMap.fromPatch(newItem.components(), currentPatch);

                newItem.verifyComponentsAfterLoad((ItemStack) (Object) this);

//                Oneenoughitem.LOGGER.debug("Successfully replaced item {} with {}", originItemId, targetItemId);
            } else {
                Oneenoughitem.LOGGER.warn("Target item not found: {}", targetItemId);
            }
        }
    }

    @Inject(method = "is(Lnet/minecraft/world/item/Item;)Z", at = @At("HEAD"), cancellable = true)
    private void extend(Item inputItem, CallbackInfoReturnable<Boolean> cir) {
        // 使用缓存的静态布尔值，避免在配置加载前访问配置
        if (!Config.IS_DEEPER_REPLACE_ENABLED) {
            return;
        }

        boolean matched = item == inputItem;
        // 直接一致了就没必要往下了
        if (matched) {
            return;
        }

        String itemId = Utils.getItemRegistryName(item);
        String inputItemId = Utils.getItemRegistryName(inputItem);

        if (Utils.isItemIdEmpty(itemId) || Utils.isItemIdEmpty(inputItemId)) return;

        Set<String> sources = ReplacementCache.trackSourceOf(itemId);
        if (sources.isEmpty()) return;

        if (sources.contains(inputItemId)) {
            cir.setReturnValue(true);
        }
    }
}