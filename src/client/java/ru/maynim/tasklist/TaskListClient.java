package ru.maynim.tasklist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TaskListClient implements ClientModInitializer {

    // List of words to display
    private static final List<String> WORD_LIST = new ArrayList<>();

    // Current displayed word
    private static String currentWord = "";

    // Cached text width for performance
    private static int cachedTextWidth = 0;

    // Timer variables
    private static int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20;

    // Random number generator
    private static final Random random = new Random();

    static {
        // Initialize word list with examples
        WORD_LIST.add("Minecraft");
        WORD_LIST.add("Fabric");
        WORD_LIST.add("Modding");
        WORD_LIST.add("Dynamic");
        WORD_LIST.add("Creative");
        WORD_LIST.add("Adventure");
        WORD_LIST.add("Survival");
        WORD_LIST.add("Redstone");
        WORD_LIST.add("Diamond");
        WORD_LIST.add("Enchanted");
    }


    @Override
	public void onInitializeClient() {
        TaskList.LOGGER.info("Initializing Dynamic HUD Mod Client");

        // Initialize with first word
        updateRandomWord();

        // Register tick event for timer (updates every second)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Only update when in game and not paused
            if (client.player == null || client.isPaused()) {
                return;
            }

            tickCounter++;

            // Every 20 ticks = 1 second
            if (tickCounter >= TICKS_PER_SECOND) {
                tickCounter = 0;
                updateRandomWord();
            }
        });

        // Register HUD rendering using NEW API (HudElementRegistry)
        // This is the correct approach for Fabric API 0.134.0+1.21.8
        HudElementRegistry.addLast(
                Identifier.of(TaskList.MOD_ID, "dynamic_text"),
                TaskListClient::renderDynamicText
        );

        TaskList.LOGGER.info("Dynamic HUD element registered successfully");
	}

    private static void updateRandomWord() {
        if (WORD_LIST.isEmpty()) {
            currentWord = "Empty List";
            return;
        }

        // Select random word
        int randomIndex = random.nextInt(WORD_LIST.size());
        currentWord = WORD_LIST.get(randomIndex);

        // Cache text width for performance (only calculate once per word change)
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.textRenderer != null) {
            cachedTextWidth = mc.textRenderer.getWidth(currentWord);
        }
    }

    /**
     * Renders the dynamic text on the HUD
     * This method is called every frame by the HudElementRegistry
     *
     * @param context DrawContext for rendering
     * @param tickCounter RenderTickCounter for animations
     */
    private static void renderDynamicText(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Safety checks
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;
        if (currentWord.isEmpty()) return;

        // Get text renderer
        TextRenderer textRenderer = mc.textRenderer;

        // Get screen dimensions
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        // Calculate position on the right side
        int margin = 5;
        int x = screenWidth - cachedTextWidth - margin;
        int y = margin;

        // Draw text with shadow using ARGB color format (required for 1.21.6+)
        // 0xFFFFFFFF = white with full opacity (ARGB format)
        context.drawTextWithShadow(
                textRenderer,
                currentWord,
                x,
                y,
                Colors.WHITE  // Use Colors class constants for proper ARGB format
        );

        // Optional: Draw a subtle background box for better visibility
        drawBackgroundBox(context, x - 2, y - 2, cachedTextWidth + 4, textRenderer.fontHeight + 4);
    }

    /**
     * Draws a semi-transparent background box behind the text
     *
     * @param context DrawContext for rendering
     * @param x Left edge X coordinate
     * @param y Top edge Y coordinate
     * @param width Box width
     * @param height Box height
     */
    private static void drawBackgroundBox(DrawContext context, int x, int y, int width, int height) {
        // Semi-transparent black background: 0xAARRGGBB format
        // 0x80 = 50% opacity, 000000 = black
        int backgroundColor = 0x80000000;
        context.fill(x, y, x + width, y + height, backgroundColor);
    }

    /**
     * Public method to add words to the list at runtime
     * Can be called from other mods or commands
     */
    public static void addWord(String word) {
        if (word != null && !word.isEmpty()) {
            WORD_LIST.add(word);
            TaskList.LOGGER.info("Added word to HUD list: {}", word);
        }
    }

    /**
     * Public method to clear all words from the list
     */
    public static void clearWords() {
        WORD_LIST.clear();
        currentWord = "";
        cachedTextWidth = 0;
        TaskList.LOGGER.info("Cleared HUD word list");
    }

    /**
     * Public method to get current word list
     */
    public static List<String> getWordList() {
        return new ArrayList<>(WORD_LIST);
    }
}