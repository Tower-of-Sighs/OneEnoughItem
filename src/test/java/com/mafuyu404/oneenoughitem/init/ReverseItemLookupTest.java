package com.mafuyu404.oneenoughitem.init;

import java.util.*;
import java.util.concurrent.*;

/**
 * Standalone regression checks; requires only Java 17, without a Minecraft runtime.
 * Compile this file together with ReverseItemLookup.java, then run this main class.
 */
public final class ReverseItemLookupTest {
    private static Set<String> scan(ConcurrentHashMap<String, String> map, String target) {
        Set<String> result = new HashSet<>();
        map.forEach((from, to) -> { if (to.equals(target)) result.add(from); });
        return result;
    }

    private static void check(ConcurrentHashMap<String, String> map, String target) {
        if (!scan(map, target).equals(new HashSet<>(ReverseItemLookup.lookup(map, target))))
            throw new AssertionError("Lookup mismatch for " + target);
    }

    public static void main(String[] args) throws Exception {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        ReverseItemLookup.put(map, "source:a", "target:a");
        check(map, "target:a");
        ReverseItemLookup.put(map, "source:a", "target:b");
        check(map, "target:a");
        check(map, "target:b");
        ReverseItemLookup.put(map, "source:b", "target:b");
        check(map, "target:b");
        ReverseItemLookup.remove(map, "source:a");
        check(map, "target:b");
        ReverseItemLookup.lookup(map, "target:b").clear();
        check(map, "target:b");
        ReverseItemLookup.clear(map);
        check(map, "target:b");

        Random random = new Random(42);
        for (int i = 0; i < 10000; i++) {
            String key = "source:" + random.nextInt(200);
            switch (random.nextInt(12)) {
                case 0 -> ReverseItemLookup.clear(map);
                case 1, 2 -> ReverseItemLookup.remove(map, key);
                default -> ReverseItemLookup.put(map, key, "target:" + random.nextInt(20));
            }
            check(map, "target:" + random.nextInt(20));
            check(map, "missing");
            check(map, null);
        }
        ConcurrentHashMap<String, String> other = new ConcurrentHashMap<>();
        ReverseItemLookup.put(other, "different", "target:a");
        check(other, "target:a");
        check(map, "target:a");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < 4; thread++) {
                final int id = thread;
                futures.add(pool.submit(() -> {
                    for (int i = 0; i < 1000; i++) {
                        String key = "thread:" + id + ":" + i;
                        ReverseItemLookup.put(map, key, "concurrent");
                        if (!ReverseItemLookup.lookup(map, "concurrent").contains(key))
                            throw new AssertionError("Missing inserted mapping");
                        ReverseItemLookup.remove(map, key);
                    }
                }));
            }
            for (Future<?> future : futures) future.get();
        } finally {
            pool.shutdownNow();
        }
        check(map, "concurrent");
        System.out.println("PASS: remaps, duplicate targets, removals, clears, defensive copies,"
                + " randomized updates, null/missing queries, map identity, concurrent mutation.");
    }
}
