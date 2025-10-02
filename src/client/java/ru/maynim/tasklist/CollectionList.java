package ru.maynim.tasklist;

import net.minecraft.item.Item;

import java.util.Collections;
import java.util.Map;

public class CollectionList {
    private final String name;
    private final Map<Item, Integer> requiredItems;

    public CollectionList(String name, Map<Item, Integer> requiredItems) {
        this.name = name;
        this.requiredItems = requiredItems;
    }

    public String getName() {
        return name;
    }

    public Map<Item, Integer> getRequiredItems() {
        return Collections.unmodifiableMap(requiredItems);
    }
}
