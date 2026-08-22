package ru.loper.suntnt.manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.bukkit.block.Block;
import ru.loper.suntnt.api.model.CustomTNT;
import ru.loper.suntnt.api.model.TNTGunProjectile;

@Getter
public class TNTSpawnManager {
    private final Cache<Block, CustomTNT> cachedTNTs =
            CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();

    private final Cache<Block, TNTGunProjectile> cachedGunProjectiles =
            CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();

    private final Cache<Block, String> cachedRunes =
            CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();
}
