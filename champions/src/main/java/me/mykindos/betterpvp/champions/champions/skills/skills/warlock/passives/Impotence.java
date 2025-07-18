package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.damage.ModifierOperation;
import me.mykindos.betterpvp.core.combat.damage.ModifierType;
import me.mykindos.betterpvp.core.combat.damage.ModifierValue;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class Impotence extends Skill implements PassiveSkill, DefensiveSkill {

    private double baseRadius;
    private double radiusIncreasePerLevel;
    private double baseDecrease;
    private double baseDecreasePerPlayer;
    private double decreaseIncreasePerLevel;
    private int maxEnemies;

    @Inject
    public Impotence(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Impotence";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "For each enemy within " + getValueString(this::getRadius, level) + " blocks you take",
                "reduced damage from all sources, at a",
                "maximum of " + getValueString(this::getMaxEnemies, level) + " players",
                "",
                "Damage Reduction:",
                "1 nearby enemy = <stat>" + String.format("%.1f",(calculateReduction(level, 1)))  + "%</stat>",
                "2 nearby enemies = <stat>" + String.format("%.1f",(calculateReduction(level, 2))) + "%</stat>",
                "3 nearby enemies = <stat>" + String.format("%.1f",(calculateReduction(level, 3))) + "%</stat>"
        };
    }

    private double getRadius(int level) {
        return baseRadius + (level - 1) * radiusIncreasePerLevel;
    }

    private double getMaxEnemies(int level) {
        return maxEnemies;
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;

        int level = getLevel(player);
        if (level <= 0) return;

        int nearbyEnemies = UtilEntity.getNearbyEnemies(player, player.getLocation(), getRadius(level)).size();

        event.getDamageModifiers().addModifier(ModifierType.DAMAGE, calculateReduction(level, nearbyEnemies), getName(), ModifierValue.PERCENTAGE, ModifierOperation.DECREASE);

        Location locationToPlayEffect = player.getLocation().add(0, 1, 0);
        player.getWorld().playEffect(locationToPlayEffect, Effect.OXIDISED_COPPER_SCRAPE, 0);
    }

    private double calculateReduction(int level, int nearby) {
        return (baseDecrease + (level - 1) * decreaseIncreasePerLevel) + (Math.min(nearby, maxEnemies) * baseDecreasePerPlayer);
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("baseRadius", 4.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 1.0, Double.class);

        baseDecrease = getConfig("baseDecrease", 15.0, Double.class);
        baseDecreasePerPlayer = getConfig("baseDecreasePerPlayer", 5.0, Double.class);
        decreaseIncreasePerLevel = getConfig("decreaseIncreasePerLevel", 0.0, Double.class);

        maxEnemies = getConfig("maxEnemies", 3, Integer.class);
    }
}
