package ru.maynim.tasklist;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

/**
 * Screen for editing a tracker list
 */
public class TrackerListEditScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 25;

    private final Screen parent;
    private final TrackerList list;
    private TextFieldWidget nameField;
    private int scrollOffset = 0;
    private final int maxScroll;

    public TrackerListEditScreen(Screen parent, TrackerList list) {
        super(Text.literal("Редактирование списка"));
        this.parent = parent;
        this.list = list;
        this.maxScroll = Math.max(0, list.size() - 6);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        // Поле для имени списка
        this.nameField = new TextFieldWidget(
                this.textRenderer,
                centerX - BUTTON_WIDTH / 2,
                40,
                BUTTON_WIDTH,
                20,
                Text.literal("Название списка")
        );
        this.nameField.setMaxLength(50);
        this.nameField.setText(list.getName());
        this.nameField.setChangedListener(text -> {
            list.setName(text);
            TrackerListManager.save();
        });
        this.addSelectableChild(this.nameField);

        int startY = 70;

        // Отображаем предметы в списке
        for (int i = scrollOffset; i < Math.min(list.size(), scrollOffset + 6); i++) {
            ItemGoal goal = list.getGoals().get(i);
            int buttonY = startY + (i - scrollOffset) * BUTTON_SPACING;

            String goalText = goal.getDisplayName() + " x" + goal.getTargetAmount();
            final int goalIndex = i;

            // Кнопка с названием предмета
            this.addDrawableChild(ButtonWidget.builder(
                            Text.literal(goalText),
                            button -> {
                                // Можно добавить редактирование количества
                            })
                    .dimensions(centerX - BUTTON_WIDTH / 2, buttonY, BUTTON_WIDTH - 30, BUTTON_HEIGHT)
                    .build()
            );

            // Кнопка удаления предмета
            this.addDrawableChild(ButtonWidget.builder(
                            Text.literal("✕"),
                            button -> {
                                list.removeGoal(goalIndex);
                                TrackerListManager.save();
                                this.clearAndInit();
                            })
                    .dimensions(centerX + BUTTON_WIDTH / 2 - 25, buttonY, 20, BUTTON_HEIGHT)
                    .build()
            );
        }

        int bottomY = startY + 6 * BUTTON_SPACING + 10;

        // Кнопка добавить предмет
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("+ Добавить предмет"),
                        button -> this.client.setScreen(new AddItemScreen(this, list)))
                .dimensions(centerX - BUTTON_WIDTH / 2, bottomY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
        );

        // Кнопка назад
        this.addDrawableChild(ButtonWidget.builder(
                        ScreenTexts.BACK,
                        button -> this.close())
                .dimensions(centerX - BUTTON_WIDTH / 2, bottomY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
        );
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

        // Рисуем поле имени
        this.nameField.render(context, mouseX, mouseY, delta);

        // Инструкция
        if (list.isEmpty()) {
            String instruction = "Список пуст. Добавьте предметы!";
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    instruction,
                    this.width / 2,
                    90,
                    Colors.GRAY
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
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
