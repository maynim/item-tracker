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

    // Список целей для сбора предметов
    private static final List<ItemGoal> ITEM_GOALS = new ArrayList<>();

    // Кэш текущего количества предметов (для производительности)
    private static final Map<Item, Integer> itemCounts = new HashMap<>();

    // Настройки отображения
    private static final int ITEM_ICON_SIZE = 16;  // Размер иконки предмета
    private static final int LINE_HEIGHT = 20;     // Высота одной строки
    private static final int PADDING = 5;          // Отступ от края
    private static final int TEXT_OFFSET = 20;     // Отступ текста от иконки

    static {
        // Инициализация целей - здесь можно задать любые предметы

        // Пример 1: 8 дубовых досок
        ITEM_GOALS.add(new ItemGoal("minecraft:oak_planks", 8));

        // Пример 2: Можно добавить больше целей
        ITEM_GOALS.add(new ItemGoal("minecraft:cobblestone", 64));
        ITEM_GOALS.add(new ItemGoal("minecraft:iron_ingot", 10));
        ITEM_GOALS.add(new ItemGoal("minecraft:diamond", 3));

        // Пример 3: Без русских названий (автоматически возьмет из игры)
        // ITEM_GOALS.add(new ItemGoal("minecraft:oak_planks", 8));
    }

    @Override
    public void onInitializeClient() {
        TaskList.LOGGER.info("Initializing Item Tracker Client");

        // Регистрация HUD элемента
        HudElementRegistry.addLast(
                Identifier.of(TaskList.MOD_ID, "item_tracker"),
                TaskListClient::renderItemTracker
        );

        TaskList.LOGGER.info("Item Tracker HUD registered with {} goals", ITEM_GOALS.size());
    }

    /**
     * Подсчитывает количество определенного предмета в инвентаре игрока
     */
    private static int countItemInInventory(PlayerInventory inventory, Item item) {
        int count = 0;

        // Проходим по всему инвентарю (основной инвентарь + хотбар + броня + оффханд)
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * Обновляет кэш количества предметов
     */
    private static void updateItemCounts(PlayerInventory inventory) {
        itemCounts.clear();

        for (ItemGoal goal : ITEM_GOALS) {
            int count = countItemInInventory(inventory, goal.getItem());
            itemCounts.put(goal.getItem(), count);
        }
    }

    /**
     * Рендерит трекер предметов в HUD
     */
    private static void renderItemTracker(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Проверки безопасности
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;
        if (ITEM_GOALS.isEmpty()) return;

        // Обновляем количество предметов
        updateItemCounts(mc.player.getInventory());

        TextRenderer textRenderer = mc.textRenderer;
        int screenWidth = mc.getWindow().getScaledWidth();

        // Стартовая позиция (правый верхний угол)
        int startX = screenWidth - 200;  // 200 пикселей от правого края
        int startY = PADDING;

        // ВАЖНО: Рисуем общий фон ПЕРВЫМ, чтобы он был под всеми элементами
        int totalHeight = LINE_HEIGHT * (ITEM_GOALS.size() + 1) + PADDING;
        drawBackgroundBox(
            context,
            startX - PADDING,
            PADDING - 2,
            210,
            totalHeight
        );

        // Рисуем заголовок
        String title = "Цели сбора:";
        int titleWidth = textRenderer.getWidth(title);
        context.drawTextWithShadow(
                textRenderer,
                title,
                startX,
                startY,
                Colors.YELLOW
        );

        startY += LINE_HEIGHT;

        // Рисуем каждую цель
        int goalIndex = 0;
        for (ItemGoal goal : ITEM_GOALS) {
            int currentY = startY + (goalIndex * LINE_HEIGHT);

            // Получаем текущее количество предметов
            int currentAmount = itemCounts.getOrDefault(goal.getItem(), 0);
            boolean completed = goal.isCompleted(currentAmount);

            // Выбираем цвет в зависимости от выполнения
            int textColor = completed ? Colors.GREEN : Colors.WHITE;

            // Рисуем иконку предмета
            ItemStack displayStack = new ItemStack(goal.getItem());
            context.drawItem(displayStack, startX, currentY);

            // Рисуем количество на иконке (если больше 1)
//            if (currentAmount > 1) {
//                context.drawItemInSlot(
//                        textRenderer,
//                        displayStack,
//                        startX,
//                        currentY
//                );
//            }

            // Рисуем название и прогресс
            String displayText = goal.getDisplayName() + ": " + goal.getProgressString(currentAmount);

            // Добавляем галочку для выполненных целей
            if (completed) {
                displayText = "✓ " + displayText;
            }

            int textX = startX + TEXT_OFFSET;
            int textY = currentY + (ITEM_ICON_SIZE - textRenderer.fontHeight) / 2;

            // Рисуем текст
            context.drawTextWithShadow(
                    textRenderer,
                    displayText,
                    textX,
                    textY,
                    textColor
            );

            // Рисуем прогресс-бар (опционально)
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

    /**
     * Рисует полупрозрачный фон
     */
    private static void drawBackgroundBox(DrawContext context, int x, int y, int width, int height) {
        int backgroundColor = 0x80000000;  // 50% прозрачности
        context.fill(x, y, x + width, y + height, backgroundColor);
    }

    /**
     * Рисует прогресс-бар
     */
    private static void drawProgressBar(DrawContext context, int x, int y, int width, int progress) {
        int barHeight = 3;

        // Фон прогресс-бара (темно-серый)
        context.fill(x, y, x + width, y + barHeight, 0xFF333333);

        // Заполненная часть (зеленый градиент)
        int filledWidth = (width * progress) / 100;
        if (filledWidth > 0) {
            // Градиент от темно-зеленого к светло-зеленому
            int color = progress < 50 ? 0xFF00AA00 : 0xFF00FF00;
            context.fill(x, y, x + filledWidth, y + barHeight, color);
        }

        // Обводка
        context.fill(x - 1, y - 1, x + width + 1, y, 0xFF000000);  // Верх
        context.fill(x - 1, y + barHeight, x + width + 1, y + barHeight + 1, 0xFF000000);  // Низ
        context.fill(x - 1, y, x, y + barHeight, 0xFF000000);  // Лево
        context.fill(x + width, y, x + width + 1, y + barHeight, 0xFF000000);  // Право
    }

    /**
     * Публичный метод для добавления новых целей во время выполнения
     */
    public static void addGoal(ItemGoal goal) {
        if (goal != null) {
            ITEM_GOALS.add(goal);
            TaskList.LOGGER.info("Added new goal: {} x{}", goal.getDisplayName(), goal.getTargetAmount());
        }
    }

    /**
     * Публичный метод для добавления цели по ID предмета
     */
    public static void addGoal(String itemId, int amount, String displayName) {
        try {
            ItemGoal goal = new ItemGoal(itemId, amount, displayName);
            addGoal(goal);
        } catch (Exception e) {
            TaskList.LOGGER.error("Failed to add goal for item {}: {}", itemId, e.getMessage());
        }
    }

    /**
     * Очищает все цели
     */
    public static void clearGoals() {
        ITEM_GOALS.clear();
        itemCounts.clear();
        TaskList.LOGGER.info("All goals cleared");
    }

    /**
     * Возвращает список всех целей
     */
    public static List<ItemGoal> getGoals() {
        return new ArrayList<>(ITEM_GOALS);
    }

    /**
     * Возвращает количество выполненных целей
     */
    public static int getCompletedGoalsCount() {
        int count = 0;
        for (ItemGoal goal : ITEM_GOALS) {
            int currentAmount = itemCounts.getOrDefault(goal.getItem(), 0);
            if (goal.isCompleted(currentAmount)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Проверяет, все ли цели выполнены
     */
    public static boolean areAllGoalsCompleted() {
        return getCompletedGoalsCount() == ITEM_GOALS.size() && !ITEM_GOALS.isEmpty();
    }
}