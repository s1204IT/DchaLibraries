package com.android.quicksearchbox;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.NowOrLater;

public abstract class AbstractSource implements Source {
    private final Context mContext;
    private IconLoader mIconLoader;
    private final NamedTaskExecutor mIconLoaderExecutor;
    private final Handler mUiThread;

    protected abstract String getIconPackage();

    public AbstractSource(Context context, Handler uiThread, NamedTaskExecutor iconLoader) {
        this.mContext = context;
        this.mUiThread = uiThread;
        this.mIconLoaderExecutor = iconLoader;
    }

    protected Context getContext() {
        return this.mContext;
    }

    protected IconLoader getIconLoader() {
        if (this.mIconLoader == null) {
            String iconPackage = getIconPackage();
            this.mIconLoader = new CachingIconLoader(new PackageIconLoader(this.mContext, iconPackage, this.mUiThread, this.mIconLoaderExecutor));
        }
        return this.mIconLoader;
    }

    @Override
    public NowOrLater<Drawable> getIcon(String drawableId) {
        return getIconLoader().getIcon(drawableId);
    }

    @Override
    public Uri getIconUri(String drawableId) {
        return getIconLoader().getIconUri(drawableId);
    }

    @Override
    public Intent createSearchIntent(String query, Bundle appData) {
        return createSourceSearchIntent(getIntentComponent(), query, appData);
    }

    public static Intent createSourceSearchIntent(ComponentName activity, String query, Bundle appData) {
        if (activity == null) {
            Log.w("QSB.AbstractSource", "Tried to create search intent with no target activity");
            return null;
        }
        Intent intent = new Intent("android.intent.action.SEARCH");
        intent.setComponent(activity);
        intent.addFlags(268435456);
        intent.addFlags(67108864);
        intent.putExtra("user_query", query);
        intent.putExtra("query", query);
        if (appData != null) {
            intent.putExtra("app_data", appData);
        }
        return intent;
    }

    protected Intent createVoiceWebSearchIntent(Bundle appData) {
        return QsbApplication.get(this.mContext).getVoiceSearch().createVoiceWebSearchIntent(appData);
    }

    @Override
    public Source getRoot() {
        return this;
    }

    public boolean equals(Object o) {
        if (o != null && (o instanceof Source)) {
            Source s = ((Source) o).getRoot();
            if (s.getClass().equals(getClass())) {
                return s.getName().equals(getName());
            }
            return false;
        }
        return false;
    }

    public int hashCode() {
        return getName().hashCode();
    }

    public String toString() {
        return "Source{name=" + getName() + "}";
    }
}
