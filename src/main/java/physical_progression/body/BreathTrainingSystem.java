package physical_progression.body;

import physical_progression.attachment.PlayerConditioningData;
import physical_progression.stamina.MovementTracker;
import net.minecraft.server.level.ServerPlayer;

/** One breathing session per submersion. Movement is not required. */
public final class BreathTrainingSystem {
  public static void tick(ServerPlayer p, PlayerConditioningData d) {
    var b = d.body;
    int air = Math.max(0, p.getAirSupply()), max = Math.max(1, p.getMaxAirSupply());
    if (!MovementTracker.eligible(p)) {
      reset(b);
      return;
    }
    boolean underwater = p.isUnderWater();
    if (underwater) {
      if (!b.breathActive) {
        b.breathActive = true;
        b.breathPaid = false;
        b.breathInvalid = b.diveDamaged;
        b.breathSpent = 0;
        b.breathTicks = 0;
        b.breathMaximum = max;
        b.breathStartAir = air;
        b.breathLastAir = air;
      }
      b.breathTicks++;
      b.breathSpent += Math.max(0, b.breathLastAir - air);
      b.breathLastAir = air;
      if (b.activity.equals("rest")) b.activity = "diving";
      if (air <= max * 0.2) settle(d);
    } else if (b.breathActive) {
      settle(d);
      // Surface once to complete, and replenish air before starting a new rewarded hold.
      if (air >= max * 0.9) reset(b);
    }
  }

  public static boolean training(BodyState b) {
    return b.breathActive
        && !b.breathPaid
        && !b.diveDamaged
        && !b.breathInvalid
        && b.breathSpent > 0;
  }

  private static void settle(PlayerConditioningData d) {
    var b = d.body;
    if (b.breathPaid || b.breathTicks < 20) return;
    double ratio = Math.min(1, b.breathSpent / Math.max(1, b.breathMaximum));
    double quality = BodyTrainingSystem.quality(ratio);
    if (b.breathSpent <= 0) return;
    if (b.breathInvalid || b.diveDamaged) quality = 0;
    double xp =
        physical_progression.config.ConditioningConfig.BREATH_XP.get()
            * ratio
            * quality
            * physical_progression.config.ConditioningConfig.TRAINING_XP.get();
    var result = BodyTrainingSystem.feedback(b, "diving", 0, b.breathTicks, ratio);
    result.putDouble("airUsed", b.breathSpent);
    result.putDouble("lungsXp", xp);
    result.putDouble(
        "lungsGrowth", b.abilities.get(BodyAbility.LUNGS).grant(xp, BodyAbility.LUNGS));
    BodyTrainingSystem.complete(b, result, "breath");
    if (b.breathInvalid || b.diveDamaged) b.lastSession.putString("outcome", "drowning");
    b.breathPaid = true;
  }

  private static void reset(BodyState b) {
    b.breathActive = false;
    b.breathInvalid = false;
    b.breathPaid = false;
    b.breathSpent = 0;
    b.breathTicks = 0;
  }
}
