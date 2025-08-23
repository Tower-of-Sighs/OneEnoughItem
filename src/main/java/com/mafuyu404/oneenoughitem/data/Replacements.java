package com.mafuyu404.oneenoughitem.data;

import com.mafuyu404.oelib.api.data.DataDriven;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

@DataDriven(
        modid = "oei",
        folder = "replacements",
        syncToClient = true,
        enableCache = false,
        validator = ReplacementValidator.class,
        supportArray = true
)
public record Replacements(List<String> matchItems, String resultItems) {
    public static final Codec<Replacements> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.listOf().fieldOf("matchItems").forGetter(Replacements::matchItems),
                    Codec.STRING.fieldOf("resultItems").forGetter(Replacements::resultItems)
            ).apply(instance, Replacements::new)
    );
}
