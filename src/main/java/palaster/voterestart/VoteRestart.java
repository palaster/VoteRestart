package palaster.voterestart;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import palaster.voterestart.VoteRestartCommand.Vote;

@Mod("voterestart")
public class VoteRestart {
    public VoteRestart() {
    	ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    	DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> ServerSetup.onServer(this));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
    	if(event.phase == Phase.END) {
    		if(VoteRestartCommand.openVote != null)
    			VoteRestartCommand.openVote.update();
    		else
    			if(Vote.timeBetweenVotes > 0)
    				Vote.timeBetweenVotes--;
    	}
    }
    
    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
    	if(VoteRestartCommand.openVote != null)
    		VoteRestartCommand.openVote.removeVote(event.getPlayer().getUUID());
    }
    
    @SubscribeEvent
	public void onCommandRegistry(RegisterCommandsEvent event) { VoteRestartCommand.register(event.getDispatcher()); }
}
