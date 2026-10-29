package physical_progression.stamina;

import physical_progression.attachment.Capacity;
import physical_progression.config.ConditioningConfig;

public final class StaminaTrainingSystem {
  // Arm capacities grow directly; global stamina is derived by BodyTrainingSystem.
  public static void grow(Capacity c, double initial) {
    double required =
        TrainingMath.requiredXp(c.maximum, initial, ConditioningConfig.GROWTH_XP.get());
    while (c.xp >= required && c.maximum < ConditioningConfig.CAP.get()) {
      c.xp -= required;
      c.maximum = Math.min(ConditioningConfig.CAP.get(), c.maximum + 1);
      required = TrainingMath.requiredXp(c.maximum, initial, ConditioningConfig.GROWTH_XP.get());
    }
  }
}
