package android.test;

import android.app.Activity;
import android.util.Log;
import java.lang.reflect.Field;

@Deprecated
public abstract class ActivityTestCase extends InstrumentationTestCase {
    private Activity mActivity;

    protected Activity getActivity() {
        return this.mActivity;
    }

    protected void setActivity(Activity testActivity) {
        this.mActivity = testActivity;
    }

    protected void scrubClass(Class<?> testCaseClass) throws IllegalAccessException {
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            Class<?> fieldClass = field.getDeclaringClass();
            if (testCaseClass.isAssignableFrom(fieldClass) && !field.getType().isPrimitive() && (field.getModifiers() & 16) == 0) {
                try {
                    field.setAccessible(true);
                    field.set(this, null);
                } catch (Exception e) {
                    Log.d("TestCase", "Error: Could not nullify field!");
                }
                if (field.get(this) != null) {
                    Log.d("TestCase", "Error: Could not nullify field!");
                }
            }
        }
    }
}
