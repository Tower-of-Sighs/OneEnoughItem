package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import io.netty.util.internal.ReflectionUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OldMCUtils {
    private static Method getRegistryNameMethod = null;
    private static boolean methodChecked = false;

    @Nullable
    public static String getItemRegistryName(Item item) {
        if (!methodChecked) {
            synchronized (ReflectionUtil.class) {
                if (!methodChecked) {
                    try {
                        getRegistryNameMethod = Item.class.getMethod("getRegistryName");
                    } catch (NoSuchMethodException e) {
                        getRegistryNameMethod = null;
                    }
                    methodChecked = true;
                }
            }
        }
        if (getRegistryNameMethod != null) {
            try {
                ResourceLocation registryName = (ResourceLocation) getRegistryNameMethod.invoke(item);
                return registryName != null ? registryName.toString() : null;
            } catch (IllegalAccessException | InvocationTargetException e) {
                Oneenoughitem.LOGGER.error("ERROR getRegistryName 失败", e);
            }
        }
        return null;
    }
}
