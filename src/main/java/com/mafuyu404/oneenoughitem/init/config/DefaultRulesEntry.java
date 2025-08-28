package com.mafuyu404.oneenoughitem.init.config;

import com.iafenvoy.jupiter.config.entry.BaseEntry;
import com.iafenvoy.jupiter.config.type.ConfigType;
import com.iafenvoy.jupiter.config.type.SingleConfigType;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import com.mojang.serialization.Codec;

public class DefaultRulesEntry extends BaseEntry<ModConfig.DefaultRules> {
    private final Codec<ModConfig.DefaultRules> codec;

    public DefaultRulesEntry(String nameKey, ModConfig.DefaultRules defaultValue, Codec<ModConfig.DefaultRules> codec) {
        super(nameKey, defaultValue);
        this.codec = codec;
    }

    @Override
    public ConfigType<ModConfig.DefaultRules> getType() {
        return new SingleConfigType<>();
    }

    @Override
    public IConfigEntry<ModConfig.DefaultRules> newInstance() {
        return new DefaultRulesEntry(this.nameKey, this.defaultValue, this.codec)
                .visible(this.visible)
                .json(this.jsonKey);
    }

    public Codec<ModConfig.DefaultRules> getCodec() {
        return codec;
    }
}