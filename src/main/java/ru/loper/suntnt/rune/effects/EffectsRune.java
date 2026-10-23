package ru.loper.suntnt.rune.effects;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.loper.suntnt.api.model.Rune;

public class EffectsRune extends Rune {

    private static final Pattern COLON_PATTERN = Pattern.compile(":");
    private static final int DEFAULT_DURATION = 20;

    protected List<PotionEffect> runeEffects;
    protected EffectsTarget effectsTarget;
    protected GiveAction giveAction;
    protected int effectsRadius;

    @Override
    public void onLoad(final ConfigurationSection section) {
        runeEffects = section.getStringList("effects.potions").stream()
                .map(this::parseEffect)
                .filter(Objects::nonNull)
                .toList();

        effectsTarget = EffectsTarget.getByName(section.getString("effects.target", ""));
        giveAction = GiveAction.getByName(section.getString("effects.give_action", ""));
        effectsRadius = section.getInt("effects.radius");
    }

    protected List<LivingEntity> getTargets(final Location location) {
        if (effectsTarget == null) {
            return List.of();
        }

        return location.getNearbyLivingEntities(effectsRadius, effectsRadius, effectsRadius).stream()
                .filter(entity -> switch (effectsTarget) {
                    case PLAYERS -> entity instanceof Player;
                    case ENTITIES -> entity.getType() != EntityType.PLAYER;
                    case ALL -> true;
                })
                .toList();
    }

    @Override
    public void handleExplosion(final TNTPrimed tntPrimed) {
        if (giveAction != GiveAction.EXPLOSION) {
            return;
        }

        getTargets(tntPrimed.getLocation()).forEach(entity -> entity.addPotionEffects(runeEffects));
    }

    @Override
    public void handleSpawn(final EntitySpawnEvent event) {
        if (giveAction != GiveAction.PRIME) {
            return;
        }

        getTargets(event.getLocation()).forEach(entity -> entity.addPotionEffects(runeEffects));
    }

    private PotionEffect parseEffect(final String effectString) {
        final String[] effectValues = COLON_PATTERN.split(effectString);
        final PotionEffectType effectType = PotionEffectType.getByName(effectValues[0]);
        if (effectType == null) {
            return null;
        }

        int duration = DEFAULT_DURATION;
        if (effectValues.length > 1) {
            try {
                duration = Integer.parseInt(effectValues[1]);
            } catch (NumberFormatException ignored) {}
        }

        int amplifier = 0;
        if (effectValues.length > 2) {
            try {
                amplifier = Integer.parseInt(effectValues[2]) - 1;
            } catch (NumberFormatException ignored) {}
        }

        return new PotionEffect(effectType, Math.max(duration, 0), Math.max(amplifier, 0));
    }
}
