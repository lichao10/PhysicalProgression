package physical_progression.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import physical_progression.arm.*;
import physical_progression.config.ConditioningConfig;

public final class MiningEventHandler {
  public static void broken(ServerPlayer p, float hardness, double weight) {
    if (hardness > 0)
      ArmSystem.act(
          p,
          InteractionHand.MAIN_HAND,
          ConditioningConfig.MINING_COST.get() * Math.clamp(hardness, 0.1, 5),
          weight,
          true);
  }
}
