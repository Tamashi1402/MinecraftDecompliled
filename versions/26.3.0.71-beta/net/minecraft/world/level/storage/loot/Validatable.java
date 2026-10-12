package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;

public interface Validatable {
    void validate(ValidationContext context);

    static void validate(ValidationContext context, String name, Validatable v) {
        v.validate(context.forField(name));
    }

    static void validate(ValidationContext context, String name, Optional<? extends Validatable> optional) {
        optional.ifPresent(v -> v.validate(context.forField(name)));
    }

    static void validateHolder(ValidationContext context, Holder<? extends Validatable> holder) {
        ValidationContext elementContext;
        if (holder instanceof Holder.Reference<?> reference) {
            ResourceKey<?> id = reference.key();
            if (context.hasVisitedElement(id)) {
                context.reportProblem(new ValidationContext.RecursiveElementReferenceProblem(id));
                return;
            }

            elementContext = context.enterElement(new ProblemReporter.ElementReferencePathElement(id), id);
        } else {
            elementContext = context;
        }

        holder.value().validate(elementContext);
    }

    static void validateHolder(ValidationContext context, String name, Holder<? extends Validatable> holder) {
        validateHolder(context.forField(name), holder);
    }

    static void validateHolder(ValidationContext context, String name, Optional<? extends Holder<? extends Validatable>> optional) {
        optional.ifPresent(v -> validateHolder(context, name, (Holder<? extends Validatable>)v));
    }

    static void validateHolder(ValidationContext context, String name, List<? extends Holder<? extends Validatable>> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).value().validate(context.forIndexedField(name, i));
        }
    }

    static void validateHolderSet(ValidationContext context, String name, HolderSet<? extends Validatable> holderSet, int minSize) {
        ValidationContext namedContext = context.forField(name);
        validateHolderSet(namedContext, holderSet);
        if (holderSet.size() < minSize) {
            namedContext.reportProblem(new ValidationContext.MinBoundsProblem(minSize));
        }
    }

    static void validateHolderSet(ValidationContext context, String name, HolderSet<? extends Validatable> holderSet) {
        validateHolderSet(context.forField(name), holderSet);
    }

    private static void validateHolderSet(ValidationContext context, HolderSet<? extends Validatable> holderSet) {
        if (holderSet.isBound()) {
            ValidationContext collectionContext;
            if (holderSet instanceof HolderSet.Named<?> reference) {
                TagKey<?> id = reference.key();
                if (context.hasVisitedTag(id)) {
                    context.reportProblem(new ValidationContext.RecursiveTagReferenceProblem(id));
                    return;
                }

                collectionContext = context.enterTag(new ProblemReporter.CollectionReferencePathElement(id), id);
            } else {
                collectionContext = context;
            }

            for (int i = 0; i < holderSet.size(); i++) {
                validateHolder(collectionContext.forChild(new ProblemReporter.IndexedPathElement(i)), holderSet.get(i));
            }
        }
    }

    static void validate(ValidationContext context, String name, List<? extends Validatable> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).validate(context.forIndexedField(name, i));
        }
    }

    static void validate(ValidationContext context, List<? extends Validatable> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).validate(context.forChild(new ProblemReporter.IndexedPathElement(i)));
        }
    }

    static <T extends Validatable> Function<T, DataResult<T>> validatorForContext(ContextKeySet params) {
        return v -> {
            ProblemReporter.Collector problemCollector = new ProblemReporter.Collector();
            ValidationContext validationContext = new ValidationContext(problemCollector, params);
            v.validate(validationContext);
            return !problemCollector.isEmpty() ? DataResult.error(() -> "Validation error: " + problemCollector.getReport()) : DataResult.success(v);
        };
    }

    static <T extends Validatable> Function<List<T>, DataResult<List<T>>> listValidatorForContext(ContextKeySet params) {
        return v -> {
            ProblemReporter.Collector problemCollector = new ProblemReporter.Collector();
            ValidationContext validationContext = new ValidationContext(problemCollector, params);
            validate(validationContext, v);
            return !problemCollector.isEmpty() ? DataResult.error(() -> "Validation error: " + problemCollector.getReport()) : DataResult.success(v);
        };
    }
}
