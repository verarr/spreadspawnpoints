package xyz.verarr.spreadspawnpoints.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.verarr.spreadspawnpoints.mixin.ServerPlayerEntityInvoker;
import xyz.verarr.spreadspawnpoints.spawnpoints.SpawnPointManager;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SpawnpointsCommand {
    private static class RespawnCommand {
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

        private static final RequiredArgumentBuilder<ServerCommandSource, EntitySelector> argumentBuilder = argument(
                "target",
                EntityArgumentType.players()
        ).executes(RespawnCommand::execute);

        public static final LiteralArgumentBuilder<ServerCommandSource> command =
                literal("respawn").then(argumentBuilder);
    }

    private static class GeneratorCommand {
        private static class QueryCommand {
            private static int execute(CommandContext<ServerCommandSource> context) {
                final ServerWorld world = context.getSource().getWorld();
                final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(world);
                context.getSource().sendFeedback(
                        () -> Text.literal(String.format(
                                "The spawn point generator is %s",
                                spawnPointManager.getSpawnPointGenerator().toString()
                        )),
                        false
                );
                return Command.SINGLE_SUCCESS;
            }

            public static final LiteralArgumentBuilder<ServerCommandSource> command =
                    literal("query").executes(QueryCommand::execute);
        }

        private static class SetCommand {
            private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                try {
                    final Identifier identifier = IdentifierArgumentType.getIdentifier(context, "generator");
                    if (!SpawnPointManager.spawnPointGeneratorExists(identifier)) {
                        throw new SimpleCommandExceptionType(Text.literal("Specified generator does not exist or has not been registered")).create();
                    }
                    final ServerWorld serverWorld = context.getSource().getWorld();
                    final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(serverWorld);
                    spawnPointManager.setSpawnPointGenerator(identifier);
                    context.getSource().sendFeedback(() -> Text.literal(String.format(
                            "Spawn point generator set to %s",
                            identifier.toString()
                    )), true);
                    spawnPointManager.resetSpawnPoints();
                    return Command.SINGLE_SUCCESS;
                } catch (Exception e) {
                    throw new SimpleCommandExceptionType(Text.literal(e.toString())).create();
                }
            }

            private static int executeWithResetFlag(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                try {
                    final Identifier identifier = IdentifierArgumentType.getIdentifier(context, "generator");
                    if (!SpawnPointManager.spawnPointGeneratorExists(identifier)) {
                        throw new SimpleCommandExceptionType(Text.literal("Specified generator does not exist or has not been registered")).create();
                    }
                    final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(context.getSource().getWorld());
                    spawnPointManager.setSpawnPointGenerator(identifier);
                    context.getSource().sendFeedback(() -> Text.literal(String.format(
                            "Spawn point generator set to %s",
                            identifier.toString()
                    )), true);
                    if (BoolArgumentType.getBool(context, "resetSpawnPoints"))
                        spawnPointManager.resetSpawnPoints();
                    return Command.SINGLE_SUCCESS;
                } catch (Exception e) {
                    throw new SimpleCommandExceptionType(Text.literal(e.toString())).create();
                }
            }

            private static final RequiredArgumentBuilder<ServerCommandSource, Boolean>
                    resetArgument = argument(
                    "resetSpawnPoints",
                    BoolArgumentType.bool()
            ).executes(SetCommand::executeWithResetFlag);

            private static final RequiredArgumentBuilder<ServerCommandSource, Identifier>
                    identifierArgument = argument(
                    "generator",
                    IdentifierArgumentType.identifier()
            )
                    .executes(SetCommand::execute)
                    .then(resetArgument);

            public static final LiteralArgumentBuilder<ServerCommandSource> command =
                    literal("set").then(identifierArgument);
        }

        private static class DataCommand {
            private static int execute(CommandContext<ServerCommandSource> context) {
                final NbtCompound nbt = NbtCompoundArgumentType.getNbtCompound(context, "nbt");
                final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(context.getSource().getWorld());
                spawnPointManager.updateGeneratorData(nbt);
                return Command.SINGLE_SUCCESS;
            }

            private static final RequiredArgumentBuilder<ServerCommandSource, NbtCompound>
                    argumentBuilder = argument(
                    "nbt",
                    NbtCompoundArgumentType.nbtCompound()
            ).executes(DataCommand::execute);

            public static final LiteralArgumentBuilder<ServerCommandSource> command =
                    literal("data").then(argumentBuilder);
        }

        public static LiteralArgumentBuilder<ServerCommandSource> command =
                literal("generator")
                        .then(QueryCommand.command)
                        .then(SetCommand.command)
                        .then(DataCommand.command);
    }

    private static class ResetCommand {
        private static int execute(CommandContext<ServerCommandSource> context) {
            final ServerWorld world = context.getSource().getWorld();
            final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(world);
            spawnPointManager.resetSpawnPoints();
            context.getSource().sendFeedback(() -> Text.literal("Reset all spawn points."), true);
            return Command.SINGLE_SUCCESS;
        }

        public static LiteralArgumentBuilder<ServerCommandSource> command =
                literal("reset").executes(ResetCommand::execute);
    }

    public static final LiteralArgumentBuilder<ServerCommandSource> command =
            literal("spawnpoints")
                    .then(RespawnCommand.command)
                    .then(GeneratorCommand.command)
                    .then(ResetCommand.command);
}
