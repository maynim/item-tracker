package ru.maynim.tasklist;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.List;

/**
 * Main screen for managing tracker lists
 */
public class TrackerMainScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 25;

    private final Screen parent;
    private int scrollOffset = 0;
    private final int maxScroll;

    public TrackerMainScreen(Screen parent) {
        super(Text.literal("Управление списками целей"));
        this.parent = parent;

        List<TrackerList> lists = TrackerListManager.getLists();
        this.maxScroll = Math.max(0, lists.size() - 8);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 40;

        List<TrackerList> lists = TrackerListManager.getLists();
        TrackerList activeList = TrackerListManager.getActiveList();

        // Отображаем списки
        for (int i = scrollOffset; i < Math.min(lists.size(), scrollOffset + 8); i++) {
            TrackerList list = lists.get(i);
            int buttonY = startY + (i - scrollOffset) * BUTTON_SPACING;

            // Кнопка выбора/редактирования списка
            String buttonText = list.getName();
            if (list == activeList) {
                buttonText = "► " + buttonText + " ◄";
            }
            buttonText += " (" + list.size() + ")";

            final int listIndex = i;

            // Левая кнопка - активировать/редактировать
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal(buttonText),
                button -> {
                    if (list == activeList) {
                        // Открыть редактор
                        this.client.setScreen(new TrackerListEditScreen(this, list));
                    } else {
                        // Активировать список
                        TrackerListManager.setActiveList(list);
                        this.clearAndInit();
                    }
                })
                .dimensions(centerX - BUTTON_WIDTH / 2 - 40, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
            );

            // Кнопка редактирования
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal("✎"),
                button -> this.client.setScreen(new TrackerListEditScreen(this, list)))
                .dimensions(centerX + BUTTON_WIDTH / 2 - 35, buttonY, 20, BUTTON_HEIGHT)
                .build()
            );

            // Кнопка удаления
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal("✕"),
                button -> {
                    TrackerListManager.removeList(listIndex);
                    this.clearAndInit();
                })
                .dimensions(centerX + BUTTON_WIDTH / 2 - 10, buttonY, 20, BUTTON_HEIGHT)
                .build()
            );
        }

        int bottomY = startY + 8 * BUTTON_SPACING + 10;

        // Кнопка создать новый список
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("+ Создать новый список"),
            button -> {
                String newName = "Новый список " + (lists.size() + 1);
                int counter = 1;
                while (TrackerListManager.listNameExists(newName)) {
                    counter++;
                    newName = "Новый список " + counter;
                }

                TrackerList newList = new TrackerList(newName);
                TrackerListManager.addList(newList);
                this.client.setScreen(new TrackerListEditScreen(this, newList));
            })
            .dimensions(centerX - BUTTON_WIDTH / 2, bottomY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build()
        );

        // Кнопка закрыть
        this.addDrawableChild(ButtonWidget.builder(
            ScreenTexts.DONE,
            button -> this.close())
            .dimensions(centerX - BUTTON_WIDTH / 2, bottomY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рисуем затемнённый фон (без blur эффекта для Minecraft 1.21+)
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        // Заголовок
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            this.title,
            this.width / 2,
            20,
            Colors.WHITE
        );

        // Инструкция
        String instruction = "Нажмите на список для активации, ✎ для редактирования";
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            instruction,
            this.width / 2,
            this.height - 30,
            Colors.GRAY
        );

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
