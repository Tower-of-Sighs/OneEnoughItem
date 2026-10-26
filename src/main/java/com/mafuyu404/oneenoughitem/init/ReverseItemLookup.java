package com.mafuyu404.oneenoughitem.init;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class ReverseItemLookup {
    private static ConcurrentHashMap<String, String> owner;
    private static Map<String, Set<String>> reverse;

    private ReverseItemLookup() {}

    static synchronized Collection<String> lookup(
            ConcurrentHashMap<String, String> source, String target) {
        if (owner != source || reverse == null) {
            Map<String, Set<String>> rebuilt = new HashMap<>();
            source.forEach((from, to) ->
                    rebuilt.computeIfAbsent(to, ignored -> new HashSet<>()).add(from));
            owner = source;
            reverse = rebuilt;
        }
        Set<String> matches = reverse.get(target);
        // Preserve the caller-owned, mutable result of trackSourceIdOf.
        return matches == null ? new HashSet<>() : new HashSet<>(matches);
    }

    // Serialize mutations with rebuilding so a reload cannot publish a stale index.
    static synchronized String put(
            ConcurrentHashMap<String, String> source, String key, String value) {
        reverse = null;
        return source.put(key, value);
    }

    static synchronized String remove(ConcurrentHashMap<String, String> source, String key) {
        reverse = null;
        return source.remove(key);
    }

    static synchronized void clear(ConcurrentHashMap<String, String> source) {
        reverse = null;
        source.clear();
    }
}
