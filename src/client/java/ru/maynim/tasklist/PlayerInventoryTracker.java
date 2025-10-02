package ru.maynim.tasklist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerInventoryTracker {

    private static Map<Item, Integer> trackedInventory = new HashMap<>();
    // Убираем final и инициализацию, так как данные будут приходить из файла
    private static Set<String> completedLists;

    /**
     * Устанавливает множество завершенных списков (вызывается при загрузке).
     */
    public static void setCompletedLists(Set<String> loadedLists) {
        completedLists = new HashSet<>(loadedLists); // Создаем копию на всякий случай
    }

    /**
     * Возвращает текущее множество завершенных списков (вызывается при сохранении).
     */
    public static Set<String> getCompletedLists() {
        // Если completedLists еще не был инициализирован, возвращаем пустой набор
        if (completedLists == null) {
            return Collections.emptySet();
        }
        return completedLists;
    }

    public static void onClientTick() {
        // --- ВАЖНОЕ ИЗМЕНЕНИЕ ---
        // Если данные еще не загружены, ничего не делаем.
        // Это предотвращает ошибки при первом запуске мира.
        if (completedLists == null) {
            completedLists = new HashSet<>(); // Инициализируем, если загрузка не удалась
        }

        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        Map<Item, Integer> currentSnapshot = createInventorySnapshot(player);

        if (!currentSnapshot.equals(trackedInventory)) {
            checkInventory(player);
            trackedInventory = currentSnapshot;
        }
    }

    // ... (остальные методы: createInventorySnapshot, checkInventory, countItems)
    // ... (остаются без изменений, просто скопируйте их сюда)
    private static Map<Item, Integer> createInventorySnapshot(PlayerEntity player) {
        Map<Item, Integer> snapshot = new HashMap<>();
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (!stack.isEmpty()) {
                snapshot.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return snapshot;
    }

    private static void checkInventory(PlayerEntity player) {
        for (Map.Entry<String, CollectionList> listEntry : ModLists.LISTS.entrySet()) {
            String listId = listEntry.getKey();
            CollectionList list = listEntry.getValue();

            if (completedLists.contains(listId)) {
                continue;
            }

            boolean isListComplete = true;
            for (Map.Entry<Item, Integer> requiredItemEntry : list.getRequiredItems().entrySet()) {
                Item requiredItem = requiredItemEntry.getKey();
                int requiredCount = requiredItemEntry.getValue();

                int countInInventory = countItems(player, requiredItem);

                if (countInInventory < requiredCount) {
                    isListComplete = false;
                    break;
                }
            }

            if (isListComplete) {
                player.sendMessage(Text.literal("Вы собрали все предметы для списка: " + list.getName()), false);
                completedLists.add(listId);
            }
        }
    }

    public static int countItems(PlayerEntity player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
