package ru.maynim.tasklist;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles adding a button to container screens for tracking global counts
 */
public class ContainerButtonHandler {

    public static void register() {
        // Добавляем кнопку в экраны контейнеров
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (isContainerScreen(screen)) {
                addContainerTrackingButton((HandledScreen<?>) screen, scaledWidth, scaledHeight);
            }
        });
    }

    /**
     * Проверяет, является ли экран контейнером (сундук, бочка, шалкер и т.д.)
     */
    private static boolean isContainerScreen(Screen screen) {
        // Исключаем инвентарь игрока и креативный режим
        if (screen instanceof InventoryScreen || screen instanceof CreativeInventoryScreen) {
            return false;
        }

        // Все остальные HandledScreen - это контейнеры
        return screen instanceof GenericContainerScreen || // Сундуки, бочки
               screen instanceof ShulkerBoxScreen ||        // Шалкер боксы
               screen instanceof HopperScreen ||            // Воронки
               screen instanceof Generic3x3ContainerScreen || // Раздатчики, дропперы
               screen instanceof HandledScreen;             // Другие контейнеры
    }

    private static void addContainerTrackingButton(HandledScreen<?> screen, int screenWidth, int screenHeight) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;

        // Получаем позицию контейнера (если доступна)
        BlockPos containerPos = null;
        String dimension = mc.world.getRegistryKey().getValue().toString();

        // Вычисляем координаты кнопки
        int x = (screenWidth - 176) / 2;
        int y = (screenHeight - 166) / 2;

        int buttonX = x + 176 + 5;
        int buttonY = y + 4;

        // Определяем, отслеживается ли уже этот контейнер
        boolean isTracked = containerPos != null &&
                            GlobalCounterManager.isTracked(containerPos, dimension);

        // Создаем кнопку
        ButtonWidget trackButton = ButtonWidget.builder(
                        Text.literal(isTracked ? "✓" : "+"),
                        button -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client == null || client.player == null || client.world == null) return;

                            // Собираем содержимое контейнера
                            List<ItemStack> contents = new ArrayList<>();
                            for (Slot slot : screen.getScreenHandler().slots) {
                                // Пропускаем слоты инвентаря игрока
                                if (slot.inventory == client.player.getInventory()) continue;

                                ItemStack stack = slot.getStack();
                                if (!stack.isEmpty()) {
                                    contents.add(stack.copy());
                                }
                            }

                            // Получаем позицию контейнера (пока используем примерную позицию)
                            // В реальности нужно получить реальную позицию из контекста
                            BlockPos pos = client.player.getBlockPos().add(1, 0, 0);
                            String dim = client.world.getRegistryKey().getValue().toString();

                            // Обновляем или удаляем отслеживание
                            if (GlobalCounterManager.isTracked(pos, dim)) {
                                GlobalCounterManager.removeContainer(pos, dim);
                                button.setMessage(Text.literal("+"));
                                client.player.sendMessage(
                                    Text.literal("§eКонтейнер удален из глобального подсчета"),
                                    true
                                );
                            } else {
                                GlobalCounterManager.updateContainer(pos, dim, contents);
                                button.setMessage(Text.literal("✓"));
                                client.player.sendMessage(
                                    Text.literal("§aКонтейнер добавлен в глобальный подсчет (" +
                                                GlobalCounterManager.getContainerCount() + " всего)"),
                                    true
                                );
                            }
                        })
                .dimensions(buttonX, buttonY + 25, 20, 20)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                    Text.literal("Учесть в глобальном счетчике")))
                .build();

        // Добавляем кнопку на экран
        Screens.getButtons(screen).add(trackButton);
    }
}
