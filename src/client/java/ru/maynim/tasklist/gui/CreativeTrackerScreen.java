package ru.maynim.tasklist.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import ru.maynim.tasklist.ItemGoal;
import ru.maynim.tasklist.TrackerList;
import ru.maynim.tasklist.TrackerListManager;

import java.util.List;

/**
 * Creative-style GUI for managing tracker lists
 */
public class CreativeTrackerScreen extends Screen {
    // Текстуры креативного инвентаря
    private static final Identifier TABS_TEXTURE =
            Identifier.ofVanilla("textures/gui/container/creative_inventory/tabs.png");
    private static final Identifier TAB_ITEMS_TEXTURE =
            Identifier.ofVanilla("textures/gui/container/creative_inventory/tab_items.png");

    // Размеры окна креативного инвентаря
    private static final int BACKGROUND_WIDTH = 195;
    private static final int BACKGROUND_HEIGHT = 136;

    // Размеры вкладки
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_SELECTED_HEIGHT = 32;
    private static final int TAB_UNSELECTED_HEIGHT = 28;

    // Позиции элементов
    private static final int ITEMS_PER_ROW = 9;
    private static final int ROWS = 5;
    private static final int SLOT_SIZE = 18;

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

    private TextFieldWidget listNameField;

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

    @Override
    protected void init() {
        super.init();

        // Вычисляем позицию окна по центру
        this.backgroundX = (this.width - BACKGROUND_WIDTH) / 2;
        this.backgroundY = (this.height - BACKGROUND_HEIGHT) / 2;

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

            // Кнопка "Активировать список"
            this.addDrawableChild(ButtonWidget.builder(
                            Text.literal(this.currentList.isActive() ? "✓ Активен" : "Активировать"),
                            button -> {
                                TrackerListManager.setActiveList(this.currentList);
                                this.clearAndInit();
                            })
                    .dimensions(this.backgroundX + 10, this.backgroundY + 116, 80, 16)
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
                    .dimensions(this.backgroundX + 95, this.backgroundY + 116, 45, 16)
                    .build()
            );
        }

        // Кнопка "Создать список"
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("+"),
                        button -> this.createNewList())
                .dimensions(this.backgroundX + 145, this.backgroundY + 116, 20, 16)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Создать новый список")))
                .build()
        );

        // Кнопка "Готово"
        this.addDrawableChild(ButtonWidget.builder(
                        ScreenTexts.DONE,
                        button -> this.close())
                .dimensions(this.backgroundX + 170, this.backgroundY + 116, 20, 16)
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
        // Затемнённый фон
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        // ВАЖНО: Сбрасываем состояния наведения перед каждым рендером
        hoveredTab = -1;
        hoveredSlot = -1;

        // 1. Сначала рисуем неактивные вкладки (задний план)
        drawInactiveTabs(context, mouseX, mouseY);

        // 2. Затем рисуем основной фон (перекрывает неактивные вкладки)
        drawBackground(context);

        // 3. Потом рисуем активную вкладку (передний план, перекрывает фон)
        drawActiveTab(context, mouseX, mouseY);

        // 4. Рисуем содержимое
        if (this.currentList != null) {
            drawListContent(context, mouseX, mouseY);

            // Рисуем поле имени
            if (this.listNameField != null) {
                this.listNameField.render(context, mouseX, mouseY, delta);
            }
        } else {
            // Нет списков
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    "Нет списков",
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
        // Рисуем фон креативного инвентаря (вкладка с предметами)
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TAB_ITEMS_TEXTURE,
                this.backgroundX,
                this.backgroundY,
                0, 0,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT,
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
            tabY = this.backgroundY + BACKGROUND_HEIGHT - 4;
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

    private int hoveredTab = -1;
    private int hoveredSlot = -1;

    private void drawListContent(DrawContext context, int mouseX, int mouseY) {
        if (this.currentList == null) return;

        List<ItemGoal> goals = this.currentList.getGoals();
        hoveredSlot = -1;

        // Рисуем сетку предметов (5 рядов по 9 слотов, как в креативном инвентаре)
        for (int i = 0; i < ROWS * ITEMS_PER_ROW; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;

            int slotX = this.backgroundX + 8 + (col * SLOT_SIZE);
            int slotY = this.backgroundY + 17 + (row * SLOT_SIZE);

            int goalIndex = i + (scrollOffset * ITEMS_PER_ROW);

            // Слоты уже отрисованы в текстуре креативного инвентаря

            if (goalIndex < goals.size()) {
                // Рисуем существующий предмет
                ItemGoal goal = goals.get(goalIndex);
                ItemStack stack = new ItemStack(goal.getItem());

                context.drawItem(stack, slotX + 1, slotY + 1);

                // Количество
                String amountText = String.valueOf(goal.getTargetAmount());
                context.drawText(
                        this.textRenderer,
                        amountText,
                        slotX + 19 - this.textRenderer.getWidth(amountText),
                        slotY + 10,
                        Colors.WHITE,
                        true
                );

                // Проверка наведения
                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    hoveredSlot = goalIndex;
                    // Подсветка слота (стандартная как в Minecraft)
                    context.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, 0x80FFFFFF);
                }
            } else if (goalIndex == goals.size()) {
                // Кнопка добавления нового предмета
                drawAddButton(context, slotX, slotY);

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    hoveredSlot = -2;
                    context.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, 0x80FFFFFF);
                }
            }
        }
    }

    private void drawSlot(DrawContext context, int x, int y) {
        // Слоты уже есть в текстуре креативного инвентаря
    }

    private void drawAddButton(DrawContext context, int x, int y) {
        // Зелёный фон для кнопки добавления
        context.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x5000AA00);

        // Знак "+"
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                "+",
                x + SLOT_SIZE / 2,
                y + SLOT_SIZE / 2 - 4,
                Colors.WHITE
        );
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

        // Подсказка для предмета
        if (hoveredSlot >= 0 && this.currentList != null) {
            List<ItemGoal> goals = this.currentList.getGoals();
            if (hoveredSlot < goals.size()) {
                ItemGoal goal = goals.get(hoveredSlot);
                context.drawTooltip(
                        this.textRenderer,
                        Text.literal(goal.getDisplayName() + " x" + goal.getTargetAmount()),
                        mouseX,
                        mouseY
                );
            }
        } else if (hoveredSlot == -2) {
            context.drawTooltip(
                    this.textRenderer,
                    Text.literal("Добавить предмет"),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // ВАЖНО: Сначала обрабатываем клики по кнопкам и виджетам
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

        // Затем клик по предмету
        if (hoveredSlot >= 0 && this.currentList != null) {
            List<ItemGoal> goals = this.currentList.getGoals();
            if (hoveredSlot < goals.size()) {
                ItemGoal goal = goals.get(hoveredSlot);

                if (button == 0) { // ЛКМ - редактировать количество
                    this.client.setScreen(new AmountEditScreen(this, this.currentList, goal));
                } else if (button == 1) { // ПКМ - удалить
                    this.currentList.removeGoal(hoveredSlot - (scrollOffset * ITEMS_PER_ROW));
                    TrackerListManager.save();
                }
                return true;
            }
        } else if (hoveredSlot == -2) {
            // Кнопка добавления
            this.client.setScreen(new ItemSelectionScreen(this, this.currentList));
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.currentList == null) return false;

        int maxRows = (int) Math.ceil(this.currentList.size() / (double) ITEMS_PER_ROW);
        int maxScroll = Math.max(0, maxRows - ROWS);

        if (verticalAmount > 0 && scrollOffset > 0) {
            scrollOffset--;
            return true;
        } else if (verticalAmount < 0 && scrollOffset < maxScroll) {
            scrollOffset++;
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
