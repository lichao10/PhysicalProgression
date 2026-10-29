package physical_progression.stamina;

import physical_progression.attachment.PlayerConditioningData;
import physical_progression.body.*;
import physical_progression.config.ConditioningConfig;
import net.minecraft.server.level.ServerPlayer;

public final class StaminaSystem {
  public static void tick(ServerPlayer p, PlayerConditioningData d) {
    DerivedPerformance.refresh(d);
    var m = MovementTracker.sample(p, d);
    var c = d.stamina;
    var b = d.body;
    if (p.tickCount % 20 == 0) DerivedPerformance.equipment(p, b);
    if (b.legActive > 0) b.legActive--;
    if (b.leftActive > 0) b.leftActive--;
    if (b.rightActive > 0) b.rightActive--;
    boolean sprint = p.isSprinting() && c.current > 0 && b.legs.current > 0;
    boolean moving = m.valid() && m.moving();
    boolean intense = b.climbEngaged || moving && (sprint || p.isInWater() || p.onClimbable());
    b.activity = b.climbEngaged ? (moving ? "climbing" : "hanging") : "rest";
    if (b.climbEngaged) {
      c.recoveryTicks = 0;
      b.legs.recoveryTicks = 0;
    }
    if (moving) {
      double distance =
          (b.climbEngaged || p.onClimbable() || p.isInWater())
              ? Math.hypot(m.distance(), m.vertical())
              : m.distance();
      double base =
          p.isInWater()
              ? ConditioningConfig.SWIM_COST.get()
              : b.climbEngaged || p.onClimbable()
                  ? ConditioningConfig.CLIMB_COST.get()
                  : sprint
                      ? ConditioningConfig.SPRINT_COST.get()
                      : ConditioningConfig.WALK_COST.get();
      double used = c.spend(distance * base * DerivedPerformance.movementCost(d, sprint));
      BodyMovementSystem.move(p, d, m, sprint, used);
    }
    JumpSystem.tick(p, d, m);
    if (!MovementTracker.eligible(p) || !m.valid() || intense || sprint) c.recoveryTicks = 0;
    else if (++c.recoveryTicks >= DerivedPerformance.recoveryDelay(b)) {
      double multiplier =
          moving
              ? ConditioningConfig.WALK_RECOVERY.get()
              : (p.isCrouching() ? ConditioningConfig.CROUCH_RECOVERY.get() : 1);
      double beforeRecovery = c.current;
      c.recover(d.recoveryRate * DerivedPerformance.recoveryMultiplier(b) / 20 * multiplier);
      if (b.sessionLoad > 0) b.sessionRecovered += c.current - beforeRecovery;
    }
    boolean gentleWalk = moving && !sprint && !p.isInWater() && !p.onClimbable();
    if (!b.climbEngaged
        && (!moving || gentleWalk)
        && MovementTracker.eligible(p)
        && ++b.legs.recoveryTicks >= DerivedPerformance.recoveryDelay(b))
      b.legs.recover(
          ConditioningConfig.LEG_RECOVERY.get()
              * DerivedPerformance.recoveryMultiplier(b)
              / 20
              * (gentleWalk ? ConditioningConfig.WALK_RECOVERY.get() : 1));
    if (c.current <= 0 || b.legs.current <= 0) p.setSprinting(false);
    if (!p.isInWater() && p.getAirSupply() >= p.getMaxAirSupply() * 0.95) b.diveDamaged = false;
    BodyTrainingSystem.tick(d, moving || b.climbEngaged);
    BreathTrainingSystem.tick(p, d);
    BodyTrainingSystem.recovered(d);
    BodyPerformanceSystem.tick(p, d, moving && sprint);
  }
}
