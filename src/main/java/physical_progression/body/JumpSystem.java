package physical_progression.body;

import static physical_progression.body.BodyAbility.*;

import physical_progression.attachment.PlayerConditioningData;
import physical_progression.config.ConditioningConfig;
import physical_progression.stamina.MovementTracker.Movement;
import net.minecraft.server.level.ServerPlayer;

public final class JumpSystem {
  public static void tick(ServerPlayer p, PlayerConditioningData d, Movement movement) {
    var b = d.body;
    boolean ground = p.onGround();
    if (!movement.valid() || p.isInWater() || p.onClimbable()) {
      b.jumping = false;
      b.groundKnown = false;
      return;
    }
    if (b.groundKnown && b.wasGrounded && !ground && movement.vertical() > 0.05) {
      b.jumping = true;
      b.takeoffY = p.getY() - movement.vertical();
      b.peakY = p.getY();
      b.jumpDistance = 0;
      b.jumpTicks = 0;
      b.jumpCost =
          d.stamina.spend(
              ConditioningConfig.JUMP_COST.get()
                  * DerivedPerformance.loadPenalty(b)
                  / (1 + 0.05 * (b.value(MOBILITY) - 1)));
      b.legs.spend(b.jumpCost * 1.5);
      b.legs.recoveryTicks = 0;
      d.stamina.recoveryTicks = 0;
      b.legActive = 40;
    }
    if (b.jumping) {
      b.peakY = Math.max(b.peakY, p.getY());
      b.jumpDistance += movement.distance();
      b.jumpTicks++;
      if (ground) {
        if (b.peakY - b.takeoffY >= 0.35
            && b.jumpDistance >= 0.75
            && b.jumpTicks >= 3
            && b.jumpTicks <= 80) {
          // Movement already recorded this jump's horizontal distance and tick.
          BodyTrainingSystem.record(d, "jumping", b.jumpCost, 0, 2);
          b.add(LEG_STRENGTH, b.jumpCost * 0.6);
          b.add(EXPLOSIVENESS, b.jumpCost * 0.5);
          b.add(MOBILITY, b.jumpCost * 0.4);
          b.add(CORE, b.jumpCost * 0.2);
        }
        b.jumping = false;
      }
      if (b.jumpTicks > 80) b.jumping = false;
    }
    b.wasGrounded = ground;
    b.groundKnown = true;
  }
}
