package physical_progression.gametest;

import static physical_progression.body.BodyAbility.*;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import physical_progression.arm.*;
import physical_progression.attachment.*;
import physical_progression.body.*;
import physical_progression.network.*;
import physical_progression.stamina.*;

public final class BodySystemTests {
  private static class TestPlayer extends ServerPlayer {
    boolean water;

    TestPlayer(GameTestHelper h) {
      super(
          h.getLevel().getServer(),
          h.getLevel(),
          new GameProfile(UUID.randomUUID(), "body-test"),
          ClientInformation.createDefault());
      TestPlayers.connect(this);
      setGameMode(GameType.SURVIVAL);
      MovementTracker.requestInput(this, true);
      setPos(h.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)).getCenter());
    }

    @Override
    public boolean isInWater() {
      return water;
    }

    @Override
    public boolean isUnderWater() {
      return water;
    }
  }

  private static void near(GameTestHelper h, double actual, double expected, String label) {
    h.assertTrue(
        Math.abs(actual - expected) < 0.00001,
        net.minecraft.network.chat.Component.literal(
            label + ": " + actual + " expected " + expected));
  }

  @ConditioningGameTest(template = "empty")
  public static void activeWallClimbing(GameTestHelper h) {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    var base = p.blockPosition();
    p.setYRot(-90);
    p.setPos(base.getX() + 0.65, p.getY(), base.getZ() + 0.5);
    for (int i = 0; i < 16; i++)
      p.level()
          .setBlockAndUpdate(
              base.east().above(i),
              net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
    h.assertTrue(
        !p.onClimbable(),
        net.minecraft.network.chat.Component.literal("wall requires active input"));
    ClimbingSystem.request(p, true);
    h.assertTrue(
        p.onClimbable(),
        net.minecraft.network.chat.Component.literal("active wall is climbable without ladder"));
    StaminaSystem.tick(p, d);
    for (int i = 0; i < 80; i++) {
      ClimbingSystem.request(p, true);
      ClimbingSystem.tick(p);
      h.assertTrue(
          p.getDeltaMovement().y > 0,
          net.minecraft.network.chat.Component.literal("climb actively produces upward motion"));
      p.move(net.minecraft.world.entity.MoverType.SELF, p.getDeltaMovement());
      StaminaSystem.tick(p, d);
    }
    h.assertTrue(
        d.body.sessionVertical > 10,
        net.minecraft.network.chat.Component.literal("active climb gains height"));
    h.assertTrue(
        d.stamina.current < 50 && d.left.endurance.current < 30 && d.right.endurance.current < 30,
        net.minecraft.network.chat.Component.literal("active climb charges resources"));
    h.assertTrue(
        d.body.pending.getOrDefault(CLIMBING, 0.0) > 0,
        net.minecraft.network.chat.Component.literal("active climb trains ability"));
    h.assertTrue(
        ClimbingSystem.speed(2) > ClimbingSystem.speed(1),
        net.minecraft.network.chat.Component.literal("ability improves speed"));
    d.left.endurance.current = 0;
    p.tickCount++;
    ClimbingSystem.tick(p);
    h.assertTrue(
        !p.onClimbable(),
        net.minecraft.network.chat.Component.literal("exhausted arm stops wall climbing"));
    d.left.endurance.current = 30;
    ClimbingSystem.request(p, false);
    h.assertTrue(
        !p.onClimbable(),
        net.minecraft.network.chat.Component.literal("releasing input stops climbing"));
    ClimbingSystem.request(p, true);
    p.tickCount += 16;
    h.assertTrue(
        !p.onClimbable(), net.minecraft.network.chat.Component.literal("stale input expires"));
    ClimbingSystem.request(p, true);
    p.setPos(p.getX() - 3, p.getY(), p.getZ());
    h.assertTrue(
        !p.onClimbable(), net.minecraft.network.chat.Component.literal("no climbing in open air"));
    h.succeed();
  }

  private static double[] trainingAtRate(GameTestHelper h, double rate) {
    physical_progression.config.ConditioningConfig.TRAINING_XP.set(rate);
    var d = new PlayerConditioningData();
    BodyTrainingSystem.record(d, "sprinting", 30, 30, 1);
    d.body.add(CARDIO, 10);
    for (int i = 0; i < physical_progression.config.ConditioningConfig.SESSION_DELAY.get(); i++)
      BodyTrainingSystem.tick(d, false);
    double bodyXp = d.body.lastSession.getDoubleOr("cardioXp", 0.0);
    near(h, d.body.abilities.get(CARDIO).xp, bodyXp, "body feedback matches awarded XP");
    var arm = new PlayerConditioningData().left;
    ArmTrainingSystem.record(arm, 12, 2);
    ArmTrainingSystem.Result result = null;
    for (int i = 0; i < physical_progression.config.ConditioningConfig.SESSION_DELAY.get(); i++)
      result = ArmTrainingSystem.tick(arm);
    var p = new TestPlayer(h);
    p.water = true;
    p.setAirSupply(300);
    var diving = p.getData(ModAttachments.PLAYER);
    BreathTrainingSystem.tick(p, diving);
    for (int i = 1; i <= 240; i++) {
      p.setAirSupply(300 - i);
      BreathTrainingSystem.tick(p, diving);
    }
    return new double[] {
      bodyXp,
      result.enduranceXp() + result.strengthXp(),
      diving.body.abilities.get(LUNGS).xp,
      d.body.recoveryCredit
    };
  }

  @ConditioningGameTest(template = "empty")
  public static void configurableTraining(GameTestHelper h) {
    var config = physical_progression.config.ConditioningConfig.TRAINING_XP;
    double original = config.get();
    try {
      var normal = trainingAtRate(h, 1);
      var doubled = trainingAtRate(h, 2);
      var disabled = trainingAtRate(h, 0);
      for (int i = 0; i < normal.length; i++) {
        h.assertTrue(
            normal[i] > 0,
            net.minecraft.network.chat.Component.literal("baseline training exists"));
        near(h, doubled[i], normal[i] * 2, "configured double reward");
        near(h, disabled[i], 0, "configured zero reward");
      }
    } finally {
      config.set(original);
    }
    var recovery = physical_progression.config.ConditioningConfig.RECOVERY_SPEED;
    double oldRecovery = recovery.get();
    try {
      recovery.set(1.5);
      near(
          h,
          DerivedPerformance.recoveryMultiplier(new BodyState()),
          1.5,
          "recovery config applies to existing body");
    } finally {
      recovery.set(oldRecovery);
    }
    var base = physical_progression.config.ConditioningConfig.CLIMB_SPEED;
    var cap = physical_progression.config.ConditioningConfig.CLIMB_MAX_SPEED;
    double oldBase = base.get(), oldCap = cap.get();
    try {
      base.set(0.1);
      cap.set(0.12);
      near(h, ClimbingSystem.speed(1), 0.1, "configured base speed");
      near(h, ClimbingSystem.speed(20), 0.12, "configured maximum speed");
    } finally {
      base.set(oldBase);
      cap.set(oldCap);
    }
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void migrateAndPersist(GameTestHelper h) {
    var d = new PlayerConditioningData();
    d.stamina.maximum = 81;
    d.stamina.current = 17;
    d.stamina.xp = 43;
    d.left.strength = 1.4;
    var legacy = d.serializeNBT(h.getLevel().registryAccess());
    legacy.remove("body");
    legacy.putInt("version", 1);
    var migrated = new PlayerConditioningData();
    migrated.deserializeNBT(h.getLevel().registryAccess(), legacy);
    DerivedPerformance.refresh(migrated);
    near(h, migrated.stamina.maximum, 81, "legacy max preserved");
    near(h, migrated.stamina.current, 17, "legacy current preserved");
    near(h, migrated.body.abilities.get(CARDIO).xp, 43, "legacy XP migrated");
    near(h, migrated.left.strength, 1.4, "arms preserved");
    migrated.body.abilities.get(LUNGS).value = 1.8;
    migrated.body.recoveryCredit = 9;
    migrated.body.add(CORE, 3);
    migrated.body.diveDamaged = true;
    var restored = new PlayerConditioningData();
    restored.deserializeNBT(
        h.getLevel().registryAccess(), migrated.serializeNBT(h.getLevel().registryAccess()));
    near(h, restored.body.value(LUNGS), 1.8, "new ability persists");
    near(h, restored.body.pending.get(CORE), 3, "pending stimulus persists");
    h.assertTrue(
        restored.body.diveDamaged,
        net.minecraft.network.chat.Component.literal("dive penalty survives relog"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void integratedTraining(GameTestHelper h) throws java.io.IOException {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    StaminaSystem.tick(p, d);
    for (int session = 0; session < 8; session++) {
      d.stamina.current = d.stamina.maximum;
      d.body.legs.current = d.body.legs.maximum;
      for (int tick = 0; tick < 160; tick++) {
        p.setSprinting(true);
        p.setPos(p.getX() + 0.25, p.getY(), p.getZ());
        StaminaSystem.tick(p, d);
      }
      if (session == 0) near(h, d.body.value(CARDIO), 1, "no immediate per-step adaptation");
      p.setSprinting(false);
      for (int tick = 0; tick < 60; tick++) StaminaSystem.tick(p, d);
    }
    h.assertTrue(
        d.body.value(CARDIO) > 1,
        net.minecraft.network.chat.Component.literal("cardio grows after sessions"));
    h.assertTrue(
        d.body.value(LEG_ENDURANCE) > 1,
        net.minecraft.network.chat.Component.literal("leg endurance grows"));
    h.assertTrue(
        d.stamina.maximum > 50,
        net.minecraft.network.chat.Component.literal("derived maximum grows"));
    h.assertTrue(
        d.body.lastSession.getDoubleOr("cardioXp", 0.0) > 0,
        net.minecraft.network.chat.Component.literal("feedback is real XP"));
    near(h, d.left.endurance.current, 30, "running leaves arms available");
    d.stamina.current = d.stamina.maximum;
    d.body.legs.current = d.body.legs.maximum;
    for (int tick = 0; tick < 80; tick++) {
      p.setSprinting(true);
      p.setPos(p.getX() + 0.25, p.getY(), p.getZ());
      StaminaSystem.tick(p, d);
    }
    p.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        net.minecraft.world.item.Items.IRON_AXE.getDefaultInstance());
    var export = new CompoundTag();
    export.put("resources", BodySnapshots.resources(p, d));
    export.put("attributes", BodySnapshots.attributes(d));
    java.nio.file.Files.createDirectories(java.nio.file.Path.of("../build/ui-qa"));
    NbtIo.writeCompressed(export, java.nio.file.Path.of("../build/ui-qa/trained-snapshot.nbt"));

    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void recoveryCredit(GameTestHelper h) {
    var d = new PlayerConditioningData();
    for (int i = 0; i < 1000; i++) BodyTrainingSystem.recovered(d);
    near(h, d.body.abilities.get(RECOVERY).xp, 0, "rest alone gives no XP");
    d.body.recoveryCredit = 10;
    d.body.trainingKinds = 3;
    d.stamina.current = 2;
    BodyTrainingSystem.recovered(d);
    near(h, d.body.recoveryCredit, 10, "must recover sufficiently");
    d.stamina.current = d.stamina.maximum;
    BodyTrainingSystem.recovered(d);
    near(h, d.body.abilities.get(RECOVERY).xp, 10, "earned credit consumed");
    for (int i = 0; i < 100; i++) BodyTrainingSystem.recovered(d);
    near(h, d.body.abilities.get(RECOVERY).xp, 10, "credit cannot be reused");
    h.succeed();
  }

  private static void jump(TestPlayer p, PlayerConditioningData d, boolean travel) {
    double y = p.getY();
    p.setOnGround(true);
    StaminaSystem.tick(p, d);
    p.setOnGround(false);
    p.setPos(p.getX() + (travel ? 0.3 : 0), y + 0.42, p.getZ());
    StaminaSystem.tick(p, d);
    p.setPos(p.getX() + (travel ? 0.3 : 0), y + 0.72, p.getZ());
    StaminaSystem.tick(p, d);
    p.setPos(p.getX() + (travel ? 0.3 : 0), y + 0.3, p.getZ());
    StaminaSystem.tick(p, d);
    p.setOnGround(true);
    p.setPos(p.getX() + (travel ? 0.3 : 0), y, p.getZ());
    StaminaSystem.tick(p, d);
  }

  @ConditioningGameTest(template = "empty")
  public static void groundedJumpValidation(GameTestHelper h) {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    jump(p, d, false);
    near(
        h,
        d.body.pending.getOrDefault(LEG_STRENGTH, 0.0),
        0,
        "standing jumps no strength stimulus");
    jump(p, d, true);
    h.assertTrue(
        d.body.pending.getOrDefault(LEG_STRENGTH, 0.0) > 0,
        net.minecraft.network.chat.Component.literal("completed moving jump stimulus"));
    h.assertTrue(
        d.body.legs.current < 80,
        net.minecraft.network.chat.Component.literal("local jump fatigue"));
    near(h, d.body.sessionDistance, 1.2, "jump distance counted once");
    near(h, d.body.sessionTicks, 4, "jump duration counts server ticks once");
    var p2 = new TestPlayer(h);
    var d2 = p2.getData(ModAttachments.PLAYER);
    p2.setOnGround(true);
    StaminaSystem.tick(p2, d2);
    p2.setOnGround(false);
    p2.setPos(p2.getX() + 0.3, p2.getY() + 0.42, p2.getZ());
    StaminaSystem.tick(p2, d2);
    MovementTracker.suppress(p2, 3);
    p2.setOnGround(true);
    StaminaSystem.tick(p2, d2);
    near(h, d2.body.pending.getOrDefault(LEG_STRENGTH, 0.0), 0, "teleport aborts jumping credit");
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void lungsAndSwimming(GameTestHelper h) {
    var p = new TestPlayer(h);
    p.water = true;
    var d = p.getData(ModAttachments.PLAYER);
    p.setAirSupply(90);
    StaminaSystem.tick(p, d);
    for (int i = 0; i < 100; i++) StaminaSystem.tick(p, d);
    near(
        h,
        d.body.pending.getOrDefault(LUNGS, 0.0),
        0,
        "unchanging air alone does not produce a breathing session");
    p.setPos(p.getX() + 0.2, p.getY(), p.getZ());
    StaminaSystem.tick(p, d);
    double low = d.body.pending.getOrDefault(LUNGS, 0.0);
    h.assertTrue(
        low > 0, net.minecraft.network.chat.Component.literal("low-air swimming stimulus"));
    d.body.diveDamaged = true;
    d.body.pending.remove(LUNGS);
    p.setAirSupply(0);
    p.setPos(p.getX() + 0.2, p.getY(), p.getZ());
    StaminaSystem.tick(p, d);
    d.body.sessionLoad = 40;
    d.body.sessionCapacity = 50;
    for (int i = 0; i < 60; i++) BodyTrainingSystem.tick(d, false);
    near(h, d.body.abilities.get(LUNGS).xp, 0, "drowning gives no lungs XP");
    d.body.abilities.get(LUNGS).value = 2;
    near(h, p.getMaxAirSupply(), 390, "lung attribute changes actual breath capacity");
    h.assertTrue(
        d.left.endurance.current < 30 && d.right.endurance.current < 30,
        net.minecraft.network.chat.Component.literal("swimming uses both arms"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void performanceAndBurst(GameTestHelper h) {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    near(h, DerivedPerformance.jumpMultiplier(d), 1, "fresh baseline jump");
    d.body.abilities.get(LEG_STRENGTH).value = 20;
    d.body.abilities.get(EXPLOSIVENESS).value = 20;
    d.body.abilities.get(MOBILITY).value = 20;
    h.assertTrue(
        DerivedPerformance.jumpMultiplier(d) <= 1.15 + 0.0001,
        net.minecraft.network.chat.Component.literal("jump gain bounded"));
    BodyPerformanceSystem.tick(p, d, true);
    h.assertTrue(
        d.body.burstTicks > 0, net.minecraft.network.chat.Component.literal("start burst"));
    double speed =
        p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
    h.assertTrue(
        speed > 0.1, net.minecraft.network.chat.Component.literal("burst modifier applied"));
    BodyPerformanceSystem.tick(p, d, false);
    BodyPerformanceSystem.tick(p, d, true);
    near(h, d.body.burstTicks, 0, "tap sprint does not reset cooldown");
    d.body.legs.current = 0;
    p.setSprinting(true);
    h.assertFalse(
        p.isSprinting(),
        net.minecraft.network.chat.Component.literal("leg exhaustion blocks sprint"));
    h.assertTrue(
        DerivedPerformance.jumpMultiplier(d) < 1.15,
        net.minecraft.network.chat.Component.literal("fatigue affects jump"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void gripAndArmFeedback(GameTestHelper h) {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    double initial = DerivedPerformance.armCostMultiplier(d, d.right);
    d.body.abilities.get(GRIP).value = 3;
    h.assertTrue(
        DerivedPerformance.armCostMultiplier(d, d.right) < initial,
        net.minecraft.network.chat.Component.literal("grip lowers cost"));
    ArmTrainingSystem.record(d.right, 24, 3);
    for (int i = 0; i < 60; i++) ArmSystem.tick(p);
    h.assertTrue(
        d.body.abilities.get(GRIP).xp > 0,
        net.minecraft.network.chat.Component.literal("qualified arm session trains grip"));
    h.assertTrue(
        d.body.lastSession.getDoubleOr("rightStrengthXp", 0.0) > 0,
        net.minecraft.network.chat.Component.literal("arm feedback uses settled XP"));
    near(h, d.body.legs.current, 80, "arm training does not drain legs");
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void snapshotContracts(GameTestHelper h) throws Exception {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    var first = BodySnapshots.attributes(d);
    d.stamina.spend(5);
    d.body.legs.spend(8);
    h.assertTrue(
        first.equals(BodySnapshots.attributes(d)),
        net.minecraft.network.chat.Component.literal(
            "resource changes do not change attribute payload"));
    var mirror = new physical_progression.client.data.ClientConditioningData();
    mirror.resources = BodySnapshots.resources(p, d);
    mirror.attributes = first;
    h.assertTrue(
        physical_progression.client.hud.HudLayout.visible(
                mirror, physical_progression.config.ConditioningClientConfig.HudMode.CONTEXTUAL)
            .contains("stamina"),
        net.minecraft.network.chat.Component.literal("contextual fatigue visible"));
    h.assertTrue(
        physical_progression.client.hud.HudLayout.visible(
                mirror, physical_progression.config.ConditioningClientConfig.HudMode.OFF)
            .isEmpty(),
        net.minecraft.network.chat.Component.literal("off hides all"));
    var buffer =
        new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
    try {
      var payload = new SyncBodyAttributesPayload(first);
      SyncBodyAttributesPayload.STREAM_CODEC.encode(buffer, payload);
      h.assertTrue(
          payload.equals(SyncBodyAttributesPayload.STREAM_CODEC.decode(buffer)),
          net.minecraft.network.chat.Component.literal("attribute codec roundtrip"));
    } finally {
      buffer.release();
    }
    // Export a real server snapshot for the opt-in client rendering test (not player-facing data).
    var export = new CompoundTag();
    export.put("resources", mirror.resources);
    export.put("attributes", mirror.attributes);
    java.nio.file.Files.createDirectories(java.nio.file.Path.of("../build/ui-qa"));
    NbtIo.writeCompressed(export, java.nio.file.Path.of("../build/ui-qa/server-snapshot.nbt"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void armSessionTiming(GameTestHelper h) {
    var arm = new PlayerConditioningData.Arm();
    ArmTrainingSystem.record(arm, 12, 3);
    for (int i = 0; i < 40; i++)
      h.assertTrue(
          ArmTrainingSystem.tick(arm) == null,
          net.minecraft.network.chat.Component.literal("session should remain open"));
    ArmTrainingSystem.record(arm, 12, 3);
    ArmTrainingSystem.Result result = null;
    for (int i = 0; i < 60; i++) result = ArmTrainingSystem.tick(arm);
    h.assertTrue(
        result != null, net.minecraft.network.chat.Component.literal("session settles after rest"));
    near(h, result.ticks(), 41, "duration measures elapsed exercise ticks, not action count");
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void walkingRecovery(GameTestHelper h) {
    var walk = new TestPlayer(h);
    var stand = new TestPlayer(h);
    var wd = walk.getData(ModAttachments.PLAYER);
    var sd = stand.getData(ModAttachments.PLAYER);
    wd.stamina.current = sd.stamina.current = 10;
    wd.body.legs.current = sd.body.legs.current = 10;
    StaminaSystem.tick(walk, wd);
    StaminaSystem.tick(stand, sd);
    for (int i = 0; i < 100; i++) {
      walk.setPos(walk.getX() + 0.2, walk.getY(), walk.getZ());
      StaminaSystem.tick(walk, wd);
      StaminaSystem.tick(stand, sd);
    }
    h.assertTrue(
        wd.stamina.current > 10,
        net.minecraft.network.chat.Component.literal("walking has net stamina recovery"));
    h.assertTrue(
        sd.stamina.current > wd.stamina.current,
        net.minecraft.network.chat.Component.literal("standing restores faster"));
    h.assertTrue(
        wd.body.legs.current > 10,
        net.minecraft.network.chat.Component.literal("walking also restores leg capacity"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void stationaryBreathTraining(GameTestHelper h) {
    var p = new TestPlayer(h);
    p.water = true;
    p.setAirSupply(300);
    var d = p.getData(ModAttachments.PLAYER);
    StaminaSystem.tick(p, d);
    for (int i = 1; i <= 240; i++) {
      p.setAirSupply(300 - i);
      StaminaSystem.tick(p, d);
    }
    h.assertTrue(
        d.body.abilities.get(LUNGS).xp > 0,
        net.minecraft.network.chat.Component.literal(
            "stationary underwater breathing trains lungs"));
    h.assertTrue(
        d.body.lastSession.getStringOr("activity", "").equals("diving"),
        net.minecraft.network.chat.Component.literal("breathing has a separate completion record"));
    double xp = d.body.abilities.get(LUNGS).xp;
    for (int i = 0; i < 100; i++) StaminaSystem.tick(p, d);
    near(h, d.body.abilities.get(LUNGS).xp, xp, "one award per breath hold");
    near(h, d.body.sessionDistance, 0, "stationary diving does not invent travel distance");
    p.water = false;
    p.setAirSupply(300);
    StaminaSystem.tick(p, d);
    h.assertFalse(
        d.body.breathActive,
        net.minecraft.network.chat.Component.literal("surface restores readiness"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void actualLadderTraining(GameTestHelper h) {
    var p = new TestPlayer(h);
    var start = p.blockPosition();
    for (int i = 0; i < 14; i++) {
      h.getLevel()
          .setBlockAndUpdate(
              start.above(i).east(),
              net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
      h.getLevel()
          .setBlockAndUpdate(
              start.above(i),
              net.minecraft.world.level.block.Blocks.LADDER
                  .defaultBlockState()
                  .setValue(
                      net.minecraft.world.level.block.LadderBlock.FACING,
                      net.minecraft.core.Direction.WEST));
    }
    h.assertTrue(
        p.onClimbable(), net.minecraft.network.chat.Component.literal("actual ladder is detected"));
    var d = p.getData(ModAttachments.PLAYER);
    StaminaSystem.tick(p, d);
    for (int i = 0; i < 100; i++) {
      p.setPos(p.getX(), p.getY() + 0.1, p.getZ());
      StaminaSystem.tick(p, d);
    }
    near(h, d.body.sessionVertical, 10, "vertical climbing distance tracked");
    near(h, d.body.sessionClimbDistance, 10, "total climb distance tracked");
    h.assertTrue(
        d.body.activity.equals("climbing"),
        net.minecraft.network.chat.Component.literal("climbing shown as activity"));
    h.assertTrue(
        d.left.endurance.current < 30 && d.right.endurance.current < 30,
        net.minecraft.network.chat.Component.literal("ladder climbing loads both arms"));
    h.assertTrue(
        d.stamina.current < 40,
        net.minecraft.network.chat.Component.literal("climbing has its own cost"));
    for (int i = 0; i < 60; i++) StaminaSystem.tick(p, d);
    h.assertTrue(
        d.body.lastSession.getDoubleOr("gripXp", 0.0) > 0,
        net.minecraft.network.chat.Component.literal("climbing settles grip stimulus"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void historyRetention(GameTestHelper h) {
    var d = new PlayerConditioningData();
    for (int i = 0; i < 25; i++) {
      var record = BodyTrainingSystem.feedback(d.body, "sprinting", 40, 160, 0.8);
      record.putDouble("cardioXp", 4);
      d.body.complete(record);
    }
    near(h, d.body.history.size(), 20, "history is bounded");
    near(h, d.body.history.getFirst().getLongOr("id", 0L), 25, "newest record first");
    near(h, d.body.history.getLast().getLongOr("id", 0L), 6, "oldest discarded");
    var copy = new PlayerConditioningData();
    copy.deserializeNBT(
        h.getLevel().registryAccess(), d.serializeNBT(h.getLevel().registryAccess()));
    near(h, copy.body.history.size(), 20, "history persists");
    copy.body.lastSession.putDouble("recoveryXp", 2);
    near(
        h,
        copy.body.history.getFirst().getDoubleOr("recoveryXp", 0.0),
        2,
        "latest recovery updates the same history record");
    near(h, d.body.lastSession.getDoubleOr("recoveryXp", 0.0), 0, "loaded history independent");
    var old = d.body.save();
    old.remove("history");
    var migrated = new BodyState();
    migrated.read(old);
    near(h, migrated.history.size(), 1, "old last session migrates without invented history");
    var synced = BodySnapshots.attributes(d);
    near(
        h,
        synced.getListOrEmpty("history").size(),
        20,
        "history included in attribute synchronization");
    h.succeed();
  }
}
