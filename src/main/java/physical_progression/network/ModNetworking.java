package physical_progression.network;

import physical_progression.attachment.*;
import physical_progression.client.data.ClientConditioningData;
import physical_progression.config.ConditioningConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = "physical_progression")
public final class ModNetworking {
  @SubscribeEvent
  public static void register(RegisterPayloadHandlersEvent e) {
    var registrar = e.registrar("5");
    registrar.playToClient(
        SyncConditioningDataPayload.TYPE,
        SyncConditioningDataPayload.STREAM_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () -> {
                  context.player().getData(ClientSnapshot.TYPE).resources = payload.values();
                  if (payload.stamina() <= 0 || payload.values().getDouble("legs") <= 0)
                    context.player().setSprinting(false);
                }));
    registrar.playToClient(
        SyncBodyAttributesPayload.TYPE,
        SyncBodyAttributesPayload.STREAM_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () -> context.player().getData(ClientSnapshot.TYPE).attributes = payload.values()));
    registrar.playToServer(
        ClimbInputPayload.TYPE,
        ClimbInputPayload.STREAM_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () -> {
                  physical_progression.body.ClimbingSystem.request(context.player(), payload.held());
                  if (context.player() instanceof ServerPlayer p)
                    physical_progression.stamina.MovementTracker.requestInput(p, payload.moving());
                }));
    registrar.playToServer(
        RequestBodySnapshotPayload.TYPE,
        RequestBodySnapshotPayload.STREAM_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () -> {
                  if (context.player() instanceof ServerPlayer p) {
                    var d = p.getData(ModAttachments.PLAYER);
                    if (p.tickCount - d.lastScreenRequest >= 20) {
                      d.lastScreenRequest = p.tickCount;
                      sync(p, true);
                    }
                  }
                }));
  }

  public static void sync(ServerPlayer p, boolean force) {
    var d = p.getData(ModAttachments.PLAYER);
    boolean exhausted =
        d.lastSent != null
            && ((d.stamina.current <= 0 && d.lastSent.stamina() > 0)
                || (d.body.legs.current <= 0 && d.lastSent.values().getDouble("legs") > 0)
                || (d.left.endurance.current <= 0 && d.lastSent.values().getDouble("left") > 0)
                || (d.right.endurance.current <= 0 && d.lastSent.values().getDouble("right") > 0));
    boolean completed =
        d.lastDetails == null
            || d.lastDetails.getCompound("lastSession").getLong("id") != d.body.completedId;
    if (force || exhausted || p.tickCount % ConditioningConfig.SYNC_INTERVAL.get() == 0) {
      var snapshot = new SyncConditioningDataPayload(BodySnapshots.resources(p, d));
      if (force || !snapshot.equals(d.lastSent)) {
        PacketDistributor.sendToPlayer(p, snapshot);
        d.lastSent = snapshot;
      }
    }
    if (force || completed || p.tickCount % 20 == 0) {
      var details = BodySnapshots.attributes(d);
      if (force || !details.equals(d.lastDetails)) {
        PacketDistributor.sendToPlayer(p, new SyncBodyAttributesPayload(details));
        d.lastDetails = details;
      }
    }
  }

  public static final class ClientSnapshot {
    public static final java.util.function.Supplier<
            net.neoforged.neoforge.attachment.AttachmentType<ClientConditioningData>>
        TYPE =
            ModAttachments.REGISTER.register(
                "hud_snapshot",
                () ->
                    net.neoforged.neoforge.attachment.AttachmentType.builder(
                            ClientConditioningData::new)
                        .build());

    public static void init() {}
  }
}
