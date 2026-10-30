package physical_progression.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import physical_progression.stamina.MovementTracker;

@Mixin(Entity.class)
abstract class EntityMovementMixin {
  @Inject(method = "move", at = @At("HEAD"))
  private void physical_progression$externalMove(MoverType type, Vec3 movement, CallbackInfo ci) {
    if ((Object) this instanceof ServerPlayer p
        && type != MoverType.SELF
        && type != MoverType.PLAYER) MovementTracker.suppress(p, 10);
  }

  @Inject(method = "push(DDD)V", at = @At("HEAD"))
  private void physical_progression$impulse(double x, double y, double z, CallbackInfo ci) {
    if ((Object) this instanceof ServerPlayer p) MovementTracker.suppress(p, 20);
  }
}
