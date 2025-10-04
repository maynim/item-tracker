package ru.maynim.tasklist;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.maynim.tasklist.gui.CreativeTrackerScreen;

/**
 * Handles adding a button to the inventory screen
 */
public class InventoryButtonHandler {

    public static void register() {
        // Добавляем кнопку в экран инвентаря после его инициализации
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen inventoryScreen) {
                addTrackerButton(inventoryScreen, scaledWidth, scaledHeight);
            }
        });
    }

    private static void addTrackerButton(InventoryScreen screen, int screenWidth, int screenHeight) {
        // Получаем координаты через HandledScreen (родительский класс)
        int x;
        int y;

        x = (screenWidth - 176) / 2;
        y = (screenHeight - 166) / 2;

        int buttonX = x + 176 + 5;
        int buttonY = y + 4;

        // Создаем кнопку
        ButtonWidget trackerButton = ButtonWidget.builder(
                        Text.literal("📋"),  // Иконка списка
                        button -> {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            if (mc != null) {
                                mc.setScreen(new CreativeTrackerScreen(screen));
                            }
                        })
                .dimensions(buttonX, buttonY, 20, 20)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Списки целей")))
                .build();

        // Добавляем кнопку на экран
        Screens.getButtons(screen).add(trackerButton);
    }
}