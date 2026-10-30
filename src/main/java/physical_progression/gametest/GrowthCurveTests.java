package physical_progression.gametest;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import physical_progression.arm.ArmSystem;
import physical_progression.attachment.*;
import physical_progression.body.*;
import physical_progression.stamina.*;

/** Controlled tick replay, not a claim of an hour of human gameplay. */
public final class GrowthCurveTests {
  private static final class Player extends ServerPlayer {
    boolean water;

    Player(GameTestHelper h) {
      super(
          h.getLevel().getServer(),
          h.getLevel(),
          new GameProfile(UUID.randomUUID(), "growth-replay"),
          ClientInformation.createDefault());
      TestPlayers.connect(this);
      setGameMode(GameType.SURVIVAL);
      setPos(h.absolutePos(new BlockPos(1, 2, 1)).getCenter());
      setOnGround(true);
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

  @ConditioningGameTest(template = "empty", timeoutTicks = 400)
  public static void growthCurves(GameTestHelper h) throws java.io.IOException {
    var csv = new ArrayList<String>();
    csv.add(
        "activity,minutes,cardio,lungs,leg_strength,leg_endurance,climbing,right_strength,stamina_max,leg_max,right_arm_max,breath_seconds,climb_blocks_per_second");
    for (String activity :
        new String[] {"walking", "running", "tools", "swimming", "breathing", "climbing"}) {
      var p = new Player(h);
      var d = p.getData(ModAttachments.PLAYER);
      double x = p.getX(), y = p.getY();
      boolean resting = false;
      if (activity.equals("climbing")) {
        var base = p.blockPosition();
        for (int j = 0; j < 5; j++)
          p.level().setBlockAndUpdate(base.east().above(j), Blocks.STONE.defaultBlockState());
        p.setYRot(-90);
        p.setPos(base.getX() + 0.65, y, p.getZ());
      }
      p.setAirSupply(p.getMaxAirSupply());
      StaminaSystem.tick(p, d);
      for (int t = 1; t <= 72000; t++) {
        p.tickCount++;
        double fraction =
            Math.min(
                DerivedPerformance.fraction(d.stamina), DerivedPerformance.fraction(d.body.legs));
        if (activity.equals("tools")) fraction = DerivedPerformance.fraction(d.right.endurance);
        if (activity.equals("climbing") || activity.equals("swimming"))
          fraction =
              Math.min(
                  fraction,
                  Math.min(
                      DerivedPerformance.fraction(d.left.endurance),
                      DerivedPerformance.fraction(d.right.endurance)));
        if (fraction < 0.25) resting = true;
        if (fraction >= 0.95) resting = false;
        ClimbingSystem.request(p, false);
        p.water = false;
        p.setSprinting(false);
        switch (activity) {
          case "walking" -> p.setPos(p.getX() + (t % 20 < 10 ? 0.215 : -0.215), y, p.getZ());
          case "running" -> {
            if (!resting) {
              p.setSprinting(true);
              p.setPos(p.getX() + (t % 20 < 10 ? 0.28 : -0.28), y, p.getZ());
            }
          }
          case "tools" -> {
            if (!resting && t % 20 == 0)
              ArmSystem.act(p, InteractionHand.MAIN_HAND, 1.5, 2.5, true);
          }
          case "swimming" -> {
            p.water = !resting;
            if (p.water) {
              MovementTracker.requestInput(p, true);
              p.setPos(p.getX() + (t % 20 < 10 ? 0.14 : -0.14), y, p.getZ());
            }
            p.setAirSupply(p.getMaxAirSupply());
          }
          case "breathing" -> {
            boolean underwater =
                !d.body.breathPaid && p.getAirSupply() > p.getMaxAirSupply() * 0.2
                    || !d.body.breathActive;
            p.water = underwater;
            p.setAirSupply(
                underwater
                    ? Math.max(0, p.getAirSupply() - 1)
                    : Math.min(p.getMaxAirSupply(), p.getAirSupply() + 4));
          }
          case "climbing" -> {
            if (!resting) {
              ClimbingSystem.request(p, true);
              ClimbingSystem.tick(p);
              double speed = ClimbingSystem.speed(d.body.value(BodyAbility.CLIMBING));
              p.setPos(p.getX(), p.getY() + (t % 8 < 4 ? speed : -speed), p.getZ());
            } else p.setPos(p.getX(), y, p.getZ());
          }
        }
        StaminaSystem.tick(p, d);
        ArmSystem.tick(p);
        h.assertTrue(
            Double.isFinite(d.stamina.current)
                && d.stamina.current >= 0
                && d.stamina.current <= d.stamina.maximum + 1e-6,
            net.minecraft.network.chat.Component.literal(
                "resources stay finite and bounded during long replay"));
        if (t == 18000 || t == 36000 || t == 72000) {
          var b = d.body;
          csv.add(
              String.format(
                  Locale.ROOT,
                  "%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                  activity,
                  t / 1200,
                  b.value(BodyAbility.CARDIO),
                  b.value(BodyAbility.LUNGS),
                  b.value(BodyAbility.LEG_STRENGTH),
                  b.value(BodyAbility.LEG_ENDURANCE),
                  b.value(BodyAbility.CLIMBING),
                  d.right.strength,
                  d.stamina.maximum,
                  b.legs.maximum,
                  d.right.endurance.maximum,
                  p.getMaxAirSupply() / 20.0,
                  ClimbingSystem.speed(b.value(BodyAbility.CLIMBING)) * 20));
        }
      }
    }
    var out = java.nio.file.Path.of("../build/reports/growth-curves.csv");
    java.nio.file.Files.createDirectories(out.getParent());
    java.nio.file.Files.write(out, csv, java.nio.charset.StandardCharsets.UTF_8);
    com.mojang.logging.LogUtils.getLogger()
        .info("GROWTH_REPLAY wrote {} controlled 15/30/60-minute profiles", csv.size() - 1);
    h.succeed();
  }
}
