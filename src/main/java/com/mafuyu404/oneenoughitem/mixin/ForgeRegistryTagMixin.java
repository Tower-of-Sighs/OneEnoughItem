package com.mafuyu404.oneenoughitem.mixin;

import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

//@Mixin(targets = "net.minecraftforge.registries.ForgeRegistryTag", remap = false)
public class ForgeRegistryTagMixin {
//    @Shadow @Final private TagKey<?> key;
//
//    @Inject(method = "getContents", at = @At("RETURN"), cancellable = true)
//    private void clearInvalidItems(CallbackInfoReturnable<List<?>> cir) {
////        if (key.location().equals())
//        System.out.print(key.location()+"\n");
//    }
}
