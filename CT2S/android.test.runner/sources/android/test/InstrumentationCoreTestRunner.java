package android.test;

import android.os.Bundle;
import android.test.suitebuilder.TestMethod;
import android.test.suitebuilder.annotation.HasAnnotation;
import android.util.Log;
import com.android.internal.util.Predicate;
import com.android.internal.util.Predicates;
import dalvik.annotation.BrokenTest;
import dalvik.annotation.SideEffect;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestListener;

public class InstrumentationCoreTestRunner extends InstrumentationTestRunner {
    private static final String TAG = "InstrumentationCoreTestRunner";
    private boolean singleTest = false;

    @Override
    public void onCreate(Bundle arguments) {
        File cacheDir = getTargetContext().getCacheDir();
        System.setProperty("user.language", "en");
        System.setProperty("user.region", "US");
        System.setProperty("java.home", cacheDir.getAbsolutePath());
        System.setProperty("user.home", cacheDir.getAbsolutePath());
        System.setProperty("java.io.tmpdir", cacheDir.getAbsolutePath());
        if (arguments != null) {
            String classArg = arguments.getString("class");
            this.singleTest = classArg != null && classArg.contains("#");
        }
        super.onCreate(arguments);
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        AndroidTestRunner runner = super.getAndroidTestRunner();
        runner.addTestListener(new TestListener() {
            private static final int MINIMUM_TIME = 100;
            private Class<?> lastClass;
            private long startTime;

            public void startTest(Test test) {
                if (test.getClass() != this.lastClass) {
                    this.lastClass = test.getClass();
                    printMemory(test.getClass());
                }
                Thread.currentThread().setContextClassLoader(test.getClass().getClassLoader());
                this.startTime = System.currentTimeMillis();
            }

            public void endTest(Test test) {
                if (test instanceof junit.framework.TestCase) {
                    cleanup((junit.framework.TestCase) test);
                    long timeTaken = System.currentTimeMillis() - this.startTime;
                    if (timeTaken < 100) {
                        try {
                            Thread.sleep(100 - timeTaken);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }

            public void addError(Test test, Throwable t) {
            }

            public void addFailure(Test test, junit.framework.AssertionFailedError t) {
            }

            private void printMemory(Class<? extends Test> testClass) {
                Runtime runtime = Runtime.getRuntime();
                long total = runtime.totalMemory();
                long free = runtime.freeMemory();
                long used = total - free;
                Log.d(InstrumentationCoreTestRunner.TAG, "Total memory  : " + total);
                Log.d(InstrumentationCoreTestRunner.TAG, "Used memory   : " + used);
                Log.d(InstrumentationCoreTestRunner.TAG, "Free memory   : " + free);
                Log.d(InstrumentationCoreTestRunner.TAG, "Now executing : " + testClass.getName());
            }

            private void cleanup(junit.framework.TestCase test) {
                for (Class<?> clazz = test.getClass(); clazz != junit.framework.TestCase.class; clazz = clazz.getSuperclass()) {
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field f : fields) {
                        if (!f.getType().isPrimitive() && !Modifier.isStatic(f.getModifiers())) {
                            try {
                                f.setAccessible(true);
                                f.set(test, null);
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        });
        return runner;
    }

    @Override
    List<Predicate<TestMethod>> getBuilderRequirements() {
        List<Predicate<TestMethod>> builderRequirements = super.getBuilderRequirements();
        Predicate<TestMethod> brokenTestPredicate = Predicates.not(new HasAnnotation(BrokenTest.class));
        builderRequirements.add(brokenTestPredicate);
        if (!this.singleTest) {
            Predicate<TestMethod> sideEffectPredicate = Predicates.not(new HasAnnotation(SideEffect.class));
            builderRequirements.add(sideEffectPredicate);
        }
        return builderRequirements;
    }
}
