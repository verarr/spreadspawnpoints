package xyz.verarr.spreadspawnpoints.spawnpoints.generators;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import xyz.verarr.spreadspawnpoints.SpreadSpawnPoints;
import xyz.verarr.spreadspawnpoints.mixin.LocalRandomAccessor;
import xyz.verarr.spreadspawnpoints.spawnpoints.SpawnPointGenerator;

import java.util.*;
import java.util.stream.Stream;

public class SpringSpawnPointGenerator implements SpawnPointGenerator {
    private static final int DEFAULT_RESERVE_RADIUS = 128;
    private static final int DEFAULT_OVERLAP_RADIUS = 256;
    private static final int DEFAULT_WORLDSPAWN_RESERVE_RADIUS = 256;
    private static final int DEFAULT_WORLDSPAWN_OVERLAP_RADIUS = 256 + 128;
    private static final List<Vector2i> VECTOR_NEIGHBORS = Arrays.asList(
            new Vector2i(-1, -1),
            new Vector2i(-1, 0),
            new Vector2i(-1, +1),
            new Vector2i(0, -1),
            new Vector2i(0, 0),
            new Vector2i(0, +1),
            new Vector2i(+1, -1),
            new Vector2i(+1, 0),
            new Vector2i(+1, +1)
    );

    // settings
    private final Vector2i worldSpawn;
    private final Pair<Vector2i, Vector2i> bounds;
    private int reserveRadius = DEFAULT_RESERVE_RADIUS;
    private int overlapRadius = DEFAULT_OVERLAP_RADIUS;
    private int worldspawnReserveRadius = DEFAULT_WORLDSPAWN_RESERVE_RADIUS;
    private int worldspawnOverlapRadius = DEFAULT_WORLDSPAWN_OVERLAP_RADIUS;

    // state
    private final Random random;
    private final Map<Vector2i, Set<Vector2i>> grid = new HashMap<>();
    private int greatestDistanceFromWorldspawn = 0;

    public SpringSpawnPointGenerator(ServerWorld serverWorld) {
        WorldBorder border = serverWorld.getWorldBorder();
        Vector2i lowerBounds = new Vector2i((int) border.getBoundWest(), (int) border.getBoundNorth());
        Vector2i upperBounds = new Vector2i((int) border.getBoundEast(), (int) border.getBoundSouth());
        this.bounds = new Pair<>(lowerBounds, upperBounds);
        this.random = new LocalRandom(serverWorld.getSeed());

        BlockPos worldSpawn = serverWorld.getSpawnPos();
        this.worldSpawn = new Vector2i(
                worldSpawn.getX(), worldSpawn.getZ()
        );
    }

    /**
     * Set the bounds in which the spawnpoints will be generated.
     *
     * @param bounds lower bound followed by upper bound.
     */
    public void setBounds(Pair<Vector2i, Vector2i> bounds) {
        this.bounds.setLeft(bounds.getLeft());
        this.bounds.setRight(bounds.getRight());
    }

    /**
     * Generate a new spawnpoint and return it.
     *
     * @return the generated spawnpoint coordinates.
     */
    @Override
    public Vector2i next() {
        int lowerX = MathHelper.clamp(
                -greatestDistanceFromWorldspawn - overlapRadius,
                bounds.getLeft().x, worldSpawn.x - worldspawnOverlapRadius
        );
        int lowerZ = MathHelper.clamp(
                -greatestDistanceFromWorldspawn - overlapRadius,
                bounds.getLeft().y, worldSpawn.y - worldspawnOverlapRadius
        );
        int upperX = MathHelper.clamp(
                greatestDistanceFromWorldspawn + overlapRadius,
                worldSpawn.x + worldspawnOverlapRadius, bounds.getRight().x
        );
        int upperZ = MathHelper.clamp(
                greatestDistanceFromWorldspawn + overlapRadius,
                worldSpawn.y + worldspawnOverlapRadius, bounds.getRight().y
        );

        int i = 0;
        while (i < 100) {
            i++;
            Vector2i randomSpawnPoint =
                    new Vector2i(
                            random.nextBetween(lowerX, upperX),
                            random.nextBetween(lowerZ, upperZ)
                    );
            if (isValid(randomSpawnPoint)) {
                SpreadSpawnPoints.LOGGER.info("Spring spawnpoint generator " +
                        "iterated over {} spawnpoints", i);
                return randomSpawnPoint;
            }
        }
        throw new RuntimeException("Spring spawnpoint generator couldn't " +
                "generate new valid spawnpoint.");
    }

    private Vector2i gridCoordinates(Vector2i worldCoordinates) {
        return new Vector2i(worldCoordinates.x / overlapRadius,
                worldCoordinates.y / overlapRadius);
    }

    private boolean overlaps(Vector2i a, Vector2i b) {
        return a.distance(b) < overlapRadius;
    }

    private boolean conflicts(Vector2i a, Vector2i b) {
        return a.distance(b) < reserveRadius;
    }

    private boolean overlapsWithWorldspawn(Vector2i vec) {
        return vec.distance(worldSpawn) < worldspawnOverlapRadius;
    }

    private boolean conflictsWithWorldspawn(Vector2i vec) {
        return vec.distance(worldSpawn) < worldspawnReserveRadius;
    }

    /**
     * Test if a spawnpoint is valid, as in it may be generated by this generator.
     *
     * @param spawnPoint spawnpoint to test
     * @return true if the spawnpoint may be generated by this generator, false otherwise.
     */
    @Override
    public boolean isValid(Vector2i spawnPoint) {
        if (!(bounds.getLeft().x <= spawnPoint.x &&
                bounds.getRight().x >= spawnPoint.x &&
                bounds.getLeft().y <= spawnPoint.y &&
                bounds.getRight().y >= spawnPoint.y))
            return false;

        int overlaps = 0;

        if (conflictsWithWorldspawn(spawnPoint))
            return false;
        if (overlapsWithWorldspawn(spawnPoint))
            overlaps++;

        List<Vector2i> affectedSpawnPoints =
                getAffectedSpawnPoints(spawnPoint).toList();
        for (Vector2i affectedSpawnPoint : affectedSpawnPoints) {
            if (conflicts(affectedSpawnPoint, spawnPoint))
                return false;
            if (overlaps(affectedSpawnPoint, spawnPoint))
                overlaps++;
        }

        if (overlaps < 1)
            return false;

        return true;
    }

    private @NotNull Stream<Vector2i> getAffectedSpawnPoints(Vector2i spawnPoint) {
        return VECTOR_NEIGHBORS.stream()
                .map(vec -> {
                    try {
                        return (Vector2i) (vec.clone());
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(vec -> vec.add(gridCoordinates(spawnPoint)))
                .filter(grid::containsKey)
                .flatMap(gridCoordinates -> grid.get(gridCoordinates).stream());
    }

    /**
     * Internal method to add a spawnpoint to the generator. Only use this if necessary.
     * <p>
     * Implementations may ignore this method.
     *
     * @param spawnPoint spawnpoint to add
     */
    @Override
    public void add(Vector2i spawnPoint) {
        if ((int) spawnPoint.distance(worldSpawn) > greatestDistanceFromWorldspawn)
            greatestDistanceFromWorldspawn = (int) spawnPoint.distance(worldSpawn);
        grid.putIfAbsent(gridCoordinates(spawnPoint), new HashSet<>());
        grid.get(gridCoordinates(spawnPoint)).add(spawnPoint);
    }

    /**
     * Internal method to remove a spawnpoint from the generator. Only use this if necessary.
     * <p>
     * Implementations may ignore this method.
     *
     * @param spawnPoint spawnpoint to remove
     */
    @Override
    public void remove(Vector2i spawnPoint) {
        if (!grid.containsKey(gridCoordinates(spawnPoint)))
            return;
        grid.get(gridCoordinates(spawnPoint)).remove(spawnPoint);
    }

    // NBTSerializable stuff
    @Override
    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("lowerX", bounds.getLeft().x);
        nbt.putInt("upperX", bounds.getRight().x);
        nbt.putInt("lowerZ", bounds.getLeft().y);
        nbt.putInt("upperZ", bounds.getRight().y);

        nbt.putLong("seed", ((LocalRandomAccessor) random).getSeed());

        nbt.putInt("reserveRadius", reserveRadius);
        nbt.putInt("overlapRadius", overlapRadius);
        nbt.putInt("worldspawnReserveRadius", worldspawnReserveRadius);
        nbt.putInt("worldspawnOverlapRadius", worldspawnOverlapRadius);

        nbt.putInt("worldspawnX", worldSpawn.x);
        nbt.putInt("worldspawnZ", worldSpawn.y);

        return nbt;
    }

    @Override
    public void modifyFromNbt(NbtCompound tag) {
        Vector2i lowerBounds = new Vector2i(
                tag.getInt("lowerX"),
                tag.getInt("lowerZ")
        );
        Vector2i upperBounds = new Vector2i(
                tag.getInt("upperX"),
                tag.getInt("upperZ")
        );
        setBounds(new Pair<>(lowerBounds, upperBounds));

        random.setSeed(tag.getLong("seed"));

        reserveRadius = tag.getInt("reserveRadius");
        overlapRadius = tag.getInt("overlapRadius");
        worldspawnReserveRadius = tag.getInt("worldspawnReserveRadius");
        worldspawnOverlapRadius = tag.getInt("worldspawnOverlapRadius");

        worldSpawn.x = tag.getInt("worldspawnX");
        worldSpawn.y = tag.getInt("worldspawnZ");
    }

    @Override
    public void modifyFromNbtPartial(NbtCompound tag) {
        Vector2i lowerBounds = bounds.getLeft();
        Vector2i upperBounds = bounds.getRight();
        if (tag.contains("lowerX", 3))
            lowerBounds.x = tag.getInt("lowerX");
        if (tag.contains("lowerZ", 3))
            lowerBounds.y = tag.getInt("lowerZ");
        if (tag.contains("upperX", 3))
            upperBounds.x = tag.getInt("upper");
        if (tag.contains("upperZ", 3))
            upperBounds.y = tag.getInt("upperZ");
        setBounds(new Pair<>(lowerBounds, upperBounds));

        if (tag.contains("seed", 4))
            random.setSeed(tag.getLong("seed"));

        if (tag.contains("reserveRadius", 3))
            reserveRadius = tag.getInt("reserveRadius");
        if (tag.contains("overlapRadius", 3))
            overlapRadius = tag.getInt("overlapRadius");
        if (tag.contains("worldspawnReserveRadius", 3))
            worldspawnReserveRadius = tag.getInt("worldspawnReserveRadius");
        if (tag.contains("worldspawnOverlapRadius", 3))
            worldspawnOverlapRadius = tag.getInt("worldspawnOverlapRadius");

        if (tag.contains("worldspawnX", 3))
            worldSpawn.x = tag.getInt("worldspawnX");
        if (tag.contains("worldspawnZ", 3))
            worldSpawn.y = tag.getInt("worldspawnZ");
    }
}
