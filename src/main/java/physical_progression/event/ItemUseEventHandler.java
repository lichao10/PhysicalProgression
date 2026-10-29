package physical_progression.event;

import physical_progression.arm.*;
import physical_progression.config.ConditioningConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@EventBusSubscriber(modid = "physical_progression")
public final class ItemUseEventHandler {
  @SubscribeEvent
  public static void use(LivingEntityUseItemEvent.Tick e) {
    if (!(e.getEntity() instanceof ServerPlayer p)) return;
    UseAnim animation = e.getItem().getUseAnimation();
    if (animation == UseAnim.BOW || animation == UseAnim.CROSSBOW || animation == UseAnim.BLOCK) {
      // Drawing trains only during the draw phase; holding forever cannot farm growth.
      boolean train = animation != UseAnim.BLOCK && p.getTicksUsingItem() < 20;
      ArmSystem.act(
          p,
          e.getHand(),
          ConditioningConfig.HOLD_COST.get() / 20,
          ItemWeightManager.weight(e.getItem()),
          train);
    }
  }
}
