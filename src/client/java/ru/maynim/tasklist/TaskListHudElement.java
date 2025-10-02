package ru.maynim.tasklist;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.EnumSet;
import java.util.Map;

public class TaskListHudElement implements HudElement {

    @Override
    public void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        CollectionList listToDisplay = ModLists.LISTS.get("basic_resources3");
        if (listToDisplay == null) return;

        // --- ВОЗВРАЩАЕМ ЛОГИКУ РАСЧЕТА ПОЗИЦИИ ---
        // Так как система больше не управляет положением, мы делаем это сами.
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = screenWidth - 160; // Отступ 160 пикселей от правого края
        int y = screenHeight / 2 - 40; // Смещаем выше центра

        // Рисуем заголовок
        drawContext.drawText(client.textRenderer, Text.literal(listToDisplay.getName()), x, y, 0xFFFFFF, true);
        y += 12;

        // Рисуем предметы
        for (Map.Entry<Item, Integer> entry : listToDisplay.getRequiredItems().entrySet()) {
            Item requiredItem = entry.getKey();
            int requiredCount = entry.getValue();
            int currentCount = PlayerInventoryTracker.countItems(client.player, requiredItem);
            currentCount = Math.min(currentCount, requiredCount);

            boolean isCompleted = currentCount >= requiredCount;

            String progressText = String.format("%d / %d", currentCount, requiredCount);
            Text itemText = isCompleted
                    ? Text.literal("✓ ").append(requiredItem.getName()).append(" " + progressText)
                    : requiredItem.getName().copy().append(" " + progressText);

            int textColor = isCompleted ? 0x55FF55 : 0xFFFFFF;

            ItemStack itemStack = new ItemStack(requiredItem);
            drawContext.drawItem(itemStack, x, y);
            drawContext.drawText(client.textRenderer, itemText, x + 20, y + 4, textColor, true);

            y += 18;
        }
    }
}
