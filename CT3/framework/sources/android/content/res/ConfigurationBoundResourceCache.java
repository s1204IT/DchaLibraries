package android.content.res;

import android.content.res.Resources;

public class ConfigurationBoundResourceCache<T> extends ThemedResourceCache<ConstantState<T>> {
    @Override
    public Object get(long key, Resources.Theme theme) {
        return super.get(key, theme);
    }

    @Override
    public void onConfigurationChange(int configChanges) {
        super.onConfigurationChange(configChanges);
    }

    @Override
    public void put(long key, Resources.Theme theme, Object obj) {
        super.put(key, theme, obj);
    }

    @Override
    public void put(long key, Resources.Theme theme, Object obj, boolean usesTheme) {
        super.put(key, theme, obj, usesTheme);
    }

    public T getInstance(long key, Resources resources, Resources.Theme theme) {
        ConstantState<T> entry = (ConstantState) get(key, theme);
        if (entry != null) {
            return entry.newInstance2(resources, theme);
        }
        return null;
    }

    @Override
    public boolean shouldInvalidateEntry(ConstantState<T> entry, int configChanges) {
        return Configuration.needNewResources(configChanges, entry.getChangingConfigurations());
    }
}
