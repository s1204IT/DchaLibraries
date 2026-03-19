package android.test;

import android.app.Activity;
import android.content.Intent;
import java.lang.reflect.Method;

@Deprecated
public abstract class ActivityInstrumentationTestCase2<T extends Activity> extends ActivityTestCase {
    Class<T> mActivityClass;
    Intent mActivityIntent;
    boolean mInitialTouchMode;

    @Deprecated
    public ActivityInstrumentationTestCase2(String pkg, Class<T> activityClass) {
        this(activityClass);
    }

    public ActivityInstrumentationTestCase2(Class<T> activityClass) {
        this.mInitialTouchMode = false;
        this.mActivityIntent = null;
        this.mActivityClass = activityClass;
    }

    @Override
    public T getActivity() {
        Activity activityLaunchActivityWithIntent = (T) super.getActivity();
        if (activityLaunchActivityWithIntent == null) {
            getInstrumentation().setInTouchMode(this.mInitialTouchMode);
            String packageName = getInstrumentation().getTargetContext().getPackageName();
            if (this.mActivityIntent == null) {
                activityLaunchActivityWithIntent = (T) launchActivity(packageName, this.mActivityClass, null);
            } else {
                activityLaunchActivityWithIntent = launchActivityWithIntent(packageName, this.mActivityClass, this.mActivityIntent);
            }
            setActivity(activityLaunchActivityWithIntent);
        }
        return (T) activityLaunchActivityWithIntent;
    }

    public void setActivityIntent(Intent i) {
        this.mActivityIntent = i;
    }

    public void setActivityInitialTouchMode(boolean initialTouchMode) {
        this.mInitialTouchMode = initialTouchMode;
    }

    protected void setUp() throws Exception {
        super.setUp();
        this.mInitialTouchMode = false;
        this.mActivityIntent = null;
    }

    protected void tearDown() throws Exception {
        Activity a = super.getActivity();
        if (a != null) {
            a.finish();
            setActivity(null);
        }
        scrubClass(ActivityInstrumentationTestCase2.class);
        super.tearDown();
    }

    protected void runTest() throws Throwable {
        try {
            Method method = getClass().getMethod(getName(), (Class[]) null);
            if (method.isAnnotationPresent(UiThreadTest.class)) {
                getActivity();
            }
        } catch (Exception e) {
        }
        super.runTest();
    }
}
