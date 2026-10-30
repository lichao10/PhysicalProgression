package physical_progression.gametest;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Developer-only registration; all test classes are excluded from the player JAR. */
@EventBusSubscriber(modid = "physical_progression")
public final class ConditioningGameTests {
  private static final Class<?>[] SUITES = {
    ConditioningTests.class,
    BodySystemTests.class,
    ReleaseRegressionTests.class,
    GrowthCurveTests.class
  };

  private static ResourceLocation id(Method method) {
    return ResourceLocation.fromNamespaceAndPath(
        "physical_progression",
        method.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT)
            + "/"
            + method.getName().toLowerCase(Locale.ROOT));
  }

  private static List<Method> methods() {
    var methods = new ArrayList<Method>();
    for (var suite : SUITES)
      for (var method : suite.getDeclaredMethods())
        if (method.isAnnotationPresent(ConditioningGameTest.class)) methods.add(method);
    methods.sort(Comparator.comparing(m -> id(m).toString()));
    return methods;
  }

  @SubscribeEvent
  public static void functions(RegisterEvent event) {
    event.register(
        Registries.TEST_FUNCTION,
        helper -> {
          for (var method : methods())
            helper.register(
                id(method),
                test -> {
                  try {
                    method.invoke(null, test);
                  } catch (InvocationTargetException error) {
                    if (error.getCause() instanceof RuntimeException cause) throw cause;
                    if (error.getCause() instanceof Error cause) throw cause;
                    throw new RuntimeException(error.getCause());
                  } catch (ReflectiveOperationException error) {
                    throw new RuntimeException(error);
                  }
                });
        });
  }

  @SubscribeEvent
  public static void instances(RegisterGameTestsEvent event) {
    var environment =
        event.registerEnvironment(
            ResourceLocation.fromNamespaceAndPath("physical_progression", "default"));
    for (var method : methods()) {
      var annotation = method.getAnnotation(ConditioningGameTest.class);
      var data =
          new TestData<>(
              environment,
              ResourceLocation.fromNamespaceAndPath("physical_progression", annotation.template()),
              annotation.timeoutTicks(),
              0,
              true);
      event.registerTest(
          id(method),
          new FunctionGameTestInstance(
              ResourceKey.create(Registries.TEST_FUNCTION, id(method)), data));
    }
  }
}
