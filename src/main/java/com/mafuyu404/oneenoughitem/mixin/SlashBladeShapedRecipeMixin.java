//package com.mafuyu404.oneenoughitem.mixin;
//
//import mods.flammpfeil.slashblade.init.SBItems;
//import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipe;
//import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
//import net.minecraft.core.RegistryAccess;
//import net.minecraft.core.registries.BuiltInRegistries;
//import net.minecraft.resources.ResourceKey;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.item.ItemStack;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Overwrite;
//import org.spongepowered.asm.mixin.Shadow;
//
//import java.util.Objects;
//
//@Mixin(SlashBladeShapedRecipe.class)
//public abstract class SlashBladeShapedRecipeMixin {
//    @Shadow @Final
//    private ResourceLocation outputBlade;
//    @Shadow
//    private static ItemStack getResultBlade(ResourceLocation outputBlade) { return null; }
//
//    /**
//     * 覆盖 getResultItem，加一个兜底
//     * @author Flechazo
//     * @reason fix recipe
//     */
//    @Overwrite
//    public ItemStack getResultItem(RegistryAccess access) {
//        ItemStack result = getResultBlade(this.outputBlade);
//        if (result.isEmpty() || !Objects.equals(BuiltInRegistries.ITEM.getKey(result.getItem()), this.outputBlade)) {
//            try {
//                var def = access.registryOrThrow(SlashBladeDefinition.REGISTRY_KEY)
//                        .getOptional(ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, this.outputBlade));
//                if (def.isPresent()) {
//                    return def.get().getBlade();
//                } else {
//                    return new ItemStack(SBItems.slashblade);
//                }
//            } catch (Exception e) {
//                return new ItemStack(SBItems.slashblade);
//            }
//        }
//        return result;
//    }
//}
