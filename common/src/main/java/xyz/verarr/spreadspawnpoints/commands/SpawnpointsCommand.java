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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.verarr.spreadspawnpoints.PermissionsService;
import xyz.verarr.spreadspawnpoints.spawnpoints.SpawnPointManager;
import xyz.verarr.spreadspawnpoints.spawnpoints.SpawnPointGeneratorManager;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Main collection of commands for Spread Spawnpoints
 */
public class SpawnpointsCommand {
    /**
     * Commands related to spawnpoint generators
     */
    private static class GeneratorCommand {
        /**
         * Command for querying the currently active spawnpoint generator
         *
         * @see SpawnPointGeneratorManager#getSpawnPointGenerator()
         */
        private static class QueryCommand {
            /**
             * Prints current generator's identifier to command feedback.
             */
            private static int execute(CommandContext<ServerCommandSource> context) {
                final ServerWorld world = context.getSource().getWorld();
                final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(world);
                context.getSource().sendFeedback(
                        () -> Text.literal(String.format(
                                "The spawn point generator is %s",
                                spawnPointManager.generatorManager.getSpawnPointGenerator().toString()
                        )),
                        false
                );
                return Command.SINGLE_SUCCESS;
            }

            /**
             * Command tree for <code>spawnpoint generator query</code> command
             */
            public static final LiteralArgumentBuilder<ServerCommandSource> command =
                    literal("query")
                            .requires(source -> PermissionsService.sourceHasPermission(source, "command.spawnpoints.generator.query", 2))
                            .executes(QueryCommand::execute);
        }

        /**
         * Command for setting (replacing) the spawnpoint generator
         *
         * @see SpawnPointGeneratorManager#setSpawnPointGenerator(Identifier)
         * @see SpawnPointGeneratorManager#registerSpawnPointGenerator(Identifier, Class)
         */
        private static class SetCommand {
            /**
             * Sets the generator to the specified one and resets all
             * spawnpoints.
             * <p>
             * Executed when only identifier argument
             * ({@link SetCommand#identifierArgument}) is specified.
             */
            private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                try {
                    final Identifier identifier = IdentifierArgumentType.getIdentifier(context, "generator");
                    if (!SpawnPointGeneratorManager.spawnPointGeneratorExists(identifier)) {
                        throw new SimpleCommandExceptionType(Text.literal("Specified generator does not exist or has not been registered")).create();
                    }

                    if (!PermissionsService.sourceHasPermission(context.getSource(), "command.spawnpoints.reset.all", 2)) {
                        throw new SimpleCommandExceptionType(Text.literal("You do not have permission to reset all spawnpoints.")).create();
                    }

                    final ServerWorld serverWorld = context.getSource().getWorld();
                    final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(serverWorld);
                    spawnPointManager.generatorManager.setSpawnPointGenerator(identifier);
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

            /**
             * Sets the generator to the specified one and resets all
             * spawnpoints according to reset argument
             * ({@link SetCommand#resetArgument}).
             * <p>
             * Executed when reset argument ({@link SetCommand#resetArgument})
             * is present.
             */
            private static int executeWithResetFlag(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                try {
                    final Identifier identifier = IdentifierArgumentType.getIdentifier(context, "generator");
                    if (!SpawnPointGeneratorManager.spawnPointGeneratorExists(identifier)) {
                        throw new SimpleCommandExceptionType(Text.literal("Specified generator does not exist or has not been registered")).create();
                    }

                    if (BoolArgumentType.getBool(context, "resetSpawnPoints") && !PermissionsService.sourceHasPermission(context.getSource(), "command.spawnpoints.reset.all", 2)) {
                        throw new SimpleCommandExceptionType(Text.literal("You do not have permission to reset all spawnpoints.")).create();
                    }

                    final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(context.getSource().getWorld());
                    spawnPointManager.generatorManager.setSpawnPointGenerator(identifier);
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

            /**
             * Reset boolean argument. Optional, default true. Executes
             * {@link SetCommand#executeWithResetFlag(CommandContext)} when
             * specified.
             *
             * @see SpawnPointManager#resetSpawnPoints()
             */
            private static final RequiredArgumentBuilder<ServerCommandSource, Boolean>
                    resetArgument = argument(
                    "resetSpawnPoints",
                    BoolArgumentType.bool()
            ).executes(SetCommand::executeWithResetFlag);

            /**
             * Identifier argument. Required. Executes
             * {@link SetCommand#execute(CommandContext)} when no further
             * arguments specified.
             */
            private static final RequiredArgumentBuilder<ServerCommandSource, Identifier>
                    identifierArgument = argument(
                    "generator",
                    IdentifierArgumentType.identifier()
            )
                    .suggests(new GeneratorSuggestionProvider())
                    .executes(SetCommand::execute)
                    .then(resetArgument);

            /**
             * Command tree for <code>spawnpoints generator set</code> command
             */
            public static final LiteralArgumentBuilder<ServerCommandSource> command =
                    literal("set")
                            .requires(source -> PermissionsService.sourceHasPermission(source, "command.spawnpoints.generator.set", 2))
                            .then(identifierArgument);
        }

        /**
         * Command for modifying the data of a spawnpoint generator
         *
         * @see SpawnPointGeneratorManager#modifyFromNbtPartial(NbtCompound) (NbtCompound)
         * @see xyz.verarr.spreadspawnpoints.spawnpoints.NBTSerializable#modifyFromNbtPartial(NbtCompound)
         */
        private static class DataCommand {
            /**
             * @see SpawnPointGeneratorManager#modifyFromNbtPartial(NbtCompound) (NbtCompound)
             */
            private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                final NbtCompound nbt = NbtCompoundArgumentType.getNbtCompound(context, "nbt");
                final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(context.getSource().getWorld());
                try {
                    spawnPointManager.generatorManager.modifyFromNbtPartial(nbt);
                } catch (IllegalArgumentException e) {
                    throw new SimpleCommandExceptionType(Text.literal("Illegal data passed to generator: " + e.getMessage())).create();
                }
                context.getSource().sendFeedback(() -> Text.literal("Data modified. (" + nbt.getKeys().size() + " keys updated)"), true);
                return Command.SINGLE_SUCCESS;
            }

            /**
             * Passed data argument
             */
            private static final RequiredArgumentBuilder<ServerCommandSource, NbtCompound>
                    argumentBuilder = argument(
                    "nbt",
                    NbtCompoundArgumentType.nbtCompound()
            ).executes(DataCommand::execute);

            /**
             * Command tree for <code>spawnpoints generator data</code> command
             */
            public static final LiteralArgumentBuilder<ServerCommandSource> command =
                    literal("data")
                            .requires(source -> PermissionsService.sourceHasPermission(source, "command.spawnpoints.generator.modify", 2))
                            .then(argumentBuilder);
        }

        /**
         * Command tree for <code>spawnpoints generator</code> command
         */
        public static LiteralArgumentBuilder<ServerCommandSource> command =
                literal("generator")
                        .requires(source -> PermissionsService.sourceHasPermission(source, "command.spawnpoints.generator", 2))
                        .then(QueryCommand.command)
                        .then(SetCommand.command)
                        .then(DataCommand.command);
    }

    /**
     * Command for resetting all spawnpoints
     * ({@link ResetCommand#execute(CommandContext)}) or spawnpoints of
     * a specified player
     * ({@link ResetCommand#executeWithArgument(CommandContext)}).
     */
    private static class ResetCommand {
        /**
         * Executed when no argument is specified.
         */
        private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
            if (!PermissionsService.sourceHasPermission(context.getSource(), "command.spawnpoints.reset.all", 2)) {
                throw new SimpleCommandExceptionType(Text.literal("You do not have permission to reset all spawnpoints.")).create();
            }

            final ServerWorld world = context.getSource().getWorld();
            final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(world);
            spawnPointManager.resetSpawnPoints();
            context.getSource().sendFeedback(() -> Text.literal("Reset all spawn points."), true);
            return Command.SINGLE_SUCCESS;
        }

        /**
         * Executed when {@link #argumentBuilder} is specified.
         */
        private static int executeWithArgument(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
            final ServerWorld world = context.getSource().getWorld();
            final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(world);
            final Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
            int affected = (int) players.stream().filter(spawnPointManager::resetSpawnPoint).count();
            if (affected > 0) {
                context.getSource().sendFeedback(() ->
                        Text.literal("Reset %d spawn points.".formatted(affected)),
                        true);
                return affected;
            } else {
                throw new SimpleCommandExceptionType(
                        Text.literal("No specified player already has a " +
                                "spawnpoint, nothing was affected.")).create();
            }
        }

        /**
         * Reset target argument. When specified, executes
         * {@link ResetCommand#executeWithArgument(CommandContext)}
         *
         * @see SpawnPointManager#resetSpawnPoint(PlayerEntity)
         */
        private static final RequiredArgumentBuilder<ServerCommandSource, EntitySelector> argumentBuilder = argument(
                "target",
                EntityArgumentType.players()
        ).executes(ResetCommand::executeWithArgument);

        /**
         * Command tree for <code>spawnpoints reset</code> command
         * @see SpawnPointManager#resetSpawnPoints()
         */
        public static LiteralArgumentBuilder<ServerCommandSource> command =
                literal("reset")
                        .requires(source -> PermissionsService.sourceHasPermission(source, "command.spawnpoints.reset", 2))
                        .executes(ResetCommand::execute)
                        .then(argumentBuilder);
    }

    /**
     * Full command tree for <code>spawnpoints</code> command
     */
    public static final LiteralArgumentBuilder<ServerCommandSource> command =
            literal("spawnpoints")
                    .requires(source -> PermissionsService.sourceHasPermission(source, "command.spawnpoints", 2))
                    .then(GeneratorCommand.command)
                    .then(ResetCommand.command);
}
