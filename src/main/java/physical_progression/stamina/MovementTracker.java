package physical_progression.stamina;

import physical_progression.attachment.PlayerConditioningData;
import physical_progression.config.ConditioningConfig;
import net.minecraft.server.level.ServerPlayer;

public final class MovementTracker {
  public record Movement(double distance, double vertical, boolean valid, boolean moving) {}

  public static boolean eligible(ServerPlayer p) {
    return p.isAlive()
        && !p.isCreative()
        && !p.isSpectator()
        && !p.isPassenger()
        && !p.isFallFlying()
        && !p.getAbilities().flying;
  }

  public static void requestInput(ServerPlayer p, boolean moving) {
    var d = p.getData(physical_progression.attachment.ModAttachments.PLAYER);
    d.movementInput = moving;
    d.movementInputUntil = p.tickCount + 15;
  }

  public static boolean swimmingInput(ServerPlayer p, PlayerConditioningData d) {
    return !ConditioningConfig.SWIM_INPUT.get()
        || d.movementInput && p.tickCount <= d.movementInputUntil;
  }

  public static Movement sample(ServerPlayer p, PlayerConditioningData d) {
    double vertical = p.getY() - d.lastY;
    double distance = Math.hypot(p.getX() - d.lastX, p.getZ() - d.lastZ);
    boolean valid =
        d.positioned
            && d.dimension == p.level().dimension()
            && eligible(p)
            && d.movementSuppression == 0
            && distance <= ConditioningConfig.MAX_DISTANCE.get()
            && Math.abs(vertical) < 1.5;
    d.lastX = p.getX();
    d.lastY = p.getY();
    d.lastZ = p.getZ();
    d.dimension = p.level().dimension();
    d.positioned = true;
    if (d.movementSuppression > 0) d.movementSuppression--;
    return new Movement(
        valid ? distance : 0,
        valid ? vertical : 0,
        valid,
        (!p.isInWater() || swimmingInput(p, d))
            && (distance > 0.003
                || ((d.body.climbEngaged || p.onClimbable() || p.isInWater())
                    && Math.abs(vertical) > 0.003)));
  }

  public static void suppress(ServerPlayer p, int ticks) {
    var d = p.getData(physical_progression.attachment.ModAttachments.PLAYER);
    d.movementSuppression = Math.max(d.movementSuppression, ticks);
    d.positioned = false;
    d.body.jumping = false;
    d.body.groundKnown = false;
  }
}
