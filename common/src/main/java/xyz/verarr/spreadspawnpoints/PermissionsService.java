package xyz.verarr.spreadspawnpoints;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
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

    @ExpectPlatform
    public static boolean hasPermission(@NotNull CommandSource commandSource,
                                        @NotNull String permission,
                                        int defaultPermissionLevel) {
        throw new AssertionError();
    }

    public static boolean sourceHasPermission(@NotNull ServerCommandSource commandSource,
                                              @NotNull String permission,
                                              int defaultPermissionLevel) {
        if (commandSource.getPlayer() != null) {
            return hasPermission(commandSource.getPlayer(), permission, defaultPermissionLevel);
        } else {
            return hasPermission(commandSource, permission, defaultPermissionLevel);
        }
    }
}
