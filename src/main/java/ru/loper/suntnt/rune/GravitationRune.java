package ru.loper.suntnt.rune;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.util.Vector;
import ru.loper.suntnt.api.model.Rune;

@Getter
public class GravitationRune extends Rune {

    private static final Vector ZERO_VELOCITY = new Vector(0, 0, 0);

    @Override
    public void onLoad(final ConfigurationSection section) {}

    @Override
    public void handleSpawn(final EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }

        tnt.setGravity(false);
        tnt.setVelocity(ZERO_VELOCITY);
    }
}
