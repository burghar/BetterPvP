package me.mykindos.betterpvp.clans.champions;

import lombok.Getter;
import me.mykindos.betterpvp.clans.champions.roles.RoleManager;
import me.mykindos.betterpvp.clans.champions.skills.SkillManager;
import me.mykindos.betterpvp.clans.combat.throwables.ThrowableHandler;
import me.mykindos.betterpvp.clans.energy.EnergyHandler;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A wrapper containing frequently used dependencies throughout the champions module
 */
@Singleton
@Getter
public class ChampionsManager {

    private final GamerManager gamers;
    private final SkillManager skills;
    private final RoleManager roles;
    private final CooldownManager cooldowns;
    private final EffectManager effects;
    private final EnergyHandler energy;
    private final ThrowableHandler throwables;

    @Inject
    public ChampionsManager(GamerManager gamers, SkillManager skills, RoleManager roles, CooldownManager cooldowns, EffectManager effects, EnergyHandler energy, ThrowableHandler throwables) {
        this.gamers = gamers;
        this.skills = skills;
        this.roles = roles;
        this.cooldowns = cooldowns;
        this.effects = effects;
        this.energy = energy;
        this.throwables = throwables;
    }
}