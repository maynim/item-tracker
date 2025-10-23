package ru.maynim.tasklist;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Represents a goal to collect a certain amount of items
 */
public class ItemGoal {
    private final Item item;
    private final int targetAmount;
    private final String displayName;

    /**
     * Creates a new item goal
     *
     * @param itemId Identifier of the item (e.g., "minecraft:oak_planks")
     * @param targetAmount Target amount to collect
     * @param displayName Display name for the HUD (e.g., "Дубовые доски")
     */
    public ItemGoal(String itemId, int targetAmount, String displayName) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            throw new IllegalArgumentException("Invalid item ID: " + itemId);
        }

        this.item = Registries.ITEM.get(id);
        this.targetAmount = targetAmount;
        this.displayName = displayName;

        if (this.item == null) {
            TaskList.LOGGER.warn("Item not found: {}", itemId);
        }
    }

    /**
     * Creates a new item goal with automatic display name from item
     */
    public ItemGoal(String itemId, int targetAmount) {
        this(itemId, targetAmount, null);
    }

    /**
     * Gets the item for this goal
     */
    public Item getItem() {
        return item;
    }

    /**
     * Gets the target amount
     */
    public int getTargetAmount() {
        return targetAmount;
    }

    /**
     * Gets the display name (custom or from item)
     */
    public String getDisplayName() {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        return new ItemStack(item).getName().getString();
    }

    /**
     * Checks if this goal is in tracking mode (no target amount)
     */
    public boolean isTrackingMode() {
        return targetAmount == 0;
    }

    /**
     * Checks if this goal is completed
     */
    public boolean isCompleted(int currentAmount) {
        if (isTrackingMode()) return false; // В режиме подсчета нет завершения
        return currentAmount >= targetAmount;
    }

    /**
     * Gets progress percentage (0-100)
     */
    public int getProgressPercentage(int currentAmount) {
        if (targetAmount == 0) return 100;
        return Math.min(100, (currentAmount * 100) / targetAmount);
    }

    /**
     * Gets formatted progress string (e.g., "3/8" or just "3" in tracking mode)
     */
    public String getProgressString(int currentAmount) {
        if (isTrackingMode()) {
            return String.valueOf(currentAmount);
        }
        return currentAmount + "/" + targetAmount;
    }
}
