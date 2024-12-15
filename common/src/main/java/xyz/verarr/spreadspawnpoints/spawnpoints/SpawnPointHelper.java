package xyz.verarr.spreadspawnpoints.spawnpoints;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.server.network.SpawnLocating;

public class SpawnPointHelper extends SpawnLocating {
    public static boolean isValidSpawnPoint(ServerWorld world, BlockPos pos) {
        int i = Math.max(0, world.getServer().getSpawnRadius(world));
        int j = MathHelper.floor(world.getWorldBorder().getDistanceInsideBorder((double)pos.getX(), (double)pos.getZ()));
        if (j < i) {
            i = j;
        }

        if (j <= 1) {
            i = 1;
        }

        long l = (long)(i * 2 + 1);
        long m = l * l;
        int k = m > 2147483647L ? Integer.MAX_VALUE : (int)m;
        int n = calculateSpawnOffsetMultiplier(k);
        int o = Random.create().nextInt(k);

        for(int p = 0; p < k; ++p) {
            int q = (o + n * p) % k;
            int r = q % (i * 2 + 1);
            int s = q / (i * 2 + 1);
            BlockPos blockPos2 = findOverworldSpawn(world, pos.getX() + r - i, pos.getZ() + s - i);
            if (blockPos2 != null) {
                return true;
            }
        }
        return false;
    }

    private static int calculateSpawnOffsetMultiplier(int horizontalSpawnArea) {
        return horizontalSpawnArea <= 16 ? horizontalSpawnArea - 1 : 17;
    }
}
