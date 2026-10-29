package physical_progression.body;

import static physical_progression.body.BodyAbility.*;

import physical_progression.arm.*;
import physical_progression.attachment.PlayerConditioningData;
import physical_progression.config.ConditioningConfig;
import physical_progression.stamina.MovementTracker.Movement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public final class BodyMovementSystem {
  public static void move(
      ServerPlayer p, PlayerConditioningData d, Movement m, boolean sprint, double spent) {
    var b = d.body;
    boolean swim = p.isInWater(), climb = d.body.climbEngaged || p.onClimbable();
    b.activity = swim ? "swimming" : climb ? "climbing" : sprint ? "sprinting" : "walking";
    b.legs.spend(spent * ConditioningConfig.LEG_COST.get());
    if (sprint || swim || climb) b.legs.recoveryTicks = 0;
    b.legActive = 40;
    BodyTrainingSystem.record(d, b.activity, spent, m.distance(), swim ? 4 : climb ? 16 : 1);
    b.add(CARDIO, spent * (sprint || swim ? 0.65 : 0.25));
    b.add(LEG_ENDURANCE, spent * (sprint ? 0.55 : 0.7));
    b.add(LUNGS, spent * 0.06);
    if (sprint) {
      b.add(LEG_STRENGTH, spent * 0.08);
      if (b.continuousSprint <= DerivedPerformance.burstDuration(b))
        b.add(EXPLOSIVENESS, spent * 0.3);
    }
    if (DerivedPerformance.loadPenalty(b) > 1.02) {
      b.add(CORE, spent * 0.25);
      b.add(LEG_STRENGTH, spent * 0.2);
    }
    if (swim) {
      b.add(CORE, spent * 0.35);
      b.add(MOBILITY, spent * 0.12);
      double air = p.getAirSupply() / (double) p.getMaxAirSupply();
      if (!b.diveDamaged
          && air >= 0.15
          && air <= 0.45
          && !p.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING))
        b.add(LUNGS, spent * 0.5);
      ArmSystem.act(p, InteractionHand.MAIN_HAND, spent * 0.08, 1, true);
      ArmSystem.act(p, InteractionHand.OFF_HAND, spent * 0.08, 1, true);
    }
    if (climb) {
      b.sessionVertical += Math.abs(m.vertical());
      b.sessionClimbDistance += Math.hypot(m.distance(), m.vertical());
      ArmSystem.act(p, InteractionHand.MAIN_HAND, spent * 0.2, 1, true);
      ArmSystem.act(p, InteractionHand.OFF_HAND, spent * 0.2, 1, true);
      b.add(CLIMBING, spent * 0.5);
      b.add(GRIP, spent * 0.4);
      b.add(CORE, spent * 0.3);
      b.add(MOBILITY, spent * 0.25);
    }
  }
}
