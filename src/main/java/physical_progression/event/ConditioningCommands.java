package physical_progression.event;

import physical_progression.attachment.ModAttachments;
import physical_progression.body.BodyAbility;
import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "physical_progression")
public final class ConditioningCommands {
  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("physical_progression")
                .then(
                    Commands.literal("stats")
                        .executes(
                            context -> {
                              var d =
                                  context
                                      .getSource()
                                      .getPlayerOrException()
                                      .getData(ModAttachments.PLAYER);
                              StringBuilder text =
                                  new StringBuilder(
                                      String.format(
                                          Locale.ROOT,
                                          "Stamina %.2f/%.2f | Legs %.2f/%.2f\n"
                                              + "Left %.2f/%.2f STR %.2f | Right %.2f/%.2f STR"
                                              + " %.2f\n",
                                          d.stamina.current,
                                          d.stamina.maximum,
                                          d.body.legs.current,
                                          d.body.legs.maximum,
                                          d.left.endurance.current,
                                          d.left.endurance.maximum,
                                          d.left.strength,
                                          d.right.endurance.current,
                                          d.right.endurance.maximum,
                                          d.right.strength));
                              for (var ability : BodyAbility.values()) {
                                var progress = d.body.abilities.get(ability);
                                text.append(
                                    String.format(
                                        Locale.ROOT,
                                        "%s: %.2f (XP %.2f/%.2f)\n",
                                        ability.key(),
                                        progress.value,
                                        progress.xp,
                                        progress.requiredXp()));
                              }
                              text.append(
                                  String.format(
                                      Locale.ROOT,
                                      "Session %s | %.2f cost | %.2f blocks | %d ticks",
                                      d.body.sessionActivity,
                                      d.body.sessionLoad,
                                      d.body.sessionDistance,
                                      d.body.sessionTicks));
                              context
                                  .getSource()
                                  .sendSuccess(() -> Component.literal(text.toString()), false);
                              return 1;
                            })));
  }
}
