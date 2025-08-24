package com.mafuyu404.oneenoughitem.event;


import com.mafuyu404.oelib.fabric.data.DataManager;
import com.mafuyu404.oelib.fabric.event.DataReloadEvent;
import com.mafuyu404.oelib.fabric.event.impl.Events;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModEventHandler {

    private static MinecraftServer SERVER;

    public static void register() {
        Events.on(DataReloadEvent.EVENT)
                .normal()
                .register(ModEventHandler::onDataReload);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SERVER = null);
    }

    public static void onDataReload(Class<?> dataClass, int loadedCount, int invalidCount) {
        if (dataClass == Replacements.class) {
            rebuildReplacementCache("data-reload");
            applyReplacementsToOnlinePlayers();

            Oneenoughitem.LOGGER.info("Replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                    loadedCount, invalidCount);
            Oneenoughitem.LOGGER.info("Recipe JSON rewrite mode (server): {}", String.valueOf(com.mafuyu404.oneenoughitem.init.ModConfig.DATA_REWRITE_MODE.getValue()));

            // 关键：Replacements 已重建，关闭本次重载的覆盖映射
            ReplacementCache.endReloadOverride();
        }
    }


    private static void rebuildReplacementCache(String reason) {
        DataManager<Replacements> manager = DataManager.get(Replacements.class);
        if (manager != null) {
            ReplacementCache.clearCache();

            var replacements = manager.getDataList();
            for (Replacements replacement : replacements) {
                ReplacementCache.putReplacement(replacement);
            }

            Oneenoughitem.LOGGER.debug("Server rebuilt replacement cache (reason: {}) with {} rules", reason, replacements.size());
        } else {
            Oneenoughitem.LOGGER.warn("ServerEventHandler: No replacement data manager found in OELib (reason: {})", reason);
        }
    }

    private static void applyReplacementsToOnlinePlayers() {
        if (SERVER == null) {
            Oneenoughitem.LOGGER.warn("applyReplacementsToOnlinePlayers: SERVER is null, skip updating online players");
            return;
        }

        int playerCount = 0;

        for (ServerPlayer player : SERVER.getPlayerList().getPlayers()) {
            boolean changed = false;

            changed |= rewriteContainerByReplacement(player.getInventory());

            changed |= rewriteContainerByReplacement(player.getEnderChestInventory());

            if (changed) {
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                playerCount++;
            }
        }

        Oneenoughitem.LOGGER.info("applyReplacementsToOnlinePlayers: updated {} players' inventories after data reload", playerCount);
    }

    private static boolean rewriteContainerByReplacement(Container container) {
        boolean changed = false;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack == null || stack.isEmpty()) continue;

            Item item = stack.getItem();
            String originId = Utils.getItemRegistryName(item);
            String targetId = ReplacementCache.matchItem(originId);
            if (targetId == null) continue;

            try {
                if ("minecraft:air".equals(targetId)) {
                    container.setItem(slot, ItemStack.EMPTY);
                    changed = true;
                    Oneenoughitem.LOGGER.debug("Inventory rewrite: {} -> AIR at slot {}", originId, slot);
                } else {
                    Item targetItem = Utils.getItemById(targetId);
                    if (targetItem == null) {
                        Oneenoughitem.LOGGER.warn("Inventory rewrite: target item '{}' not found for origin '{}'", targetId, originId);
                        continue;
                    }

                    CompoundTag tag = new CompoundTag();
                    stack.save(tag);
                    tag.putString("id", targetId);

                    ItemStack newStack = ItemStack.of(tag);
                    if (newStack.isEmpty()) {
                        newStack = new ItemStack(targetItem, stack.getCount());
                    }

                    container.setItem(slot, newStack);
                    changed = true;
                    Oneenoughitem.LOGGER.debug("Inventory rewrite: {} -> {} at slot {}", originId, targetId, slot);
                }
            } catch (Exception e) {
                Oneenoughitem.LOGGER.error("Inventory rewrite failed at slot {}: {} -> {}", slot, originId, targetId, e);
            }
        }

        return changed;
    }
}