package ru.loper.suntnt.rune.effects;

public enum GiveAction {
    EXPLOSION,
    PRIME;

    public static GiveAction getByName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
