package ru.loper.suntnt.runes.effects;


public enum EffectsTarget {
    PLAYERS, ALL, ENTITIES;

    public static EffectsTarget getByName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
