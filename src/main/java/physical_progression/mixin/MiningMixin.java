package physical_progression.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
abstract class MiningMixin {
  @Shadow @Final protected ServerPlayer player;
  @Unique private float physical_progression$hardness;
  @Unique private double physical_progression$weight;

  @Inject(method = "destroyBlock", at = @At("HEAD"))
  private void physical_progression$before(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
    physical_progression$hardness =
        player.level().getBlockState(pos).getDestroySpeed(player.level(), pos);
    physical_progression$weight =
        physical_progression.arm.ItemWeightManager.weight(player.getMainHandItem());
  }

  @Inject(method = "removeBlock", at = @At("RETURN"))
  private void physical_progression$after(
      BlockPos pos,
      net.minecraft.world.level.block.state.BlockState state,
      boolean canHarvest,
      net.minecraft.world.item.ItemStack tool,
      CallbackInfoReturnable<Boolean> ci) {
    if (ci.getReturnValueZ())
      physical_progression.event.MiningEventHandler.broken(
          player, physical_progression$hardness, physical_progression$weight);
  }
}
