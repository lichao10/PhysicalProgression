package physical_progression.mixin;

import physical_progression.attachment.ModAttachments;
import physical_progression.network.ModNetworking.ClientSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
abstract class SprintMixin {
  // Change the argument before both the sprint flag AND its speed modifier are applied.
  @ModifyVariable(method = "setSprinting", at = @At("HEAD"), argsOnly = true)
  private boolean physical_progression$allowSprint(boolean sprinting) {
    if (!sprinting || !((Object) this instanceof Player p) || p.isCreative() || p.isSpectator())
      return sprinting;
    if (p instanceof ServerPlayer server)
      return server.getData(ModAttachments.PLAYER).stamina.current > 0
          && server.getData(ModAttachments.PLAYER).body.legs.current > 0;
    return !p.hasData(ClientSnapshot.TYPE)
        || !p.getData(ClientSnapshot.TYPE).ready()
        || (p.getData(ClientSnapshot.TYPE).resource("stamina") > 0
            && p.getData(ClientSnapshot.TYPE).resource("legs") > 0);
  }
}
