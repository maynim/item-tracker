package ru.maynim.tasklist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class TaskListClient implements ClientModInitializer {

    // Кэш текущего количества предметов для каждой цели
    private static final Map<ItemGoal, Integer> itemCounts = new HashMap<>();

    // Настройки отображения
    private static final int ITEM_ICON_SIZE = 16;
    private static final int LINE_HEIGHT = 20;
    private static final int PADDING = 5;
    private static final int TEXT_OFFSET = 20;

    @Override
    public void onInitializeClient() {
        TaskList.LOGGER.info("Initializing Item Tracker Client");

        // Инициализация менеджера списков
        TrackerListManager.initialize();

        // Инициализация глобального счетчика
        GlobalCounterManager.load();

        // Регистрация кнопки в инвентаре
        InventoryButtonHandler.register();

        // Регистрация кнопки в контейнерах
        ContainerButtonHandler.register();

        // Регистрация HUD элемента
        HudElementRegistry.addLast(
                Identifier.of(TaskList.MOD_ID, "item_tracker"),
                TaskListClient::renderItemTracker
        );

        TaskList.LOGGER.info("Item Tracker HUD registered");
    }

    private static int countItemInInventory(PlayerInventory inventory, Item item) {
        int count = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void updateItemCounts(PlayerInventory inventory, TrackerList list) {
        itemCounts.clear();

        if (list != null) {
            for (ItemGoal goal : list.getGoals()) {
                int count;
                if (goal.isGlobal()) {
                    // Глобальный режим: инвентарь + все контейнеры
                    count = countItemInInventory(inventory, goal.getItem()) +
                            GlobalCounterManager.getGlobalCount(goal.getItem());
                } else {
                    // Обычный режим: только инвентарь
                    count = countItemInInventory(inventory, goal.getItem());
                }
                itemCounts.put(goal, count);
            }
        }
    }

    private static void renderItemTracker(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null) return;
        if (mc.options.hudHidden) return;

        TrackerList activeList = TrackerListManager.getActiveList();
        if (activeList == null || activeList.isEmpty()) return;

        updateItemCounts(mc.player.getInventory(), activeList);

        TextRenderer textRenderer = mc.textRenderer;
        int screenWidth = mc.getWindow().getScaledWidth();

        int startX = screenWidth - 200;
        int startY = PADDING;

        // Рисуем общий фон ПЕРВЫМ
        int containerCount = GlobalCounterManager.getContainerCount();
        int extraLines = containerCount > 0 ? 1 : 0; // Добавляем строку для информации о контейнерах
        int totalHeight = LINE_HEIGHT * (activeList.size() + 1 + extraLines) + PADDING;
        drawBackgroundBox(context, startX - PADDING, PADDING - 2, 210, totalHeight);

        // Рисуем заголовок с именем списка
        String title = activeList.getName() + ":";
        context.drawTextWithShadow(textRenderer, title, startX, startY, Colors.YELLOW);

        startY += LINE_HEIGHT;

        // Рисуем каждую цель
        int goalIndex = 0;
        for (ItemGoal goal : activeList.getGoals()) {
            int currentY = startY + (goalIndex * LINE_HEIGHT);

            int currentAmount = itemCounts.getOrDefault(goal, 0);
            boolean completed = goal.isCompleted(currentAmount);
            boolean isTrackingMode = goal.isTrackingMode();

            int textColor = completed ? Colors.GREEN : Colors.WHITE;

            // Иконка предмета
            ItemStack displayStack = new ItemStack(goal.getItem());
            context.drawItem(displayStack, startX, currentY);

            // Отображаем количество на иконке (вручную, как в инвентаре)
            if (currentAmount != 1 || isTrackingMode) {
                String countText = String.valueOf(currentAmount);
                int textX = startX + 16 - textRenderer.getWidth(countText);
                int textY = currentY + 9;

                // Рисуем текст с тенью для лучшей читаемости
                context.drawText(textRenderer, countText, textX, textY, Colors.WHITE, true);
            }

            // Текст
            String displayText = goal.getDisplayName();

            // Добавляем индикатор глобального режима
            if (goal.isGlobal()) {
                displayText = "[G] " + displayText;
            }

            // В режиме отслеживания показываем только название
            // В режиме цели показываем прогресс
            if (!isTrackingMode) {
                displayText += ": " + goal.getProgressString(currentAmount);
                if (completed) {
                    displayText = "✓ " + displayText;
                }
            }

            int textX = startX + TEXT_OFFSET;
            int textY = currentY + (ITEM_ICON_SIZE - textRenderer.fontHeight) / 2;

            context.drawTextWithShadow(textRenderer, displayText, textX, textY, textColor);

            // Прогресс-бар (только для целей, не для отслеживания)
            if (!isTrackingMode && !completed) {
                int textWidth = textRenderer.getWidth(displayText);
                drawProgressBar(
                        context,
                        textX,
                        textY + textRenderer.fontHeight + 2,
                        textWidth,
                        goal.getProgressPercentage(currentAmount)
                );
            }

            goalIndex++;
        }

        // Показываем информацию о глобальных контейнерах, если есть
        int containerCount = GlobalCounterManager.getContainerCount();
        if (containerCount > 0) {
            int infoY = startY + (goalIndex * LINE_HEIGHT);
            String containerInfo = "§7[" + containerCount + " контейнеров]";
            context.drawTextWithShadow(textRenderer, containerInfo, startX + TEXT_OFFSET, infoY, Colors.GRAY);
        }
    }

    private static void drawBackgroundBox(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0x80000000);
    }

    private static void drawProgressBar(DrawContext context, int x, int y, int width, int progress) {
        int barHeight = 3;
        context.fill(x, y, x + width, y + barHeight, 0xFF333333);

        int filledWidth = (width * progress) / 100;
        if (filledWidth > 0) {
            int color = progress < 50 ? 0xFF00AA00 : 0xFF00FF00;
            context.fill(x, y, x + filledWidth, y + barHeight, color);
        }

        context.fill(x - 1, y - 1, x + width + 1, y, 0xFF000000);
        context.fill(x - 1, y + barHeight, x + width + 1, y + barHeight + 1, 0xFF000000);
        context.fill(x - 1, y, x, y + barHeight, 0xFF000000);
        context.fill(x + width, y, x + width + 1, y + barHeight, 0xFF000000);
    }
}