package palaster.voterestart;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor.SafeRunnable;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ServerSetup {
	public static SafeRunnable onServer(VoteRestart voteRestart) {
    	return new SafeRunnable() {			
			@Override
			public void run() {
				ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigurationHandler.SERVER_SPEC);
				MinecraftForge.EVENT_BUS.register(voteRestart);
			}
		};
    }
}
