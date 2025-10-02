package ru.maynim.tasklist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskListClient implements ClientModInitializer {

    public static final String MOD_ID = "task-list"; // Используем ID из вашего gradle.properties
    private static final Identifier TASK_LIST_HUD_ID = Identifier.of(MOD_ID, "task_list_hud");


    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
	public void onInitializeClient() {
        // 1. Загружаем прогресс при запуске клиента.
        ProgressManager.loadProgress();

        // 2. Регистрируем наши списки.
        ModLists.registerLists();
        LOGGER.info("Зарегистрировано клиентских списков: " + ModLists.LISTS.size());

        // 3. Регистрируем событие тика для отслеживания инвентаря.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                PlayerInventoryTracker.onClientTick();
            }
        });

        // 4. (НОВОЕ) Регистрируем событие для сохранения прогресса при выходе из игры.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ProgressManager.saveProgress();
        });

        // Регистрируем наш метод для отрисовки HUD.
        // Он будет вызываться каждый кадр после отрисовки основного HUD'а игры.
        // --- ИСПРАВЛЕННАЯ РЕГИСТРАЦИЯ СОГЛАСНО ДОКУМЕНТАЦИИ ---
        // 1. Мы выбираем "зону" экрана. Нам нужна правая сторона, центр по вертикали.
        // 2. Мы создаем HudElementEntry, который "заворачивает" наш HudElement и его ID.
        // 3. Мы регистрируем эту запись в выбранной зоне.
        HudElementRegistry.addLast(TASK_LIST_HUD_ID, new TaskListHudElement());
	}
}