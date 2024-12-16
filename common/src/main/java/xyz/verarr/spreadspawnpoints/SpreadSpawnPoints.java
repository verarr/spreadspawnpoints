package xyz.verarr.spreadspawnpoints;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.verarr.spreadspawnpoints.spawnpoints.SpawnPointManager;
import xyz.verarr.spreadspawnpoints.commands.SpawnpointsCommand;
import xyz.verarr.spreadspawnpoints.spawnpoints.generators.GridSpawnPointGenerator;
import xyz.verarr.spreadspawnpoints.spawnpoints.generators.RandomSpawnPointGenerator;
import xyz.verarr.spreadspawnpoints.spawnpoints.generators.SpringSpawnPointGenerator;
import xyz.verarr.spreadspawnpoints.spawnpoints.generators.VanillaSpawnPointGenerator;

public final class SpreadSpawnPoints {
    public static final String MOD_ID = "spreadspawnpoints";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Hello Minecraft modding world!");

        registerSpawnPointGenerators();
        LOGGER.info("Registered Spawn Point Generators!");

        initCommands();
    }

    static void registerSpawnPointGenerators() {
        SpawnPointManager.registerSpawnPointGenerator(
                Identifier.of(MOD_ID, "vanilla"),
                VanillaSpawnPointGenerator.class
        );
        SpawnPointManager.registerSpawnPointGenerator(
                Identifier.of(MOD_ID, "random"),
                RandomSpawnPointGenerator.class
        );
        SpawnPointManager.registerSpawnPointGenerator(
                Identifier.of(MOD_ID, "grid"),
                GridSpawnPointGenerator.class
        );
        SpawnPointManager.registerSpawnPointGenerator(
                Identifier.of(MOD_ID, "spring"),
                SpringSpawnPointGenerator.class
        );
    }

    static void initCommands() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(SpawnpointsCommand.command));
    }
}
