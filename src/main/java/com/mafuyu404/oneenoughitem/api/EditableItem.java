package com.mafuyu404.oneenoughitem.api;

import net.minecraft.world.food.FoodProperties;
import org.jetbrains.annotations.Nullable;

public interface EditableItem {
    void setFoodProperties(@Nullable FoodProperties foodProperties);
}
