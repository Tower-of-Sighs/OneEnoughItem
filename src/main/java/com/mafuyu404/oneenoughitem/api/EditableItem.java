package com.mafuyu404.oneenoughitem.api;

import net.minecraft.world.food.FoodProperties;

import javax.annotation.Nullable;

public interface EditableItem {
    void setFoodProperties(@Nullable FoodProperties foodProperties);
}
