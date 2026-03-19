package android.content.res;

import android.content.res.Resources;

public class ConfigurationBoundResourceCache<T> extends ThemedResourceCache<ConstantState<T>> {
    @Override
    public void onConfigurationChange(int i) {
        super.onConfigurationChange(i);
    }

    public T getInstance(long j, Resources resources, Resources.Theme theme) {
        ConstantState constantState = (ConstantState) get(j, theme);
        if (constantState != null) {
            return (T) constantState.newInstance2(resources, theme);
        }
        return null;
    }

    @Override
    public boolean shouldInvalidateEntry(ConstantState<T> constantState, int i) {
        return Configuration.needNewResources(i, constantState.getChangingConfigurations());
    }
}
