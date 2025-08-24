package com.mafuyu404.oneenoughitem.client;


public final class ClientContext {
    private static final ThreadLocal<Boolean> IS_BUILDING = ThreadLocal.withInitial(() -> false);

    public static void beginBuilding() {
        IS_BUILDING.set(true);
    }

    public static void endBuilding() {
        IS_BUILDING.set(false);
    }

    public static boolean isBuilding() {
        return IS_BUILDING.get();
    }
}