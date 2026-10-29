package physical_progression.body;

import static physical_progression.body.BodyAbility.*;

import physical_progression.attachment.PlayerConditioningData;
import physical_progression.config.ConditioningConfig;
import physical_progression.stamina.TrainingMath;
import net.minecraft.nbt.CompoundTag;

public final class BodyTrainingSystem {
  public static void record(
      PlayerConditioningData d, String activity, double cost, double distance, int kind) {
    if (cost <= 0) return;
    var b = d.body;
    if (b.sessionCapacity <= 0) b.sessionCapacity = d.stamina.maximum;
    b.sessionLoad += cost;
    b.sessionPeakFatigue =
        Math.max(b.sessionPeakFatigue, 1 - DerivedPerformance.fraction(d.stamina));
    b.sessionDistance += distance;
    b.sessionIdle = 0;
    b.sessionKinds |= kind;
    if (b.sessionActivity.equals("rest")) b.sessionActivity = activity;
    else if (!b.sessionActivity.equals(activity)) b.sessionActivity = "mixed";
    d.stamina.load = b.sessionLoad;
    d.stamina.distance = b.sessionDistance;
    d.stamina.activeTicks = b.sessionTicks;
  }

  public static double intensity(BodyState b) {
    return b.sessionLoad / Math.max(1, b.sessionCapacity);
  }

  public static double quality(double intensity) {
    return TrainingMath.quality(
        intensity, ConditioningConfig.MIN_FATIGUE.get(), ConditioningConfig.OPTIMAL.get());
  }

  public static void tick(PlayerConditioningData d, boolean active) {
    var b = d.body;
    if (b.sessionLoad <= 0) return;
    if (active) {
      b.sessionTicks++;
      d.stamina.activeTicks = b.sessionTicks;
      b.sessionIdle = 0;
    } else b.sessionIdle++;
    String reason =
        b.sessionLoad >= b.sessionCapacity * ConditioningConfig.SESSION_LIMIT.get()
            ? "load_limit"
            : b.sessionTicks >= ConditioningConfig.SESSION_MAX_TICKS.get()
                ? "window"
                : b.sessionIdle >= ConditioningConfig.SESSION_DELAY.get() ? "idle" : null;
    if (reason == null) return;
    double ratio = intensity(b),
        q = TrainingMath.sessionQuality(ratio, b.sessionPeakFatigue),
        before = d.stamina.maximum;
    var feedback = feedback(b, b.sessionActivity, b.sessionDistance, b.sessionTicks, ratio);
    for (var a : BodyAbility.values()) {
      double stimulus = b.pending.getOrDefault(a, 0.0) * q * ConditioningConfig.TRAINING_XP.get();
      if (a == LUNGS && b.diveDamaged) stimulus = 0;
      double growth = b.abilities.get(a).grant(stimulus, a);
      feedback.putDouble(a.key() + "Xp", stimulus);
      feedback.putDouble(a.key() + "Growth", growth);
    }
    if (q > 0) {
      b.recoveryCredit =
          Math.min(
              100,
              b.recoveryCredit + b.sessionLoad * q * 0.08 * ConditioningConfig.TRAINING_XP.get());
      b.trainingKinds |= b.sessionKinds;
    }
    physical_progression.arm.ArmSystem.settleAttached(d, feedback);
    DerivedPerformance.refresh(d);
    feedback.putDouble("maxBefore", before);
    feedback.putDouble("maxAfter", d.stamina.maximum);
    feedback.putDouble("cost", b.sessionLoad);
    feedback.putDouble("verticalDistance", b.sessionVertical);
    feedback.putDouble("climbDistance", b.sessionClimbDistance);
    feedback.putDouble("peakFatigue", b.sessionPeakFatigue);
    feedback.putDouble("recovered", b.sessionRecovered);
    complete(b, feedback, reason);
    b.clearSession();
    d.stamina.clearSession();
  }

  public static double totalXp(CompoundTag tag) {
    double xp = 0;
    for (String key : tag.getAllKeys()) if (key.endsWith("Xp")) xp += tag.getDouble(key);
    return xp;
  }

  public static void complete(BodyState body, CompoundTag tag, String reason) {
    tag.putString("reason", reason);
    tag.putString(
        "outcome",
        totalXp(tag) > 0
            ? "rewarded"
            : ConditioningConfig.TRAINING_XP.get() == 0
                ? "disabled"
                : body.diveDamaged && tag.getString("activity").equals("diving")
                    ? "drowning"
                    : "too_light");
    body.complete(tag);
  }

  public static CompoundTag feedback(
      BodyState b, String activity, double distance, int ticks, double intensity) {
    var t = new CompoundTag();
    t.putLong("id", ++b.completedId);
    t.putString("activity", activity);
    t.putDouble("distance", distance);
    t.putInt("ticks", ticks);
    t.putDouble("intensity", intensity);
    return t;
  }

  public static void recovered(PlayerConditioningData d) {
    var b = d.body;
    if (b.recoveryCredit <= 0
        || DerivedPerformance.fraction(d.stamina) < 0.9
        || DerivedPerformance.fraction(b.legs) < 0.9
        || DerivedPerformance.fraction(d.left.endurance) < 0.9
        || DerivedPerformance.fraction(d.right.endurance) < 0.9) return;
    double stimulus = b.recoveryCredit * (Integer.bitCount(b.trainingKinds) >= 2 ? 1 : 0.5);
    double growth = b.abilities.get(RECOVERY).grant(stimulus);
    b.recoveryCredit = 0;
    b.trainingKinds = 0;
    // Spend a finite credit earned by training, so resting alone cannot generate XP.
    var target=b.lastSession;
    for(var entry:b.history)if(totalXp(entry)>0){target=entry;break;}
    boolean recoveryOnly=totalXp(target)<=0;
    target.putDouble("recoveryXp",target.getDouble("recoveryXp")+stimulus);
    target.putDouble("recoveryGrowth",target.getDouble("recoveryGrowth")+growth);
    target.putLong("recoveredAt",System.currentTimeMillis());
    target.putString("outcome",recoveryOnly?"recovery_only":"rewarded");
  }
}
