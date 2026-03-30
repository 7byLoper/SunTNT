package ru.loper.suntnt.runes.effects;


public enum GiveAction {
    EXPLOSION, PRIME;

    public static GiveAction getByName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
