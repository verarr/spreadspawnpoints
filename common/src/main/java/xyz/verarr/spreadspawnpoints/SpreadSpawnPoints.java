package xyz.verarr.spreadspawnpoints;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.verarr.spreadspawnpoints.mixin.ServerPlayerEntityInvoker;
import xyz.verarr.spreadspawnpoints.spawnpoints.SpawnPointManager;
import xyz.verarr.spreadspawnpoints.spawnpoints.generators.RandomSpawnPointGenerator;
import xyz.verarr.spreadspawnpoints.spawnpoints.generators.VanillaSpawnPointGenerator;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class SpreadSpawnPoints {
    public static final String MOD_ID = "spreadspawnpoints";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Hello Minecraft modding world!");

        registerSpawnPointGenerators();
        LOGGER.info("Registered Spawn Point Generators!");

        initCommands();
    }

    static void registerSpawnPointGenerators() {
        SpawnPointManager.registerSpawnPointGenerator(
                Identifier.of(MOD_ID, "vanilla"),
                VanillaSpawnPointGenerator.class
        );
        SpawnPointManager.registerSpawnPointGenerator(
                Identifier.of(MOD_ID, "random"),
                RandomSpawnPointGenerator.class
        );
    }

    static void initCommands() {
        CommandRegistrationEvent.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("spawnpoints")
                                .then(literal("respawn")
                                        .then(argument("target", EntityArgumentType.players())
                                                .executes(context -> {
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
                                                }))
                                )
                                .then(literal("generator")
                                        .then(literal("query")
                                                .executes(context -> {
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
                                                })
                                        )
                                        .then(literal("set")
                                                .then(argument("generator", IdentifierArgumentType.identifier())
                                                        .executes(context -> {
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
                                                                return Command.SINGLE_SUCCESS;
                                                            } catch (Exception e) {
                                                                throw new SimpleCommandExceptionType(Text.literal(e.toString())).create();
                                                            }
                                                        })
                                                )
                                        )
                                        .then(literal("data")
                                                .then(argument("nbt",
                                                        NbtCompoundArgumentType.nbtCompound())
                                                        .executes(context -> {
                                                            final NbtCompound nbt = NbtCompoundArgumentType.getNbtCompound(context, "nbt");
                                                            final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(context.getSource().getWorld());
                                                            spawnPointManager.updateGeneratorData(nbt);
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                )
                                .then(literal("reset")
                                        .executes(context -> {
                                            final ServerWorld world = context.getSource().getWorld();
                                            final SpawnPointManager spawnPointManager = SpawnPointManager.getInstance(world);
                                            spawnPointManager.resetSpawnPoints();
                                            context.getSource().sendFeedback(() -> Text.literal("Reset all spawn points."), true);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                )
                );
    }
}
