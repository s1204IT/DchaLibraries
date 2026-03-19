package android.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import junit.framework.TestCase;

@Deprecated
public class InstrumentationTestCase extends TestCase {
    private Instrumentation mInstrumentation;

    public void injectInstrumentation(Instrumentation instrumentation) {
        this.mInstrumentation = instrumentation;
    }

    @Deprecated
    public void injectInsrumentation(Instrumentation instrumentation) {
        injectInstrumentation(instrumentation);
    }

    public Instrumentation getInstrumentation() {
        return this.mInstrumentation;
    }

    public final <T extends Activity> T launchActivity(String str, Class<T> cls, Bundle bundle) {
        Intent intent = new Intent("android.intent.action.MAIN");
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        return (T) launchActivityWithIntent(str, cls, intent);
    }

    public final <T extends Activity> T launchActivityWithIntent(String str, Class<T> cls, Intent intent) {
        intent.setClassName(str, cls.getName());
        intent.addFlags(268435456);
        T t = (T) getInstrumentation().startActivitySync(intent);
        getInstrumentation().waitForIdleSync();
        return t;
    }

    public void runTestOnUiThread(final Runnable runnable) throws Throwable {
        final Throwable[] thArr = new Throwable[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable th) {
                    thArr[0] = th;
                }
            }
        });
        if (thArr[0] != null) {
            throw thArr[0];
        }
    }

    @Override
    protected void runTest() throws Throwable {
        final Method method;
        final boolean z;
        final int iNumIterations;
        String name = getName();
        assertNotNull(name);
        try {
            method = getClass().getMethod(name, (Class[]) null);
        } catch (NoSuchMethodException e) {
            fail("Method \"" + name + "\" not found");
            method = null;
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            fail("Method \"" + name + "\" should be public");
        }
        if (method.isAnnotationPresent(FlakyTest.class)) {
            iNumIterations = ((FlakyTest) method.getAnnotation(FlakyTest.class)).tolerance();
            z = false;
        } else if (method.isAnnotationPresent(RepetitiveTest.class)) {
            iNumIterations = ((RepetitiveTest) method.getAnnotation(RepetitiveTest.class)).numIterations();
            z = true;
        } else {
            z = false;
            iNumIterations = 1;
        }
        if (method.isAnnotationPresent(UiThreadTest.class)) {
            final Throwable[] thArr = new Throwable[1];
            getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    try {
                        InstrumentationTestCase.this.runMethod(method, iNumIterations, z);
                    } catch (Throwable th) {
                        thArr[0] = th;
                    }
                }
            });
            if (thArr[0] != null) {
                throw thArr[0];
            }
            return;
        }
        runMethod(method, iNumIterations, z);
    }

    private void runMethod(Method method, int i) throws Throwable {
        runMethod(method, i, false);
    }

    private void runMethod(Method method, int i, boolean z) throws Throwable {
        Throwable th;
        Bundle bundle;
        int i2 = 0;
        while (true) {
            th = null;
            Throwable th2 = null;
            try {
                try {
                    method.invoke(this, (Object[]) null);
                    i2++;
                } catch (IllegalAccessException e) {
                    e.fillInStackTrace();
                    i2++;
                    th = e;
                    if (z) {
                        bundle = new Bundle();
                        th2 = e;
                    }
                } catch (InvocationTargetException e2) {
                    e2.fillInStackTrace();
                    Throwable targetException = e2.getTargetException();
                    i2++;
                    th = targetException;
                    if (z) {
                        bundle = new Bundle();
                        th2 = targetException;
                    }
                }
                if (z) {
                    bundle = new Bundle();
                    bundle.putInt("currentiterations", i2);
                    getInstrumentation().sendStatus(2, bundle);
                    th = th2;
                }
                if (i2 >= i || (!z && th == null)) {
                    break;
                }
            } catch (Throwable th3) {
                int i3 = i2 + 1;
                if (z) {
                    Bundle bundle2 = new Bundle();
                    bundle2.putInt("currentiterations", i3);
                    getInstrumentation().sendStatus(2, bundle2);
                }
                throw th3;
            }
        }
        if (th != null) {
            throw th;
        }
    }

    public void sendKeys(String str) {
        int i;
        String[] strArrSplit = str.split(" ");
        Instrumentation instrumentation = getInstrumentation();
        for (String strSubstring : strArrSplit) {
            int iIndexOf = strSubstring.indexOf(42);
            if (iIndexOf != -1) {
                try {
                    i = Integer.parseInt(strSubstring.substring(0, iIndexOf));
                } catch (NumberFormatException e) {
                    Log.w("ActivityTestCase", "Invalid repeat count: " + strSubstring);
                }
            } else {
                i = 1;
            }
            if (iIndexOf != -1) {
                strSubstring = strSubstring.substring(iIndexOf + 1);
            }
            for (int i2 = 0; i2 < i; i2++) {
                try {
                    try {
                        instrumentation.sendKeyDownUpSync(KeyEvent.class.getField("KEYCODE_" + strSubstring).getInt(null));
                    } catch (SecurityException e2) {
                    }
                } catch (IllegalAccessException e3) {
                    Log.w("ActivityTestCase", "Unknown keycode: KEYCODE_" + strSubstring);
                } catch (NoSuchFieldException e4) {
                    Log.w("ActivityTestCase", "Unknown keycode: KEYCODE_" + strSubstring);
                }
            }
        }
        instrumentation.waitForIdleSync();
    }

    public void sendKeys(int... iArr) {
        Instrumentation instrumentation = getInstrumentation();
        for (int i : iArr) {
            try {
                instrumentation.sendKeyDownUpSync(i);
            } catch (SecurityException e) {
            }
        }
        instrumentation.waitForIdleSync();
    }

    public void sendRepeatedKeys(int... iArr) {
        int length = iArr.length;
        if ((length & 1) == 1) {
            throw new IllegalArgumentException("The size of the keys array must be a multiple of 2");
        }
        Instrumentation instrumentation = getInstrumentation();
        for (int i = 0; i < length; i += 2) {
            int i2 = iArr[i];
            int i3 = iArr[i + 1];
            for (int i4 = 0; i4 < i2; i4++) {
                try {
                    instrumentation.sendKeyDownUpSync(i3);
                } catch (SecurityException e) {
                }
            }
        }
        instrumentation.waitForIdleSync();
    }

    @Override
    protected void tearDown() throws Exception {
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        super.tearDown();
    }
}
