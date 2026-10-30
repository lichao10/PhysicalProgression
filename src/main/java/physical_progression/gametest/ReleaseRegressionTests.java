package physical_progression.gametest;

import static physical_progression.body.BodyAbility.*;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import physical_progression.arm.*;
import physical_progression.attachment.*;
import physical_progression.body.*;
import physical_progression.network.*;
import physical_progression.stamina.*;

public final class ReleaseRegressionTests {
  private static class TestPlayer extends ServerPlayer {
    boolean water;

    TestPlayer(GameTestHelper h) {
      super(
          h.getLevel().getServer(),
          h.getLevel(),
          new GameProfile(UUID.randomUUID(), "release-test"),
          ClientInformation.createDefault());
      setGameMode(GameType.SURVIVAL);
      setPos(h.absolutePos(new BlockPos(1, 2, 1)).getCenter());
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

  private static void tick(TestPlayer p) {
    p.tickCount++;
    var d = p.getData(ModAttachments.PLAYER);
    StaminaSystem.tick(p, d);
    ArmSystem.tick(p);
  }

  private static void wall(TestPlayer p, int height) {
    var base = p.blockPosition();
    for (int i = 0; i < height; i++)
      p.level().setBlockAndUpdate(base.east().above(i), Blocks.STONE.defaultBlockState());
    p.setYRot(-90);
    p.setPos(base.getX() + 0.65, p.getY(), base.getZ() + 0.5);
  }

  @ConditioningGameTest(template = "empty")
  public static void paidCeilingHoldAndExhaustion(GameTestHelper h) {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    var base = p.blockPosition();
    wall(p, 5);
    p.setPos(p.getX(), base.getY() + 0.2, p.getZ());
    p.level().setBlockAndUpdate(base.above(2), Blocks.STONE.defaultBlockState());
    double y = p.getY();
    tick(p);
    for (int i = 0; i < 100; i++) {
      ClimbingSystem.request(p, true);
      ClimbingSystem.tick(p);
      p.move(MoverType.SELF, p.getDeltaMovement());
      tick(p);
    }
    h.assertTrue(
        Math.abs(p.getY() - y) < 1e-5,
        net.minecraft.network.chat.Component.literal("ceiling blocks displacement"));
    h.assertTrue(
        d.stamina.current < 46 && d.left.endurance.current < 24 && d.right.endurance.current < 24,
        net.minecraft.network.chat.Component.literal(
            "stationary grip pays every tick without recovering"));
    h.assertTrue(
        d.body.activity.equals("hanging"),
        net.minecraft.network.chat.Component.literal("blocked grip is visibly hanging"));
    d.left.endurance.current = 0;
    ClimbingSystem.tick(p);
    h.assertTrue(
        !p.onClimbable() && d.body.climbExhausted,
        net.minecraft.network.chat.Component.literal("exhaustion releases grip"));
    d.left.endurance.current = 30;
    d.stamina.current = 50;
    p.fallDistance = 6;
    ClimbingSystem.tick(p);
    h.assertTrue(
        p.fallDistance == 6 && !p.onClimbable(),
        net.minecraft.network.chat.Component.literal(
            "held key cannot reattach after exhaustion or cancel a fall"));
    ClimbingSystem.request(p, false);
    ClimbingSystem.request(p, true);
    h.assertTrue(
        p.onClimbable(),
        net.minecraft.network.chat.Component.literal("release key explicitly re-arms grip"));
    h.succeed();
  }

  private static double walk(GameTestHelper h, int ticks) {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    tick(p);
    for (int i = 0; i < ticks; i++) {
      p.setPos(p.getX() + 0.215, p.getY(), p.getZ());
      tick(p);
    }
    for (int i = 0; i < 60; i++) tick(p);
    double xp = 0;
    for (var entry : d.body.history) xp += entry.getDoubleOr("cardioXp", 0.0);
    com.mojang.logging.LogUtils.getLogger()
        .info(
            "RELEASE_REGRESSION walk ticks={} cardioXp={} records={}",
            ticks,
            xp,
            d.body.history.size());
    return xp;
  }

  @ConditioningGameTest(template = "empty")
  public static void longerWalkKeepsCompletedRewards(GameTestHelper h) {
    double shortXp = walk(h, 700), longXp = walk(h, 3600);
    h.assertTrue(
        shortXp > 0 && longXp > shortXp * 3,
        net.minecraft.network.chat.Component.literal(
            "long walking retains earlier rewards and continues improving"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void oneClimbOneDetailedRecord(GameTestHelper h) {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    wall(p, 25);
    tick(p);
    for (int i = 0; i < 100; i++) {
      ClimbingSystem.request(p, true);
      ClimbingSystem.tick(p);
      p.move(MoverType.SELF, p.getDeltaMovement());
      tick(p);
    }
    ClimbingSystem.request(p, false);
    p.setDeltaMovement(0, 0, 0);
    for (int i = 0; i < 60; i++) tick(p);
    var last = d.body.lastSession;
    h.assertTrue(
        d.body.history.size() == 1 && last.getStringOr("activity", "").equals("climbing"),
        net.minecraft.network.chat.Component.literal("one coordinated climb creates one record"));
    h.assertTrue(
        last.getDoubleOr("climbingXp", 0.0) > 0
            && last.contains("leftCost")
            && last.contains("rightCost"),
        net.minecraft.network.chat.Component.literal("record contains body and both arm details"));
    h.assertTrue(
        last.contains("outcome") && last.contains("reason") && last.contains("finishedAt"),
        net.minecraft.network.chat.Component.literal("record explains outcome and completion"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void stationaryDiveAndToolsHaveLiveFeedback(GameTestHelper h) {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    p.water = true;
    p.setAirSupply(300);
    tick(p);
    for (int i = 1; i <= 150; i++) {
      p.setAirSupply(300 - i);
      tick(p);
    }
    var snapshot = BodySnapshots.resources(p, d);
    h.assertTrue(
        snapshot.getBooleanOr("lungTraining", false)
            && snapshot.getStringOr("sessionActivity", "").equals("diving")
            && snapshot.getIntOr("sessionTicks", 0) >= 150
            && Math.abs(snapshot.getDoubleOr("intensity", 0.0) - 0.5) < 1e-5,
        net.minecraft.network.chat.Component.literal(
            "stationary breathing reports real time and intensity"));
    p.water = false;
    d.body.breathActive = false;
    ArmSystem.act(p, net.minecraft.world.InteractionHand.MAIN_HAND, 2, 2, true);
    tick(p);
    snapshot = BodySnapshots.resources(p, d);
    h.assertTrue(
        snapshot.getStringOr("sessionActivity", "").equals("right_arm")
            && snapshot.getDoubleOr("sessionCost", 0.0) > 0,
        net.minecraft.network.chat.Component.literal("tool-only activity is shown live"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void passiveWaterDriftDoesNotTrainMovement(GameTestHelper h) {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    p.water = true;
    p.setAirSupply(300);
    MovementTracker.requestInput(p, false);
    tick(p);
    for (int i = 0; i < 100; i++) {
      p.setPos(p.getX() + 0.1, p.getY(), p.getZ());
      tick(p);
    }
    h.assertTrue(
        d.body.sessionLoad == 0 && d.body.pending.getOrDefault(CARDIO, 0.0) == 0,
        net.minecraft.network.chat.Component.literal(
            "water displacement without input produces no locomotion training"));
    for (int i = 0; i < 50; i++) {
      MovementTracker.requestInput(p, true);
      p.setPos(p.getX() + 0.2, p.getY(), p.getZ());
      tick(p);
    }
    h.assertTrue(
        d.body.sessionLoad > 0 && d.body.pending.getOrDefault(CARDIO, 0.0) > 0,
        net.minecraft.network.chat.Component.literal("active swimming produces real training"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void cancelledBreathCannotPayAfterReplenishing(GameTestHelper h) {
    var p = new TestPlayer(h);
    var d = p.getData(ModAttachments.PLAYER);
    p.water = true;
    p.setAirSupply(300);
    tick(p);
    for (int i = 1; i <= 150; i++) {
      p.setAirSupply(300 - i);
      tick(p);
    }
    d.body.diveDamaged = d.body.breathInvalid = true;
    p.water = false;
    p.setAirSupply(300);
    tick(p);
    h.assertTrue(
        d.body.abilities.get(LUNGS).xp == 0
            && d.body.lastSession.getStringOr("outcome", "").equals("drowning"),
        net.minecraft.network.chat.Component.literal(
            "cancelled breath never awards XP after surface reset"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void diagonalClimbAndInputCodec(GameTestHelper h) {
    var p = new TestPlayer(h);
    wall(p, 5);
    p.setYRot(-45);
    ClimbingSystem.request(p, true);
    h.assertTrue(
        p.onClimbable(),
        net.minecraft.network.chat.Component.literal("diagonal facing can grip the wall"));
    var buf =
        new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
    try {
      var input = new ClimbInputPayload(true, false);
      ClimbInputPayload.STREAM_CODEC.encode(buf, input);
      h.assertTrue(
          ClimbInputPayload.STREAM_CODEC.decode(buf).equals(input),
          net.minecraft.network.chat.Component.literal(
              "intent codec preserves independent jump and movement input"));
    } finally {
      buf.release();
    }
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void visibleFirstAdaptationAndV3Migration(GameTestHelper h) {
    var d = new PlayerConditioningData();
    d.body.abilities.get(LUNGS).grant(100, LUNGS);
    d.body.abilities.get(CLIMBING).grant(100, CLIMBING);
    h.assertTrue(
        DerivedPerformance.breathBonus(d.body) >= 8,
        net.minecraft.network.chat.Component.literal(
            "first lung adaptation adds a visible air duration"));
    h.assertTrue(
        ClimbingSystem.speed(d.body.value(CLIMBING)) >= ClimbingSystem.speed(1) * 1.024,
        net.minecraft.network.chat.Component.literal("first climbing adaptation is measurable"));
    var tag = d.serializeNBT(h.getLevel().registryAccess());
    tag.putInt("version", 3);
    var body = tag.getCompoundOrEmpty("body");
    body.remove("sessionPeakFatigue");
    body.remove("sessionRecovered");
    body.remove("breathInvalid");
    var copy = new PlayerConditioningData();
    copy.deserializeNBT(h.getLevel().registryAccess(), tag);
    h.assertTrue(
        copy.body.value(LUNGS) == d.body.value(LUNGS) && copy.body.sessionPeakFatigue == 0,
        net.minecraft.network.chat.Component.literal(
            "version 3 save retains adaptations and supplies new defaults"));
    h.succeed();
  }

  @ConditioningGameTest(template = "empty")
  public static void recoveryFeedbackUsesValidTraining(GameTestHelper h) {
    var d = new PlayerConditioningData();
    var valid = BodyTrainingSystem.feedback(d.body, "sprinting", 40, 160, 0.8);
    valid.putDouble("cardioXp", 10);
    d.body.complete(valid);
    var light = BodyTrainingSystem.feedback(d.body, "walking", 1, 10, 0.02);
    light.putString("outcome", "too_light");
    d.body.complete(light);
    d.body.recoveryCredit = 10;
    d.body.trainingKinds = 3;
    BodyTrainingSystem.recovered(d);
    h.assertTrue(
        d.body.lastSession.getDoubleOr("recoveryXp", 0.0) == 0
            && d.body.lastSession.getStringOr("outcome", "").equals("too_light"),
        net.minecraft.network.chat.Component.literal(
            "low-load activity does not claim earlier recovery reward"));
    h.assertTrue(
        d.body.history.getLast().getDoubleOr("recoveryXp", 0.0) == 10,
        net.minecraft.network.chat.Component.literal(
            "cumulative recovery belongs to most recent valid training"));
    h.succeed();
  }
}
