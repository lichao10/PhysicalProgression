package physical_progression.arm;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import physical_progression.attachment.*;
import physical_progression.body.*;
import physical_progression.config.ConditioningConfig;
import physical_progression.stamina.MovementTracker;

public final class ArmSystem {
  public static void act(
      ServerPlayer p, InteractionHand hand, double base, double weight, boolean training) {
    if (!MovementTracker.eligible(p)) return;
    var d = p.getData(ModAttachments.PLAYER);
    var side = ArmSide.resolve(p, hand);
    var arm = side.data(d);
    double spent =
        arm.endurance.spend(base * weight * DerivedPerformance.armCostMultiplier(d, arm));
    arm.endurance.recoveryTicks = 0;
    if (side == ArmSide.LEFT) d.body.leftActive = 40;
    else d.body.rightActive = 40;
    if (training && spent > 0) ArmTrainingSystem.record(arm, spent, weight);
  }

  public static void tick(ServerPlayer p) {
    var d = p.getData(ModAttachments.PLAYER);
    boolean eligible = MovementTracker.eligible(p);
    recover(d, d.left, eligible);
    recover(d, d.right, eligible);
    boolean standalone = d.body.sessionLoad <= 0;
    var left = ArmTrainingSystem.tick(d.left, standalone);
    var right = ArmTrainingSystem.tick(d.right, standalone);
    if (left == null && right == null) return;
    String activity =
        left != null && right != null ? "mixed_arms" : left != null ? "left_arm" : "right_arm";
    int ticks = Math.max(left == null ? 0 : left.ticks(), right == null ? 0 : right.ticks());
    double intensity =
        Math.max(left == null ? 0 : left.intensity(), right == null ? 0 : right.intensity());
    var summary = BodyTrainingSystem.feedback(d.body, activity, 0, ticks, intensity);
    addResult(d, summary, "left", left);
    addResult(d, summary, "right", right);
    summary.putDouble(
        "cost", (left == null ? 0 : left.cost()) + (right == null ? 0 : right.cost()));
    BodyTrainingSystem.complete(d.body, summary, left != null ? left.reason() : right.reason());
  }

  private static void recover(
      PlayerConditioningData d, PlayerConditioningData.Arm arm, boolean eligible) {
    if (d.body.climbEngaged) {
      arm.endurance.recoveryTicks = 0;
      return;
    }
    if (eligible && ++arm.endurance.recoveryTicks >= DerivedPerformance.recoveryDelay(d.body))
      arm.endurance.recover(
          ConditioningConfig.ARM_RECOVERY.get()
              * DerivedPerformance.recoveryMultiplier(d.body)
              / 20);
  }

  public static void settleAttached(
      PlayerConditioningData d, net.minecraft.nbt.CompoundTag summary) {
    addResult(d, summary, "left", ArmTrainingSystem.finish(d.left, "body"));
    addResult(d, summary, "right", ArmTrainingSystem.finish(d.right, "body"));
  }

  private static void add(net.minecraft.nbt.CompoundTag t, String key, double value) {
    t.putDouble(key, t.getDoubleOr(key, 0.0) + value);
  }

  private static void addResult(
      PlayerConditioningData d,
      net.minecraft.nbt.CompoundTag summary,
      String side,
      ArmTrainingSystem.Result result) {
    if (result == null) return;
    var b = d.body;
    add(summary, side + "EnduranceXp", result.enduranceXp());
    add(summary, side + "StrengthXp", result.strengthXp());
    add(summary, side + "EnduranceGrowth", result.enduranceGrowth());
    add(summary, side + "StrengthGrowth", result.strengthGrowth());
    summary.putDouble(side + "Cost", result.cost());
    summary.putDouble(side + "Intensity", result.intensity());
    summary.putString(
        side + "Outcome",
        result.enduranceXp() + result.strengthXp() > 0
            ? "rewarded"
            : ConditioningConfig.TRAINING_XP.get() == 0 ? "disabled" : "too_light");
    double stimulus = result.enduranceXp() + result.strengthXp(),
        grip = stimulus * 0.25,
        core = result.meanWeight() >= 2 ? stimulus * 0.12 : 0;
    add(summary, "gripXp", grip);
    add(summary, "coreXp", core);
    add(summary, "gripGrowth", b.abilities.get(BodyAbility.GRIP).grant(grip));
    add(summary, "coreGrowth", b.abilities.get(BodyAbility.CORE).grant(core));
    if (stimulus > 0) {
      b.recoveryCredit = Math.min(100, b.recoveryCredit + stimulus * 0.08);
      b.trainingKinds |= 8;
    }
  }
}
