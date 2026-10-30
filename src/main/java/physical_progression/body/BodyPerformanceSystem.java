package physical_progression.body;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import physical_progression.attachment.PlayerConditioningData;
import physical_progression.stamina.MovementTracker;

public final class BodyPerformanceSystem {
  private static final Identifier
      JUMP = Identifier.fromNamespaceAndPath("physical_progression", "jump"),
      BURST = Identifier.fromNamespaceAndPath("physical_progression", "burst");

  public static void tick(ServerPlayer p, PlayerConditioningData d, boolean movingSprint) {
    var b = d.body;
    if (b.burstCooldown > 0) b.burstCooldown--;
    if (b.burstTicks > 0) b.burstTicks--;
    if (movingSprint) {
      if (b.continuousSprint == 0
          && b.burstCooldown == 0
          && DerivedPerformance.fraction(d.stamina) > 0.25
          && DerivedPerformance.fraction(b.legs) > 0.25) {
        b.burstTicks = DerivedPerformance.burstDuration(b);
        b.burstCooldown = 160;
      }
      b.continuousSprint++;
    } else {
      b.continuousSprint = 0;
      b.burstTicks = 0;
    }
    boolean eligible = MovementTracker.eligible(p);
    modifier(
        p.getAttribute(Attributes.JUMP_STRENGTH),
        JUMP,
        eligible ? DerivedPerformance.jumpMultiplier(d) - 1 : 0);
    modifier(
        p.getAttribute(Attributes.MOVEMENT_SPEED),
        BURST,
        eligible && movingSprint && b.burstTicks > 0 ? DerivedPerformance.burstBonus(b) : 0);
  }

  private static void modifier(AttributeInstance attribute, Identifier id, double amount) {
    if (attribute == null) return;
    var old = attribute.getModifier(id);
    if (old != null && Math.abs(old.amount() - amount) < 0.0001) return;
    attribute.removeModifier(id);
    if (Math.abs(amount) > 0.0001)
      attribute.addTransientModifier(
          new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
  }
}
