package xyz.verarr.spreadspawnpoints.spawnpoints;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Contract;
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
import java.util.Objects;
import java.util.UUID;

public class SpawnPointManager extends PersistentState {
    private static final Class<? extends SpawnPointGenerator> DEFAULT_SPAWNPOINT_GENERATOR = VanillaSpawnPointGenerator.class;

    private static final BiMap<Identifier, Class<? extends SpawnPointGenerator>> registeredSpawnPointGenerators = HashBiMap.create();

    /**
     * Register a new spawnpoint generator type. Make sure to call this for
     * any spawnpoint generator implementations you wish to use.
     *
     * @param identifier the desired identifier for the generator
     * @param generator the generator's class
     */
    public static void registerSpawnPointGenerator(Identifier identifier, Class<? extends SpawnPointGenerator> generator) {
        registeredSpawnPointGenerators.put(identifier, generator);
    }

    /**
     * Check if a generator has been registered by an identifier.
     *
     * @param identifier the identifier to check for
     * @return true if the generator has been registered before, false otherwise
     */
    public static boolean spawnPointGeneratorExists(Identifier identifier) {
        return registeredSpawnPointGenerators.containsKey(identifier);
    }

    /**
     * Gets the class of a spawnpoint generator from its identifier.
     *
     * @param identifier the identifier to query for
     * @return the generator's class represented by the given identifier
     */
    private static Class<? extends SpawnPointGenerator> lookupSpawnPointGenerator(Identifier identifier) {
        return registeredSpawnPointGenerators.get(identifier);
    }

    /**
     * Gets the identifier of a spawnpoint generator from its class.
     *
     * @param generator the class to query for
     * @return the generator's identifier representing its class
     */
    private static Identifier lookupSpawnPointGeneratorIdentifier(Class<? extends SpawnPointGenerator> generator) {
        return registeredSpawnPointGenerators.inverse().get(generator);
    }

    /**
     * Constructs a spawnpoint generator of the specified type for the
     * specified world.
     *
     * @param generatorType type of the generator to construct
     * @param world server world to use in the constructor
     * @return the newly constructed spawnpoint generator
     */
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

    private final Map<UUID, Vector2i> playerSpawnPoints = new HashMap<>();

    private ServerWorld serverWorld;
    private SpawnPointGenerator spawnPointGenerator;

    private SpawnPointManager() {}
    private SpawnPointManager(ServerWorld world) {
        this.spawnPointGenerator = constructSpawnPointGeneratorForWorld(DEFAULT_SPAWNPOINT_GENERATOR, world);
        this.serverWorld = world;
    }

    /**
     * Gets the identifier of the spawnpoint generator currently in use.
     *
     * @return the identifier of the spawnpoint generator
     */
    public Identifier getSpawnPointGenerator() {
        return lookupSpawnPointGeneratorIdentifier(spawnPointGenerator.getClass());
    }

    /**
     * Replaces the currently used spawnpoint generator with a newly
     * constructed one.
     *
     * @param identifier the identifier of the new spawnpoint generator type
     */
    public void setSpawnPointGenerator(Identifier identifier) {
        spawnPointGenerator = constructSpawnPointGeneratorForWorld(lookupSpawnPointGenerator(identifier), serverWorld);
    }

    /**
     * Erases all spawnpoints.
     */
    public void resetSpawnPoints() {
        playerSpawnPoints.clear();
    }

    private Vector2i getNewSpawnpoint(ServerWorld world) {
        int i = 0;
        while (true) {
            i++;
            Vector2i spawnPoint = spawnPointGenerator.next();
            if (SpawnPointHelper.isValidSpawnPoint(world,
                    new BlockPos(spawnPoint.x, 0, spawnPoint.y))) {
               if (i > 1)
                   SpreadSpawnPoints.LOGGER.info("Iterated through {} " +
                        "spawnpoints before valid spawnpoint found", i);
               spawnPointGenerator.add(spawnPoint);
               return spawnPoint;
            }
        }
    }

    /**
     * Gets the spawnpoint of a player, or generates a new one if it doesn't
     * exist yet.
     * @param player the player to get the spawnpoint for.
     * @return the spawnpoint of the player
     */
    public Vector2i getSpawnPoint(PlayerEntity player) {
        try {
            return (Vector2i) playerSpawnPoints.computeIfAbsent(
                    player.getUuid(),
                    uuid -> getNewSpawnpoint(getPlayerServerWorld(player))
            ).clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private static ServerWorld getPlayerServerWorld(PlayerEntity player) {
        return Objects.requireNonNull(player.getServer())
                .getWorld(player.getWorld().getRegistryKey());
    }

    public void updateGeneratorData(NbtCompound nbt) {
        spawnPointGenerator.modifyFromNbtPartial(nbt);
    }

    // PersistentState stuff
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
        spawnPointManager.serverWorld = world;
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
                    playerNbt.getInt("z")
            );
            spawnPointManager.playerSpawnPoints.put(uuid, spawnPoint);
            spawnPointManager.spawnPointGenerator.add(spawnPoint);
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
}
