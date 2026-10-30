package physical_progression.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import physical_progression.arm.*;
import physical_progression.attachment.*;
import physical_progression.body.*;
import physical_progression.config.ConditioningConfig;

public final class BodySnapshots {
  public static CompoundTag resources(PlayerConditioningData d) {
    var t = new CompoundTag();
    var b = d.body;
    capacity(t, "stamina", d.stamina);
    capacity(t, "legs", b.legs);
    capacity(t, "left", d.left.endurance);
    capacity(t, "right", d.right.endurance);
    t.putInt("legActive", b.legActive);
    t.putString(
        "sessionState", b.sessionLoad > 0 ? (b.sessionIdle > 0 ? "settling" : "active") : "rest");
    t.putInt("leftActive", b.leftActive);
    t.putInt("rightActive", b.rightActive);
    t.putString("activity", b.activity);
    t.putBoolean("climbEngaged", b.climbEngaged);
    t.putBoolean("movementSuppressed", d.movementSuppression > 0);
    t.putBoolean("climbExhausted", b.climbExhausted);
    t.putString("sessionActivity", b.sessionActivity);
    t.putInt("sessionTicks", b.sessionTicks);
    t.putDouble("sessionDistance", b.sessionDistance);
    t.putDouble("sessionVertical", b.sessionVertical);
    t.putDouble("climbDistance", b.sessionClimbDistance);
    t.putDouble("sessionCost", b.sessionLoad);
    t.putDouble("intensity", BodyTrainingSystem.intensity(b));
    t.putDouble("leftIntensity", d.left.endurance.load / d.left.endurance.maximum);
    t.putDouble("rightIntensity", d.right.endurance.load / d.right.endurance.maximum);
    t.putDouble("jump", DerivedPerformance.jumpMultiplier(d));
    t.putDouble("sprintCost", DerivedPerformance.movementCost(d, true));
    t.putDouble("walkCost", DerivedPerformance.movementCost(d, false));
    t.putDouble("fatiguePenalty", 1 - DerivedPerformance.fatigueMultiplier(d));
    t.putDouble("loadPenalty", DerivedPerformance.loadPenalty(b) - 1);
    t.putDouble("recoveryRate", d.recoveryRate * DerivedPerformance.recoveryMultiplier(b));
    t.putDouble(
        "walkingRecoveryRate",
        d.recoveryRate
            * DerivedPerformance.recoveryMultiplier(b)
            * ConditioningConfig.WALK_RECOVERY.get());
    t.putInt(
        "recoveryDelay",
        Math.max(0, DerivedPerformance.recoveryDelay(b) - d.stamina.recoveryTicks));
    t.putInt("burstTicks", b.burstTicks);
    t.putDouble("burstBonus", b.burstTicks > 0 ? DerivedPerformance.burstBonus(b) : 0);
    t.putBoolean(
        "recovering",
        d.stamina.current < d.stamina.maximum
            && d.stamina.recoveryTicks >= DerivedPerformance.recoveryDelay(b)
            && (b.activity.equals("rest") || b.activity.equals("walking")));
    t.putLong("completedId", b.completedId);
    return t;
  }

  private static void capacity(CompoundTag t, String key, Capacity c) {
    t.putDouble(key, c.current);
    t.putDouble(key + "Max", c.maximum);
  }

  public static CompoundTag resources(ServerPlayer p, PlayerConditioningData d) {
    var t = resources(d);
    t.putInt("air", Math.max(0, p.getAirSupply()));
    t.putInt("airMax", p.getMaxAirSupply());
    t.putBoolean("underwater", p.isUnderWater());
    t.putBoolean("lungTraining", p.isUnderWater() && BreathTrainingSystem.training(d.body));
    t.putDouble("breathIntensity", d.body.breathSpent / Math.max(1, d.body.breathMaximum));
    if (p.isUnderWater() && breatheAtRest(d)) {
      t.putString("sessionActivity", "diving");
      t.putInt("sessionTicks", d.body.breathTicks);
      t.putDouble("intensity", d.body.breathSpent / Math.max(1, d.body.breathMaximum));
      t.putDouble("sessionCost", 0);
      t.putDouble("sessionDistance", 0);
      t.putDouble("sessionAirUsed", d.body.breathSpent);
      t.putString(
          "sessionState",
          (d.body.diveDamaged || d.body.breathInvalid)
              ? "invalid"
              : d.body.breathPaid ? "settled" : "active");
    } else if (d.body.sessionLoad <= 0
        && (d.left.endurance.load > 0 || d.right.endurance.load > 0)) {
      var left = d.left.endurance;
      var right = d.right.endurance;
      boolean both = left.load > 0 && right.load > 0;
      var arm = left.load >= right.load ? left : right;
      t.putString(
          "sessionActivity", both ? "mixed_arms" : left.load > 0 ? "left_arm" : "right_arm");
      t.putInt(
          "sessionTicks",
          Math.max(1, arm.elapsedTicks - (arm.idleTicks > 1 ? arm.idleTicks : 0) + 1));
      t.putDouble("intensity", Math.max(left.load / left.maximum, right.load / right.maximum));
      t.putDouble("sessionCost", left.load + right.load);
      t.putString("sessionState", arm.idleTicks > 1 ? "settling" : "active");
    }
    arm(t, p, d, ArmSide.LEFT);
    arm(t, p, d, ArmSide.RIGHT);
    return t;
  }

  private static boolean breatheAtRest(PlayerConditioningData d) {
    return d.body.breathActive && !d.body.activity.equals("swimming");
  }

  private static void arm(CompoundTag t, ServerPlayer p, PlayerConditioningData d, ArmSide side) {
    String key = side == ArmSide.LEFT ? "left" : "right";
    var hand =
        ArmSide.resolve(p, InteractionHand.MAIN_HAND) == side
            ? InteractionHand.MAIN_HAND
            : InteractionHand.OFF_HAND;
    var stack = p.getItemInHand(hand);
    var arm = side.data(d);
    double weight = ItemWeightManager.weight(stack),
        multiplier = DerivedPerformance.armCostMultiplier(d, arm);
    t.putString(
        key + "Item",
        stack.isEmpty()
            ? "ui.physical_progression.empty_hand"
            : stack.getItem().getDescriptionId());
    if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))
      t.putString(key + "CustomName", stack.getHoverName().getString());
    t.putDouble(key + "Weight", weight);

    t.putDouble(key + "Cost", ConditioningConfig.ATTACK_COST.get() * weight * multiplier);
    t.putDouble(key + "Penalty", DerivedPerformance.armFatigue(arm) - 1);
  }

  public static CompoundTag attributes(PlayerConditioningData d) {
    var t = new CompoundTag();
    var b = d.body;
    for (var a : BodyAbility.values()) {
      var progress = b.abilities.get(a);
      t.putDouble(a.key(), progress.value);
      t.putDouble(a.key() + "Xp", progress.xp);
      t.putDouble(a.key() + "Required", progress.requiredXp());
    }
    t.putDouble("leftStrength", d.left.strength);
    t.putDouble("rightStrength", d.right.strength);
    t.putDouble("leftEndurance", d.left.endurance.maximum);
    t.putDouble("rightEndurance", d.right.endurance.maximum);
    t.putDouble("leftStrengthXp", d.left.strengthXp);
    t.putDouble("rightStrengthXp", d.right.strengthXp);
    t.putDouble("leftEnduranceXp", d.left.endurance.xp);
    t.putDouble("rightEnduranceXp", d.right.endurance.xp);
    t.putDouble(
        "leftStrengthRequired", ConditioningConfig.GROWTH_XP.get() * Math.pow(d.left.strength, 2));
    t.putDouble(
        "rightStrengthRequired",
        ConditioningConfig.GROWTH_XP.get() * Math.pow(d.right.strength, 2));
    t.putDouble(
        "leftEnduranceRequired",
        physical_progression.stamina.TrainingMath.requiredXp(
            d.left.endurance.maximum,
            ConditioningConfig.ARM_INITIAL.get(),
            ConditioningConfig.GROWTH_XP.get()));
    t.putDouble(
        "rightEnduranceRequired",
        physical_progression.stamina.TrainingMath.requiredXp(
            d.right.endurance.maximum,
            ConditioningConfig.ARM_INITIAL.get(),
            ConditioningConfig.GROWTH_XP.get()));
    t.putDouble("cardioContribution", DerivedPerformance.cardioCapacity(b));
    t.putDouble("legContribution", DerivedPerformance.enduranceCapacity(b));
    t.putDouble("cardioRecovery", Math.min(2, 0.15 * (b.value(BodyAbility.CARDIO) - 1)));
    t.putDouble("sustainedEfficiency", 1 - DerivedPerformance.cardioEfficiency(b));
    t.putDouble("lungsEfficiency", 1 - 1 / (1 + 0.03 * (b.value(BodyAbility.LUNGS) - 1)));
    t.putDouble("lungsRecovery", 0.05 * (b.value(BodyAbility.LUNGS) - 1));
    t.putDouble("breathBonus", DerivedPerformance.breathBonus(b));
    t.putDouble("climbingSpeed", ClimbingSystem.speed(b.value(BodyAbility.CLIMBING)) * 20);
    t.putDouble("climbingHoldCost", ConditioningConfig.CLIMB_HOLD_COST.get());
    t.putDouble("climbingArmHoldCost", ConditioningConfig.CLIMB_ARM_HOLD_COST.get());
    t.putDouble("climbingMoveCost", ConditioningConfig.CLIMB_COST.get());
    t.putDouble("minimumFatigue", ConditioningConfig.MIN_FATIGUE.get());
    t.putDouble("optimalFatigue", ConditioningConfig.OPTIMAL.get());
    t.put("lastSession", b.lastSession.copy());
    t.put("history", b.historyTag());
    return t;
  }
}
