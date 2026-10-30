package physical_progression.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemUseAnimation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import physical_progression.arm.*;
import physical_progression.config.ConditioningConfig;

@EventBusSubscriber(modid = "physical_progression")
public final class ItemUseEventHandler {
  @SubscribeEvent
  public static void use(LivingEntityUseItemEvent.Tick e) {
    if (!(e.getEntity() instanceof ServerPlayer p)) return;
    ItemUseAnimation animation = e.getItem().getUseAnimation();
    if (animation == ItemUseAnimation.BOW
        || animation == ItemUseAnimation.CROSSBOW
        || animation == ItemUseAnimation.BLOCK) {
      // Drawing trains only during the draw phase; holding forever cannot farm growth.
      boolean train = animation != ItemUseAnimation.BLOCK && p.getTicksUsingItem() < 20;
      ArmSystem.act(
          p,
          p.getUsedItemHand(),
          ConditioningConfig.HOLD_COST.get() / 20,
          ItemWeightManager.weight(e.getItem()),
          train);
    }
  }
}
