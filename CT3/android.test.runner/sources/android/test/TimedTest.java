package android.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface TimedTest {
    boolean includeDetailedStats() default false;
}
