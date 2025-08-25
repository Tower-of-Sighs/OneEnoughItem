package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.client.ClientContext;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {

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
}
