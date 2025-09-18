package com.mafuyu404.oneenoughitem.data;

import com.mafuyu404.oelib.api.data.DataValidator;

import java.util.Objects;
import java.util.function.Function;

public final class ValidationStreams {
    private ValidationStreams() {}

    public static final class Accumulator {
        private final boolean hasValid;
        private final boolean hasDeferred;
        private final int validCount;
        private final String failureMessage; // 非 null 表示硬失败

        private Accumulator(boolean hasValid, boolean hasDeferred, int validCount, String failureMessage) {
            this.hasValid = hasValid;
            this.hasDeferred = hasDeferred;
            this.validCount = validCount;
            this.failureMessage = failureMessage;
        }

        public static Accumulator identity()   { return new Accumulator(false, false, 0, null); }
        public static Accumulator valid(int c) { return new Accumulator(true, false, Math.max(0, c), null); }
        public static Accumulator deferred()   { return new Accumulator(false, true, 0, null); }
        public static Accumulator invalid()    { return identity(); }
        public static Accumulator failure(String msg) { return new Accumulator(false, false, 0, Objects.requireNonNull(msg)); }

        public Accumulator combine(Accumulator other) {
            if (this.failureMessage != null) return this;
            if (other.failureMessage != null) return other;
            return new Accumulator(
                    this.hasValid   || other.hasValid,
                    this.hasDeferred|| other.hasDeferred,
                    this.validCount + other.validCount,
                    null
            );
        }

        public Accumulator map(Function<Accumulator, Accumulator> f) {
            return f.apply(this);
        }

        public Accumulator flatCombine(Function<Accumulator, Accumulator> f) {
            if (failureMessage != null) return this;
            return f.apply(this);
        }

        public DataValidator.ValidationResult toResult(String deferredMessage, String noValidMessage) {
            if (failureMessage != null) return DataValidator.ValidationResult.failure(failureMessage);
            if (hasValid) return DataValidator.ValidationResult.success();
            if (hasDeferred) return DataValidator.ValidationResult.deferred(deferredMessage);
            return DataValidator.ValidationResult.failure(noValidMessage);
        }
    }
}
