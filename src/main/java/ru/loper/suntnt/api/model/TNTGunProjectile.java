package ru.loper.suntnt.api.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;

@Getter
@Setter
@RequiredArgsConstructor
public class TNTGunProjectile {

    private final Block block;
    private TNTPrimed tntPrimed;
}
