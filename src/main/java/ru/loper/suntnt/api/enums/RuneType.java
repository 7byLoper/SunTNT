package ru.loper.suntnt.api.enums;

import java.util.Arrays;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.loper.suntnt.api.model.Rune;
import ru.loper.suntnt.rune.*;
import ru.loper.suntnt.rune.effects.EffectsRune;

@Getter
@RequiredArgsConstructor
public enum RuneType {
    EFFECTS("EFFECTS", EffectsRune::new),
    GRAVITATION("GRAVITATION", GravitationRune::new),
    AUTO_IGNITE("AUTO_IGNITE", AutoIgniteRune::new),
    ASCENSION("ASCENSION", AscensionRune::new);

    private final String name;
    private final Supplier<Rune> runeSupplier;

    public Rune createRune() {
        return runeSupplier.get();
    }

    public static RuneType getByName(String name) {
        return Arrays.stream(values())
                .filter(type -> type.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
