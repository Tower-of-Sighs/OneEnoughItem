package com.mafuyu404.oneenoughitem.data;

import com.mafuyu404.oelib.api.data.DataDriven;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@DataDriven(
        modid = "oei",
        folder = "replacements",
        syncToClient = true,
        validator = ReplacementValidator.class,
        supportArray = true
)
public record Replacements(
        List<String> matchItems,
        String resultItems,
        Optional<Rules> rules
) {
    public static final Codec<Replacements> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.listOf().fieldOf("matchItems").forGetter(Replacements::matchItems),
                    Codec.STRING.fieldOf("resultItems").forGetter(Replacements::resultItems),
                    Rules.CODEC.optionalFieldOf("rules").forGetter(Replacements::rules)
            ).apply(instance, Replacements::new)
    );

    public enum ProcessingMode {
        REPLACE, RETAIN;

        public static final Codec<ProcessingMode> CODEC = Codec.STRING.flatXmap(
                name -> {
                    try {
                        return DataResult.success(
                                ProcessingMode.valueOf(name.toUpperCase())
                        );
                    } catch (IllegalArgumentException e) {
                        return DataResult.error(
                                () -> "Invalid processing mode: " + name + ". Must be 'replace' or 'retain'"
                        );
                    }
                },
                mode -> DataResult.success(mode.name().toLowerCase())
        );
    }

    public record Rules(
            Optional<Map<String, ProcessingMode>> data,
            Optional<Map<String, ProcessingMode>> tag
    ) {
        public static final Codec<Rules> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.unboundedMap(Codec.STRING, ProcessingMode.CODEC)
                                .optionalFieldOf("data")
                                .forGetter(Rules::data),
                        Codec.unboundedMap(Codec.STRING, ProcessingMode.CODEC)
                                .optionalFieldOf("tag")
                                .forGetter(Rules::tag)
                ).apply(instance, Rules::new)
        );
    }
}