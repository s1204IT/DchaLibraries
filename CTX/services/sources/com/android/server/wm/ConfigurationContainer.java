package com.android.server.wm;

import android.app.WindowConfiguration;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.proto.ProtoOutputStream;
import com.android.server.wm.ConfigurationContainer;
import java.io.PrintWriter;
import java.util.ArrayList;

public abstract class ConfigurationContainer<E extends ConfigurationContainer> {
    static final int BOUNDS_CHANGE_NONE = 0;
    static final int BOUNDS_CHANGE_POSITION = 1;
    static final int BOUNDS_CHANGE_SIZE = 2;
    private boolean mHasOverrideConfiguration;
    private Rect mReturnBounds = new Rect();
    private Configuration mOverrideConfiguration = new Configuration();
    private Configuration mFullConfiguration = new Configuration();
    private Configuration mMergedOverrideConfiguration = new Configuration();
    private ArrayList<ConfigurationContainerListener> mChangeListeners = new ArrayList<>();
    private final Configuration mTmpConfig = new Configuration();
    private final Rect mTmpRect = new Rect();

    protected abstract E getChildAt(int i);

    protected abstract int getChildCount();

    protected abstract ConfigurationContainer getParent();

    public Configuration getConfiguration() {
        return this.mFullConfiguration;
    }

    public void onConfigurationChanged(Configuration configuration) {
        this.mFullConfiguration.setTo(configuration);
        this.mFullConfiguration.updateFrom(this.mOverrideConfiguration);
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            getChildAt(childCount).onConfigurationChanged(this.mFullConfiguration);
        }
    }

    public Configuration getOverrideConfiguration() {
        return this.mOverrideConfiguration;
    }

    public void onOverrideConfigurationChanged(Configuration configuration) {
        this.mHasOverrideConfiguration = !Configuration.EMPTY.equals(configuration);
        this.mOverrideConfiguration.setTo(configuration);
        ConfigurationContainer parent = getParent();
        onConfigurationChanged(parent != null ? parent.getConfiguration() : Configuration.EMPTY);
        onMergedOverrideConfigurationChanged();
        this.mTmpConfig.setTo(this.mOverrideConfiguration);
        for (int size = this.mChangeListeners.size() - 1; size >= 0; size--) {
            this.mChangeListeners.get(size).onOverrideConfigurationChanged(this.mTmpConfig);
        }
    }

    public Configuration getMergedOverrideConfiguration() {
        return this.mMergedOverrideConfiguration;
    }

    void onMergedOverrideConfigurationChanged() {
        ConfigurationContainer parent = getParent();
        if (parent != null) {
            this.mMergedOverrideConfiguration.setTo(parent.getMergedOverrideConfiguration());
            this.mMergedOverrideConfiguration.updateFrom(this.mOverrideConfiguration);
        } else {
            this.mMergedOverrideConfiguration.setTo(this.mOverrideConfiguration);
        }
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            getChildAt(childCount).onMergedOverrideConfigurationChanged();
        }
    }

    public boolean matchParentBounds() {
        return getOverrideBounds().isEmpty();
    }

    public boolean equivalentOverrideBounds(Rect rect) {
        return equivalentBounds(getOverrideBounds(), rect);
    }

    public static boolean equivalentBounds(Rect rect, Rect rect2) {
        return rect == rect2 || (rect != null && (rect.equals(rect2) || (rect.isEmpty() && rect2 == null))) || (rect2 != null && rect2.isEmpty() && rect == null);
    }

    public Rect getBounds() {
        this.mReturnBounds.set(getConfiguration().windowConfiguration.getBounds());
        return this.mReturnBounds;
    }

    public void getBounds(Rect rect) {
        rect.set(getBounds());
    }

    public Rect getOverrideBounds() {
        this.mReturnBounds.set(getOverrideConfiguration().windowConfiguration.getBounds());
        return this.mReturnBounds;
    }

    public boolean hasOverrideBounds() {
        return !getOverrideBounds().isEmpty();
    }

    public void getOverrideBounds(Rect rect) {
        rect.set(getOverrideBounds());
    }

    public int setBounds(Rect rect) {
        int iDiffOverrideBounds = diffOverrideBounds(rect);
        if (iDiffOverrideBounds == 0) {
            return iDiffOverrideBounds;
        }
        this.mTmpConfig.setTo(getOverrideConfiguration());
        this.mTmpConfig.windowConfiguration.setBounds(rect);
        onOverrideConfigurationChanged(this.mTmpConfig);
        return iDiffOverrideBounds;
    }

    public int setBounds(int i, int i2, int i3, int i4) {
        this.mTmpRect.set(i, i2, i3, i4);
        return setBounds(this.mTmpRect);
    }

    int diffOverrideBounds(Rect rect) {
        if (equivalentOverrideBounds(rect)) {
            return 0;
        }
        Rect overrideBounds = getOverrideBounds();
        int i = (rect != null && overrideBounds.left == rect.left && overrideBounds.top == rect.top) ? 0 : 1;
        if (rect == null || overrideBounds.width() != rect.width() || overrideBounds.height() != rect.height()) {
            return i | 2;
        }
        return i;
    }

    public WindowConfiguration getWindowConfiguration() {
        return this.mFullConfiguration.windowConfiguration;
    }

    public int getWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode();
    }

    public void setWindowingMode(int i) {
        this.mTmpConfig.setTo(getOverrideConfiguration());
        this.mTmpConfig.windowConfiguration.setWindowingMode(i);
        onOverrideConfigurationChanged(this.mTmpConfig);
    }

    public boolean inMultiWindowMode() {
        int windowingMode = this.mFullConfiguration.windowConfiguration.getWindowingMode();
        return (windowingMode == 1 || windowingMode == 0) ? false : true;
    }

    public boolean inSplitScreenWindowingMode() {
        int windowingMode = this.mFullConfiguration.windowConfiguration.getWindowingMode();
        return windowingMode == 3 || windowingMode == 4;
    }

    public boolean inSplitScreenSecondaryWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode() == 4;
    }

    public boolean inSplitScreenPrimaryWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode() == 3;
    }

    public boolean supportsSplitScreenWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.supportSplitScreenWindowingMode();
    }

    public boolean inPinnedWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode() == 2;
    }

    public boolean inFreeformWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode() == 5;
    }

    public int getActivityType() {
        return this.mFullConfiguration.windowConfiguration.getActivityType();
    }

    public void setActivityType(int i) {
        int activityType = getActivityType();
        if (activityType == i) {
            return;
        }
        if (activityType != 0) {
            throw new IllegalStateException("Can't change activity type once set: " + this + " activityType=" + WindowConfiguration.activityTypeToString(i));
        }
        this.mTmpConfig.setTo(getOverrideConfiguration());
        this.mTmpConfig.windowConfiguration.setActivityType(i);
        onOverrideConfigurationChanged(this.mTmpConfig);
    }

    public boolean isActivityTypeHome() {
        return getActivityType() == 2;
    }

    public boolean isActivityTypeRecents() {
        return getActivityType() == 3;
    }

    public boolean isActivityTypeAssistant() {
        return getActivityType() == 4;
    }

    public boolean isActivityTypeStandard() {
        return getActivityType() == 1;
    }

    public boolean isActivityTypeStandardOrUndefined() {
        int activityType = getActivityType();
        return activityType == 1 || activityType == 0;
    }

    public boolean hasCompatibleActivityType(ConfigurationContainer configurationContainer) {
        int activityType = getActivityType();
        int activityType2 = configurationContainer.getActivityType();
        if (activityType == activityType2) {
            return true;
        }
        if (activityType == 4) {
            return false;
        }
        return activityType == 0 || activityType2 == 0;
    }

    public boolean isCompatible(int i, int i2) {
        int activityType = getActivityType();
        int windowingMode = getWindowingMode();
        boolean z = false;
        boolean z2 = activityType == i2;
        if (windowingMode == i) {
            z = true;
        }
        if (z2 && z) {
            return true;
        }
        if ((i2 != 0 && i2 != 1) || !isActivityTypeStandardOrUndefined()) {
            return z2;
        }
        return z;
    }

    public void registerConfigurationChangeListener(ConfigurationContainerListener configurationContainerListener) {
        if (this.mChangeListeners.contains(configurationContainerListener)) {
            return;
        }
        this.mChangeListeners.add(configurationContainerListener);
        configurationContainerListener.onOverrideConfigurationChanged(this.mOverrideConfiguration);
    }

    public void unregisterConfigurationChangeListener(ConfigurationContainerListener configurationContainerListener) {
        this.mChangeListeners.remove(configurationContainerListener);
    }

    protected void onParentChanged() {
        ConfigurationContainer parent = getParent();
        if (parent != null) {
            onConfigurationChanged(parent.mFullConfiguration);
            onMergedOverrideConfigurationChanged();
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j, boolean z) {
        long jStart = protoOutputStream.start(j);
        if (!z || this.mHasOverrideConfiguration) {
            this.mOverrideConfiguration.writeToProto(protoOutputStream, 1146756268033L);
        }
        if (!z) {
            this.mFullConfiguration.writeToProto(protoOutputStream, 1146756268034L);
            this.mMergedOverrideConfiguration.writeToProto(protoOutputStream, 1146756268035L);
        }
        protoOutputStream.end(jStart);
    }

    public void dumpChildrenNames(PrintWriter printWriter, String str) {
        String str2 = str + " ";
        printWriter.println(getName() + " type=" + WindowConfiguration.activityTypeToString(getActivityType()) + " mode=" + WindowConfiguration.windowingModeToString(getWindowingMode()));
        for (int childCount = getChildCount() + (-1); childCount >= 0; childCount += -1) {
            ConfigurationContainer childAt = getChildAt(childCount);
            printWriter.print(str2 + "#" + childCount + " ");
            childAt.dumpChildrenNames(printWriter, str2);
        }
    }

    String getName() {
        return toString();
    }

    boolean isAlwaysOnTop() {
        return this.mFullConfiguration.windowConfiguration.isAlwaysOnTop();
    }
}
