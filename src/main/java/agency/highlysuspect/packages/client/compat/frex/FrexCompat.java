package agency.highlysuspect.packages.client.compat.frex;

import agency.highlysuspect.packages.PackagesInit;
import net.fabricmc.loader.api.FabricLoader;

public class FrexCompat {
	public static final FrexProxy PROXY;
	
	static {
		FrexProxy p;
		
		if(FabricLoader.getInstance().isModLoaded("frex")) {
			try {
				p = (FrexProxy) Class.forName("agency.highlysuspect.packages.client.compat.frex.YesFrex").newInstance();
			} catch (ReflectiveOperationException e) {
				PackagesInit.LOGGER.error("Problem initializing FREX compat, special stuff will be disabled: ", e);
				p = new NoFrex();
			}
		} else {
			p = new NoFrex();
		}
		
		PROXY = p;
	}
	
	public static void onInitializeClient() {
		//Triggers static init
	}
}
