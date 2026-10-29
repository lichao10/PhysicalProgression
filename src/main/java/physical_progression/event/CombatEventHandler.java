package physical_progression.event;

import physical_progression.arm.*;
import physical_progression.attachment.ModAttachments;
import physical_progression.config.ConditioningConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = "physical_progression")
public final class CombatEventHandler {
  @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
  public static void block(net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent e) {
    if (e.getBlocked()
        && e.getBlockedDamage() > 0
        && e.getEntity() instanceof ServerPlayer p
        && p.isUsingItem())
      ArmSystem.act(
          p,
          p.getUsedItemHand(),
          Math.min(4, e.getBlockedDamage()),
          ItemWeightManager.weight(p.getUseItem()),
          true);
  }

  @SubscribeEvent
  public static void damage(LivingDamageEvent.Post e) {
    if (e.getNewDamage() > 0
        && e.getSource().getDirectEntity() instanceof ServerPlayer p
        && e.getSource().getEntity() == p) {
      var d = p.getData(ModAttachments.PLAYER);
      // Sweep attacks hit several targets, but represent one physical swing.
      if (p.tickCount - d.lastAttackTick >= 5) {
        d.lastAttackTick = p.tickCount;
        ArmSystem.act(
            p,
            InteractionHand.MAIN_HAND,
            ConditioningConfig.ATTACK_COST.get(),
            ItemWeightManager.weight(p.getMainHandItem()),
            true);
      }
    }
    if (e.getEntity() instanceof ServerPlayer p) {
      if (e.getSource().is(net.minecraft.world.damagesource.DamageTypes.DROWN)) {
        p.getData(ModAttachments.PLAYER).body.diveDamaged = true;
        p.getData(ModAttachments.PLAYER).body.breathInvalid = true;
        p.getData(ModAttachments.PLAYER).body.pending.remove(physical_progression.body.BodyAbility.LUNGS);
      }
      if (e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION))
        physical_progression.stamina.MovementTracker.suppress(p, 30);
    }
  }
}
