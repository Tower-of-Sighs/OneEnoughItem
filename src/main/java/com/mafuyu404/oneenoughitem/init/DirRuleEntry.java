package com.mafuyu404.oneenoughitem.init;

import com.iafenvoy.jupiter.config.entry.BaseEntry;
import com.iafenvoy.jupiter.config.type.ConfigType;
import com.iafenvoy.jupiter.config.type.SingleConfigType;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import com.mojang.serialization.Codec;

public class DirRuleEntry extends BaseEntry<ModConfig.DirRule> {
    private final Codec<ModConfig.DirRule> codec;

    public DirRuleEntry(String nameKey, ModConfig.DirRule defaultValue, Codec<ModConfig.DirRule> codec) {
        super(nameKey, defaultValue);
        this.codec = codec;
    }

    @Override
    public ConfigType<ModConfig.DirRule> getType() {
        return new SingleConfigType<>();
    }

    @Override
    public IConfigEntry<ModConfig.DirRule> newInstance() {
        return new DirRuleEntry(this.nameKey, this.defaultValue, this.codec)
                .visible(this.visible)
                .json(this.jsonKey);
    }

    public Codec<ModConfig.DirRule> getCodec() {
        return codec;
    }
}
