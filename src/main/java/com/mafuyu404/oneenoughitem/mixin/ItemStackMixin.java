package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.ClientContext;
import com.mafuyu404.oneenoughitem.init.ModConfig;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.ReplacementControl;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

import java.util.function.Predicate;

@Mixin(value = ItemStack.class)
public abstract class ItemStackMixin {
    @Mutable
    @Shadow
    @Final
    @Deprecated
    @Nullable
    private Item item;

    @Shadow
    public abstract Item getItem();

    @Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;I)V", at = @At("TAIL"))
    private void replace(ItemLike itemLike, int count, CallbackInfo ci) {
        if (this.item == null) {
            return;
        }

        if (ClientContext.isInCreativeInventory()) {
            return;
        }

        if (ReplacementControl.shouldSkipReplacement()) {
            return;
        }

        try {
            String originItemId = Utils.getItemRegistryName(this.item);

            String targetItemId = ReplacementCache.matchItem(originItemId);
            if (targetItemId != null) {
                if ("minecraft:air".equals(targetItemId)) {
                    this.item = Items.AIR;
                    ((ItemStack) (Object) this).setCount(0);
                    Oneenoughitem.LOGGER.debug("ItemStackMixin: Replaced {} -> AIR (emptied stack)", originItemId);
                    return;
                }
                Item newItem = Utils.getItemById(targetItemId);
                if (newItem != null) {
                    this.item = newItem;
                    Oneenoughitem.LOGGER.debug("ItemStackMixin: Replaced {} -> {}", originItemId, targetItemId);
                } else {
                    Oneenoughitem.LOGGER.warn("ItemStackMixin: Replacement item is null for targetItemId: {}, original item: {}",
                            targetItemId, originItemId);
                }
            }
        } catch (Exception e) {
            String itemInfo = item != null ? Utils.getItemRegistryName(item) : "null";
            Oneenoughitem.LOGGER.error("ItemStackMixin: Failed to replace item: {}", itemInfo, e);
        }
    }

    @Inject(method = "is(Ljava/util/function/Predicate;)Z", at = @At("HEAD"), cancellable = true)
    private void extend(Predicate<Holder<Item>> predicate, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.DEEPER_REPLACE.getValue()) return;
        if (!predicate.test(getItem().builtInRegistryHolder())) {
            String itemId = Utils.getItemRegistryName(item);

            boolean matched = false;

            for (Item matchItem : ReplacementCache.trackSourceOf(itemId)) {
                if (predicate.test(matchItem.builtInRegistryHolder())) matched = true;
            }
            cir.setReturnValue(matched);
        }
    }

    @Inject(method = "is(Lnet/minecraft/core/Holder;)Z", at = @At("HEAD"), cancellable = true)
    private void extend(Holder<Item> itemHolder, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.DEEPER_REPLACE.getValue()) return;
        if (getItem().builtInRegistryHolder() != itemHolder) {
            String itemId = Utils.getItemRegistryName(item);

            boolean matched = false;

            for (Item matchItem : ReplacementCache.trackSourceOf(itemId)) {
                if (matchItem.builtInRegistryHolder() == itemHolder) {
                    matched = true;
                    break;
                }
            }
            cir.setReturnValue(matched);
        }
    }

    @Inject(method = "is(Lnet/minecraft/world/item/Item;)Z", at = @At("HEAD"), cancellable = true)
    private void extend(Item inputItem, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.DEEPER_REPLACE.getValue()) return;
        if (item != inputItem) {
            String inputItemId = Utils.getItemRegistryName(inputItem);
            String ItemId = Utils.getItemRegistryName(item);

            if (Utils.isItemIdEmpty(inputItemId) || Utils.isItemIdEmpty(ItemId)) return;

            boolean matched = false;

            for (String matchId : ReplacementCache.trackSourceIdOf(ItemId)) {
                if (matchId.equals(inputItemId)) {
                    matched = true;
                    break;
                }
            }

            cir.setReturnValue(matched);
        }
    }
}