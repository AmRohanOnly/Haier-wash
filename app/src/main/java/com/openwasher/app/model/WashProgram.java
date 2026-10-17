package com.openwasher.app.model;

import java.io.Serializable;

public class WashProgram implements Serializable {
    private String id;
    private String name;
    private String description;
    private int durationMinutes;
    private int defaultTempCelsius;
    private int defaultSpinRpm;
    private String iconEmoji;

    public WashProgram(String id, String name, String description, int durationMinutes, int defaultTempCelsius, int defaultSpinRpm, String iconEmoji) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.defaultTempCelsius = defaultTempCelsius;
        this.defaultSpinRpm = defaultSpinRpm;
        this.iconEmoji = iconEmoji;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getDefaultTempCelsius() { return defaultTempCelsius; }
    public int getDefaultSpinRpm() { return defaultSpinRpm; }
    public String getIconEmoji() { return iconEmoji; }

    public static WashProgram getQuickWash() {
        return new WashProgram("quick", "Quick Wash", "Lightly soiled items", 15, 30, 800, "⚡");
    }

    public static WashProgram getStandardCotton() {
        return new WashProgram("standard", "Standard Cotton", "Daily mixed laundry", 35, 40, 1000, "👕");
    }

    public static WashProgram getHeavyDuty() {
        return new WashProgram("heavy", "Heavy Duty", "Towels, bedding & stains", 50, 60, 1200, "🛏️");
    }

    public static WashProgram getShoeClean() {
        return new WashProgram("shoes", "Shoe Clean", "Sneakers & canvas shoes", 30, 30, 700, "👟");
    }

    public static WashProgram getHotDry() {
        return new WashProgram("dry", "Hot Air Dry", "Tumble dry warm air", 40, 60, 0, "💨");
    }
}
