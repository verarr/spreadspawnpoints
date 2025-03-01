package xyz.verarr.spreadspawnpoints.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import xyz.verarr.spreadspawnpoints.spawnpoints.SpawnPointGeneratorManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class GeneratorSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
        for (Identifier generator : SpawnPointGeneratorManager.getRegisteredSpawnPointGenerators()) {
            suggestionsBuilder.suggest(generator.toString());
        }

        return suggestionsBuilder.buildFuture();
    }
}
