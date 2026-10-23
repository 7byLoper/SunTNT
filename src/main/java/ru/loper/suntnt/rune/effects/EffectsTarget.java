package ru.loper.suntnt.rune.effects;

import java.util.Locale;

public enum EffectsTarget {

    PLAYERS,
    ALL,
    ENTITIES;

    public static EffectsTarget getByName(final String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
