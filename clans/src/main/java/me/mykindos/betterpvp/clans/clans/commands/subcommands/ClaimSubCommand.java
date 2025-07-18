package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class ClaimSubCommand extends ClanSubCommand {

    @Inject
    public ClaimSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
        aliases.add("c");
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return "Claim territory for your clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (player.getWorld().getName().equalsIgnoreCase(BPvPWorld.BOSS_WORLD_NAME)) return;

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, "Clans", "You need to be a clan admin to claim land");
            return;
        }

        if (player.getWorld().getEnvironment().equals(World.Environment.NETHER) && !client.isAdministrating()) {
            UtilMessage.message(player, "Clans", "You cannot claim land in the nether.");
            return;
        }

        if (clan.getTerritory().size() >= clanManager.getMaximumClaimsForClan(clan)) {
            if(!(clan.isAdmin() || client.isAdministrating())) {
                UtilMessage.message(player, "Clans", "Your Clan cannot claim more Territory.");
                return;
            } else {
                clientManager.sendMessageToRank("Clans",
                        UtilMessage.deserialize("<yellow>%s<gray> is attempting to claim territory over the limit for <yellow>%s",
                                player.getName(), clan.getName()), Rank.TRIAL_MOD);
            }
        }

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (locationClan.equals(clan)) {
                UtilMessage.message(player, "Clans", "Your clan already owns this territory");
            } else {
                UtilMessage.message(player, "Clans", "This territory is owned by <alt2>Clan " + locationClan.getName() + "</alt2>.");
            }
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        World world = player.getWorld();
        if (chunk.getEntities() != null) {
            for (Entity entitys : chunk.getEntities()) {
                if (entitys instanceof Player target) {
                    if (entitys.equals(player)) {
                        continue;
                    }

                    if (clanManager.canHurt(player, target)) {
                        Optional<Clan> targetClanOptional = clanManager.getClanByPlayer(target);
                        if (targetClanOptional.isEmpty()) continue;
                        UtilMessage.message(player, "Clans", "You cannot claim Territory containing enemies.");
                        return;
                    }

                }
            }
        }

        if (!clan.isAdmin()) {
            if (clanManager.adjacentOtherClans(player.getChunk(), clan)) {
                UtilMessage.message(player, "Clans", "You cannot claim next to enemy territory.");
                return;
            }

            if(!chunk.getWorld().getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) {
                UtilMessage.simpleMessage(player, "Clans", "You can only claim territory in the main world.");
                return;
            }
        }

        if (!clan.getTerritory().isEmpty() && !clanManager.adjacentToOwnClan(player.getChunk(), clan) && !clan.isAdmin()) {
            UtilMessage.message(player, "Clans", "You must claim next to your own territory");
            return;
        }

        long claimCooldown = clanManager.getRemainingClaimCooldown(chunk);
        if (claimCooldown > 0) {
            if (!client.isAdministrating()) {
                UtilMessage.message(player, "Clans",
                        "This territory was recently disbanded and cannot be claimed for <green>%s</green>",
                        UtilTime.getTime(claimCooldown, 1));
                return;
            }
            clientManager.sendMessageToRank("Clans",
                    UtilMessage.deserialize("<yellow>%s</yellow> claimed territory on claim cooldown for <yellow>%s</yellow>", player.getName(), clan.getName()),
                    Rank.TRIAL_MOD);
        }


        UtilServer.callEvent(new ChunkClaimEvent(player, clan));

    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
