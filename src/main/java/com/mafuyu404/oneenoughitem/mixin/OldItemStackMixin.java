package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Config;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.ReplacementControl;
import com.mafuyu404.oneenoughitem.init.Utils;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

@Mixin(value = ItemStack.class)
public abstract class OldItemStackMixin {
    @Mutable
    @Shadow
    @Final
    @Deprecated
    @Nullable
    private Item item;

    @Shadow public abstract Item getItem();

    @Inject(method = "forgeInit", at = @At("HEAD"), remap = false)
    private void replace(CallbackInfo ci) {
        if (item == null) {
            return;
        }

        // 检查是否应该跳过替换
        if (ReplacementControl.shouldSkipReplacement()) {
            return;
        }

        try {
            String originItemId = Utils.getItemRegistryName(item);
            if (originItemId == null) {
                return;
            }

            String targetItemId = ReplacementCache.matchItem(originItemId);
            if (targetItemId != null) {
                if (targetItemId.equals("minecraft:air") && isInCreativeModeTabBuilding()) return;
                Item replacementItem = Utils.getItemById(targetItemId);
                if (replacementItem != null) {
                    item = replacementItem;
                    try {
                        Field itemDelegateField = Item.class.getField("delegate");
                        var delegate = itemDelegateField.get(item);
                        Field stackDelegateField = ItemStack.class.getDeclaredField("delegate");
                        stackDelegateField.setAccessible(true);
                        stackDelegateField.set(this, delegate);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
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

//    @Inject(method = "is(Ljava/util/function/Predicate;)Z", at = @At("HEAD"), cancellable = true)
//    private void extend(Predicate<Holder<Item>> predicate, CallbackInfoReturnable<Boolean> cir) {
//        if (!Config.DEEPER_REPLACE.get()) return;
//        if (!predicate.test(getItem().builtInRegistryHolder())) {
//            String itemId = Utils.getItemRegistryName(item);
//
//            boolean matched = false;
//
//            for (Item matchItem : ReplacementCache.trackSourceOf(itemId)) {
//                if (predicate.test(matchItem.builtInRegistryHolder())) matched = true;
//            }
//            cir.setReturnValue(matched);
//        }
//    }

//    @Inject(method = "is(Lnet/minecraft/core/Holder;)Z", at = @At("HEAD"), cancellable = true)
//    private void extend(Holder<Item> itemHolder, CallbackInfoReturnable<Boolean> cir) {
//        if (!Config.DEEPER_REPLACE.get()) return;
//        if (getItem().builtInRegistryHolder() != itemHolder) {
//            String itemId = Utils.getItemRegistryName(item);
//
//            boolean matched = false;
//
//            for (Item matchItem : ReplacementCache.trackSourceOf(itemId)) {
//                if (matchItem.builtInRegistryHolder() == itemHolder) matched = true;
//            }
//            cir.setReturnValue(matched);
//        }
//    }

    @Inject(method = "is(Lnet/minecraft/world/item/Item;)Z", at = @At("HEAD"), cancellable = true)
    private void extend(Item inputItem, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.DEEPER_REPLACE.get()) return;
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