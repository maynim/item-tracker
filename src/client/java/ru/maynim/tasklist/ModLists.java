package ru.maynim.tasklist;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

public class ModLists {

    // Хранилище для всех наших списков. Ключ - ID списка, значение - сам список.
    public static final Map<String, CollectionList> LISTS = new HashMap<>();

    // Метод для регистрации всех списков в моде.
    public static void registerLists() {
        // Создаем первый список
        Map<Item, Integer> basicResources = new HashMap<>();
//        Map<Item, Integer> basicResources3 = new HashMap<>();
        basicResources.put(Items.OAK_PLANKS, 4);
//        basicResources.put(Items.COBBLESTONE, 8);
//        basicResources.put(Items.IRON_INGOT, 1);
//        basicResources3.put(Items.OAK_LOG, 2);
        // Добавляем его в наше хранилище
        LISTS.put("basic_resources", new CollectionList("Базовые ресурсы", basicResources));
//        LISTS.put("basic_resources2", new CollectionList("Базовые ресурсы - 3", basicResources3));

        // Сюда можно добавлять другие списки по аналогии
    }
}
