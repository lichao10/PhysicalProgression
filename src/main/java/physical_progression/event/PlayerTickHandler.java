package physical_progression.event;

import physical_progression.attachment.ModAttachments;
import physical_progression.stamina.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "physical_progression")
public final class PlayerTickHandler {
  @SubscribeEvent
  public static void climb(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e) {
    if (e.getEntity() instanceof ServerPlayer p) physical_progression.body.ClimbingSystem.tick(p);
  }

  @SubscribeEvent
  public static void tick(PlayerTickEvent.Post e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      StaminaSystem.tick(p, p.getData(ModAttachments.PLAYER));
      physical_progression.arm.ArmSystem.tick(p);
      physical_progression.network.ModNetworking.sync(p, false);
    }
  }

  @SubscribeEvent
  public static void login(
      net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) physical_progression.network.ModNetworking.sync(p, true);
  }

  @SubscribeEvent
  public static void respawn(
      net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) physical_progression.network.ModNetworking.sync(p, true);
  }

  @SubscribeEvent
  public static void dimension(
      net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      MovementTracker.suppress(p, 3);
      physical_progression.network.ModNetworking.sync(p, true);
    }
  }

  @SubscribeEvent
  public static void teleport(EntityTeleportEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) MovementTracker.suppress(p, 3);
  }

  @SubscribeEvent
  public static void knockback(LivingKnockBackEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      MovementTracker.suppress(p, 20);
      var b = p.getData(ModAttachments.PLAYER).body;
      e.setStrength(
          (float)
              (e.getStrength()
                  / (1 + Math.min(0.2, 0.08 * (b.value(physical_progression.body.BodyAbility.CORE) - 1)))));
    }
  }
}
