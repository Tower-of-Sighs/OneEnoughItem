package com.mafuyu404.oneenoughitem.init;

import com.iafenvoy.jupiter.config.entry.BaseEntry;
import com.iafenvoy.jupiter.config.type.ConfigType;
import com.iafenvoy.jupiter.config.type.SingleConfigType;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import com.mojang.serialization.Codec;

public class DirRuleEntry extends BaseEntry<Config.DirRule> {
    private final Codec<Config.DirRule> codec;

    public DirRuleEntry(String nameKey, Config.DirRule defaultValue, Codec<Config.DirRule> codec) {
        super(nameKey, defaultValue);
        this.codec = codec;
    }

    @Override
    public ConfigType<Config.DirRule> getType() {
        return new SingleConfigType<>();
    }

    @Override
    public IConfigEntry<Config.DirRule> newInstance() {
        return new DirRuleEntry(this.nameKey, this.defaultValue, this.codec)
                .visible(this.visible)
                .json(this.jsonKey);
    }

    public Codec<Config.DirRule> getCodec() {
        return codec;
    }
}
