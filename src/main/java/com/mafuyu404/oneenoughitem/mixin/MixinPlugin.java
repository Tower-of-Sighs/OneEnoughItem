package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.init.Utils;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

import static com.ibm.icu.impl.ClassLoaderUtil.getClassLoader;

public class MixinPlugin implements IMixinConfigPlugin {
    private Boolean isOldVersion = null;

    private boolean isOldMC() {
        if (isOldVersion == null) {
            try {
                String ver = net.minecraftforge.fml.loading.FMLLoader.versionInfo().mcVersion();
                isOldVersion = ver.startsWith("1.18") || ver.startsWith("1.17");
            } catch (Exception e) {
                // 如果获取版本失败，默认为新版本
                isOldVersion = false;
            }
        }
        return isOldVersion;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean whenItemStackMixin = mixinClassName.equals("com.mafuyu404.oneenoughitem.mixin.ItemStackMixin");
        boolean whenOldItemStackMixin = mixinClassName.equals("com.mafuyu404.oneenoughitem.mixin.OldItemStackMixin");
        boolean isOldVer = isOldMC();

        if (whenItemStackMixin && isOldVer) {
            return false;
        }
        if (whenOldItemStackMixin && !isOldVer) {
            return false;
        }
        return true;
    }
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    // 其他方法留空或默认实现
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
