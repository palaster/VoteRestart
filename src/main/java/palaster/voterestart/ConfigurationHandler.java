package palaster.voterestart;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigurationHandler {

	public static class Server {
		public final ForgeConfigSpec.IntValue timeBetweenVotes;
		public final ForgeConfigSpec.IntValue timeUntilVoteClose;
		public final ForgeConfigSpec.IntValue timeUntilRestart;
		
		public Server(ForgeConfigSpec.Builder builder) {
			timeBetweenVotes = builder
					.comment("The time (in ticks) between votes")
					.defineInRange("timeBetweenVotes", 36000, 0, Integer.MAX_VALUE);
			timeUntilVoteClose = builder
					.comment("The time (in ticks) until the vote closes")
					.defineInRange("timeUntilVoteClose", 3000, 0, Integer.MAX_VALUE);
			timeUntilRestart = builder
					.comment("The time (in ticks) until the restart")
					.defineInRange("timeUntilRestart", 3000, 0, Integer.MAX_VALUE);
		}
	}
	
	public static final Server SERVER;
	public static final ForgeConfigSpec SERVER_SPEC;
	static {
		final Pair<Server,ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
		SERVER_SPEC = specPair.getRight();
		SERVER = specPair.getLeft();
	}
}
