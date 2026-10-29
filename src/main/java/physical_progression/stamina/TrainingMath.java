package physical_progression.stamina;

public final class TrainingMath {
  private TrainingMath() {}

  /** Smooth quality makes a fixed route less effective as capacity improves. */
  public static double quality(double ratio, double minimum, double optimal) {
    if (ratio < minimum) return 0;
    if (ratio < 0.4) return 0.3 * (ratio - minimum) / (0.4 - minimum);
    if (ratio < optimal) return 0.3 + 0.6 * (ratio - 0.4) / (optimal - 0.4);
    if (ratio < 0.9) return 0.9 + 0.1 * (ratio - optimal) / (0.9 - optimal);
    return 0.8 / Math.max(1, ratio * ratio);
  }

  public static double sessionQuality(double workload, double peakFatigue) {
    double quality =
        quality(
            Math.min(workload, 0.89),
            physical_progression.config.ConditioningConfig.MIN_FATIGUE.get(),
            physical_progression.config.ConditioningConfig.OPTIMAL.get());
    return quality * (peakFatigue >= 0.95 ? 0.75 : 1);
  }

  public static double requiredXp(double maximum, double initial, double base) {
    return base * Math.pow(maximum / initial, 1.5);
  }
}
