package agency.highlysuspect.packages.platform.fabric.mixin;

import agency.highlysuspect.packages.platform.fabric.client.FabricClientInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Shadow @Nullable public LocalPlayer player;
	@Shadow @Nullable public ClientLevel level;
	
	@SuppressWarnings("InvalidInjectorMethodSignature") //simply an MCDev false positive I think
	@Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
	private void packages$startAttack$beforeStartDestroyingBlock(CallbackInfoReturnable<Boolean> cir, boolean miscLocal, BlockHitResult hit, BlockPos hitPos) {
		if(player != null && FabricClientInit.instanceFabric.EARLY_LEFT_CLICK_EVENT.invoker().interact(player, level, hitPos, hit.getDirection())) {
			player.swing(InteractionHand.MAIN_HAND);
			cir.setReturnValue(true);
		}
	}
}
