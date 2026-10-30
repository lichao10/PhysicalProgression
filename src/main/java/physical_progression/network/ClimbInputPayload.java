package physical_progression.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClimbInputPayload(boolean held, boolean moving) implements CustomPacketPayload {
  public static final Type<ClimbInputPayload> TYPE =
      new Type<>(Identifier.fromNamespaceAndPath("physical_progression", "climb_input"));
  public static final StreamCodec<RegistryFriendlyByteBuf, ClimbInputPayload> STREAM_CODEC =
      StreamCodec.of(
          (buf, p) -> {
            buf.writeBoolean(p.held());
            buf.writeBoolean(p.moving());
          },
          buf -> new ClimbInputPayload(buf.readBoolean(), buf.readBoolean()));

  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
