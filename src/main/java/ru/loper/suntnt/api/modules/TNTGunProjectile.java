package ru.loper.suntnt.api.modules;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;

@Data
@RequiredArgsConstructor
public class TNTGunProjectile {
    private final Block block;
    private TNTPrimed tntPrimed;
}
