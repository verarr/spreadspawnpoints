package xyz.verarr.spreadspawnpoints;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

public class PermissionsService {
    public static final String PERMISSION_ROOT = SpreadSpawnPoints.MOD_ID;

    @ExpectPlatform
    public static boolean hasPermission(@NotNull ServerPlayerEntity player,
                                        @NotNull String permission,
                                        int defaultPermissionLevel) {
        throw new AssertionError();
    }
}
