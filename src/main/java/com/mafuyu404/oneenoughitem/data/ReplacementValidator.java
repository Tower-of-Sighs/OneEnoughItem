package com.mafuyu404.oneenoughitem.data;

import com.mafuyu404.oelib.api.data.DataValidator;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.resources.ResourceLocation;

public class ReplacementValidator implements DataValidator<Replacements> {
    @Override
    public ValidationResult validate(Replacements replacement, ResourceLocation source) {
        // 硬失败检查
        if (Utils.getItemById(replacement.resultItems()) == null) {
            return ValidationResult.failure("Target item '" + replacement.resultItems() + "' does not exist");
        }

        var acc = replacement.matchItems().stream()
                .map(item -> Validators.fromItem(item, source))
                .reduce(ValidationStreams.Accumulator.identity(), ValidationStreams.Accumulator::combine);

        return acc.toResult(
                "Contains unresolved tags, validation deferred until tag system is ready",
                "No valid source items found for target '" + replacement.resultItems() + "'"
        );
    }
}
