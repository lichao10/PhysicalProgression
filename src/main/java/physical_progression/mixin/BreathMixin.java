package physical_progression.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import physical_progression.attachment.ModAttachments;
import physical_progression.body.DerivedPerformance;

@Mixin(Entity.class)
abstract class BreathMixin {
  @Inject(method = "getMaxAirSupply", at = @At("RETURN"), cancellable = true)
  private void physical_progression$breath(CallbackInfoReturnable<Integer> ci) {
    if ((Object) this instanceof ServerPlayer p && p.hasData(ModAttachments.PLAYER))
      ci.setReturnValue(
          ci.getReturnValueI()
              + DerivedPerformance.breathBonus(p.getData(ModAttachments.PLAYER).body));
    else if ((Object) this instanceof net.minecraft.world.entity.player.Player p
        && p.hasData(physical_progression.network.ModNetworking.ClientSnapshot.TYPE))
      ci.setReturnValue(
          ci.getReturnValueI()
              + (int)
                  p.getData(physical_progression.network.ModNetworking.ClientSnapshot.TYPE)
                      .attribute("breathBonus"));
  }
}
