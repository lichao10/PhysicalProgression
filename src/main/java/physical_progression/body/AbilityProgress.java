package physical_progression.body;

import net.minecraft.nbt.CompoundTag;
import physical_progression.attachment.Capacity;
import physical_progression.config.ConditioningConfig;

public final class AbilityProgress {
  public double value = 1, xp;

  public double grant(double stimulus) {
    return grant(stimulus, null);
  }

  public double grant(double stimulus, BodyAbility ability) {
    double step =
        ability == BodyAbility.LUNGS
            ? ConditioningConfig.LUNGS_STEP.get()
            : ability == BodyAbility.CLIMBING
                ? ConditioningConfig.CLIMBING_STEP.get()
                : ConditioningConfig.ABILITY_STEP.get();
    double before = value;
    xp += Math.max(0, stimulus);
    while (value < 20 && xp >= requiredXp()) {
      xp -= requiredXp();
      value = Math.min(20, value + step);
    }
    return value - before;
  }

  public double requiredXp() {
    return ConditioningConfig.ABILITY_XP.get() * Math.pow(value, 1.5);
  }

  public CompoundTag save() {
    var t = new CompoundTag();
    t.putDouble("value", value);
    t.putDouble("xp", xp);
    return t;
  }

  public void read(CompoundTag t) {
    value = Capacity.valid(t, "value", 1, 1, 20);
    xp = Capacity.valid(t, "xp", 0, 0, 1e7);
  }
}
