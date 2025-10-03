package ru.maynim.tasklist;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a named list of item goals that can be tracked
 */
public class TrackerList {
    private String name;
    private final List<ItemGoal> goals;
    private boolean active;

    public TrackerList(String name) {
        this.name = name;
        this.goals = new ArrayList<>();
        this.active = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ItemGoal> getGoals() {
        return goals;
    }

    public void addGoal(ItemGoal goal) {
        goals.add(goal);
    }

    public void removeGoal(int index) {
        if (index >= 0 && index < goals.size()) {
            goals.remove(index);
        }
    }

    public void clearGoals() {
        goals.clear();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int size() {
        return goals.size();
    }

    public boolean isEmpty() {
        return goals.isEmpty();
    }

    // JSON serialization
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("active", active);

        JsonArray goalsArray = new JsonArray();
        for (ItemGoal goal : goals) {
            JsonObject goalObj = new JsonObject();
            goalObj.addProperty("item", goal.getItem().toString());
            goalObj.addProperty("amount", goal.getTargetAmount());
            goalObj.addProperty("displayName", goal.getDisplayName());
            goalsArray.add(goalObj);
        }
        obj.add("goals", goalsArray);

        return obj;
    }

    public static TrackerList fromJson(JsonObject obj) {
        String name = obj.get("name").getAsString();
        TrackerList list = new TrackerList(name);

        if (obj.has("active")) {
            list.setActive(obj.get("active").getAsBoolean());
        }

        if (obj.has("goals")) {
            JsonArray goalsArray = obj.getAsJsonArray("goals");
            for (JsonElement element : goalsArray) {
                JsonObject goalObj = element.getAsJsonObject();
                String itemId = goalObj.get("item").getAsString();
                int amount = goalObj.get("amount").getAsInt();
                String displayName = goalObj.has("displayName") ?
                        goalObj.get("displayName").getAsString() : null;

                try {
                    list.addGoal(new ItemGoal(itemId, amount, displayName));
                } catch (Exception e) {
                    TaskList.LOGGER.warn("Failed to load goal: {}", itemId);
                }
            }
        }

        return list;
    }

    public TrackerList copy() {
        TrackerList copy = new TrackerList(this.name + " (копия)");
        for (ItemGoal goal : this.goals) {
            copy.addGoal(new ItemGoal(
                    goal.getItem().toString(),
                    goal.getTargetAmount(),
                    goal.getDisplayName()
            ));
        }
        return copy;
    }
}
