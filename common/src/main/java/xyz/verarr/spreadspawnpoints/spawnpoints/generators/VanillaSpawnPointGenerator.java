package xyz.verarr.spreadspawnpoints.spawnpoints.generators;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import org.joml.Vector2i;
import xyz.verarr.spreadspawnpoints.spawnpoints.SpawnPointGenerator;

public class VanillaSpawnPointGenerator implements SpawnPointGenerator {
    private final ServerWorld world;

    public VanillaSpawnPointGenerator(ServerWorld world) {
        this.world = world;
    }

    /**
     * Generate a new spawnpoint and return it.
     *
     * @return the generated spawnpoint coordinates.
     */
    @Override
    public Vector2i next() {
        return new Vector2i(
                world.getLevelProperties().getSpawnX(),
                world.getLevelProperties().getSpawnZ()
        );
    }

    /**
     * Test if a spawnpoint is valid, as in it may be generated by this generator.
     *
     * @param spawnPoint spawnpoint to test
     * @return true if the spawnpoint may be generated by this generator, false otherwise.
     */
    @Override
    public boolean isValid(Vector2i spawnPoint) {
        return (
                spawnPoint.x == world.getLevelProperties().getSpawnX() &&
                        spawnPoint.y == world.getLevelProperties().getSpawnZ()
        );
    }

    /**
     * This method is ignored.
     */
    @Override
    public void add(Vector2i spawnPoint) {}

    /**
     * This method is ignored.
     */
    @Override
    public void remove(Vector2i spawnPoint) {}

    // NBTSerializable stuff (ignored)
    @Override
    public NbtCompound writeNbt() {
        return new NbtCompound();
    }

    @Override
    public void modifyFromNbt(NbtCompound tag) {}

    @Override
    public void modifyFromNbtPartial(NbtCompound tag) throws IllegalArgumentException {}
}
