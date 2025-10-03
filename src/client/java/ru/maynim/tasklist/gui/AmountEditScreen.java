package ru.maynim.tasklist.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import ru.maynim.tasklist.ItemGoal;
import ru.maynim.tasklist.TrackerList;
import ru.maynim.tasklist.TrackerListManager;

/**
 * Screen for editing item amount
 */
public class AmountEditScreen extends Screen {
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 120;

    private final Screen parent;
    private final TrackerList targetList;
    private final Item item;
    private final ItemGoal existingGoal;

    private TextFieldWidget amountField;
    private TextFieldWidget displayNameField;
    private Text errorMessage = null;

    private int panelX;
    private int panelY;

    public AmountEditScreen(Screen parent, TrackerList targetList, Item item) {
        super(Text.literal("Количество предмета"));
        this.parent = parent;
        this.targetList = targetList;
        this.item = item;
        this.existingGoal = null;
    }

    public AmountEditScreen(Screen parent, TrackerList targetList, ItemGoal goal) {
        super(Text.literal("Редактировать количество"));
        this.parent = parent;
        this.targetList = targetList;
        this.item = goal.getItem();
        this.existingGoal = goal;
    }

    @Override
    protected void init() {
        super.init();

        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;

        // Поле количества
        this.amountField = new TextFieldWidget(
                this.textRenderer,
                this.panelX + 10,
                this.panelY + 50,
                PANEL_WIDTH - 20,
                20,
                Text.literal("Количество")
        );
        this.amountField.setMaxLength(3);
        this.amountField.setText(existingGoal != null ?
                String.valueOf(existingGoal.getTargetAmount()) : "1");
        this.addSelectableChild(this.amountField);
        this.setInitialFocus(this.amountField);

        // Поле названия
        this.displayNameField = new TextFieldWidget(
                this.textRenderer,
                this.panelX + 10,
                this.panelY + 80,
                PANEL_WIDTH - 20,
                20,
                Text.literal("Название")
        );
        this.displayNameField.setMaxLength(30);
        if (existingGoal != null) {
            this.displayNameField.setText(existingGoal.getDisplayName());
        } else {
            this.displayNameField.setPlaceholder(
                    Text.literal(new ItemStack(item).getName().getString())
            );
        }
        this.addSelectableChild(this.displayNameField);

        // Кнопка сохранить
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Сохранить"),
                        button -> this.saveAndClose())
                .dimensions(this.panelX + 10, this.panelY + PANEL_HEIGHT - 25, 85, 20)
                .build()
        );

        // Кнопка отмена
        this.addDrawableChild(ButtonWidget.builder(
                        ScreenTexts.CANCEL,
                        button -> this.close())
                .dimensions(this.panelX + 105, this.panelY + PANEL_HEIGHT - 25, 85, 20)
                .build()
        );
    }

    private void saveAndClose() {
        String amountStr = this.amountField.getText().trim();
        String displayName = this.displayNameField.getText().trim();

        // Валидация
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount < 1 || amount > 999) {
                this.errorMessage = Text.literal("Количество: 1-999").withColor(Colors.RED);
                return;
            }
        } catch (NumberFormatException e) {
            this.errorMessage = Text.literal("Неверное число").withColor(Colors.RED);
            return;
        }

        if (existingGoal != null) {
            // Обновляем существующую цель
            int index = targetList.getGoals().indexOf(existingGoal);
            if (index >= 0) {
                targetList.removeGoal(index);
                targetList.getGoals().add(
                        index,
                        new ItemGoal(
                                item.toString(),
                                amount,
                                displayName.isEmpty() ? null : displayName
                        )
                );
            }
        } else {
            // Добавляем новую цель
            targetList.addGoal(new ItemGoal(
                    item.toString(),
                    amount,
                    displayName.isEmpty() ? null : displayName
            ));
        }

        TrackerListManager.save();
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Полупрозрачный фон
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        // Панель
        context.fill(
                this.panelX,
                this.panelY,
                this.panelX + PANEL_WIDTH,
                this.panelY + PANEL_HEIGHT,
                0xFF2B2B2B
        );

        // Обводка
        context.drawBorder(
                this.panelX,
                this.panelY,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                Colors.WHITE
        );

        // Заголовок
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                this.panelY + 10,
                Colors.WHITE
        );

        // Иконка предмета
        ItemStack stack = new ItemStack(item);
        context.drawItem(stack, this.width / 2 - 8, this.panelY + 25);

        // Подписи
        context.drawTextWithShadow(
                this.textRenderer,
                "Количество:",
                this.panelX + 10,
                this.panelY + 38,
                Colors.LIGHT_GRAY
        );

        context.drawTextWithShadow(
                this.textRenderer,
                "Название:",
                this.panelX + 10,
                this.panelY + 68,
                Colors.LIGHT_GRAY
        );

        // Поля ввода
        this.amountField.render(context, mouseX, mouseY, delta);
        this.displayNameField.render(context, mouseX, mouseY, delta);

        // Ошибка
        if (this.errorMessage != null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    this.errorMessage,
                    this.width / 2,
                    this.panelY + PANEL_HEIGHT + 5,
                    Colors.RED
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter сохраняет
        if (keyCode == 257 || keyCode == 335) {
            this.saveAndClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
