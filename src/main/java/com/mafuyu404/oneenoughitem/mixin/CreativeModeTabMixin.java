package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.client.ClientContext;
import com.mafuyu404.oneenoughitem.init.access.CreativeModeTabIconRefresher;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin implements CreativeModeTabIconRefresher {

    @Mutable
    @Shadow
    @Nullable
    private ItemStack iconItemStack;

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
        ClientContext.endBuilding();
    }
    @Override
    public void oei$refreshIconCache() {
        iconItemStack = null;
    }
}