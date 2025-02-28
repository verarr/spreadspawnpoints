package xyz.verarr.spreadspawnpoints.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.verarr.spreadspawnpoints.PermissionsService;
import xyz.verarr.spreadspawnpoints.mixin.ServerPlayerEntityInvoker;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Command for moving players to their spawns
 */
public class RespawnCommand {
    /**
     * Moves specified players to their spawnpoint and teleports them in
     * place (so the players' position is actually sent to the clients).
     */
    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        players.forEach(player -> {
            ((ServerPlayerEntityInvoker) player).invokeMoveToSpawn(player.getServerWorld());
            player.teleport(
                    player.getServerWorld(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYaw(),
                    player.getPitch()
            );
        });
        context.getSource().sendFeedback(
                () -> Text.literal(String.format("Respawned %d players", players.size())),
                true
        );
        return 1;
    }

    /**
     * Target selector argument.
     */
    private static final RequiredArgumentBuilder<ServerCommandSource, EntitySelector> argumentBuilder = argument(
            "target",
            EntityArgumentType.players()
    ).executes(RespawnCommand::execute);

    /**
     * Full command tree for <code>respawn</code> command.
     * Executes {@link RespawnCommand#execute(CommandContext)}.
     */
    public static final LiteralArgumentBuilder<ServerCommandSource> command =
            literal("respawn").then(argumentBuilder)
                    .requires(source -> PermissionsService.hasPermission(source.getPlayer(), "command.respawn", 2));
}
