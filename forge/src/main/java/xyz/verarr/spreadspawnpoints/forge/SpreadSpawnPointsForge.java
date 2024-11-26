package xyz.verarr.spreadspawnpoints.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import xyz.verarr.spreadspawnpoints.SpreadSpawnPoints;

@Mod(SpreadSpawnPoints.MOD_ID)
public final class SpreadSpawnPointsForge {
    public SpreadSpawnPointsForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(SpreadSpawnPoints.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        SpreadSpawnPoints.init();
    }
}
