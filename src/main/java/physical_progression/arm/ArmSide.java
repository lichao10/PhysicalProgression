package physical_progression.arm;

import physical_progression.attachment.PlayerConditioningData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

public enum ArmSide {
  LEFT,
  RIGHT;

  public static ArmSide resolve(Player p, InteractionHand hand) {
    HumanoidArm arm =
        hand == InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite();
    return arm == HumanoidArm.LEFT ? LEFT : RIGHT;
  }

  public PlayerConditioningData.Arm data(PlayerConditioningData d) {
    return this == LEFT ? d.left : d.right;
  }
}
