package physical_progression.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ConnectionTeleportMixin {
  @Shadow public ServerPlayer player;

  @Inject(
      method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V",
      at = @At("HEAD"))
  private void physical_progression$teleport(
      net.minecraft.world.entity.PositionMoveRotation movement,
      java.util.Set<net.minecraft.world.entity.Relative> relative,
      CallbackInfo ci) {
    physical_progression.stamina.MovementTracker.suppress(player, 3);
  }
}
