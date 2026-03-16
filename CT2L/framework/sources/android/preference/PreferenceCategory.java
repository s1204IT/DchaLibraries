package android.preference;

import android.content.Context;
import android.util.AttributeSet;

public class PreferenceCategory extends PreferenceGroup {
    private static final String TAG = "PreferenceCategory";

    public PreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PreferenceCategory(Context context, AttributeSet attrs) {
        this(context, attrs, 16842892);
    }

    public PreferenceCategory(Context context) {
        this(context, null);
    }

    @Override
    protected boolean onPrepareAddPreference(Preference preference) {
        if (preference instanceof PreferenceCategory) {
            throw new IllegalArgumentException("Cannot add a PreferenceCategory directly to a PreferenceCategory");
        }
        return super.onPrepareAddPreference(preference);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean shouldDisableDependents() {
        return !super.isEnabled();
    }
}
