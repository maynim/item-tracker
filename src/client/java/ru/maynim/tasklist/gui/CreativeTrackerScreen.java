package ru.maynim.tasklist.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import ru.maynim.tasklist.ItemGoal;
import ru.maynim.tasklist.TrackerList;
import ru.maynim.tasklist.TrackerListManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creative-style GUI for managing tracker lists
 */
public class CreativeTrackerScreen extends Screen {
    // Текстуры окна достижений для списка предметов
    private static final Identifier WINDOW_TEXTURE =
            Identifier.ofVanilla("textures/gui/advancements/window.png");
    private static final Identifier BACKGROUND_TEXTURE =
            Identifier.ofVanilla("textures/gui/advancements/backgrounds/adventure.png");

    // Размеры окна списка
    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 140;

    // Размеры фоновой текстуры
    private static final int BACKGROUND_TILE_SIZE = 16;

    // Размеры вкладки
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_SELECTED_HEIGHT = 32;
    private static final int TAB_UNSELECTED_HEIGHT = 28;

    // Размеры элемента списка
    private static final int ITEM_ROW_HEIGHT = 22;
    private static final int ITEMS_VISIBLE = 4;  // Количество видимых строк

    /**
     * Получает текстуру вкладки на основе позиции, состояния и индекса
     */
    private static Identifier getTabTexture(boolean top, boolean selected, int tabNumber) {
        String position = top ? "top" : "bottom";
        String state = selected ? "selected" : "unselected";

        return Identifier.ofVanilla(
                "textures/gui/sprites/container/creative_inventory/tab_" +
                        position + "_" + state + "_" + tabNumber + ".png"
        );
    }

    private final Screen parent;
    private int backgroundX;
    private int backgroundY;

    private TrackerList currentList;
    private int selectedTabIndex = 0;
    private int scrollOffset = 0;

    // Состояния наведения
    private int hoveredTab = -1;
    private int hoveredItemIcon = -1;

    // Виджеты для редактирования
    private TextFieldWidget listNameField;
    private final List<TextFieldWidget> amountFields = new ArrayList<>();
    private final List<ButtonWidget> deleteButtons = new ArrayList<>();

    // Кэш текущего количества предметов в инвентаре
    private final Map<Item, Integer> currentItemCounts = new HashMap<>();

    public CreativeTrackerScreen(Screen parent) {
        super(Text.literal("Управление списками"));
        this.parent = parent;

        List<TrackerList> lists = TrackerListManager.getLists();
        if (!lists.isEmpty()) {
            this.currentList = TrackerListManager.getActiveList();
            if (this.currentList == null) {
                this.currentList = lists.get(0);
            }

            // Найти индекс текущего списка
            for (int i = 0; i < lists.size(); i++) {
                if (lists.get(i) == this.currentList) {
                    this.selectedTabIndex = i;
                    break;
                }
            }
        }
    }

    /**
     * Подсчитывает количество предмета в инвентаре игрока
     */
    private int countItemInInventory(Item item) {
        if (this.client == null || this.client.player == null) return 0;

        PlayerInventory inventory = this.client.player.getInventory();
        int count = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * Обновляет кэш текущего количества предметов
     */
    private void updateItemCounts() {
        currentItemCounts.clear();

        if (this.currentList != null) {
            for (ItemGoal goal : this.currentList.getGoals()) {
                int count;
                if (goal.isGlobal()) {
                    // Глобальный режим: инвентарь + все контейнеры
                    count = countItemInInventory(goal.getItem()) +
                            GlobalCounterManager.getGlobalCount(goal.getItem());
                } else {
                    // Обычный режим: только инвентарь
                    count = countItemInInventory(goal.getItem());
                }
                currentItemCounts.put(goal.getItem(), count);
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        // Обновляем количество предметов
        updateItemCounts();

        // Вычисляем позицию окна по центру
        this.backgroundX = (this.width - WINDOW_WIDTH) / 2;
        this.backgroundY = (this.height - WINDOW_HEIGHT) / 2;

        // Очищаем старые виджеты
        amountFields.clear();
        deleteButtons.clear();

        if (this.currentList != null) {
            // Поле для редактирования имени списка
            this.listNameField = new TextFieldWidget(
                    this.textRenderer,
                    this.backgroundX + 8,
                    this.backgroundY + 6,
                    120,
                    12,
                    Text.literal("Название списка")
            );
            this.listNameField.setMaxLength(30);
            this.listNameField.setText(this.currentList.getName());
            this.listNameField.setDrawsBackground(false);
            this.listNameField.setChangedListener(text -> {
                if (this.currentList != null) {
                    this.currentList.setName(text);
                    TrackerListManager.save();
                }
            });
            this.addSelectableChild(this.listNameField);

            // Создаем поля ввода и кнопки для каждого предмета
            List<ItemGoal> goals = this.currentList.getGoals();
            int visibleCount = Math.min(ITEMS_VISIBLE, goals.size() - scrollOffset);

            for (int i = 0; i < visibleCount; i++) {
                int goalIndex = i + scrollOffset;
                if (goalIndex >= goals.size()) break;

                ItemGoal goal = goals.get(goalIndex);
                int rowY = this.backgroundY + 20 + (i * ITEM_ROW_HEIGHT);

                // Поле количества
                TextFieldWidget amountField = new TextFieldWidget(
                        this.textRenderer,
                        this.backgroundX + 175,
                        rowY + 5,
                        40,
                        14,
                        Text.literal("Количество")
                );
                amountField.setMaxLength(3);
                amountField.setText(String.valueOf(goal.getTargetAmount()));

                final int finalGoalIndex = goalIndex;
                amountField.setChangedListener(text -> {
                    try {
                        // Разрешаем 0 для режима отслеживания
                        int amount = text.isEmpty() ? 0 : Integer.parseInt(text);
                        if (amount >= 0 && amount <= 999) {
                            // Обновляем количество в цели, сохраняя флаг isGlobal
                            ItemGoal currentGoal = this.currentList.getGoals().get(finalGoalIndex);
                            this.currentList.removeGoal(finalGoalIndex);
                            this.currentList.getGoals().add(finalGoalIndex, new ItemGoal(
                                    currentGoal.getItem().toString(),
                                    amount,
                                    null,
                                    currentGoal.isGlobal()
                            ));
                            TrackerListManager.save();
                        }
                    } catch (NumberFormatException ignored) {
                    }
                });
                this.addSelectableChild(amountField);
                amountFields.add(amountField);

                // Кнопка удаления
                ButtonWidget deleteButton = ButtonWidget.builder(
                                Text.literal("✕"),
                                button -> {
                                    this.currentList.removeGoal(finalGoalIndex);
                                    TrackerListManager.save();
                                    this.clearAndInit();
                                })
                        .dimensions(this.backgroundX + 220, rowY + 4, 16, 16)
                        .build();
                this.addDrawableChild(deleteButton);
                deleteButtons.add(deleteButton);
            }

            // Кнопка "Активировать список"
            this.addDrawableChild(ButtonWidget.builder(
                            Text.literal(this.currentList.isActive() ? "✓ Активен" : "Активировать"),
                            button -> {
                                TrackerListManager.setActiveList(this.currentList);
                                this.clearAndInit();
                            })
                    .dimensions(this.backgroundX + 10, this.backgroundY + WINDOW_HEIGHT - 24, 80, 16)
                    .build()
            );

            // Кнопка "Удалить список"
            this.addDrawableChild(ButtonWidget.builder(
                            Text.literal("Удалить"),
                            button -> {
                                TrackerListManager.removeList(this.currentList);
                                List<TrackerList> lists = TrackerListManager.getLists();
                                if (!lists.isEmpty()) {
                                    this.currentList = lists.get(0);
                                    this.selectedTabIndex = 0;
                                } else {
                                    this.currentList = null;
                                }
                                this.clearAndInit();
                            })
                    .dimensions(this.backgroundX + 95, this.backgroundY + WINDOW_HEIGHT - 24, 55, 16)
                    .build()
            );
        }

        // Кнопка "Создать список"
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("+"),
                        button -> this.createNewList())
                .dimensions(this.backgroundX + 155, this.backgroundY + WINDOW_HEIGHT - 24, 20, 16)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Создать новый список")))
                .build()
        );

        // Кнопка "Готово"
        this.addDrawableChild(ButtonWidget.builder(
                        ScreenTexts.DONE,
                        button -> this.close())
                .dimensions(this.backgroundX + WINDOW_WIDTH - 40, this.backgroundY + WINDOW_HEIGHT - 24, 35, 16)
                .build()
        );
    }

    private void createNewList() {
        String newName = "Новый список";
        int counter = 1;
        while (TrackerListManager.listNameExists(newName)) {
            counter++;
            newName = "Новый список " + counter;
        }

        TrackerList newList = new TrackerList(newName);
        TrackerListManager.addList(newList);

        this.currentList = newList;
        List<TrackerList> lists = TrackerListManager.getLists();
        this.selectedTabIndex = lists.indexOf(newList);

        this.clearAndInit();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Обновляем количество предметов в реальном времени
        updateItemCounts();

        // Затемнённый фон
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        // Сбрасываем состояния наведения
        hoveredTab = -1;
        hoveredItemIcon = -1;

        // 1. Сначала рисуем неактивные вкладки (задний план)
        drawInactiveTabs(context, mouseX, mouseY);

        // 2. Затем рисуем основное окно
        drawBackground(context);

        // 3. Потом рисуем активную вкладку (передний план)
        drawActiveTab(context, mouseX, mouseY);

        // 4. Рисуем содержимое
        if (this.currentList != null) {
            // Поле имени
            if (this.listNameField != null) {
                this.listNameField.render(context, mouseX, mouseY, delta);
            }

            // Список предметов
            drawItemList(context, mouseX, mouseY);

            // Рисуем все текстовые поля и кнопки
            for (TextFieldWidget field : amountFields) {
                field.render(context, mouseX, mouseY, delta);
            }
        } else {
            // Нет списков
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    "Нет списков. Создайте новый!",
                    this.width / 2,
                    this.height / 2,
                    Colors.GRAY
            );
        }

        super.render(context, mouseX, mouseY, delta);

        // Подсказки
        drawTooltips(context, mouseX, mouseY);
    }

    private void drawBackground(DrawContext context) {
        // 1. Рисуем повторяющийся фон (текстура backgrounds/adventure.png)
        int tilesX = (WINDOW_WIDTH / BACKGROUND_TILE_SIZE) + 1;
        int tilesY = (WINDOW_HEIGHT / BACKGROUND_TILE_SIZE) + 1;

        for (int x = 0; x < tilesX - 1; x++) {
            for (int y = 0; y < tilesY - 1; y++) {
                context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        BACKGROUND_TEXTURE,
                        this.backgroundX + (x * BACKGROUND_TILE_SIZE) + 6,
                        this.backgroundY + (y * BACKGROUND_TILE_SIZE) + 6,
                        0, 0,
                        BACKGROUND_TILE_SIZE, BACKGROUND_TILE_SIZE,
                        BACKGROUND_TILE_SIZE, BACKGROUND_TILE_SIZE
                );
            }
        }

        // 2. Рисуем рамку окна (window.png) поверх фона
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                WINDOW_TEXTURE,
                this.backgroundX,
                this.backgroundY,
                0, 0,
                WINDOW_WIDTH, WINDOW_HEIGHT,
                256, 256
        );
    }

    private void drawTabs(DrawContext context, int mouseX, int mouseY) {
        List<TrackerList> lists = TrackerListManager.getLists();

        // Верхние вкладки (первые 5)
        for (int i = 0; i < Math.min(5, lists.size()); i++) {
            drawTab(context, i, i == selectedTabIndex, true, mouseX, mouseY);
        }

        // Нижние вкладки (6-10)
        for (int i = 5; i < Math.min(10, lists.size()); i++) {
            drawTab(context, i, i == selectedTabIndex, false, mouseX, mouseY);
        }
    }

    /**
     * Рисует только неактивные вкладки (задний план)
     */
    private void drawInactiveTabs(DrawContext context, int mouseX, int mouseY) {
        List<TrackerList> lists = TrackerListManager.getLists();

        // Верхние неактивные вкладки
        for (int i = 0; i < Math.min(5, lists.size()); i++) {
            if (i != selectedTabIndex) {
                drawTab(context, i, false, true, mouseX, mouseY);
            }
        }

        // Нижние неактивные вкладки
        for (int i = 5; i < Math.min(10, lists.size()); i++) {
            if (i != selectedTabIndex) {
                drawTab(context, i, false, false, mouseX, mouseY);
            }
        }
    }

    /**
     * Рисует только активную вкладку (передний план)
     */
    private void drawActiveTab(DrawContext context, int mouseX, int mouseY) {
        if (selectedTabIndex < 0) return;

        List<TrackerList> lists = TrackerListManager.getLists();
        if (selectedTabIndex >= lists.size()) return;

        boolean isTop = selectedTabIndex < 5;
        drawTab(context, selectedTabIndex, true, isTop, mouseX, mouseY);
    }

    private void drawTab(DrawContext context, int index, boolean selected, boolean top, int mouseX, int mouseY) {
        List<TrackerList> lists = TrackerListManager.getLists();
        if (index >= lists.size()) return;

        TrackerList list = lists.get(index);
        int displayIndex = top ? index : (index - 5);

        // Позиция вкладки (смещение на 2 пикселя влево - убираем +2)
        int tabX = this.backgroundX + (displayIndex * TAB_WIDTH);
        int tabY;
        int tabHeight;

        if (top) {
            tabHeight = selected ? TAB_SELECTED_HEIGHT : TAB_UNSELECTED_HEIGHT;
            tabY = this.backgroundY - tabHeight + 4;
        } else {
            tabHeight = selected ? TAB_SELECTED_HEIGHT : TAB_UNSELECTED_HEIGHT;
            tabY = this.backgroundY + WINDOW_HEIGHT - 4;
        }

        // Номер вкладки для текстуры (1-5)
        int tabNumber = displayIndex + 1;

        // Получаем правильную текстуру для этой вкладки
        Identifier tabTexture = getTabTexture(top, selected, tabNumber);

        // Рисуем вкладку (спрайт загружается целиком, без UV)
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                tabTexture,
                tabX, tabY,
                0, 0,  // UV координаты (0,0) - спрайт загружается целиком
                TAB_WIDTH, tabHeight,
                TAB_WIDTH, tabHeight  // Размер текстуры = размеру вкладки
        );

        // Иконка списка (первый предмет или книга по умолчанию)
        ItemStack icon = new ItemStack(net.minecraft.item.Items.WRITABLE_BOOK);
        if (!list.getGoals().isEmpty()) {
            icon = new ItemStack(list.getGoals().get(0).getItem());
        }

        // Позиция иконки (также смещена на 2 пикселя влево)
        int iconX = tabX + 6;
        int iconY = tabY + (selected ? 8 : 6);
        context.drawItem(icon, iconX, iconY);

        // Проверка наведения для подсказки
        if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH &&
                mouseY >= tabY && mouseY < tabY + tabHeight) {
            this.hoveredTab = index;
        }
    }

    /**
     * Рисует список предметов в стиле HUD
     */
    private void drawItemList(DrawContext context, int mouseX, int mouseY) {
        if (this.currentList == null) return;

        List<ItemGoal> goals = this.currentList.getGoals();

        // Рисуем видимые строки
        int visibleCount = Math.min(ITEMS_VISIBLE, goals.size() - scrollOffset);

        for (int i = 0; i < visibleCount; i++) {
            int goalIndex = i + scrollOffset;
            if (goalIndex >= goals.size()) break;

            ItemGoal goal = goals.get(goalIndex);
            int rowY = this.backgroundY + 20 + (i * ITEM_ROW_HEIGHT);

            // Фон строки (полупрозрачный при наведении)
            if (isMouseOverRow(mouseX, mouseY, rowY)) {
                context.fill(
                        this.backgroundX + 10,
                        rowY,
                        this.backgroundX + WINDOW_WIDTH - 10,
                        rowY + ITEM_ROW_HEIGHT,
                        0x40FFFFFF
                );
            }

            // Иконка предмета (кликабельная)
            ItemStack stack = new ItemStack(goal.getItem());
            int iconX = this.backgroundX + 15;
            int iconY = rowY + 4;

            context.drawItem(stack, iconX, iconY);

            // Проверка наведения на иконку
            if (mouseX >= iconX && mouseX < iconX + 16 &&
                    mouseY >= iconY && mouseY < iconY + 16) {
                hoveredItemIcon = goalIndex;
                // Подсветка иконки
                context.fill(iconX, iconY, iconX + 16, iconY + 16, 0x80FFFFFF);
            }

            // Название предмета (не редактируемое)
            String itemName = stack.getName().getString();

            // Добавляем индикатор глобального режима
            if (goal.isGlobal()) {
                itemName = "[G] " + itemName;
            }

            context.drawTextWithShadow(
                    this.textRenderer,
                    itemName,
                    this.backgroundX + 38,
                    rowY + 8,
                    goal.isGlobal() ? Colors.AQUA : Colors.WHITE
            );

            // Текущее количество предметов в инвентаре (слева от поля количества)
            int currentCount = currentItemCounts.getOrDefault(goal.getItem(), 0);
            String countText = String.valueOf(currentCount);
            int countX = this.backgroundX + 165;
            context.drawTextWithShadow(
                    this.textRenderer,
                    countText,
                    countX,
                    rowY + 8,
                    goal.isTrackingMode() ? Colors.YELLOW : (goal.isCompleted(currentCount) ? Colors.GREEN : Colors.WHITE)
            );
        }

        // Кнопки добавления (две кнопки: обычный и глобальный счетчики)
        int addButtonY = this.backgroundY + 20 + (Math.min(ITEMS_VISIBLE, goals.size() - scrollOffset) * ITEM_ROW_HEIGHT);
        int buttonWidth = (WINDOW_WIDTH - 25) / 2;

        // Левая кнопка - обычный счетчик
        int leftButtonX = this.backgroundX + 10;
        if (mouseX >= leftButtonX && mouseX < leftButtonX + buttonWidth &&
                mouseY >= addButtonY && mouseY < addButtonY + ITEM_ROW_HEIGHT) {
            context.fill(
                    leftButtonX,
                    addButtonY,
                    leftButtonX + buttonWidth,
                    addButtonY + ITEM_ROW_HEIGHT,
                    0x4000AA00  // Зелёный полупрозрачный
            );
            hoveredItemIcon = -2;  // Специальный индекс для обычной кнопки
        }

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                "+ Предмет",
                leftButtonX + buttonWidth / 2,
                addButtonY + 8,
                Colors.GREEN
        );

        // Правая кнопка - глобальный счетчик
        int rightButtonX = leftButtonX + buttonWidth + 5;
        if (mouseX >= rightButtonX && mouseX < rightButtonX + buttonWidth &&
                mouseY >= addButtonY && mouseY < addButtonY + ITEM_ROW_HEIGHT) {
            context.fill(
                    rightButtonX,
                    addButtonY,
                    rightButtonX + buttonWidth,
                    addButtonY + ITEM_ROW_HEIGHT,
                    0x4000AAAA  // Бирюзовый полупрозрачный
            );
            hoveredItemIcon = -3;  // Специальный индекс для глобальной кнопки
        }

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                "+ Глобальный",
                rightButtonX + buttonWidth / 2,
                addButtonY + 8,
                Colors.AQUA
        );
    }

    /**
     * Проверяет, находится ли курсор над строкой
     */
    private boolean isMouseOverRow(int mouseX, int mouseY, int rowY) {
        return mouseX >= this.backgroundX + 10 &&
                mouseX < this.backgroundX + WINDOW_WIDTH - 10 &&
                mouseY >= rowY &&
                mouseY < rowY + ITEM_ROW_HEIGHT;
    }

    private void drawTooltips(DrawContext context, int mouseX, int mouseY) {
        // Подсказка для вкладки
        if (hoveredTab >= 0) {
            List<TrackerList> lists = TrackerListManager.getLists();
            if (hoveredTab < lists.size()) {
                TrackerList list = lists.get(hoveredTab);
                String tooltip = list.getName() + " (" + list.size() + " предметов)";
                context.drawTooltip(
                        this.textRenderer,
                        Text.literal(tooltip),
                        mouseX,
                        mouseY
                );
            }
        }

        // Подсказка для иконки предмета
        if (hoveredItemIcon >= 0 && this.currentList != null) {
            List<ItemGoal> goals = this.currentList.getGoals();
            if (hoveredItemIcon < goals.size()) {
                context.drawTooltip(
                        this.textRenderer,
                        Text.literal("Нажмите, чтобы изменить предмет"),
                        mouseX,
                        mouseY
                );
            }
        } else if (hoveredItemIcon == -2) {
            context.drawTooltip(
                    this.textRenderer,
                    Text.literal("Добавить новый предмет в список"),
                    mouseX,
                    mouseY
            );
        } else if (hoveredItemIcon == -3) {
            context.drawTooltip(
                    this.textRenderer,
                    Text.literal("Добавить глобальный счетчик (с учетом контейнеров)"),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Сначала обрабатываем клики по кнопкам и виджетам
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Затем проверяем клик по вкладке
        if (hoveredTab >= 0) {
            List<TrackerList> lists = TrackerListManager.getLists();
            if (hoveredTab < lists.size()) {
                this.currentList = lists.get(hoveredTab);
                this.selectedTabIndex = hoveredTab;
                this.scrollOffset = 0;
                this.clearAndInit();
                return true;
            }
        }

        // Клик по иконке предмета - открыть окно поиска
        if (button == 0 && hoveredItemIcon >= 0 && this.currentList != null) {
            this.client.setScreen(new ItemSelectionScreen(this, this.currentList));
            return true;
        }

        // Клик по кнопке добавления обычного предмета
        if (button == 0 && hoveredItemIcon == -2 && this.currentList != null) {
            this.client.setScreen(new ItemSelectionScreen(this, this.currentList, false));
            return true;
        }

        // Клик по кнопке добавления глобального счетчика
        if (button == 0 && hoveredItemIcon == -3 && this.currentList != null) {
            this.client.setScreen(new ItemSelectionScreen(this, this.currentList, true));
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.currentList == null) return false;

        int maxScroll = Math.max(0, this.currentList.size() - ITEMS_VISIBLE);

        if (verticalAmount > 0 && scrollOffset > 0) {
            scrollOffset--;
            this.clearAndInit();
            return true;
        } else if (verticalAmount < 0 && scrollOffset < maxScroll) {
            scrollOffset++;
            this.clearAndInit();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}