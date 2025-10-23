package ru.maynim.tasklist;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles adding a button to container screens for tracking global counts
 */
public class ContainerButtonHandler {

    // Хранит позицию контейнера для каждого открытого экрана
    private static final Map<Screen, ContainerInfo> openContainers = new HashMap<>();

    // Хранит последний открытый экран для отслеживания закрытия
    private static Screen lastScreen = null;

    private static class ContainerInfo {
        final BlockPos pos;
        final String dimension;

        ContainerInfo(BlockPos pos, String dimension) {
            this.pos = pos;
            this.dimension = dimension;
        }
    }

    public static void register() {
        // Добавляем кнопку в экраны контейнеров
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (isContainerScreen(screen)) {
                addContainerTrackingButton((HandledScreen<?>) screen, scaledWidth, scaledHeight);
            }
        });

        // Отслеживаем закрытие экранов через ClientTickEvents
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen currentScreen = client.currentScreen;

            // Проверяем, изменился ли экран (закрылся контейнер)
            if (lastScreen != null && lastScreen != currentScreen) {
                // Предыдущий экран закрылся, синхронизируем если это был контейнер
                if (isContainerScreen(lastScreen) && openContainers.containsKey(lastScreen)) {
                    ContainerInfo info = openContainers.get(lastScreen);

                    // Синхронизируем только если контейнер отслеживается
                    if (GlobalCounterManager.isTracked(info.pos, info.dimension)) {
                        if (client.player != null) {
                            List<ItemStack> contents = collectContainerContents((HandledScreen<?>) lastScreen, client);
                            GlobalCounterManager.updateContainer(info.pos, info.dimension, contents);
                        }
                    }

                    // Удаляем из списка открытых
                    openContainers.remove(lastScreen);
                }
            }

            // Обновляем последний экран
            lastScreen = currentScreen;
        });
    }

    /**
     * Проверяет, является ли экран контейнером (сундук, бочка, шалкер и т.д.)
     */
    private static boolean isContainerScreen(Screen screen) {
        // Исключаем инвентарь игрока и креативный режим
        if (screen instanceof InventoryScreen || screen instanceof CreativeInventoryScreen) {
            return false;
        }

        // Все остальные HandledScreen - это контейнеры
        return screen instanceof GenericContainerScreen || // Сундуки, бочки
               screen instanceof ShulkerBoxScreen ||        // Шалкер боксы
               screen instanceof HopperScreen ||            // Воронки
               screen instanceof Generic3x3ContainerScreen || // Раздатчики, дропперы
               screen instanceof HandledScreen;             // Другие контейнеры
    }

    /**
     * Получает позицию контейнера, на который смотрит игрок
     */
    private static BlockPos getContainerPosition(MinecraftClient mc) {
        if (mc == null || mc.player == null || mc.world == null) return null;

        HitResult hitResult = mc.crosshairTarget;
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            return blockHit.getBlockPos();
        }

        // Если raycast не сработал, ищем ближайший контейнер
        BlockPos playerPos = mc.player.getBlockPos();
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockEntity blockEntity = mc.world.getBlockEntity(pos);
                    if (blockEntity != null) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Нормализует позицию для двойных сундуков
     * Всегда возвращает позицию "левого" сундука
     */
    private static BlockPos normalizeChestPosition(MinecraftClient mc, BlockPos pos) {
        if (mc == null || mc.world == null || pos == null) return pos;

        BlockState state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return pos; // Не сундук, возвращаем как есть
        }

        ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
        if (chestType == ChestType.SINGLE) {
            return pos; // Одинарный сундук
        }

        Direction facing = state.get(ChestBlock.FACING);
        BlockPos connectedPos = null;

        // Определяем позицию парного сундука
        if (chestType == ChestType.RIGHT) {
            // Текущий сундук - правый, нужна позиция левого
            connectedPos = pos.offset(ChestBlock.getFacing(state));
        } else if (chestType == ChestType.LEFT) {
            // Текущий сундук уже левый
            return pos;
        }

        // Проверяем, что парный сундук существует
        if (connectedPos != null) {
            BlockState connectedState = mc.world.getBlockState(connectedPos);
            if (connectedState.getBlock() instanceof ChestBlock) {
                ChestType connectedType = connectedState.get(ChestBlock.CHEST_TYPE);
                if (connectedType == ChestType.LEFT) {
                    return connectedPos; // Возвращаем позицию левого сундука
                }
            }
        }

        // Для двойных сундуков возвращаем меньшую позицию
        if (connectedPos != null) {
            int compareX = Integer.compare(pos.getX(), connectedPos.getX());
            int compareZ = Integer.compare(pos.getZ(), connectedPos.getZ());

            if (compareX < 0 || (compareX == 0 && compareZ < 0)) {
                return pos;
            } else {
                return connectedPos;
            }
        }

        return pos;
    }

    private static void addContainerTrackingButton(HandledScreen<?> screen, int screenWidth, int screenHeight) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;

        // Получаем позицию контейнера
        BlockPos rawPos = getContainerPosition(mc);
        if (rawPos == null) return;

        // Нормализуем позицию для двойных сундуков
        BlockPos containerPos = normalizeChestPosition(mc, rawPos);
        String dimension = mc.world.getRegistryKey().getValue().toString();

        // Сохраняем информацию о контейнере для синхронизации при закрытии
        openContainers.put(screen, new ContainerInfo(containerPos, dimension));

        // Вычисляем координаты кнопки
        int x = (screenWidth - 176) / 2;
        int y = (screenHeight - 166) / 2;

        int buttonX = x + 176 + 5;
        int buttonY = y + 4;

        // Определяем, отслеживается ли уже этот контейнер
        boolean isTracked = GlobalCounterManager.isTracked(containerPos, dimension);

        final BlockPos finalPos = containerPos;

        // Создаем кнопку
        ButtonWidget trackButton = ButtonWidget.builder(
                        Text.literal(isTracked ? "✓" : "+"),
                        button -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client == null || client.player == null || client.world == null) return;

                            String dim = client.world.getRegistryKey().getValue().toString();
                            boolean tracked = GlobalCounterManager.isTracked(finalPos, dim);

                            if (tracked) {
                                // Удаляем из отслеживания
                                GlobalCounterManager.removeContainer(finalPos, dim);
                                button.setMessage(Text.literal("+"));
                                client.player.sendMessage(
                                    Text.literal("§eКонтейнер удален из глобального подсчета"),
                                    true
                                );
                            } else {
                                // Добавляем в отслеживание с текущим содержимым
                                List<ItemStack> contents = collectContainerContents(screen, client);
                                GlobalCounterManager.updateContainer(finalPos, dim, contents);
                                button.setMessage(Text.literal("✓"));
                                client.player.sendMessage(
                                    Text.literal("§aКонтейнер добавлен в глобальный подсчет (" +
                                                GlobalCounterManager.getContainerCount() + " всего)"),
                                    true
                                );
                            }
                        })
                .dimensions(buttonX, buttonY + 25, 20, 20)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                    Text.literal(isTracked ? "Убрать из учета" : "Учесть в глобальном счетчике")))
                .build();

        // Добавляем кнопку на экран
        Screens.getButtons(screen).add(trackButton);
    }

    /**
     * Собирает содержимое контейнера из слотов
     */
    private static List<ItemStack> collectContainerContents(HandledScreen<?> screen, MinecraftClient mc) {
        List<ItemStack> contents = new ArrayList<>();

        for (Slot slot : screen.getScreenHandler().slots) {
            // Пропускаем слоты инвентаря игрока
            if (slot.inventory == mc.player.getInventory()) continue;

            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                contents.add(stack.copy());
            }
        }

        return contents;
    }
}
