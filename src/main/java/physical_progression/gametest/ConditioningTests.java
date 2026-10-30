package physical_progression.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import physical_progression.arm.*;
import physical_progression.attachment.*;
import physical_progression.stamina.*;

@GameTestHolder("physical_progression")
@PrefixGameTestTemplate(false)
public final class ConditioningTests {
  private static ServerPlayer player(GameTestHelper h) {
    var p =
        new ServerPlayer(
            h.getLevel().getServer(),
            h.getLevel(),
            new GameProfile(UUID.randomUUID(), "physical_progression-test"),
            ClientInformation.createDefault());
    p.setGameMode(GameType.SURVIVAL);
    p.setPos(h.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)).getCenter());
    return p;
  }

  private static void near(GameTestHelper h, double a, double b, String label) {
    h.assertTrue(Math.abs(a - b) < 0.0001, label + ": " + a + " expected " + b);
  }

  @GameTest(template = "empty")
  public static void distanceAndRecovery(GameTestHelper h) {
    var p = player(h);
    var d = p.getData(ModAttachments.PLAYER);
    StaminaSystem.tick(p, d);
    for (int i = 0; i < 200; i++) {
      p.setSprinting(true);
      p.setPos(p.getX() + 0.25, p.getY(), p.getZ());
      StaminaSystem.tick(p, d);
    }
    near(h, d.stamina.current, 0, "50 sprint blocks");
    h.assertFalse(p.isSprinting(), "exhaustion stops sprint");
    double walkSpeed =
        p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
    p.setSprinting(true);
    h.assertFalse(p.isSprinting(), "exhaustion rejects reactivation");
    near(
        h,
        p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED),
        walkSpeed,
        "exhaustion rejects sprint speed modifier");

    for (int i = 0; i < 49; i++) StaminaSystem.tick(p, d);
    near(h, d.stamina.current, 0, "recovery delay");
    StaminaSystem.tick(p, d);
    near(h, d.stamina.current, 0.25, "recovery starts after 50 ticks");
    var walk = player(h);
    var wd = walk.getData(ModAttachments.PLAYER);
    StaminaSystem.tick(walk, wd);
    for (int i = 0; i < 800; i++) {
      walk.setPos(walk.getX() + 0.25, walk.getY(), walk.getZ());
      StaminaSystem.tick(walk, wd);
    }
    h.assertTrue(wd.stamina.current > 40, "ordinary walking restores stamina in 1.2");
    h.assertTrue(
        wd.body.pending.getOrDefault(physical_progression.body.BodyAbility.LEG_ENDURANCE, 0.0) > 0,
        "long walking trains leg endurance in v2");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void rejectExternalMovement(GameTestHelper h) {
    var p = player(h);
    var d = p.getData(ModAttachments.PLAYER);
    StaminaSystem.tick(p, d);
    p.setSprinting(true);
    p.setPos(p.getX() + 100, p.getY(), p.getZ());
    StaminaSystem.tick(p, d);
    near(h, d.stamina.load, 0, "teleport jump");
    p.move(
        net.minecraft.world.entity.MoverType.PISTON, new net.minecraft.world.phys.Vec3(0.1, 0, 0));
    StaminaSystem.tick(p, d);
    near(h, d.stamina.load, 0, "piston");
    p.push(0.2, 0.1, 0);
    h.assertTrue(d.movementSuppression > 0, "external impulse hook");
    for (int i = 0; i < 30; i++) {
      p.setSprinting(true);
      StaminaSystem.tick(p, d);
    }
    near(h, d.stamina.load, 0, "wall sprint");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void adaptation(GameTestHelper h) {
    var d = new PlayerConditioningData();
    physical_progression.body.BodyTrainingSystem.record(d, "sprinting", 5, 5, 1);
    d.body.add(physical_progression.body.BodyAbility.CARDIO, 5);
    for (int i = 0; i < 60; i++) physical_progression.body.BodyTrainingSystem.tick(d, false);
    near(
        h,
        d.body.abilities.get(physical_progression.body.BodyAbility.CARDIO).xp,
        0,
        "tiny session");
    for (int session = 0; session < 8; session++) {
      physical_progression.body.BodyTrainingSystem.record(d, "sprinting", 40, 40, 1);
      d.body.add(physical_progression.body.BodyAbility.CARDIO, 40);
      for (int i = 0; i < 60; i++) physical_progression.body.BodyTrainingSystem.tick(d, false);
    }
    h.assertTrue(d.stamina.maximum > 50, "repeated sessions grow derived capacity");
    h.assertTrue(
        TrainingMath.quality(40.0 / 51, 0.2, 0.7) < TrainingMath.quality(40.0 / 50, 0.2, 0.7),
        "same route slightly less effective after growth");
    h.assertTrue(
        TrainingMath.quality(40.0 / 100, 0.2, 0.7) < TrainingMath.quality(40.0 / 50, 0.2, 0.7),
        "progressive overload");
    h.assertTrue(
        TrainingMath.requiredXp(100, 50, 100) > TrainingMath.requiredXp(50, 50, 100),
        "diminishing growth");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void persistence(GameTestHelper h) {
    var p = player(h);
    var d = p.getData(ModAttachments.PLAYER);
    d.stamina.maximum = 81;
    d.stamina.current = 17;
    d.stamina.xp = 42;
    d.left.strength = 1.4;
    d.right.endurance.load = 12;
    var restored = new PlayerConditioningData();
    restored.deserializeNBT(
        h.getLevel().registryAccess(), d.serializeNBT(h.getLevel().registryAccess()));
    near(h, restored.stamina.maximum, 81, "maximum persists");
    near(h, restored.stamina.current, 17, "current persists");
    near(h, restored.stamina.xp, 42, "xp persists");
    near(h, restored.left.strength, 1.4, "strength persists");
    near(h, restored.right.endurance.load, 12, "session persists");
    h.assertFalse(restored.positioned, "position resets");
    var clone = player(h);
    clone.copyAttachmentsFrom(p, true);
    near(
        h,
        clone.getData(ModAttachments.PLAYER).stamina.maximum,
        81,
        "death copy retains adaptation");
    clone.getData(ModAttachments.PLAYER).stamina.maximum = 90;
    near(h, d.stamina.maximum, 81, "clone independent");
    var snapshot = physical_progression.network.SyncConditioningDataPayload.of(d);
    var buffer =
        new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
    try {
      physical_progression.network.SyncConditioningDataPayload.STREAM_CODEC.encode(
          buffer, snapshot);
      h.assertTrue(
          snapshot.equals(
              physical_progression.network.SyncConditioningDataPayload.STREAM_CODEC.decode(buffer)),
          "network round trip");
    } finally {
      buffer.release();
    }
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void armsAndWeights(GameTestHelper h) {
    var p = player(h);
    var d = p.getData(ModAttachments.PLAYER);
    ArmSystem.act(p, InteractionHand.MAIN_HAND, 2, 2.5, true);
    near(h, d.right.endurance.current, 25, "right cost");
    near(h, d.left.endurance.current, 30, "left isolated");
    var info = ClientInformation.createDefault();
    p.updateOptions(
        new ClientInformation(
            info.language(),
            info.viewDistance(),
            info.chatVisibility(),
            info.chatColors(),
            info.modelCustomisation(),
            HumanoidArm.LEFT,
            info.textFilteringEnabled(),
            info.allowsListing(),
            info.particleStatus()));
    h.assertTrue(ArmSide.resolve(p, InteractionHand.MAIN_HAND) == ArmSide.LEFT, "left handed main");
    h.assertTrue(ArmSide.resolve(p, InteractionHand.OFF_HAND) == ArmSide.RIGHT, "left handed off");
    near(
        h,
        ItemWeightManager.weight(net.minecraft.world.item.Items.WOODEN_SWORD.getDefaultInstance()),
        0.8,
        "datamap loaded");
    var arm = d.left;
    arm.strength = 3;
    ArmSystem.act(p, InteractionHand.MAIN_HAND, 1, 3, true);
    near(h, arm.endurance.current, 29, "strength reduces cost");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void armAdaptation(GameTestHelper h) {
    var light = new PlayerConditioningData.Arm();
    var heavy = new PlayerConditioningData.Arm();
    for (int i = 0; i < 20; i++) {
      ArmTrainingSystem.record(light, 24, 0.8);
      ArmTrainingSystem.record(heavy, 24, 3);
      for (int t = 0; t < 60; t++) {
        ArmTrainingSystem.tick(light);
        ArmTrainingSystem.tick(heavy);
      }
    }
    h.assertTrue(light.endurance.maximum > 30, "light endurance growth");
    h.assertTrue(heavy.strength > 1, "heavy strength growth");
    near(h, light.strength, 1, "light strength isolated");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void miningAndCombat(GameTestHelper h) {
    var p = player(h);
    var d = p.getData(ModAttachments.PLAYER);
    var pos = h.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
    h.getLevel()
        .setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
    p.setItemInHand(
        InteractionHand.MAIN_HAND,
        net.minecraft.world.item.Items.IRON_PICKAXE.getDefaultInstance());
    p.gameMode.destroyBlock(pos);
    h.assertTrue(d.right.endurance.current < 30, "real block removal costs stamina");
    double remaining = d.right.endurance.current;
    p.gameMode.destroyBlock(pos);
    near(h, d.right.endurance.current, remaining, "air mining has no cost");
    var cow =
        h.spawn(
            net.minecraft.world.entity.EntityType.COW, new net.minecraft.core.BlockPos(1, 2, 1));
    p.setItemInHand(
        InteractionHand.MAIN_HAND, net.minecraft.world.item.Items.IRON_SWORD.getDefaultInstance());
    cow.hurt(p.damageSources().playerAttack(p), 3);
    h.assertTrue(d.right.endurance.current < remaining, "successful melee damage costs stamina");
    near(h, d.left.endurance.current, 30, "combat and mining keep opposite arm unchanged");
    h.succeed();
  }
}
