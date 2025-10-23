package ru.maynim.tasklist;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages global item counting across all tracked containers
 */
public class GlobalCounterManager {
    private static final Path CONTAINERS_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("itemtracker-containers.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    // Хранит содержимое контейнеров: позиция -> (предмет -> количество)
    private static final Map<String, Map<Item, Integer>> containerContents = new HashMap<>();

    /**
     * Добавляет или обновляет содержимое контейнера
     */
    public static void updateContainer(BlockPos pos, String dimension, List<ItemStack> contents) {
        String key = getContainerKey(pos, dimension);
        Map<Item, Integer> itemCounts = new HashMap<>();

        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                itemCounts.put(item, itemCounts.getOrDefault(item, 0) + stack.getCount());
            }
        }

        containerContents.put(key, itemCounts);
        save();
    }

    /**
     * Удаляет контейнер из учета
     */
    public static void removeContainer(BlockPos pos, String dimension) {
        String key = getContainerKey(pos, dimension);
        containerContents.remove(key);
        save();
    }

    /**
     * Подсчитывает общее количество предмета во всех контейнерах
     */
    public static int getGlobalCount(Item item) {
        int total = 0;
        for (Map<Item, Integer> container : containerContents.values()) {
            total += container.getOrDefault(item, 0);
        }
        return total;
    }

    /**
     * Очищает все данные о контейнерах
     */
    public static void clearAll() {
        containerContents.clear();
        save();
    }

    /**
     * Получает количество отслеживаемых контейнеров
     */
    public static int getContainerCount() {
        return containerContents.size();
    }

    /**
     * Получает список всех отслеживаемых позиций контейнеров
     */
    public static Set<String> getTrackedContainers() {
        return new HashSet<>(containerContents.keySet());
    }

    /**
     * Проверяет, отслеживается ли контейнер
     */
    public static boolean isTracked(BlockPos pos, String dimension) {
        String key = getContainerKey(pos, dimension);
        return containerContents.containsKey(key);
    }

    /**
     * Создает уникальный ключ для контейнера
     */
    private static String getContainerKey(BlockPos pos, String dimension) {
        return dimension + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /**
     * Сохраняет данные в файл
     */
    public static void save() {
        try {
            JsonObject root = new JsonObject();
            JsonArray containersArray = new JsonArray();

            for (Map.Entry<String, Map<Item, Integer>> entry : containerContents.entrySet()) {
                JsonObject containerObj = new JsonObject();
                containerObj.addProperty("location", entry.getKey());

                JsonArray itemsArray = new JsonArray();
                for (Map.Entry<Item, Integer> itemEntry : entry.getValue().entrySet()) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("item", Registries.ITEM.getId(itemEntry.getKey()).toString());
                    itemObj.addProperty("count", itemEntry.getValue());
                    itemsArray.add(itemObj);
                }
                containerObj.add("items", itemsArray);
                containersArray.add(containerObj);
            }

            root.add("containers", containersArray);
            Files.writeString(CONTAINERS_PATH, GSON.toJson(root));

        } catch (IOException e) {
            TaskList.LOGGER.error("Failed to save container data", e);
        }
    }

    /**
     * Загружает данные из файла
     */
    public static void load() {
        try {
            if (!Files.exists(CONTAINERS_PATH)) {
                return;
            }

            String json = Files.readString(CONTAINERS_PATH);
            JsonObject root = GSON.fromJson(json, JsonObject.class);

            containerContents.clear();

            if (root.has("containers")) {
                JsonArray containersArray = root.getAsJsonArray("containers");

                for (JsonElement element : containersArray) {
                    JsonObject containerObj = element.getAsJsonObject();
                    String location = containerObj.get("location").getAsString();

                    Map<Item, Integer> items = new HashMap<>();
                    if (containerObj.has("items")) {
                        JsonArray itemsArray = containerObj.getAsJsonArray("items");
                        for (JsonElement itemElement : itemsArray) {
                            JsonObject itemObj = itemElement.getAsJsonObject();
                            String itemId = itemObj.get("item").getAsString();
                            int count = itemObj.get("count").getAsInt();

                            Identifier id = Identifier.tryParse(itemId);
                            if (id != null) {
                                Item item = Registries.ITEM.get(id);
                                if (item != null) {
                                    items.put(item, count);
                                }
                            }
                        }
                    }

                    containerContents.put(location, items);
                }
            }

            TaskList.LOGGER.info("Loaded {} tracked containers", containerContents.size());

        } catch (Exception e) {
            TaskList.LOGGER.error("Failed to load container data", e);
        }
    }
}
