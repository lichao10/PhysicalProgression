package physical_progression.body;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import physical_progression.arm.ArmSystem;
import physical_progression.attachment.ModAttachments;
import physical_progression.config.ConditioningConfig;
import physical_progression.network.ModNetworking.ClientSnapshot;

public final class ClimbingSystem {
  private static final class Intent {
    int until, paidTick = -100;
    boolean exhausted;
  }

  private static final java.util.Map<Player, Intent> INPUT =
      java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

  public static void request(Player p, boolean held) {
    if (!held) {
      INPUT.remove(p);
      if (p instanceof ServerPlayer sp) {
        var b = sp.getData(ModAttachments.PLAYER).body;
        b.climbEngaged = false;
        b.climbExhausted = false;
      }
      return;
    }
    var input = INPUT.computeIfAbsent(p, k -> new Intent());
    input.until = p.tickCount + 15;
  }

  private static boolean contact(Player p) {
    var direction = Vec3.directionFromRotation(0, p.getYRot());
    var body =
        p.getBoundingBox()
            .deflate(0.02, 0.35, 0.02)
            .expandTowards(direction.x * 0.16, 0, direction.z * 0.16);
    return p.level().getBlockCollisions(p, body).iterator().hasNext();
  }

  private static boolean eligible(Player p) {
    return p.isAlive()
        && !p.isCreative()
        && !p.isSpectator()
        && !p.isPassenger()
        && !p.isFallFlying()
        && !p.getAbilities().flying
        && !p.isInWater();
  }

  public static boolean canClimb(Player p) {
    var input = INPUT.get(p);
    if (input == null
        || input.until < p.tickCount
        || input.exhausted
        || !eligible(p)
        || !contact(p)) return false;
    if (p instanceof ServerPlayer sp) {
      var d = sp.getData(ModAttachments.PLAYER);
      if (d.movementSuppression > 0) return false;
      if (d.body.climbEngaged && input.paidTick >= p.tickCount - 1) return true;
      double global = ConditioningConfig.CLIMB_HOLD_COST.get() / 20,
          arm = ConditioningConfig.CLIMB_ARM_HOLD_COST.get() / 20;
      return d.stamina.current > 0
          && d.body.legs.current > 0
          && d.stamina.current + 1e-8 >= global
          && d.left.endurance.current > 0
          && d.right.endurance.current > 0
          && d.left.endurance.current + 1e-8
              >= arm * DerivedPerformance.armCostMultiplier(d, d.left)
          && d.right.endurance.current + 1e-8
              >= arm * DerivedPerformance.armCostMultiplier(d, d.right);
    }
    var d = p.getData(ClientSnapshot.TYPE);
    return !d.resources.getBooleanOr("movementSuppressed", false)
        && d.ready()
        && !d.resources.getBooleanOr("climbExhausted", false)
        && d.resource("stamina") > 0
        && d.resource("legs") > 0
        && d.resource("left") > 0
        && d.resource("right") > 0;
  }

  public static double speed(double ability) {
    return Math.min(
        ConditioningConfig.CLIMB_MAX_SPEED.get(),
        ConditioningConfig.CLIMB_SPEED.get()
            + Math.max(0, ability - 1) * ConditioningConfig.CLIMB_SPEED_GAIN.get());
  }

  public static void tick(Player p) {
    var input = INPUT.get(p);
    if (p instanceof ServerPlayer sp) {
      var d = sp.getData(ModAttachments.PLAYER);
      d.body.climbEngaged = false;
      if (!canClimb(p)) {
        if (input != null
            && input.until >= p.tickCount
            && eligible(p)
            && contact(p)
            && d.movementSuppression == 0) input.exhausted = true;
        d.body.climbExhausted = input != null && input.exhausted;
        return;
      }
      d.body.climbEngaged = true;
      d.body.climbExhausted = false;
      if (input.paidTick != p.tickCount) {
        double spent = d.stamina.spend(ConditioningConfig.CLIMB_HOLD_COST.get() / 20);
        input.paidTick = p.tickCount;
        ArmSystem.act(
            sp,
            InteractionHand.MAIN_HAND,
            ConditioningConfig.CLIMB_ARM_HOLD_COST.get() / 20,
            1,
            true);
        ArmSystem.act(
            sp,
            InteractionHand.OFF_HAND,
            ConditioningConfig.CLIMB_ARM_HOLD_COST.get() / 20,
            1,
            true);
        BodyTrainingSystem.record(d, "climbing", spent, 0, 16);
        d.body.add(BodyAbility.GRIP, spent * 0.4);
        d.body.add(BodyAbility.CORE, spent * 0.1);
        d.body.add(BodyAbility.CLIMBING, spent * 0.2);
      }
    } else if (!canClimb(p)) return;
    double ability =
        p instanceof ServerPlayer sp
            ? sp.getData(ModAttachments.PLAYER).body.value(BodyAbility.CLIMBING)
            : p.getData(ClientSnapshot.TYPE).attribute("climbing");
    var velocity = p.getDeltaMovement();
    p.setDeltaMovement(velocity.x, speed(ability), velocity.z);
    p.resetFallDistance();
  }
}
