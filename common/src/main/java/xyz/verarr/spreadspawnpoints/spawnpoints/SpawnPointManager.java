package xyz.verarr.spreadspawnpoints.spawnpoints;

import net.minecraft.util.Identifier;
import org.spongepowered.include.com.google.common.collect.BiMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import org.joml.Vector2i;
import org.spongepowered.include.com.google.common.collect.HashBiMap;
import xyz.verarr.spreadspawnpoints.SpreadSpawnPoints;
import xyz.verarr.spreadspawnpoints.spawnpoints.generators.VanillaSpawnPointGenerator;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnPointManager extends PersistentState {
    private static final Class<? extends SpawnPointGenerator> DEFAULT_SPAWNPOINT_GENERATOR = VanillaSpawnPointGenerator.class;

    private static final BiMap<Identifier, Class<? extends SpawnPointGenerator>> registeredSpawnPointGenerators = HashBiMap.create();

    public static void registerSpawnPointGenerator(Identifier identifier, Class<? extends SpawnPointGenerator> generator) {
        registeredSpawnPointGenerators.put(identifier, generator);
    }
    public static boolean spawnPointGeneratorExists(Identifier identifier) {
        return registeredSpawnPointGenerators.containsKey(identifier);
    }
    private static Class<? extends SpawnPointGenerator> lookupSpawnPointGenerator(Identifier identifier) {
        return registeredSpawnPointGenerators.get(identifier);
    }
    private static Identifier lookupSpawnPointGeneratorIdentifier(Class<? extends SpawnPointGenerator> generator) {
        return registeredSpawnPointGenerators.inverse().get(generator);
    }

    private final Map<UUID, Vector2i> playerSpawnPoints = new HashMap<>();
    private final Map<UUID, Vector2i> teamSpawnPoints = new HashMap<>();

    private ServerWorld serverWorld;
    private SpawnPointGenerator spawnPointGenerator;

    private static SpawnPointGenerator constructSpawnPointGeneratorForWorld(Class<? extends SpawnPointGenerator> generatorType, ServerWorld world) {
        SpawnPointGenerator generator;
        try {
            generator = generatorType.getConstructor(ServerWorld.class).newInstance(world);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("SpawnPointGenerator must have a constructor with a ServerWorld parameter", e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate SpawnPointGenerator: " + e, e);
        }
        return generator;
    }

    private SpawnPointManager() {}
    private SpawnPointManager(ServerWorld world) {
        this.spawnPointGenerator = constructSpawnPointGeneratorForWorld(DEFAULT_SPAWNPOINT_GENERATOR, world);
        this.serverWorld = world;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound playerSpawnPointsNbt = new NbtCompound();
        playerSpawnPoints.forEach((uuid, spawnPoint) -> {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putInt("x", spawnPoint.x);
            playerNbt.putInt("z", spawnPoint.y);
            playerSpawnPointsNbt.put(uuid.toString(), playerNbt);
        });
        nbt.put("playerSpawnPoints", playerSpawnPointsNbt);

        nbt.putString("spawnPointGenerator", lookupSpawnPointGeneratorIdentifier(spawnPointGenerator.getClass()).toString());

        nbt.put("spawnPointGeneratorData", spawnPointGenerator.writeNbt());

        return nbt;
    }
    public static SpawnPointManager createFromNbt(NbtCompound tag, ServerWorld world) {
        SpawnPointManager spawnPointManager = new SpawnPointManager();
        spawnPointManager.spawnPointGenerator = constructSpawnPointGeneratorForWorld(
                lookupSpawnPointGenerator(new Identifier(tag.getString("spawnPointGenerator"))),
                world
        );
        NbtCompound playerSpawnPointsNbt = tag.getCompound("playerSpawnPoints");
        playerSpawnPointsNbt.getKeys().forEach(key -> {
            UUID uuid = UUID.fromString(key);
            NbtCompound playerNbt = playerSpawnPointsNbt.getCompound(key);
            Vector2i spawnPoint = new Vector2i(
                    playerNbt.getInt("x"),
                    playerNbt.getInt("y")
            );
            spawnPointManager.playerSpawnPoints.put(uuid, spawnPoint);
        });
        spawnPointManager.spawnPointGenerator.modifyFromNbt(tag.getCompound("spawnPointGeneratorData"));
        return spawnPointManager;
    }
    public static SpawnPointManager getInstance(ServerWorld world) {
        SpawnPointManager spawnPointManager = world.getPersistentStateManager().getOrCreate(
                tag -> createFromNbt(tag, world),
                () -> new SpawnPointManager(world),
                SpreadSpawnPoints.MOD_ID
        );
        spawnPointManager.markDirty(); // mark dirty always, as per the Fabric wiki
        return spawnPointManager;
    }

    public Identifier getSpawnPointGenerator() {
        return lookupSpawnPointGeneratorIdentifier(spawnPointGenerator.getClass());
    }
    public void setSpawnPointGenerator(Identifier identifier) {
        spawnPointGenerator = constructSpawnPointGeneratorForWorld(lookupSpawnPointGenerator(identifier), serverWorld);
    }

    public void resetSpawnPoints() {
        playerSpawnPoints.clear();
    }

    public Vector2i getSpawnPoint(PlayerEntity player) {
        return playerSpawnPoints.computeIfAbsent(
                player.getUuid(),
                uuid -> spawnPointGenerator.next()
        );
    }
}