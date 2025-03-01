package xyz.verarr.spreadspawnpoints.spawnpoints;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector2i;
import xyz.verarr.spreadspawnpoints.SpreadSpawnPoints;
import xyz.verarr.spreadspawnpoints.spawnpoints.generators.VanillaSpawnPointGenerator;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public class SpawnPointGeneratorManager {
    private static final Class<? extends SpawnPointGenerator> DEFAULT_SPAWNPOINT_GENERATOR = VanillaSpawnPointGenerator.class;

    private static final BiMap<Identifier, Class<? extends SpawnPointGenerator>> registeredSpawnPointGenerators = HashBiMap.create();

    /**
     * Register a new spawnpoint generator type. Make sure to call this for
     * any spawnpoint generator implementations you wish to use.
     *
     * @param identifier the desired identifier for the generator
     * @param generator  the generator's class
     * @throws IllegalArgumentException if given SpawnPointGenerator doesn't
     *                                  have a constructor with a ServerWorld parameter
     */
    public static void registerSpawnPointGenerator(Identifier identifier, Class<? extends SpawnPointGenerator> generator) {
        try {
            generator.getConstructor(ServerWorld.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("SpawnPointGenerator must have a constructor with a ServerWorld parameter", e);
        }

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

    public static Collection<Identifier> getRegisteredSpawnPointGenerators() {
        return registeredSpawnPointGenerators.keySet();
    }

    /**
     * Constructs a spawnpoint generator of the specified type for the
     * specified world.
     *
     * @param generatorType type of the generator to construct
     * @param world         server world to use in the constructor
     * @return the newly constructed spawnpoint generator
     * @throws IllegalArgumentException if given SpawnPointGenerator doesn't
     *                                  have a constructor with a ServerWorld parameter
     */
    private static SpawnPointGenerator constructSpawnPointGeneratorForWorld(Class<? extends SpawnPointGenerator> generatorType, ServerWorld world) {
        SpawnPointGenerator generator;
        try {
            generator = generatorType.getConstructor(ServerWorld.class).newInstance(world);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("SpawnPointGenerator must have a constructor with a ServerWorld parameter", e);
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate SpawnPointGenerator: " + e, e);
        }
        return generator;
    }

    private final ServerWorld serverWorld;
    private SpawnPointGenerator generator;

    public SpawnPointGeneratorManager(ServerWorld world) {
        this.generator = constructSpawnPointGeneratorForWorld(DEFAULT_SPAWNPOINT_GENERATOR, world);
        this.serverWorld = world;
    }

    /**
     * Gets the identifier of the spawnpoint generator currently in use.
     *
     * @return the identifier of the spawnpoint generator
     */
    public Identifier getSpawnPointGenerator() {
        return lookupSpawnPointGeneratorIdentifier(generator.getClass());
    }

    /**
     * Replaces the currently used spawnpoint generator with a newly
     * constructed one.
     *
     * @see #registerSpawnPointGenerator(Identifier, Class)
     *
     * @param identifier the identifier of the new spawnpoint generator type
     */
    public void setSpawnPointGenerator(Identifier identifier) {
        generator = constructSpawnPointGeneratorForWorld(lookupSpawnPointGenerator(identifier), serverWorld);
    }

    /**
     * Generate a new spawnpoint that is <b>not</b> guaranteed to be safe.
     *
     * @return new spawnpoint
     */
    public Vector2i nextUnsafe() {
        return generator.next();
    }

    /**
     * Generate a new spawnpoint, <b>iteratively trying</b> until a valid
     * spawnpoint is found.
     *
     * @return new valid spawnpoint
     */
    public Vector2i nextSafe() {
        int vanillaInvalid = 0;
        int customInvalid = 0;
        while (true) {
            if (vanillaInvalid + customInvalid % 100 == 0 && vanillaInvalid + customInvalid != 0)
                SpreadSpawnPoints.LOGGER.warn("Iterating through {}th spawnpoint", vanillaInvalid + customInvalid);

            Vector2i spawnPoint = generator.next();

            boolean customValid = generator.isValid(spawnPoint);
            if (!customValid) {
                customInvalid++;
                continue;
            }

            boolean vanillaValid = SpawnPointHelper.isValidSpawnPoint(serverWorld, new BlockPos(spawnPoint.x, 0, spawnPoint.y));
            if (!vanillaValid) {
                vanillaInvalid++;
                continue;
            }

            if (vanillaInvalid + customInvalid > 1)
                SpreadSpawnPoints.LOGGER.info("Iterated through {} spawnpoints ({} gamerule-invalid, {} generator-invalid) before valid spawnpoint found",
                        vanillaInvalid + customInvalid, vanillaInvalid, customInvalid);
            generator.add(spawnPoint);
            return spawnPoint;
        }
    }

    /**
     * Serialize data of currently active spawnpoint generator to NBT. This may
     * be settings or state. It is up to the generator implementation to
     * construct the NBT Compound.
     *
     * @return data of currently active spawnpoint generator as NBT
     */
    public NbtCompound writeNbt() {
        return generator.writeNbt();
    }

    /**
     * Update data of currently active spawnpoint generator. This may be
     * settings or state. It is up to the generator implementation to handle
     * the data passed.
     *
     * @param nbt NBT data to be passed to the generator
     * @see SpawnPointGenerator#modifyFromNbt(NbtCompound)
     */
    public void modifyFromNbt(NbtCompound nbt) {
        generator.modifyFromNbt(nbt);
    }

    /**
     * Update data of currently active spawnpoint generator. This may be
     * settings or state. It is up to the generator implementation to handle
     * the data passed.
     *
     * @param nbt NBT data to be passed to the generator
     * @see SpawnPointGenerator#modifyFromNbtPartial(NbtCompound)
     */
    public void modifyFromNbtPartial(NbtCompound nbt) throws IllegalArgumentException {
        generator.modifyFromNbtPartial(nbt);
    }

    public void addSpawnPoint(Vector2i spawnPoint) {
        generator.add(spawnPoint);
    }

    public void removeSpawnPoint(Vector2i spawnPoint) {
        generator.remove(spawnPoint);
    }
}
