package com.mafuyu404.oneenoughitem.init;

import com.iafenvoy.jupiter.config.entry.ListBaseEntry;
import com.iafenvoy.jupiter.config.type.ConfigType;
import com.iafenvoy.jupiter.config.type.ListConfigType;
import com.iafenvoy.jupiter.config.type.SingleConfigType;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import com.mojang.serialization.Codec;

import java.util.List;

public class ListDirRuleEntry extends ListBaseEntry<Config.DirRule> {
    public ListDirRuleEntry(String nameKey, List<Config.DirRule> defaultValue) {
        super(nameKey, defaultValue);
    }

    @Override
    public Codec<Config.DirRule> getValueCodec() {
        return Config.DirRule.CODEC;
    }

    @Override
    public IConfigEntry<Config.DirRule> newSingleInstance(Config.DirRule value, int index, Runnable reload) {
        return new DirRuleEntry(this.nameKey, value, Config.DirRule.CODEC) {
            @Override
            public void reset() {
                ListDirRuleEntry.this.getValue().remove(index);
                reload.run();
            }

            @Override
            public void setValue(Config.DirRule newValue) {
                super.setValue(newValue);
                ListDirRuleEntry.this.getValue().set(index, newValue);
            }
        };
    }

    @Override
    public Config.DirRule newValue() {
        return new Config.DirRule("recipes", List.of("item", "id", "result"), true);
    }

    @Override
    public ConfigType<List<Config.DirRule>> getType() {
        return new ListConfigType<>(new SingleConfigType<>());
    }

    @Override
    public IConfigEntry<List<Config.DirRule>> newInstance() {
        return new ListDirRuleEntry(this.nameKey, this.defaultValue)
                .visible(this.visible)
                .json(this.jsonKey);
    }
}
