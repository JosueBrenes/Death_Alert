package com.josuebrenes.toquedeathalert.command;

import com.josuebrenes.toquedeathalert.core.ToqueLog;
import com.josuebrenes.toquedeathalert.core.ToqueRuntime;
import com.josuebrenes.toquedeathalert.migration.VanillaDeathsLookup;
import com.josuebrenes.toquedeathalert.role.PlayerRole;
import com.josuebrenes.toquedeathalert.series.PlayerDeathRecord;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.List;

/**
 * The {@code /toque} command tree.
 *
 * <p>Registered while the data packs load, which happens before the server object
 * exists, so every action resolves its services from {@link ToqueRuntime} at
 * execution time rather than capturing them here.
 */
public final class ToqueCommands {
    private static final int ADMIN_LEVEL = 2;
    private static final String SKULL = "☠";

    private final ToqueRuntime runtime;

    public ToqueCommands(ToqueRuntime runtime) {
        this.runtime = runtime;
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("toque")
                .then(CommandManager.literal("deaths")
                        .executes(this::listDeaths))
                .then(CommandManager.literal("status")
                        .requires(source -> source.hasPermissionLevel(ADMIN_LEVEL))
                        .executes(context -> status(context, context.getSource().getPlayer()))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> status(context,
                                        EntityArgumentType.getPlayer(context, "player")))))
                .then(CommandManager.literal("set")
                        .requires(source -> source.hasPermissionLevel(ADMIN_LEVEL))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("deaths", IntegerArgumentType.integer(0))
                                        .executes(this::setDeaths))))
                .then(CommandManager.literal("import")
                        .requires(source -> source.hasPermissionLevel(ADMIN_LEVEL))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(this::importDeaths)))
                .then(CommandManager.literal("objective")
                        .requires(source -> source.hasPermissionLevel(ADMIN_LEVEL))
                        .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(this::setObjective)))
                .then(CommandManager.literal("try")
                        .requires(source -> source.hasPermissionLevel(ADMIN_LEVEL))
                        .then(CommandManager.argument("number", IntegerArgumentType.integer(1))
                                .executes(this::setTry)))
                .then(CommandManager.literal("resetDeaths")
                        .requires(source -> source.hasPermissionLevel(ADMIN_LEVEL))
                        .executes(this::resetSeries)));
    }

    private int listDeaths(CommandContext<ServerCommandSource> context) {
        ToqueRuntime.Services services = require(context);
        if (services == null) {
            return 0;
        }
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal(SKULL + " TOQUE — Serie #"
                + services.stats().seriesNumber()).formatted(Formatting.RED, Formatting.BOLD), false);

        List<PlayerDeathRecord> records = services.stats().all().stream()
                .sorted(Comparator.comparingInt(PlayerDeathRecord::deaths).reversed())
                .toList();

        if (records.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Sin muertes registradas.")
                    .formatted(Formatting.GRAY), false);
            return 0;
        }
        for (PlayerDeathRecord record : records) {
            PlayerRole role = PlayerRole.fromDeaths(record.deaths());
            source.sendFeedback(() -> Text.literal(record.displayName() + " ").formatted(Formatting.WHITE)
                    .append(role.badge())
                    .append(Text.literal("  " + SKULL + " " + record.deaths()).formatted(Formatting.RED)), false);
        }
        return records.size();
    }

    /** Diagnostics: what TOQUE stored, what vanilla says, and where the file lives. */
    private int status(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        ToqueRuntime.Services services = require(context);
        if (services == null || player == null) {
            return 0;
        }
        ServerCommandSource source = context.getSource();
        PlayerDeathRecord record = services.stats().find(player.getUuid())
                .orElseGet(() -> PlayerDeathRecord.fresh(player.getUuid(), player.getGameProfile().getName()));
        VanillaDeathsLookup.Result vanilla = services.importer().lookup()
                .lookup(source.getServer(), player.getUuid(), player);

        line(source, "Archivo", services.stats().file().toString());
        line(source, "Serie", "#" + services.stats().seriesNumber());
        line(source, "Try", "#" + services.stats().tryNumber());
        line(source, "Objetivo", services.stats().objective());
        line(source, "Import vanilla abierto", Boolean.toString(services.stats().isVanillaImportOpen()));
        line(source, "Jugador", record.displayName() + " (" + player.getUuid() + ")");
        line(source, "Muertes TOQUE", Integer.toString(record.deaths()));
        line(source, "Rol", PlayerRole.fromDeaths(record.deaths()).plain());
        line(source, "Ya migrado", Boolean.toString(record.migrated()));
        line(source, "Muertes vanilla", vanilla.deaths() + " [" + vanilla.origin() + "]");
        return 1;
    }

    private int setDeaths(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ToqueRuntime.Services services = require(context);
        if (services == null) {
            return 0;
        }
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int deaths = IntegerArgumentType.getInteger(context, "deaths");

        services.stats().setDeaths(player.getUuid(), player.getGameProfile().getName(), deaths);
        services.tabList().invalidate(player.getUuid());

        context.getSource().sendFeedback(() -> Text.literal(SKULL + " "
                + player.getGameProfile().getName() + " queda en " + deaths + " muertes.")
                .formatted(Formatting.RED), true);
        ToqueLog.info("Admin set deaths for {} to {}.", player.getGameProfile().getName(), deaths);
        return deaths;
    }

    private int importDeaths(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ToqueRuntime.Services services = require(context);
        if (services == null) {
            return 0;
        }
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int starting = services.importer().forceImport(context.getSource().getServer(), player);
        services.tabList().invalidate(player.getUuid());

        context.getSource().sendFeedback(() -> Text.literal(SKULL + " "
                + player.getGameProfile().getName() + " importado desde vanilla: "
                + starting + " muertes.").formatted(Formatting.RED), true);
        return starting;
    }

    /** The objective line shown at the bottom of the player list. */
    private int setObjective(CommandContext<ServerCommandSource> context) {
        ToqueRuntime.Services services = require(context);
        if (services == null) {
            return 0;
        }
        String objective = StringArgumentType.getString(context, "text");
        services.stats().setObjective(objective);
        services.tabList().invalidateAll();
        services.tabList().sendHeaderAndFooter(context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.literal(SKULL + " Objetivo: " + objective)
                .formatted(Formatting.GOLD), true);
        ToqueLog.info("Objective set to: {}", objective);
        return 1;
    }

    /** Manual correction, for when the Try counter and the real series disagree. */
    private int setTry(CommandContext<ServerCommandSource> context) {
        ToqueRuntime.Services services = require(context);
        if (services == null) {
            return 0;
        }
        int number = IntegerArgumentType.getInteger(context, "number");
        services.stats().setTryNumber(number);
        services.tabList().invalidateAll();
        services.tabList().sendHeaderAndFooter(context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.literal(SKULL + " Try #" + number + ".")
                .formatted(Formatting.GOLD), true);
        ToqueLog.info("Try counter set to #{}", number);
        return number;
    }

    private int resetSeries(CommandContext<ServerCommandSource> context) {
        ToqueRuntime.Services services = require(context);
        if (services == null) {
            return 0;
        }
        // Worlds, seeds, Hardcore World Reset and vanilla statistics stay untouched:
        // only the TOQUE counters go back to zero, and the vanilla import closes so a
        // restart can never turn a fresh 0 back into an old number.
        services.stats().startNewSeries();
        services.tabList().invalidateAll();
        services.tabList().sendHeaderAndFooter(context.getSource().getServer());

        int series = services.stats().seriesNumber();
        context.getSource().sendFeedback(() -> Text.literal(SKULL + " Nueva serie iniciada (#"
                + series + "). Muertes reiniciadas a 0.").formatted(Formatting.RED, Formatting.BOLD), true);
        ToqueLog.info("Death counters reset. New series: #{}", series);
        return 1;
    }

    private ToqueRuntime.Services require(CommandContext<ServerCommandSource> context) {
        ToqueRuntime.Services services = runtime.services();
        if (services == null) {
            context.getSource().sendError(Text.literal("TOQUE aun no esta listo."));
        }
        return services;
    }

    private static void line(ServerCommandSource source, String label, String value) {
        source.sendFeedback(() -> Text.literal(label + ": ").formatted(Formatting.GRAY)
                .append(Text.literal(value).formatted(Formatting.WHITE)), false);
    }
}
