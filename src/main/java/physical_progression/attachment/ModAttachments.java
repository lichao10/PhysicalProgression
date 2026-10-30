package physical_progression.attachment;

import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;
import physical_progression.Conditioning;

public final class ModAttachments {
  public static final DeferredRegister<AttachmentType<?>> REGISTER =
      DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Conditioning.MOD_ID);
  public static final Supplier<AttachmentType<PlayerConditioningData>> PLAYER =
      REGISTER.register(
          "body",
          () -> AttachmentType.serializable(PlayerConditioningData::new).copyOnDeath().build());
}
