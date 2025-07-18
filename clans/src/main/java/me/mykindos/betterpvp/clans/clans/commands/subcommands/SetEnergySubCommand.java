package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ClanCommand.class)
public class SetEnergySubCommand extends ClanSubCommand {

    @Inject
    public SetEnergySubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "setenergy";
    }

    @Override
    public String getDescription() {
        return "Force set the energy for your current clan";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <energy>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 1) {
            UtilMessage.message(player, "Clans", Component.text("This command requires 1 argument as input."));
            return;
        }
        int energy;
        try {
            energy = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "Clans", Component.text(args[0] + " is not a valid input. Input must be an integer."));
            return;
        }

        if (energy < 0) energy = 0;

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();

        int oldEnergy = playerClan.getEnergy();
        playerClan.setEnergy(energy);

        Component component = Component.text("Energy set to: ", NamedTextColor.GRAY)
                .append(Component.text(playerClan.getEnergy() + " - (", NamedTextColor.YELLOW)
                        .append(Component.text(playerClan.getEnergyTimeRemaining(), NamedTextColor.GOLD)
                                .append(Component.text(")", NamedTextColor.YELLOW))))
                .append(Component.text(" Previous: ", NamedTextColor.GRAY)).append(Component.text(oldEnergy, NamedTextColor.YELLOW));

        UtilMessage.message(player, "Clans", component);
        clientManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> set the energy of <yellow>%s<gray> to <green>%s <white>(<yellow>%s<white>)",
                player.getName(), playerClan.getName(), playerClan.getEnergy(), playerClan.getEnergyTimeRemaining()), Rank.TRIAL_MOD);
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }
}
