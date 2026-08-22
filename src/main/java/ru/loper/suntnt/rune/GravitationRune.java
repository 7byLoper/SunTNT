package ru.loper.suntnt.rune;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.util.Vector;
import ru.loper.suntnt.api.model.Rune;

@Getter
public class GravitationRune extends Rune {
    @Override
    public void onLoad(ConfigurationSection section) {}

    @Override
    public void handleSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }

        tnt.setGravity(false);
        tnt.setVelocity(new Vector(0, 0, 0));
    }
}
