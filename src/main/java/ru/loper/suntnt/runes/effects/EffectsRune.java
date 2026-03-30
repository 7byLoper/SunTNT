package ru.loper.suntnt.runes.effects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.loper.suntnt.api.modules.Rune;

import java.util.List;
import java.util.Objects;

public class EffectsRune extends Rune {
    protected List<PotionEffect> runeEffects;
    protected EffectsTarget effectsTarget;
    protected GiveAction giveAction;

    protected int effectsRadius;

    @Override
    public void onLoad(ConfigurationSection section) {
        runeEffects = section.getStringList("effects.potions").stream()
                .map(this::parseEffect)
                .filter(Objects::nonNull)
                .toList();

        effectsTarget = EffectsTarget.getByName(section.getString("effects.target", ""));
        giveAction = GiveAction.getByName(section.getString("effects.give_action", ""));

        effectsRadius = section.getInt("effects.radius");
    }

    protected List<LivingEntity> getTargets(Location location) {
        return location.getNearbyLivingEntities(effectsRadius, effectsRadius, effectsRadius)
                .stream()
                .filter(entity -> switch (effectsTarget) {
                    case PLAYERS -> entity instanceof Player;
                    case ENTITIES -> entity.getType() != EntityType.PLAYER && entity.getType() != EntityType.PRIMED_TNT;
                    case ALL -> true;
                })
                .toList();
    }

    public void handleExplosion(TNTPrimed tntPrimed) {
        if (giveAction != GiveAction.EXPLOSION) {
            return;
        }

        getTargets(tntPrimed.getLocation())
                .forEach(entity -> entity.addPotionEffects(runeEffects));
    }

    public void handleSpawn(EntitySpawnEvent event) {
        if (giveAction != GiveAction.PRIME) {
            return;
        }


        getTargets(event.getLocation())
                .forEach(entity -> entity.addPotionEffects(runeEffects));
    }

    private PotionEffect parseEffect(String effectString) {
        String[] effectValues = effectString.split(":");

        PotionEffectType effectType = PotionEffectType.getByName(effectValues[0]);
        if (effectType == null) {
            return null;
        }

        int effectDuration = Integer.parseInt(effectValues.length > 1 ? effectValues[1] : "20");
        int effectAmplifier = Integer.parseInt(effectValues.length > 2 ? effectValues[2] : "1") - 1;

        return new PotionEffect(effectType, Math.max(effectDuration, 0), Math.max(effectAmplifier, 0));
    }
}
