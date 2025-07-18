package me.mykindos.betterpvp.core;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.punishments.rules.RuleManager;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.stats.impl.GlobalCombatStatsRepository;
import me.mykindos.betterpvp.core.combat.weapon.WeaponManager;
import me.mykindos.betterpvp.core.command.loader.CoreCommandLoader;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ConfigInjectorModule;
import me.mykindos.betterpvp.core.coretips.CoreTipLoader;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.CurrentMode;
import me.mykindos.betterpvp.core.framework.adapter.Adapters;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapters;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEventExecutor;
import me.mykindos.betterpvp.core.injector.CoreInjectorModule;
import me.mykindos.betterpvp.core.inventory.InvUI;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDManager;
import me.mykindos.betterpvp.core.leaderboards.CoreLeaderboardLoader;
import me.mykindos.betterpvp.core.listener.loader.CoreListenerLoader;
import me.mykindos.betterpvp.core.logging.LoggerFactory;
import me.mykindos.betterpvp.core.logging.appenders.DatabaseAppender;
import me.mykindos.betterpvp.core.logging.appenders.LegacyAppender;
import me.mykindos.betterpvp.core.recipes.RecipeHandler;
import me.mykindos.betterpvp.core.redis.Redis;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.Bukkit;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Field;
import java.util.Set;

@CustomLog
public class Core extends BPvPPlugin {

    private final String PACKAGE = getClass().getPackageName();

    @Getter
    @Setter
    private Injector injector;

    @Inject
    private Database database;


    @Inject
    private Redis redis;

    private ClientManager clientManager;

    @Inject
    private UpdateEventExecutor updateEventExecutor;

    @Getter
    private ScoreboardLibrary scoreboardLibrary;

    @Getter
    @Setter
    private CurrentMode currentMode;

    @Getter
    @Setter
    private static String currentServer;

    @Getter
    @Setter
    private static String currentSeason;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("StudioEngine") != null) {
            String serverName = getCommonNameViaReflection();
            if (serverName == null) return;
            if (serverName.toLowerCase().startsWith("champions")) {
                serverName = "Champions";
            }

            Core.setCurrentServer(serverName);
        } else {
            setCurrentServer(getConfig().getOrSaveString("core.info.server", "unknown"));
        }

        setCurrentSeason(getConfig().getOrSaveString("core.info.season", "unknown"));

        // Add this appender first to ensure we still capture all logs before database is initialized
        LoggerFactory.getInstance().addAppender(new LegacyAppender());

        Reflections reflections = new Reflections(PACKAGE, Scanners.FieldsAnnotated);
        Set<Field> fields = reflections.getFieldsAnnotatedWith(Config.class);

        injector = Guice.createInjector(new CoreInjectorModule(this), new ConfigInjectorModule(this, fields));
        injector.injectMembers(this);

        LoggerFactory.getInstance().addAppender(new DatabaseAppender(database, this));

        database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:core-migrations/local", "local", TargetDatabase.LOCAL);
        database.getConnection().runDatabaseMigrations(getClass().getClassLoader(), "classpath:core-migrations/global", "global", TargetDatabase.GLOBAL);
        redis.credentials(this.getConfig());

        var coreListenerLoader = injector.getInstance(CoreListenerLoader.class);
        coreListenerLoader.registerListeners(PACKAGE);

        var coreCommandLoader = injector.getInstance(CoreCommandLoader.class);
        coreCommandLoader.loadCommands(PACKAGE);

        clientManager = injector.getInstance(ClientManager.class);

        var itemHandler = injector.getInstance(ItemHandler.class);
        itemHandler.loadItemData("core");

        var recipeHandler = injector.getInstance(RecipeHandler.class);
        recipeHandler.loadConfig(this.getConfig(), "minecraft");
        recipeHandler.loadConfig(this.getConfig(), "core");
        this.saveConfig();

        var weaponManager = injector.getInstance(WeaponManager.class);
        weaponManager.load();

        var uuidManager = injector.getInstance(UUIDManager.class);
        uuidManager.loadObjectsFromNamespace("core");

        var leaderboardLoader = injector.getInstance(CoreLeaderboardLoader.class);
        leaderboardLoader.registerLeaderboards(PACKAGE);

        var coreTipLoader = injector.getInstance(CoreTipLoader.class);
        coreTipLoader.loadTips(PACKAGE);

        var ruleManager = injector.getInstance(RuleManager.class);
        ruleManager.load(this);

        updateEventExecutor.loadPlugin(this);
        updateEventExecutor.initialize();

        InvUI.getInstance().setPlugin(this);

        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
        } catch (NoPacketAdapterAvailableException e) {
            // If no packet adapter was found, you can fallback to the no-op implementation:
            scoreboardLibrary = new NoopScoreboardLibrary();
            log.warn("No packet adapter found, falling back to no-op implementation");
        }

        final Adapters adapters = new Adapters(this);
        final Reflections reflectionAdapters = new Reflections(PACKAGE);
        adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapter.class));
        adapters.loadAdapters(reflectionAdapters.getTypesAnnotatedWith(PluginAdapters.class));

        UtilServer.runTaskLater(this, () -> UtilServer.callEvent(new ServerStartEvent()), 1L);
    }

    @Override
    public void onDisable() {
        clientManager.processStatUpdates(false);
        clientManager.shutdown();
        redis.shutdown();
        injector.getInstance(GlobalCombatStatsRepository.class).shutdown();
        scoreboardLibrary.close();
        LoggerFactory.getInstance().close();

    }

    private String getCommonNameViaReflection() {
        try {
            Class<?> namespaceUtilClass = Class.forName("com.mineplex.studio.sdk.util.NamespaceUtil");
            java.lang.reflect.Method getCommonNameMethod = namespaceUtilClass.getMethod("getCommonName");
            return (String) getCommonNameMethod.invoke(null);
        } catch (Exception e) {
            log.error("Failed to get server common name", e).submit();
        }

        return "unknown";
    }
}