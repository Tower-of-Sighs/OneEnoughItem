package com.mafuyu404.oneenoughitem.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.EggItem;
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
    @Shadow @Final @org.jetbrains.annotations.Nullable private Holder.@org.jetbrains.annotations.Nullable Reference<Item> delegate;

    @Inject(method = "forgeInit", at = @At("HEAD"))
    private void qqq(CallbackInfo ci) {
        item = new EggItem(new Item.Properties()).asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem().asItem();
        delegate = ForgeRegistries.ITEMS.getDelegateOrThrow(item);
    }
}
