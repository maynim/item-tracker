package ru.maynim.tasklist.gui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import ru.maynim.tasklist.GlobalCounterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Экран для управления отслеживаемыми контейнерами
 */
public class ContainerListScreen extends Screen {
    // Текстуры окна
    private static final Identifier WINDOW_TEXTURE =
            Identifier.ofVanilla("textures/gui/advancements/window.png");
    private static final Identifier BACKGROUND_TEXTURE =
            Identifier.ofVanilla("textures/gui/advancements/backgrounds/adventure.png");

    // Размеры окна
    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 180;
    private static final int BACKGROUND_TILE_SIZE = 16;

    // Размеры элемента списка
    private static final int ITEM_ROW_HEIGHT = 28;
    private static final int ITEMS_VISIBLE = 5;

    private final Screen parent;
    private int backgroundX;
    private int backgroundY;
    private int scrollOffset = 0;

    private List<GlobalCounterManager.ContainerInfo> containers;
    private int hoveredContainer = -1;

    public ContainerListScreen(Screen parent) {
        super(Text.literal("Управление контейнерами"));
        this.parent = parent;
        this.containers = GlobalCounterManager.getContainerDetails();
    }

    @Override
    protected void init() {
        super.init();

        // Обновляем список контейнеров
        this.containers = GlobalCounterManager.getContainerDetails();

        // Вычисляем позицию окна по центру
        this.backgroundX = (this.width - WINDOW_WIDTH) / 2;
        this.backgroundY = (this.height - WINDOW_HEIGHT) / 2;

        // Кнопка "Очистить всё"
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Очистить всё"),
                        button -> {
                            GlobalCounterManager.clearAll();
                            this.containers = GlobalCounterManager.getContainerDetails();
                            this.clearAndInit();
                        })
                .dimensions(this.backgroundX + 10, this.backgroundY + WINDOW_HEIGHT - 24, 80, 16)
                .build()
        );

        // Кнопка "Назад"
        this.addDrawableChild(ButtonWidget.builder(
                        ScreenTexts.BACK,
                        button -> this.close())
                .dimensions(this.backgroundX + WINDOW_WIDTH - 50, this.backgroundY + WINDOW_HEIGHT - 24, 45, 16)
                .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Затемнённый фон
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        // Сбрасываем состояние наведения
        hoveredContainer = -1;

        // Рисуем основное окно
        drawBackground(context);

        // Заголовок
        String title = "Отслеживаемые контейнеры (" + containers.size() + ")";
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                title,
                this.width / 2,
                this.backgroundY + 8,
                Colors.WHITE
        );

        // Список контейнеров
        drawContainerList(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);

        // Подсказки
        drawTooltips(context, mouseX, mouseY);
    }

    private void drawBackground(DrawContext context) {
        // Рисуем повторяющийся фон
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

        // Рисуем рамку окна
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

    private void drawContainerList(DrawContext context, int mouseX, int mouseY) {
        if (containers.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    "Нет отслеживаемых контейнеров",
                    this.width / 2,
                    this.backgroundY + 60,
                    Colors.GRAY
            );
            return;
        }

        int visibleCount = Math.min(ITEMS_VISIBLE, containers.size() - scrollOffset);

        for (int i = 0; i < visibleCount; i++) {
            int containerIndex = i + scrollOffset;
            if (containerIndex >= containers.size()) break;

            GlobalCounterManager.ContainerInfo container = containers.get(containerIndex);
            int rowY = this.backgroundY + 25 + (i * ITEM_ROW_HEIGHT);

            // Фон строки
            boolean hovered = isMouseOverRow(mouseX, mouseY, rowY);
            if (hovered) {
                context.fill(
                        this.backgroundX + 10,
                        rowY,
                        this.backgroundX + WINDOW_WIDTH - 10,
                        rowY + ITEM_ROW_HEIGHT,
                        0x40FFFFFF
                );
                hoveredContainer = containerIndex;
            }

            // Координаты контейнера
            String coords = String.format("§e[%d, %d, %d]",
                    container.getPosition().getX(),
                    container.getPosition().getY(),
                    container.getPosition().getZ());

            context.drawTextWithShadow(
                    this.textRenderer,
                    coords,
                    this.backgroundX + 15,
                    rowY + 4,
                    Colors.WHITE
            );

            // Измерение
            String dimension = "§7" + container.getDimensionName();
            context.drawTextWithShadow(
                    this.textRenderer,
                    dimension,
                    this.backgroundX + 15,
                    rowY + 14,
                    Colors.WHITE
            );

            // Информация о содержимом
            String info = String.format("§a%d тип(ов), %d шт.",
                    container.getUniqueItemCount(),
                    container.getTotalItemCount());

            int infoWidth = this.textRenderer.getWidth(info);
            context.drawTextWithShadow(
                    this.textRenderer,
                    info,
                    this.backgroundX + WINDOW_WIDTH - 15 - infoWidth,
                    rowY + 9,
                    Colors.WHITE
            );

            // Отрисовка первых 3 предметов из контейнера
            List<Map.Entry<Item, Integer>> itemList = new ArrayList<>(container.getItems().entrySet());
            int itemsToShow = Math.min(3, itemList.size());

            for (int j = 0; j < itemsToShow; j++) {
                Map.Entry<Item, Integer> entry = itemList.get(j);
                ItemStack stack = new ItemStack(entry.getKey(), entry.getValue());
                int iconX = this.backgroundX + 140 + (j * 18);
                int iconY = rowY + 6;

                context.drawItem(stack, iconX, iconY);

                // Отрисовка количества
                String count = String.valueOf(entry.getValue());
                int countX = iconX + 16 - this.textRenderer.getWidth(count);
                int countY = iconY + 9;
                context.drawText(this.textRenderer, count, countX, countY, Colors.WHITE, true);
            }
        }

        // Индикатор прокрутки
        if (containers.size() > ITEMS_VISIBLE) {
            int scrollBarHeight = ITEMS_VISIBLE * ITEM_ROW_HEIGHT;
            int scrollBarY = this.backgroundY + 25;
            int scrollBarX = this.backgroundX + WINDOW_WIDTH - 8;

            // Фон полосы прокрутки
            context.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarHeight, 0x80000000);

            // Ползунок
            int sliderHeight = Math.max(10, (ITEMS_VISIBLE * scrollBarHeight) / containers.size());
            int maxScroll = containers.size() - ITEMS_VISIBLE;
            int sliderY = scrollBarY + (scrollOffset * (scrollBarHeight - sliderHeight)) / maxScroll;

            context.fill(scrollBarX, sliderY, scrollBarX + 4, sliderY + sliderHeight, 0xFFAAAAAA);
        }
    }

    private boolean isMouseOverRow(int mouseX, int mouseY, int rowY) {
        return mouseX >= this.backgroundX + 10 &&
                mouseX < this.backgroundX + WINDOW_WIDTH - 10 &&
                mouseY >= rowY &&
                mouseY < rowY + ITEM_ROW_HEIGHT;
    }

    private void drawTooltips(DrawContext context, int mouseX, int mouseY) {
        if (hoveredContainer >= 0 && hoveredContainer < containers.size()) {
            GlobalCounterManager.ContainerInfo container = containers.get(hoveredContainer);

            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.literal("§eКонтейнер"));
            tooltip.add(Text.literal("§7Координаты: §f" +
                    container.getPosition().getX() + ", " +
                    container.getPosition().getY() + ", " +
                    container.getPosition().getZ()));
            tooltip.add(Text.literal("§7Измерение: §f" + container.getDimensionName()));
            tooltip.add(Text.literal("§7Типов предметов: §f" + container.getUniqueItemCount()));
            tooltip.add(Text.literal("§7Всего предметов: §f" + container.getTotalItemCount()));

            // Добавляем список предметов (первые 5)
            if (!container.getItems().isEmpty()) {
                tooltip.add(Text.literal(""));
                tooltip.add(Text.literal("§7Содержимое:"));

                List<Map.Entry<Item, Integer>> itemList = new ArrayList<>(container.getItems().entrySet());
                int itemsToShow = Math.min(5, itemList.size());

                for (int i = 0; i < itemsToShow; i++) {
                    Map.Entry<Item, Integer> entry = itemList.get(i);
                    ItemStack stack = new ItemStack(entry.getKey());
                    String itemName = stack.getName().getString();
                    tooltip.add(Text.literal("  §f" + itemName + " §7x" + entry.getValue()));
                }

                if (itemList.size() > 5) {
                    tooltip.add(Text.literal("  §7... и ещё " + (itemList.size() - 5)));
                }
            }

            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Обработка кликов по кнопкам
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // TODO: В будущем здесь можно добавить кнопку подсветки контейнера в игре

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (containers.isEmpty()) return false;

        int maxScroll = Math.max(0, containers.size() - ITEMS_VISIBLE);

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
