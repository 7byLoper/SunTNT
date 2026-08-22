package ru.loper.suntnt.runnable;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Snowball;
import org.bukkit.util.Vector;
import ru.loper.suncore.api.scheduler.CoreRunnable;

public class TnTGunRunnable extends CoreRunnable {
    private final Snowball snowball;
    private final Vector velocity;

    public TnTGunRunnable(Block block, BlockFace blockFace, Snowball snowball) {
        this.snowball = snowball;
        this.velocity = block.getRelative(blockFace)
                .getLocation()
                .toVector()
                .subtract(block.getLocation().toVector());
    }

    @Override
    public void run() {
        if (snowball.isDead() || !snowball.isValid()) {
            cancel();
            return;
        }

        if (snowball.getPassengers().isEmpty()) {
            snowball.remove();
            cancel();
            return;
        }

        snowball.setVelocity(velocity);
    }
}
