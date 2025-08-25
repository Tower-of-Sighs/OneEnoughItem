package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.client.ClientContext;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.Utils;
import com.mafuyu404.oneenoughitem.init.access.CreativeModeTabIconRefresher;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin implements CreativeModeTabIconRefresher {

    @Mutable
    @Shadow
    @Nullable
    private ItemStack iconItemStack;
    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Shadow
    @Final
    private Supplier<ItemStack> iconGenerator;

    @Inject(
            method = "buildContents",
            at = @At("HEAD")
    )
    private void onBuildStart(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CallbackInfo ci) {
        ClientContext.beginBuilding();
    }

    @Inject(
            method = "buildContents",
            at = @At("RETURN")
    )
    private void onBuildEnd(CreativeModeTab.ItemDisplayParameters itemDisplayParameters, CallbackInfo ci) {
        // 在构建结束但仍处于 isBuilding=true 时做后处理替换，手动按替换映射重新构建 stack
        oei$applyReplacementToTabItems();
        ClientContext.endBuilding();
    }

    @Override
    public void oei$refreshIconCache() {
        iconItemStack = null;
    }

    private void oei$applyReplacementToTabItems() {
        try {
            if (this.displayItems != null && !this.displayItems.isEmpty()) {
                var original = new ArrayList<>(this.displayItems);
                this.displayItems.clear();
                for (ItemStack stack : original) {
                    ItemStack replaced = oei$replaceStackPreservingComponents(stack);
                    this.displayItems.add(replaced);
                }
            }
            if (this.displayItemsSearchTab != null && !this.displayItemsSearchTab.isEmpty()) {
                var originalSearch = new ArrayList<>(this.displayItemsSearchTab);
                this.displayItemsSearchTab.clear();
                for (ItemStack stack : originalSearch) {
                    ItemStack replaced = oei$replaceStackPreservingComponents(stack);
                    this.displayItemsSearchTab.add(replaced);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private ItemStack oei$replaceStackPreservingComponents(ItemStack original) {
        try {
            Item srcItem = original.getItem();
            String srcId = Utils.getItemRegistryName(srcItem);
            if (Utils.isItemIdEmpty(srcId)) {
                return original;
            }
            String targetId = ReplacementCache.matchItem(srcId);
            if (targetId == null || targetId.equals(srcId)) {
                return original;
            }
            Item targetItem = Utils.getItemById(targetId);
            if (targetItem == null) {
                return original;
            }

            PatchedDataComponentMap originalComponents = ((ItemStackAccessor) (Object) original).oei$getComponents();
            var patch = originalComponents.asPatch();

            return new ItemStack(targetItem.builtInRegistryHolder(), 1, patch);
        } catch (Throwable t) {
            return original;
        }
    }
}