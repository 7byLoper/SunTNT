package ru.loper.suntnt.api.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.loper.suntnt.api.model.Rune;
import ru.loper.suntnt.rune.AscensionRune;
import ru.loper.suntnt.rune.AutoIgniteRune;
import ru.loper.suntnt.rune.GravitationRune;
import ru.loper.suntnt.rune.effects.EffectsRune;

@Getter
@RequiredArgsConstructor
public enum RuneType {

    EFFECTS("EFFECTS", EffectsRune::new),
    GRAVITATION("GRAVITATION", GravitationRune::new),
    AUTO_IGNITE("AUTO_IGNITE", AutoIgniteRune::new),
    ASCENSION("ASCENSION", AscensionRune::new);

    private static final Map<String, RuneType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(type -> type.name.toUpperCase(Locale.ROOT), Function.identity()));

    private final String name;
    private final Supplier<Rune> runeSupplier;

    public Rune createRune() {
        return runeSupplier.get();
    }

    public static RuneType getByName(final String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        return BY_NAME.get(name.toUpperCase(Locale.ROOT));
    }
}
