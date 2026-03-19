package android.test;

import android.app.Activity;

@Deprecated
public abstract class SingleLaunchActivityTestCase<T extends Activity> extends InstrumentationTestCase {
    private static Activity sActivity;
    Class<T> mActivityClass;
    String mPackage;
    private static int sTestCaseCounter = 0;
    private static boolean sActivityLaunchedFlag = false;

    public SingleLaunchActivityTestCase(String pkg, Class<T> activityClass) {
        this.mPackage = pkg;
        this.mActivityClass = activityClass;
        sTestCaseCounter++;
    }

    public T getActivity() {
        return (T) sActivity;
    }

    protected void setUp() throws Exception {
        super.setUp();
        if (sActivityLaunchedFlag) {
            return;
        }
        getInstrumentation().setInTouchMode(false);
        sActivity = launchActivity(this.mPackage, this.mActivityClass, null);
        sActivityLaunchedFlag = true;
    }

    protected void tearDown() throws Exception {
        sTestCaseCounter--;
        if (sTestCaseCounter == 0) {
            sActivity.finish();
        }
        super.tearDown();
    }

    public void testActivityTestCaseSetUpProperly() throws Exception {
        assertNotNull("activity should be launched successfully", sActivity);
    }
}
