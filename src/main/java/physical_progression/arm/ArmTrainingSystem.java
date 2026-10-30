package physical_progression.arm;

import physical_progression.attachment.PlayerConditioningData.Arm;
import physical_progression.config.ConditioningConfig;
import physical_progression.stamina.*;

public final class ArmTrainingSystem {
  public static void record(Arm arm, double spent, double weight) {
    var c = arm.endurance;
    c.load += spent;
    c.weightedLoad += spent * weight;
    c.activeTicks++;
    c.idleTicks = 0;
    c.peakFatigue =
        Math.max(c.peakFatigue, 1 - physical_progression.body.DerivedPerformance.fraction(c));
  }

  public record Result(
      double intensity,
      double enduranceXp,
      double strengthXp,
      double enduranceGrowth,
      double strengthGrowth,
      int ticks,
      String reason,
      double cost,
      double meanWeight) {}

  public static Result tick(Arm arm) {
    return tick(arm, true);
  }

  public static Result tick(Arm arm, boolean allowSettlement) {
    var c = arm.endurance;
    if (c.load <= 0) return null;
    c.elapsedTicks++;
    c.idleTicks++;
    if (!allowSettlement) return null;
    String reason =
        c.load >= c.maximum * ConditioningConfig.SESSION_LIMIT.get()
            ? "load_limit"
            : c.elapsedTicks >= ConditioningConfig.SESSION_MAX_TICKS.get()
                ? "window"
                : c.idleTicks >= ConditioningConfig.SESSION_DELAY.get() ? "idle" : null;
    return reason == null ? null : finish(arm, reason);
  }

  public static Result finish(Arm arm, String reason) {
    var c = arm.endurance;
    if (c.load <= 0) return null;
    double cost = c.load;
    double meanWeight = c.weightedLoad / c.load;
    double oldEndurance = c.maximum, oldStrength = arm.strength, ratio = c.load / c.maximum;
    int ticks =
        Math.max(1, reason.equals("idle") ? c.elapsedTicks - c.idleTicks + 1 : c.elapsedTicks);
    double quality = TrainingMath.sessionQuality(c.load / c.maximum, c.peakFatigue);
    double relativeWeight = c.weightedLoad / c.load / arm.strength;
    double strengthShare =
        Math.clamp((relativeWeight - 1) / (ConditioningConfig.HEAVY_WEIGHT.get()), 0, 0.9);
    double stimulus = c.load * quality * ConditioningConfig.TRAINING_XP.get();
    c.xp += stimulus * (1 - strengthShare) * ConditioningConfig.ENDURANCE_GROWTH.get();
    arm.strengthXp += stimulus * strengthShare * ConditioningConfig.STRENGTH_GROWTH.get();
    StaminaTrainingSystem.grow(c, ConditioningConfig.ARM_INITIAL.get());
    double required = ConditioningConfig.GROWTH_XP.get() * Math.pow(arm.strength, 2);
    while (arm.strengthXp >= required && arm.strength < 1000) {
      arm.strengthXp -= required;
      arm.strength = Math.min(1000, arm.strength + 0.01);
      required = ConditioningConfig.GROWTH_XP.get() * Math.pow(arm.strength, 2);
    }
    c.clearSession();
    return new Result(
        ratio,
        stimulus * (1 - strengthShare) * ConditioningConfig.ENDURANCE_GROWTH.get(),
        stimulus * strengthShare * ConditioningConfig.STRENGTH_GROWTH.get(),
        c.maximum - oldEndurance,
        arm.strength - oldStrength,
        ticks,
        reason,
        cost,
        meanWeight);
  }
}
