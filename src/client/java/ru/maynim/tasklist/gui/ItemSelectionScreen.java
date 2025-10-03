package ru.maynim.tasklist.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import ru.maynim.tasklist.TrackerList;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for selecting items (Creative-style search)
 */
public class ItemSelectionScreen extends Screen {
    private static final Identifier CREATIVE_INVENTORY_TEXTURE =
            Identifier.ofVanilla("textures/gui/container/creative_inventory/tabs.png");

    private static final int BACKGROUND_WIDTH = 195;
    private static final int BACKGROUND_HEIGHT = 136;
    private static final int ITEMS_PER_ROW = 9;
    private static final int ROWS = 5;
    private static final int SLOT_SIZE = 18;

    private final Screen parent;
    private final TrackerList targetList;
    private int backgroundX;
    private int backgroundY;

    private TextFieldWidget searchField;
    private List<Item> filteredItems;
    private int scrollOffset = 0;
    private int hoveredSlot = -1;

    public ItemSelectionScreen(Screen parent, TrackerList targetList) {
        super(Text.literal("Выберите предмет"));
        this.parent = parent;
        this.targetList = targetList;
        this.filteredItems = new ArrayList<>();

        // Заполняем список всех предметов
        for (Item item : Registries.ITEM) {
            if (item != Items.AIR) {
                filteredItems.add(item);
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        this.backgroundX = (this.width - BACKGROUND_WIDTH) / 2;
        this.backgroundY = (this.height - BACKGROUND_HEIGHT) / 2;

        // Поле поиска
        this.searchField = new TextFieldWidget(
                this.textRenderer,
                this.backgroundX + 10,
                this.backgroundY + 6,
                175,
                12,
                Text.literal("Поиск")
        );
        this.searchField.setMaxLength(50);
        this.searchField.setPlaceholder(Text.literal("Поиск предметов..."));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addSelectableChild(this.searchField);
        this.setInitialFocus(this.searchField);
    }

    private void onSearchChanged(String search) {
        this.filteredItems.clear();
        this.scrollOffset = 0;

        String searchLower = search.toLowerCase();

        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;

            // Поиск по ID
            String itemId = Registries.ITEM.getId(item).toString().toLowerCase();
            if (itemId.contains(searchLower)) {
                filteredItems.add(item);
                continue;
            }

            // Поиск по названию
            String itemName = new ItemStack(item).getName().getString().toLowerCase();
            if (itemName.contains(searchLower)) {
                filteredItems.add(item);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Фон
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        // Рисуем фон инвентаря
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                CREATIVE_INVENTORY_TEXTURE,
                this.backgroundX,
                this.backgroundY,
                0, 0,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT,
                256, 256
        );

        // Поле поиска
        this.searchField.render(context, mouseX, mouseY, delta);

        // Рисуем сетку предметов
        hoveredSlot = -1;
        for (int i = 0; i < ROWS * ITEMS_PER_ROW; i++) {
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;

            int slotX = this.backgroundX + 9 + (col * SLOT_SIZE);
            int slotY = this.backgroundY + 18 + (row * SLOT_SIZE);

            int itemIndex = i + (scrollOffset * ITEMS_PER_ROW);

            // Рисуем слот
            drawSlot(context, slotX, slotY);

            if (itemIndex < filteredItems.size()) {
                Item item = filteredItems.get(itemIndex);
                ItemStack stack = new ItemStack(item);

                context.drawItem(stack, slotX + 1, slotY + 1);

                // Проверка наведения
                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    hoveredSlot = itemIndex;
                    context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x80FFFFFF);
                }
            }
        }

        super.render(context, mouseX, mouseY, delta);

        // Подсказка
        if (hoveredSlot >= 0 && hoveredSlot < filteredItems.size()) {
            Item item = filteredItems.get(hoveredSlot);
            ItemStack stack = new ItemStack(item);
            context.drawTooltip(
                    this.textRenderer,
                    stack.getName(),
                    mouseX,
                    mouseY
            );
        }

        // Инструкция
        String info = filteredItems.size() + " предметов";
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                info,
                this.width / 2,
                this.backgroundY + BACKGROUND_HEIGHT + 5,
                Colors.GRAY
        );
    }

    private void drawSlot(DrawContext context, int x, int y) {
        context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        context.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF373737);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredSlot >= 0 && hoveredSlot < filteredItems.size()) {
            Item selectedItem = filteredItems.get(hoveredSlot);

            // Открываем экран редактирования количества
            this.client.setScreen(new AmountEditScreen(this.parent, this.targetList, selectedItem));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxRows = (int) Math.ceil(filteredItems.size() / (double) ITEMS_PER_ROW);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC возвращает назад
        if (keyCode == 256) {
            this.close();
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
