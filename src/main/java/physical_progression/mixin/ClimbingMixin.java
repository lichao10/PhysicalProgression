package physical_progression.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import physical_progression.body.ClimbingSystem;

@Mixin(LivingEntity.class)
public abstract class ClimbingMixin {
  @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
  private void physical_progression$climb(CallbackInfoReturnable<Boolean> cir) {
    if ((Object) this instanceof Player p && ClimbingSystem.canClimb(p)) cir.setReturnValue(true);
  }
}
