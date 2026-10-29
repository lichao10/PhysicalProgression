package physical_progression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Empty request: clients cannot supply any body values or training activity. */
public record RequestBodySnapshotPayload() implements CustomPacketPayload {
  public static final RequestBodySnapshotPayload INSTANCE = new RequestBodySnapshotPayload();
  public static final Type<RequestBodySnapshotPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("physical_progression", "request_snapshot"));
  public static final StreamCodec<RegistryFriendlyByteBuf, RequestBodySnapshotPayload>
      STREAM_CODEC = StreamCodec.unit(INSTANCE);

  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
