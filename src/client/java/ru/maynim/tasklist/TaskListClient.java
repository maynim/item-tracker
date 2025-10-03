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

    // Кэш текущего количества предметов
    private static final Map<Item, Integer> itemCounts = new HashMap<>();

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

        // Регистрация кнопки в инвентаре
        InventoryButtonHandler.register();

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
                int count = countItemInInventory(inventory, goal.getItem());
                itemCounts.put(goal.getItem(), count);
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
        int totalHeight = LINE_HEIGHT * (activeList.size() + 1) + PADDING;
        drawBackgroundBox(context, startX - PADDING, PADDING - 2, 210, totalHeight);

        // Рисуем заголовок с именем списка
        String title = activeList.getName() + ":";
        context.drawTextWithShadow(textRenderer, title, startX, startY, Colors.YELLOW);

        startY += LINE_HEIGHT;

        // Рисуем каждую цель
        int goalIndex = 0;
        for (ItemGoal goal : activeList.getGoals()) {
            int currentY = startY + (goalIndex * LINE_HEIGHT);

            int currentAmount = itemCounts.getOrDefault(goal.getItem(), 0);
            boolean completed = goal.isCompleted(currentAmount);

            int textColor = completed ? Colors.GREEN : Colors.WHITE;

            // Иконка предмета
            ItemStack displayStack = new ItemStack(goal.getItem());
            context.drawItem(displayStack, startX, currentY);

//            if (currentAmount > 1) {
//                context.drawItemInSlot(textRenderer, displayStack, startX, currentY);
//            }

            // Текст
            String displayText = goal.getDisplayName() + ": " + goal.getProgressString(currentAmount);
            if (completed) {
                displayText = "✓ " + displayText;
            }

            int textX = startX + TEXT_OFFSET;
            int textY = currentY + (ITEM_ICON_SIZE - textRenderer.fontHeight) / 2;

            context.drawTextWithShadow(textRenderer, displayText, textX, textY, textColor);

            // Прогресс-бар
            if (!completed) {
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