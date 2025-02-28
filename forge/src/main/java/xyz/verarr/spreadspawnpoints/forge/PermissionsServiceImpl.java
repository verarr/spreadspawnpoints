package xyz.verarr.spreadspawnpoints.forge;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.jetbrains.annotations.NotNull;
import xyz.verarr.spreadspawnpoints.PermissionsService;

public class PermissionsServiceImpl extends PermissionsService {
    public static boolean hasPermission(@NotNull ServerPlayerEntity player,
                                        @NotNull String permission,
                                        int defaultPermissionLevel) {
        if (player.hasPermissionLevel(defaultPermissionLevel)) {
            return true;
        }

        String permissionNode = String.format("%s.%s", PERMISSION_ROOT, permission);

        return PermissionAPI.getRegisteredNodes().stream()
                .filter(node -> node.getType() == PermissionTypes.BOOLEAN)
                .filter(node -> node.getNodeName().equals(permissionNode))
                .anyMatch(node -> (boolean)node.getDefaultResolver().resolve(player,player.getUuid()));
    }
}
