package android.support.v7.preference;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;

public final class PreferenceScreen extends PreferenceGroup {
    private boolean mShouldUseGeneratedIds;

    public PreferenceScreen(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(context, R.attr.preferenceScreenStyle, android.R.attr.preferenceScreenStyle));
        this.mShouldUseGeneratedIds = true;
    }

    @Override
    protected void onClick() {
        PreferenceManager.OnNavigateToScreenListener listener;
        if (getIntent() == null && getFragment() == null && getPreferenceCount() != 0 && (listener = getPreferenceManager().getOnNavigateToScreenListener()) != null) {
            listener.onNavigateToScreen(this);
        }
    }

    @Override
    protected boolean isOnSameScreenAsChildren() {
        return false;
    }

    public boolean shouldUseGeneratedIds() {
        return this.mShouldUseGeneratedIds;
    }
}
