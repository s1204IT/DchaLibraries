package android.test.mock;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import java.io.InputStream;

@Deprecated
public class MockResources extends Resources {
    public MockResources() {
        super(new AssetManager(), null, null);
    }

    @Override
    public void updateConfiguration(Configuration config, DisplayMetrics metrics) {
    }

    @Override
    public CharSequence getText(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public CharSequence getQuantityText(int id, int quantity) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public String getString(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public String getString(int id, Object... formatArgs) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public String getQuantityString(int id, int quantity, Object... formatArgs) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public String getQuantityString(int id, int quantity) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public CharSequence getText(int id, CharSequence def) {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public CharSequence[] getTextArray(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public String[] getStringArray(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public int[] getIntArray(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public TypedArray obtainTypedArray(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public float getDimension(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public int getDimensionPixelOffset(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public int getDimensionPixelSize(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public Drawable getDrawable(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public Movie getMovie(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public int getColor(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public ColorStateList getColorStateList(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public int getInteger(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public XmlResourceParser getLayout(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public XmlResourceParser getAnimation(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public XmlResourceParser getXml(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public InputStream openRawResource(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public AssetFileDescriptor openRawResourceFd(int id) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public void getValue(int id, TypedValue outValue, boolean resolveRefs) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public void getValue(String name, TypedValue outValue, boolean resolveRefs) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public TypedArray obtainAttributes(AttributeSet set, int[] attrs) {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public DisplayMetrics getDisplayMetrics() {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public Configuration getConfiguration() {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public int getIdentifier(String name, String defType, String defPackage) {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public String getResourceName(int resid) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public String getResourcePackageName(int resid) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public String getResourceTypeName(int resid) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }

    @Override
    public String getResourceEntryName(int resid) throws Resources.NotFoundException {
        throw new UnsupportedOperationException("mock object, not implemented");
    }
}
