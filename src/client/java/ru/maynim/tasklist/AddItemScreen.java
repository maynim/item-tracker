package ru.maynim.tasklist;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

/**
 * Screen for adding a new item to a tracker list
 */
public class AddItemScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private final TrackerList list;
    private TextFieldWidget itemIdField;
    private TextFieldWidget amountField;
    private TextFieldWidget displayNameField;
    private Text errorMessage = null;

    public AddItemScreen(Screen parent, TrackerList list) {
        super(Text.literal("Добавить предмет"));
        this.parent = parent;
        this.list = list;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 60;

        // Поле ID предмета
        this.itemIdField = new TextFieldWidget(
                this.textRenderer,
                centerX - BUTTON_WIDTH / 2,
                startY,
                BUTTON_WIDTH,
                20,
                Text.literal("ID предмета")
        );
        this.itemIdField.setMaxLength(100);
        this.itemIdField.setPlaceholder(Text.literal("minecraft:diamond"));
        this.addSelectableChild(this.itemIdField);
        this.setInitialFocus(this.itemIdField);

        // Поле количества
        this.amountField = new TextFieldWidget(
                this.textRenderer,
                centerX - BUTTON_WIDTH / 2,
                startY + 30,
                BUTTON_WIDTH,
                20,
                Text.literal("Количество")
        );
        this.amountField.setMaxLength(5);
        this.amountField.setText("1");
        this.amountField.setPlaceholder(Text.literal("1"));
        this.addSelectableChild(this.amountField);

        // Поле названия (опционально)
        this.displayNameField = new TextFieldWidget(
                this.textRenderer,
                centerX - BUTTON_WIDTH / 2,
                startY + 60,
                BUTTON_WIDTH,
                20,
                Text.literal("Название (опционально)")
        );
        this.displayNameField.setMaxLength(50);
        this.displayNameField.setPlaceholder(Text.literal("Алмаз"));
        this.addSelectableChild(this.displayNameField);

        // Кнопки быстрого выбора популярных предметов
        int quickButtonY = startY + 100;
        addQuickSelectButton("Дуб", "minecraft:oak_planks", centerX - 105, quickButtonY, 50);
        addQuickSelectButton("Камень", "minecraft:cobblestone", centerX - 50, quickButtonY, 50);
        addQuickSelectButton("Железо", "minecraft:iron_ingot", centerX + 5, quickButtonY, 50);
        addQuickSelectButton("Алмаз", "minecraft:diamond", centerX + 60, quickButtonY, 50);

        // Кнопка добавить
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Добавить"),
                        button -> this.addItem())
                .dimensions(centerX - BUTTON_WIDTH / 2, startY + 140, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
        );

        // Кнопка отмена
        this.addDrawableChild(ButtonWidget.builder(
                        ScreenTexts.CANCEL,
                        button -> this.close())
                .dimensions(centerX - BUTTON_WIDTH / 2, startY + 170, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
        );
    }

    private void addQuickSelectButton(String label, String itemId, int x, int y, int width) {
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal(label),
                        button -> {
                            this.itemIdField.setText(itemId);
                            this.errorMessage = null;
                        })
                .dimensions(x, y, width, 20)
                .build()
        );
    }

    private void addItem() {
        String itemId = this.itemIdField.getText().trim();
        String amountStr = this.amountField.getText().trim();
        String displayName = this.displayNameField.getText().trim();

        // Валидация
        if (itemId.isEmpty()) {
            this.errorMessage = Text.literal("Введите ID предмета!").withColor(Colors.RED);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount < 1 || amount > 999) {
                this.errorMessage = Text.literal("Количество должно быть от 1 до 999!").withColor(Colors.RED);
                return;
            }
        } catch (NumberFormatException e) {
            this.errorMessage = Text.literal("Неверное количество!").withColor(Colors.RED);
            return;
        }

        // Проверка существования предмета
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            this.errorMessage = Text.literal("Неверный формат ID!").withColor(Colors.RED);
            return;
        }

        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR || item == null) {
            this.errorMessage = Text.literal("Предмет не найден!").withColor(Colors.RED);
            return;
        }

        // Добавляем цель
        try {
            ItemGoal goal = new ItemGoal(
                    itemId,
                    amount,
                    displayName.isEmpty() ? null : displayName
            );
            list.addGoal(goal);
            TrackerListManager.save();

            TaskList.LOGGER.info("Added item goal: {} x{}", itemId, amount);
            this.close();

        } catch (Exception e) {
            this.errorMessage = Text.literal("Ошибка добавления предмета!").withColor(Colors.RED);
            TaskList.LOGGER.error("Failed to add item goal", e);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рисуем затемнённый фон
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        // Заголовок
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                20,
                Colors.WHITE
        );

        // Подписи к полям
        context.drawTextWithShadow(
                this.textRenderer,
                "ID предмета:",
                this.width / 2 - BUTTON_WIDTH / 2,
                48,
                Colors.LIGHT_GRAY
        );

        context.drawTextWithShadow(
                this.textRenderer,
                "Количество:",
                this.width / 2 - BUTTON_WIDTH / 2,
                78,
                Colors.LIGHT_GRAY
        );

        context.drawTextWithShadow(
                this.textRenderer,
                "Название:",
                this.width / 2 - BUTTON_WIDTH / 2,
                108,
                Colors.LIGHT_GRAY
        );

        // Рисуем поля ввода
        this.itemIdField.render(context, mouseX, mouseY, delta);
        this.amountField.render(context, mouseX, mouseY, delta);
        this.displayNameField.render(context, mouseX, mouseY, delta);

        // Сообщение об ошибке
        if (this.errorMessage != null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    this.errorMessage,
                    this.width / 2,
                    this.height - 30,
                    Colors.RED
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter добавляет предмет
        if (keyCode == 257 || keyCode == 335) { // Enter или Numpad Enter
            this.addItem();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
