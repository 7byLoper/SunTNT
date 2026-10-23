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

    private static final long CACHE_EXPIRE_SECONDS = 10L;

    private final Cache<Block, CustomTNT> cachedTNTs =
            CacheBuilder.newBuilder().expireAfterWrite(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).build();

    private final Cache<Block, TNTGunProjectile> cachedGunProjectiles =
            CacheBuilder.newBuilder().expireAfterWrite(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).build();

    private final Cache<Block, String> cachedRunes =
            CacheBuilder.newBuilder().expireAfterWrite(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS).build();
}
