package com.mafuyu404.oneenoughitem.init;

import java.util.function.Supplier;

public class ReplacementControl {
    private static final ThreadLocal<Boolean> SKIP_REPLACEMENT = ThreadLocal.withInitial(() -> false);
    
    public static void setSkipReplacement(boolean skip) {
        SKIP_REPLACEMENT.set(skip);
    }
    
    public static boolean shouldSkipReplacement() {
        return SKIP_REPLACEMENT.get();
    }
    
    public static void clearSkipReplacement() {
        SKIP_REPLACEMENT.remove();
    }
    
    public static <T> T withSkipReplacement(Supplier<T> supplier) {
        setSkipReplacement(true);
        try {
            return supplier.get();
        } finally {
            clearSkipReplacement();
        }
    }
    
    public static void withSkipReplacement(Runnable runnable) {
        setSkipReplacement(true);
        try {
            runnable.run();
        } finally {
            clearSkipReplacement();
        }
    }
}