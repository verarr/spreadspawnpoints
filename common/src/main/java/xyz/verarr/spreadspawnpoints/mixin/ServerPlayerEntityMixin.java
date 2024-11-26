package xyz.verarr.spreadspawnpoints.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.verarr.spreadspawnpoints.SpreadSpawnPoints;
import xyz.verarr.spreadspawnpoints.spawnpoints.SpawnPointManager;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Redirect(
            method = "moveToSpawn(Lnet/minecraft/server/world/ServerWorld;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getSpawnPos()Lnet/minecraft/util/math/BlockPos;")
    )
    BlockPos getSpecificSpawnPos(ServerWorld world) {
        SpreadSpawnPoints.LOGGER.info("Player is being spawned in the world: {}", world.getRegistryKey().getValue().toString());
        SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(world);
        Vector2i spawnPoint = spawnPointManager.getSpawnPoint((PlayerEntity) (Object) this);
        SpreadSpawnPoints.LOGGER.info("Player will spawn at: {}, {}", spawnPoint.x, spawnPoint.y);
        return new BlockPos(spawnPoint.x, 0, spawnPoint.y);
    }
}
