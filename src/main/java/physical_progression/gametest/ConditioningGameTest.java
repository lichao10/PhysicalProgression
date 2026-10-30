package physical_progression.gametest;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConditioningGameTest {
  String template() default "empty";

  int timeoutTicks() default 100;
}
