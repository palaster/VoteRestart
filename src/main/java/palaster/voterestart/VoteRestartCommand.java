package palaster.voterestart;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class VoteRestartCommand {
	
	public static Vote openVote = null;
	
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		LiteralArgumentBuilder<CommandSource> firstCommand = Commands.literal("voterestart").executes((commandContext) -> {
			return startVote(commandContext.getSource());
		}).then(Commands.literal("force").then(Commands.argument("vote",  MessageArgument.message()).executes((commandContext) -> {
			return vote(commandContext.getSource(), true, MessageArgument.getMessage(commandContext, "vote"));
		})));
		
		LiteralArgumentBuilder<CommandSource> secondCommand = Commands.literal("voterestart").executes((commandContext) -> {
			return startVote(commandContext.getSource());
		}).then(Commands.argument("vote",  MessageArgument.message()).executes((commandContext) -> {
			return vote(commandContext.getSource(), false, MessageArgument.getMessage(commandContext, "vote"));
		}));
		
		dispatcher.register(firstCommand);
		dispatcher.register(secondCommand);
	}
	
	private static int startVote(CommandSource commandSource) {
		if(openVote == null) {
			if(Vote.timeBetweenVotes <= 0) {
				openVote = new Vote(commandSource);
				commandSource.sendSuccess(new StringTextComponent("New Vote has started"), true);
			} else
				commandSource.sendFailure(new StringTextComponent("Can't open new vote for " + Vote.timeBetweenVotes + " ticks"));
		} else
			commandSource.sendFailure(new StringTextComponent("Vote has already started"));
		return 0;
	}
	
	private static int vote(CommandSource commandSource, boolean force, ITextComponent iTextComponent) {
		if(openVote != null) {
			if(iTextComponent.getContents().equals("yes") || iTextComponent.getContents().equals("no")) {
				boolean result = openVote.addVote(commandSource.getEntity().getUUID(), force, iTextComponent.getContents().equals("yes"));
				if(result)
					commandSource.sendSuccess(new StringTextComponent("Your vote has been cast"), true);
				else
					commandSource.sendFailure(new StringTextComponent("You already cast your vote. Use force to change it"));
			} else
				commandSource.sendFailure(new StringTextComponent("Vote not in correct format (yes/no)"));
		} else
			commandSource.sendFailure(new StringTextComponent("There isn't an open vote"));
		return 0;
	}
	
	public static class Vote {
		
		public static int timeBetweenVotes = ConfigurationHandler.SERVER.timeBetweenVotes.get();
		
		private final CommandSource commandSource;
		
		private int timeUntilVoteClose = ConfigurationHandler.SERVER.timeUntilVoteClose.get(),
				timeUntilRestart = ConfigurationHandler.SERVER.timeUntilRestart.get();
		private Map<UUID, Boolean> votes = new HashMap<UUID, Boolean>();
		
		private boolean shouldRestart = false;
		
		public Vote(CommandSource commandSource) {
			this.commandSource = commandSource;
		}
		
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
		
		public void update() {
			if(shouldRestart) {
				if(timeUntilRestart <= 0)
					commandSource.getServer().halt(false);
				else
					timeUntilRestart--;
			} else {
				if(timeUntilVoteClose <= 0) {
					int amountOfVotes = votes.size();
					int amountOfPlayers = commandSource.getServer().getPlayerList().getPlayerCount();
					if (amountOfVotes >= (amountOfPlayers / 2)) {
						int yes = 0, no = 0;
						for(Boolean vote : votes.values()) {
							if(vote)
								yes++;
							else
								no++;
						}
						if(yes > no) {
							commandSource.getServer().getPlayerList().broadcastMessage(new StringTextComponent("Vote passed will restart in " + timeUntilRestart + " ticks"), ChatType.CHAT, commandSource.getEntity().getUUID());
							shouldRestart = true;
						} else {
							commandSource.getServer().getPlayerList().broadcastMessage(new StringTextComponent("Vote failed will not restart"), ChatType.CHAT, commandSource.getEntity().getUUID());
							timeBetweenVotes = ConfigurationHandler.SERVER.timeBetweenVotes.get();
							VoteRestartCommand.openVote = null;
						}
					} else {
						commandSource.getServer().getPlayerList().broadcastMessage(new StringTextComponent("Vote failed due to more than half of the players not voting"), ChatType.CHAT, commandSource.getEntity().getUUID());
						timeBetweenVotes = ConfigurationHandler.SERVER.timeBetweenVotes.get();
						VoteRestartCommand.openVote = null;
					}
				} else
					timeUntilVoteClose--;
			}
		}
	}
}
