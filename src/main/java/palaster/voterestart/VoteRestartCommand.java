package palaster.voterestart;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;

public class VoteRestartCommand {
	
	public static Vote openVote = null;
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		LiteralArgumentBuilder<CommandSource> startVoteCommand = Commands.literal("voterestart").executes((commandContext) -> {
			return startVote(commandContext.getSource());
		});
		
		LiteralArgumentBuilder<CommandSource> forceYesCommand = Commands.literal("voterestart").then(Commands.literal("force").then(Commands.literal("yes").executes((commandContext) -> {
			return vote(commandContext.getSource(), true, true);
		})));
		LiteralArgumentBuilder<CommandSource> forceNoCommand = Commands.literal("voterestart").then(Commands.literal("force").then(Commands.literal("no").executes((commandContext) -> {
			return vote(commandContext.getSource(), true, false);
		})));
		
		LiteralArgumentBuilder<CommandSource> yesCommand = Commands.literal("voterestart").then(Commands.literal("yes").executes((commandContext) -> {
			return vote(commandContext.getSource(), false, true);
		}));
		LiteralArgumentBuilder<CommandSource> noCommand = Commands.literal("voterestart").then(Commands.literal("no").executes((commandContext) -> {
			return vote(commandContext.getSource(), false, false);
		}));
		
		dispatcher.register(startVoteCommand);
		dispatcher.register(forceYesCommand);
		dispatcher.register(forceNoCommand);
		dispatcher.register(yesCommand);
		dispatcher.register(noCommand);
	}
	
	private static int startVote(CommandSource commandSource) {
		if(commandSource.getEntity() != null) {
			if(openVote == null) {
				if(Vote.timeBetweenVotes <= 0) {
					openVote = new Vote(commandSource);
					commandSource.getServer().getPlayerList().broadcastMessage(new StringTextComponent("New Restart Vote has started"), ChatType.CHAT, commandSource.getEntity().getUUID());
				} else
					commandSource.sendFailure(new StringTextComponent("Can't open new vote for " + Vote.timeBetweenVotes + " ticks"));
			} else
				commandSource.sendFailure(new StringTextComponent("Vote has already started"));
		} else
			commandSource.sendFailure(new StringTextComponent("Servers aren't allowed to start a vote"));
		return 0;
	}
	
	private static int vote(CommandSource commandSource, boolean force, boolean isYes) {
		if(commandSource.getEntity() != null) {
			if(openVote != null) {
				boolean result = openVote.addVote(commandSource.getEntity().getUUID(), force, isYes);
				if(result)
					commandSource.sendSuccess(new StringTextComponent("Your vote has been cast"), true);
				else
					commandSource.sendFailure(new StringTextComponent("You already cast your vote. Use force to change it"));
				openVote.tryFinishVote();
			} else
				commandSource.sendFailure(new StringTextComponent("There isn't an open vote"));
		} else
			commandSource.sendFailure(new StringTextComponent("Servers aren't allowed to vote"));
		return 0;
	}
	
	public static class Vote {
		
		public static int timeBetweenVotes = ConfigurationHandler.SERVER.timeBetweenVotes.get();
		
		private final MinecraftServer minecraftServer;
		
		private int timeUntilVoteClose = ConfigurationHandler.SERVER.timeUntilVoteClose.get(),
				timeUntilRestart = ConfigurationHandler.SERVER.timeUntilRestart.get();
		private Map<UUID, Boolean> votes = new HashMap<UUID, Boolean>();
		
		private boolean shouldRestart = false;
		
		public Vote(CommandSource commandSource) { this.minecraftServer = commandSource.getServer(); }
		
		public boolean addVote(UUID uuid, boolean force, boolean vote) {
			if(votes.containsKey(uuid)) {
				if(force) {
					votes.replace(uuid, vote);
					return true;
				}
				return false;
			} else {
				votes.put(uuid, vote);
				return true;
			}
		}
		
		public void removeVote(UUID uuid) { votes.remove(uuid); }
		
		public void update() {
			if(shouldRestart) {
				if(timeUntilRestart <= 0)
					minecraftServer.halt(false);
				else
					timeUntilRestart--;
			} else {
				if(timeUntilVoteClose <= 0)
					finishVote();
				else
					timeUntilVoteClose--;
			}
		}
		
		public void tryFinishVote() {
			int amountOfVotes = votes.size();
			int amountOfPlayers = minecraftServer.getPlayerList().getPlayerCount();
			if(amountOfVotes >= amountOfPlayers)
				finishVote();
		}
		
		private void finishVote() {
			int amountOfVotes = votes.size();
			int amountOfPlayers = minecraftServer.getPlayerList().getPlayerCount();
			if (amountOfVotes >= (amountOfPlayers / 2)) {
				int yes = 0, no = 0;
				for(Boolean vote : votes.values()) {
					if(vote)
						yes++;
					else
						no++;
				}
				if(yes > no) {
					minecraftServer.getPlayerList().broadcastMessage(new StringTextComponent("Vote passed will restart in " + timeUntilRestart + " ticks"), ChatType.CHAT, Util.NIL_UUID);
					shouldRestart = true;
				} else {
					minecraftServer.getPlayerList().broadcastMessage(new StringTextComponent("Vote failed will not restart"), ChatType.CHAT, Util.NIL_UUID);
					timeBetweenVotes = ConfigurationHandler.SERVER.timeBetweenVotes.get();
					VoteRestartCommand.openVote = null;
				}
			} else {
				minecraftServer.getPlayerList().broadcastMessage(new StringTextComponent("Vote failed due to more than half of the players not voting"), ChatType.CHAT, Util.NIL_UUID);
				timeBetweenVotes = ConfigurationHandler.SERVER.timeBetweenVotes.get();
				VoteRestartCommand.openVote = null;
			}
		}
	}
}
