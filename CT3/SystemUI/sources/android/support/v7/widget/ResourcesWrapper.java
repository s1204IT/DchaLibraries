package android.support.v7.widget;

import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParserException;

class ResourcesWrapper extends Resources {
    private final Resources mResources;

    public ResourcesWrapper(Resources resources) {
        super(resources.getAssets(), resources.getDisplayMetrics(), resources.getConfiguration());
        this.mResources = resources;
    }

    @Override
    public CharSequence getText(int id) throws Resources.NotFoundException {
        return this.mResources.getText(id);
    }

    @Override
    public CharSequence getQuantityText(int id, int quantity) throws Resources.NotFoundException {
        return this.mResources.getQuantityText(id, quantity);
    }

    @Override
    public String getString(int id) throws Resources.NotFoundException {
        return this.mResources.getString(id);
    }

    @Override
    public String getString(int id, Object... formatArgs) throws Resources.NotFoundException {
        return this.mResources.getString(id, formatArgs);
    }

    @Override
    public String getQuantityString(int id, int quantity, Object... formatArgs) throws Resources.NotFoundException {
        return this.mResources.getQuantityString(id, quantity, formatArgs);
    }

    @Override
    public String getQuantityString(int id, int quantity) throws Resources.NotFoundException {
        return this.mResources.getQuantityString(id, quantity);
    }

    @Override
    public CharSequence getText(int id, CharSequence def) {
        return this.mResources.getText(id, def);
    }

    @Override
    public CharSequence[] getTextArray(int id) throws Resources.NotFoundException {
        return this.mResources.getTextArray(id);
    }

    @Override
    public String[] getStringArray(int id) throws Resources.NotFoundException {
        return this.mResources.getStringArray(id);
    }

    @Override
    public int[] getIntArray(int id) throws Resources.NotFoundException {
        return this.mResources.getIntArray(id);
    }

    @Override
    public TypedArray obtainTypedArray(int id) throws Resources.NotFoundException {
        return this.mResources.obtainTypedArray(id);
    }

    @Override
    public float getDimension(int id) throws Resources.NotFoundException {
        return this.mResources.getDimension(id);
    }

    @Override
    public int getDimensionPixelOffset(int id) throws Resources.NotFoundException {
        return this.mResources.getDimensionPixelOffset(id);
    }

    @Override
    public int getDimensionPixelSize(int id) throws Resources.NotFoundException {
        return this.mResources.getDimensionPixelSize(id);
    }

    @Override
    public float getFraction(int id, int base, int pbase) {
        return this.mResources.getFraction(id, base, pbase);
    }

    @Override
    public Drawable getDrawable(int id) throws Resources.NotFoundException {
        return this.mResources.getDrawable(id);
    }

    @Override
    public Drawable getDrawable(int id, Resources.Theme theme) throws Resources.NotFoundException {
        return this.mResources.getDrawable(id, theme);
    }

    @Override
    public Drawable getDrawableForDensity(int id, int density) throws Resources.NotFoundException {
        return this.mResources.getDrawableForDensity(id, density);
    }

    @Override
    public Drawable getDrawableForDensity(int id, int density, Resources.Theme theme) {
        return this.mResources.getDrawableForDensity(id, density, theme);
    }

    @Override
    public Movie getMovie(int id) throws Resources.NotFoundException {
        return this.mResources.getMovie(id);
    }

    @Override
    public int getColor(int id) throws Resources.NotFoundException {
        return this.mResources.getColor(id);
    }

    @Override
    public ColorStateList getColorStateList(int id) throws Resources.NotFoundException {
        return this.mResources.getColorStateList(id);
    }

    @Override
    public boolean getBoolean(int id) throws Resources.NotFoundException {
        return this.mResources.getBoolean(id);
    }

    @Override
    public int getInteger(int id) throws Resources.NotFoundException {
        return this.mResources.getInteger(id);
    }

    @Override
    public XmlResourceParser getLayout(int id) throws Resources.NotFoundException {
        return this.mResources.getLayout(id);
    }

    @Override
    public XmlResourceParser getAnimation(int id) throws Resources.NotFoundException {
        return this.mResources.getAnimation(id);
    }

    @Override
    public XmlResourceParser getXml(int id) throws Resources.NotFoundException {
        return this.mResources.getXml(id);
    }

    @Override
    public InputStream openRawResource(int id) throws Resources.NotFoundException {
        return this.mResources.openRawResource(id);
    }

    @Override
    public InputStream openRawResource(int id, TypedValue value) throws Resources.NotFoundException {
        return this.mResources.openRawResource(id, value);
    }

    @Override
    public AssetFileDescriptor openRawResourceFd(int id) throws Resources.NotFoundException {
        return this.mResources.openRawResourceFd(id);
    }

    @Override
    public void getValue(int id, TypedValue outValue, boolean resolveRefs) throws Resources.NotFoundException {
        this.mResources.getValue(id, outValue, resolveRefs);
    }

    @Override
    public void getValueForDensity(int id, int density, TypedValue outValue, boolean resolveRefs) throws Resources.NotFoundException {
        this.mResources.getValueForDensity(id, density, outValue, resolveRefs);
    }

    @Override
    public void getValue(String name, TypedValue outValue, boolean resolveRefs) throws Resources.NotFoundException {
        this.mResources.getValue(name, outValue, resolveRefs);
    }

    @Override
    public TypedArray obtainAttributes(AttributeSet set, int[] attrs) {
        return this.mResources.obtainAttributes(set, attrs);
    }

    @Override
    public void updateConfiguration(Configuration config, DisplayMetrics metrics) {
        super.updateConfiguration(config, metrics);
        if (this.mResources == null) {
            return;
        }
        this.mResources.updateConfiguration(config, metrics);
    }

    @Override
    public DisplayMetrics getDisplayMetrics() {
        return this.mResources.getDisplayMetrics();
    }

    @Override
    public Configuration getConfiguration() {
        return this.mResources.getConfiguration();
    }

    @Override
    public int getIdentifier(String name, String defType, String defPackage) {
        return this.mResources.getIdentifier(name, defType, defPackage);
    }

    @Override
    public String getResourceName(int resid) throws Resources.NotFoundException {
        return this.mResources.getResourceName(resid);
    }

    @Override
    public String getResourcePackageName(int resid) throws Resources.NotFoundException {
        return this.mResources.getResourcePackageName(resid);
    }

    @Override
    public String getResourceTypeName(int resid) throws Resources.NotFoundException {
        return this.mResources.getResourceTypeName(resid);
    }

    @Override
    public String getResourceEntryName(int resid) throws Resources.NotFoundException {
        return this.mResources.getResourceEntryName(resid);
    }

    @Override
    public void parseBundleExtras(XmlResourceParser parser, Bundle outBundle) throws XmlPullParserException, IOException {
        this.mResources.parseBundleExtras(parser, outBundle);
    }

    @Override
    public void parseBundleExtra(String tagName, AttributeSet attrs, Bundle outBundle) throws XmlPullParserException {
        this.mResources.parseBundleExtra(tagName, attrs, outBundle);
    }
}
