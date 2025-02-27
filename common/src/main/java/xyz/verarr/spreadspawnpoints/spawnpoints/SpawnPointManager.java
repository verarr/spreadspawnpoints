package xyz.verarr.spreadspawnpoints.spawnpoints;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import org.joml.Vector2i;
import xyz.verarr.spreadspawnpoints.SpreadSpawnPoints;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SpawnPointManager extends PersistentState {
    private final Map<UUID, Vector2i> playerSpawnPoints = new HashMap<>();

    public SpawnPointGeneratorManager generatorManager;

    private SpawnPointManager() {}

    private SpawnPointManager(ServerWorld world) {
        this.generatorManager = new SpawnPointGeneratorManager(world);
    }

    /**
     * Erases all spawnpoints.
     */
    public void resetSpawnPoints() {
        playerSpawnPoints.clear();
    }

    /**
     * Gets the spawnpoint of a player, or generates a new one if it doesn't
     * exist yet.
     *
     * @param player the player to get the spawnpoint for.
     * @return the spawnpoint of the player
     */
    public Vector2i getSpawnPoint(PlayerEntity player) {
        try {
            return (Vector2i) playerSpawnPoints.computeIfAbsent(
                    player.getUuid(),
                    uuid -> generatorManager.nextSafe()
            ).clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resets the spawnpoint of a player.
     *
     * @param player the player to reset the spawnpoint of.
     * @return <code>true</code> if there was a spawnpoint associated with
     * <code>player</code>, otherwise <code>false</code>.
     * @see #resetSpawnPoints()
     */
    public boolean resetSpawnPoint(PlayerEntity player) {
        return Objects.nonNull(playerSpawnPoints.remove(player.getUuid()));
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

        nbt.putString("spawnPointGenerator", generatorManager.getSpawnPointGenerator().toString());

        nbt.put("spawnPointGeneratorData", generatorManager.writeNbt());

        return nbt;
    }

    public static SpawnPointManager createFromNbt(NbtCompound tag, ServerWorld world) {
        SpawnPointManager spawnPointManager = new SpawnPointManager();

        spawnPointManager.generatorManager = new SpawnPointGeneratorManager(world);
        spawnPointManager.generatorManager.setSpawnPointGenerator(new Identifier(tag.getString("spawnPointGenerator")));

        NbtCompound playerSpawnPointsNbt = tag.getCompound("playerSpawnPoints");
        playerSpawnPointsNbt.getKeys().forEach(key -> {
            UUID uuid = UUID.fromString(key);
            NbtCompound playerNbt = playerSpawnPointsNbt.getCompound(key);
            Vector2i spawnPoint = new Vector2i(
                    playerNbt.getInt("x"),
                    playerNbt.getInt("z")
            );
            spawnPointManager.playerSpawnPoints.put(uuid, spawnPoint);
            spawnPointManager.generatorManager.addSpawnPoint(spawnPoint);
        });
        spawnPointManager.generatorManager.modifyFromNbt(tag.getCompound("spawnPointGeneratorData"));
        return spawnPointManager;
    }

    /**
     * Get the SpawnPointManager instance associated with a world.
     *
     * @param world the world to get the manager instance for
     * @return SpawnPointManager instance for the specified world
     */
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
