package com.mafuyu404.oneenoughitem.init;

import com.iafenvoy.jupiter.config.entry.ListBaseEntry;
import com.iafenvoy.jupiter.config.type.ConfigType;
import com.iafenvoy.jupiter.config.type.ListConfigType;
import com.iafenvoy.jupiter.config.type.SingleConfigType;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import com.mojang.serialization.Codec;

import java.util.List;

public class ListDirRuleEntry extends ListBaseEntry<ModConfig.DirRule> {
    public ListDirRuleEntry(String nameKey, List<ModConfig.DirRule> defaultValue) {
        super(nameKey, defaultValue);
    }

    @Override
    public Codec<ModConfig.DirRule> getValueCodec() {
        return ModConfig.DirRule.CODEC;
    }

    @Override
    public IConfigEntry<ModConfig.DirRule> newSingleInstance(ModConfig.DirRule value, int index, Runnable reload) {
        return new DirRuleEntry(this.nameKey, value, ModConfig.DirRule.CODEC) {
            @Override
            public void reset() {
                ListDirRuleEntry.this.getValue().remove(index);
                reload.run();
            }

            @Override
            public void setValue(ModConfig.DirRule newValue) {
                super.setValue(newValue);
                ListDirRuleEntry.this.getValue().set(index, newValue);
            }
        };
    }

    @Override
    public ModConfig.DirRule newValue() {
        return new ModConfig.DirRule("recipes", List.of("item", "id", "result"), true);
    }

    @Override
    public ConfigType<List<ModConfig.DirRule>> getType() {
        return new ListConfigType<>(new SingleConfigType<>());
    }

    @Override
    public IConfigEntry<List<ModConfig.DirRule>> newInstance() {
        return new ListDirRuleEntry(this.nameKey, this.defaultValue)
                .visible(this.visible)
                .json(this.jsonKey);
    }
}
