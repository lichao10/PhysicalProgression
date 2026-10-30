package physical_progression.attachment;

import net.minecraft.nbt.CompoundTag;

/** Short-term resource, persistent adaptation, and one pending training session. */
public final class Capacity {
  public double current, maximum, xp, load, distance, weightedLoad;
  public double peakFatigue;
  public int activeTicks, idleTicks, recoveryTicks, elapsedTicks;

  public Capacity(double initial) {
    current = maximum = initial;
  }

  public double spend(double amount) {
    double used = Math.min(current, Math.max(0, amount));
    current -= used;
    return used;
  }

  public void recover(double amount) {
    current = Math.min(maximum, current + Math.max(0, amount));
  }

  public void clearSession() {
    load = distance = weightedLoad = 0;
    activeTicks = idleTicks = elapsedTicks = 0;
    peakFatigue = 0;
  }

  public CompoundTag save() {
    var t = new CompoundTag();
    t.putDouble("current", current);
    t.putDouble("maximum", maximum);
    t.putDouble("xp", xp);
    t.putDouble("load", load);
    t.putDouble("distance", distance);
    t.putDouble("weightedLoad", weightedLoad);
    t.putDouble("peakFatigue", peakFatigue);
    t.putInt("activeTicks", activeTicks);
    t.putInt("elapsedTicks", elapsedTicks);
    t.putInt("idleTicks", idleTicks);
    t.putInt("recoveryTicks", recoveryTicks);
    return t;
  }

  public void read(CompoundTag t) {
    maximum = valid(t, "maximum", maximum, 1, 100000);
    current = valid(t, "current", maximum, 0, maximum);
    xp = valid(t, "xp", 0, 0, 1e9);
    load = valid(t, "load", 0, 0, 1e6);
    distance = valid(t, "distance", 0, 0, 1e6);
    weightedLoad = valid(t, "weightedLoad", 0, 0, 1e8);
    peakFatigue = valid(t, "peakFatigue", 0, 0, 1);
    activeTicks = Math.clamp(t.getIntOr("activeTicks", 0), 0, 1000000);
    elapsedTicks = Math.clamp(t.getIntOr("elapsedTicks", 0), 0, 1000000);
    idleTicks = Math.clamp(t.getIntOr("idleTicks", 0), 0, 12000);
    recoveryTicks = Math.clamp(t.getIntOr("recoveryTicks", 0), 0, 12000);
  }

  public static double valid(CompoundTag t, String k, double fallback, double min, double max) {
    double v = t.getDoubleOr(k, fallback);
    return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback;
  }
}
