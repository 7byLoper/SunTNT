package ru.loper.suntnt.rune;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.util.Vector;
import ru.loper.suntnt.api.model.Rune;

public class AscensionRune extends Rune {

    private double upwardVelocity;
    private boolean randomDirection;
    private double horizontalSpread;

    @Override
    public void onLoad(final ConfigurationSection section) {
        upwardVelocity = section.getDouble("velocity.upward", 2.0);
        randomDirection = section.getBoolean("velocity.random_direction", false);
        horizontalSpread = section.getDouble("velocity.horizontal_spread", 0.5);
    }

    @Override
    public void handleSpawn(final EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }

        final Vector velocity = new Vector(0, upwardVelocity, 0);
        if (randomDirection) {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            velocity.setX((random.nextDouble() - 0.5) * horizontalSpread);
            velocity.setZ((random.nextDouble() - 0.5) * horizontalSpread);
        }

        tnt.setVelocity(velocity);
    }
}
