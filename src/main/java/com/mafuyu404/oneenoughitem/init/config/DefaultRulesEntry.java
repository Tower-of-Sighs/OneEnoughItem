package com.mafuyu404.oneenoughitem.init.config;

import com.iafenvoy.jupiter.config.entry.BaseEntry;
import com.iafenvoy.jupiter.config.type.ConfigType;
import com.iafenvoy.jupiter.config.type.SingleConfigType;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import com.mojang.serialization.Codec;

public class DefaultRulesEntry extends BaseEntry<Config.DefaultRules> {
    private final Codec<Config.DefaultRules> codec;

    public DefaultRulesEntry(String nameKey, Config.DefaultRules defaultValue, Codec<Config.DefaultRules> codec) {
        super(nameKey, defaultValue);
        this.codec = codec;
    }

    @Override
    public ConfigType<Config.DefaultRules> getType() {
        return new SingleConfigType<>();
    }

    @Override
    public IConfigEntry<Config.DefaultRules> newInstance() {
        return new DefaultRulesEntry(this.nameKey, this.defaultValue, this.codec)
                .visible(this.visible)
                .json(this.jsonKey);
    }

    public Codec<Config.DefaultRules> getCodec() {
        return codec;
    }
}