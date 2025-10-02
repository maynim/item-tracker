package ru.maynim.tasklist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ProgressManager {
    // Создаем "красивый" GSON объект для форматирования JSON файла
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Определяем путь к нашему файлу сохранения.
    // Он будет лежать в ".minecraft/config/task-list/progress.json"
    private static final Path SAVE_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(TaskListClient.MOD_ID)
            .resolve("progress.json");

    /**
     * Сохраняет текущий прогресс (множество ID завершенных списков) в файл.
     */
    public static void saveProgress() {
        try {
            // Получаем данные для сохранения из нашего трекера
            Set<String> completed = PlayerInventoryTracker.getCompletedLists();

            // Убеждаемся, что родительская папка (config/task-list) существует
            Files.createDirectories(SAVE_FILE.getParent());

            // Открываем файл для записи
            try (BufferedWriter writer = Files.newBufferedWriter(SAVE_FILE)) {
                // Превращаем наше множество в JSON строку и записываем в файл
                GSON.toJson(completed, writer);
                TaskListClient.LOGGER.info("Прогресс успешно сохранен.");
            }
        } catch (IOException e) {
            TaskListClient.LOGGER.error("Не удалось сохранить прогресс!", e);
        }
    }

    /**
     * Загружает прогресс из файла и передает его в трекер.
     */
    public static void loadProgress() {
        // Если файл еще не существует, просто выходим
        if (!Files.exists(SAVE_FILE)) {
            return;
        }

        try {
            // Открываем файл для чтения
            try (BufferedReader reader = Files.newBufferedReader(SAVE_FILE)) {
                // Определяем тип данных, который мы ожидаем из JSON (множество строк)
                Type type = new TypeToken<HashSet<String>>() {}.getType();
                // Читаем и преобразуем JSON в объект Set<String>
                Set<String> loadedLists = GSON.fromJson(reader, type);

                if (loadedLists != null) {
                    // Передаем загруженные данные в наш трекер
                    PlayerInventoryTracker.setCompletedLists(loadedLists);
                    TaskListClient.LOGGER.info("Прогресс успешно загружен: " + loadedLists.size() + " списков завершено.");
                }
            }
        } catch (IOException e) {
            TaskListClient.LOGGER.error("Не удалось загрузить прогресс!", e);
        }
    }
}
