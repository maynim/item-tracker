package ru.maynim.tasklist;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Manages multiple tracker lists and handles persistence
 */
public class TrackerListManager {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("itemtracker-lists.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final List<TrackerList> lists = new ArrayList<>();
    private static TrackerList activeList = null;

    /**
     * Initializes with default lists if config doesn't exist
     */
    public static void initialize() {
        if (Files.exists(CONFIG_PATH)) {
            load();
        } else {
            createDefaultLists();
            save();
        }
    }

    /**
     * Creates default example lists
     */
    private static void createDefaultLists() {
        // Список 1: Начало игры
        TrackerList starter = new TrackerList("Начало игры");
        starter.addGoal(new ItemGoal("minecraft:oak_planks", 8, "Дубовые доски"));
        starter.addGoal(new ItemGoal("minecraft:stick", 4, "Палки"));
        starter.addGoal(new ItemGoal("minecraft:crafting_table", 1, "Верстак"));
        lists.add(starter);

        // Список 2: Строительство
        TrackerList building = new TrackerList("Строительство дома");
        building.addGoal(new ItemGoal("minecraft:oak_planks", 64, "Доски"));
        building.addGoal(new ItemGoal("minecraft:glass", 16, "Стекло"));
        building.addGoal(new ItemGoal("minecraft:torch", 20, "Факелы"));
        lists.add(building);

        // Список 3: Майнинг
        TrackerList mining = new TrackerList("Майнинг");
        mining.addGoal(new ItemGoal("minecraft:cobblestone", 64, "Булыжник"));
        mining.addGoal(new ItemGoal("minecraft:coal", 32, "Уголь"));
        mining.addGoal(new ItemGoal("minecraft:iron_ore", 10, "Железная руда"));
        lists.add(mining);

        // Активируем первый список
        if (!lists.isEmpty()) {
            setActiveList(lists.get(0));
        }
    }

    /**
     * Gets all tracker lists
     */
    public static List<TrackerList> getLists() {
        return new ArrayList<>(lists);
    }

    /**
     * Gets the currently active list
     */
    public static TrackerList getActiveList() {
        return activeList;
    }

    /**
     * Sets the active list
     */
    public static void setActiveList(TrackerList list) {
        // Deactivate all lists
        for (TrackerList l : lists) {
            l.setActive(false);
        }

        // Activate the selected list
        if (list != null) {
            list.setActive(true);
            activeList = list;
        } else {
            activeList = null;
        }

        save();
    }

    /**
     * Adds a new list
     */
    public static void addList(TrackerList list) {
        lists.add(list);
        save();
    }

    /**
     * Removes a list
     */
    public static void removeList(TrackerList list) {
        if (list == activeList) {
            activeList = null;
        }
        lists.remove(list);
        save();
    }

    /**
     * Removes a list by index
     */
    public static void removeList(int index) {
        if (index >= 0 && index < lists.size()) {
            TrackerList list = lists.get(index);
            removeList(list);
        }
    }

    /**
     * Saves all lists to config file
     */
    public static void save() {
        try {
            JsonObject root = new JsonObject();
            JsonArray listsArray = new JsonArray();

            for (TrackerList list : lists) {
                listsArray.add(list.toJson());
            }

            root.add("lists", listsArray);

            Files.writeString(CONFIG_PATH, GSON.toJson(root));
            TaskList.LOGGER.info("Saved {} tracker lists", lists.size());

        } catch (IOException e) {
            TaskList.LOGGER.error("Failed to save tracker lists", e);
        }
    }

    /**
     * Loads all lists from config file
     */
    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                return;
            }

            String json = Files.readString(CONFIG_PATH);
            JsonObject root = GSON.fromJson(json, JsonObject.class);

            lists.clear();
            activeList = null;

            if (root.has("lists")) {
                JsonArray listsArray = root.getAsJsonArray("lists");

                for (JsonElement element : listsArray) {
                    JsonObject listObj = element.getAsJsonObject();
                    TrackerList list = TrackerList.fromJson(listObj);
                    lists.add(list);

                    if (list.isActive()) {
                        activeList = list;
                    }
                }
            }

            TaskList.LOGGER.info("Loaded {} tracker lists", lists.size());

        } catch (Exception e) {
            TaskList.LOGGER.error("Failed to load tracker lists", e);
            createDefaultLists();
        }
    }

    /**
     * Checks if a list name already exists
     */
    public static boolean listNameExists(String name) {
        return lists.stream().anyMatch(l -> l.getName().equalsIgnoreCase(name));
    }
}