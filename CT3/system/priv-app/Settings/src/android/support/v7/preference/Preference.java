package android.support.v7.preference;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.SharedPreferencesCompat$EditorCompat;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.AbsSavedState;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class Preference implements Comparable<Preference> {
    private boolean mBaseMethodCalled;
    private final View.OnClickListener mClickListener;
    private Context mContext;
    private Object mDefaultValue;
    private String mDependencyKey;
    private boolean mDependencyMet;
    private List<Preference> mDependents;
    private boolean mEnabled;
    private Bundle mExtras;
    private String mFragment;
    private boolean mHasId;
    private Drawable mIcon;
    private int mIconResId;
    private long mId;
    private Intent mIntent;
    private String mKey;
    private int mLayoutResId;
    private OnPreferenceChangeInternalListener mListener;
    private OnPreferenceChangeListener mOnChangeListener;
    private OnPreferenceClickListener mOnClickListener;
    private int mOrder;
    private boolean mParentDependencyMet;
    private boolean mPersistent;
    private PreferenceManager mPreferenceManager;
    private boolean mRequiresKey;
    private boolean mSelectable;
    private boolean mShouldDisableView;
    private CharSequence mSummary;
    private CharSequence mTitle;
    private int mViewId;
    private boolean mVisible;
    private int mWidgetLayoutResId;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnPreferenceChangeInternalListener {
        void onPreferenceChange(Preference preference);

        void onPreferenceHierarchyChange(Preference preference);

        void onPreferenceVisibilityChange(Preference preference);
    }

    /* loaded from: classes.dex */
    public interface OnPreferenceChangeListener {
        boolean onPreferenceChange(Preference preference, Object obj);
    }

    /* loaded from: classes.dex */
    public interface OnPreferenceClickListener {
        boolean onPreferenceClick(Preference preference);
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.mOrder = Integer.MAX_VALUE;
        this.mViewId = 0;
        this.mEnabled = true;
        this.mSelectable = true;
        this.mPersistent = true;
        this.mDependencyMet = true;
        this.mParentDependencyMet = true;
        this.mVisible = true;
        this.mShouldDisableView = true;
        this.mLayoutResId = R$layout.preference;
        this.mClickListener = new View.OnClickListener() { // from class: android.support.v7.preference.Preference.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Preference.this.performClick(v);
            }
        };
        this.mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.Preference, defStyleAttr, defStyleRes);
        this.mIconResId = TypedArrayUtils.getResourceId(a, R$styleable.Preference_icon, R$styleable.Preference_android_icon, 0);
        this.mKey = TypedArrayUtils.getString(a, R$styleable.Preference_key, R$styleable.Preference_android_key);
        this.mTitle = TypedArrayUtils.getString(a, R$styleable.Preference_title, R$styleable.Preference_android_title);
        this.mSummary = TypedArrayUtils.getString(a, R$styleable.Preference_summary, R$styleable.Preference_android_summary);
        this.mOrder = TypedArrayUtils.getInt(a, R$styleable.Preference_order, R$styleable.Preference_android_order, Integer.MAX_VALUE);
        this.mFragment = TypedArrayUtils.getString(a, R$styleable.Preference_fragment, R$styleable.Preference_android_fragment);
        this.mLayoutResId = TypedArrayUtils.getResourceId(a, R$styleable.Preference_layout, R$styleable.Preference_android_layout, R$layout.preference);
        this.mWidgetLayoutResId = TypedArrayUtils.getResourceId(a, R$styleable.Preference_widgetLayout, R$styleable.Preference_android_widgetLayout, 0);
        this.mEnabled = TypedArrayUtils.getBoolean(a, R$styleable.Preference_enabled, R$styleable.Preference_android_enabled, true);
        this.mSelectable = TypedArrayUtils.getBoolean(a, R$styleable.Preference_selectable, R$styleable.Preference_android_selectable, true);
        this.mPersistent = TypedArrayUtils.getBoolean(a, R$styleable.Preference_persistent, R$styleable.Preference_android_persistent, true);
        this.mDependencyKey = TypedArrayUtils.getString(a, R$styleable.Preference_dependency, R$styleable.Preference_android_dependency);
        if (a.hasValue(R$styleable.Preference_defaultValue)) {
            this.mDefaultValue = onGetDefaultValue(a, R$styleable.Preference_defaultValue);
        } else if (a.hasValue(R$styleable.Preference_android_defaultValue)) {
            this.mDefaultValue = onGetDefaultValue(a, R$styleable.Preference_android_defaultValue);
        }
        this.mShouldDisableView = TypedArrayUtils.getBoolean(a, R$styleable.Preference_shouldDisableView, R$styleable.Preference_android_shouldDisableView, true);
        a.recycle();
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Preference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R$attr.preferenceStyle, 16842894));
    }

    public Preference(Context context) {
        this(context, null);
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        return null;
    }

    public void setIntent(Intent intent) {
        this.mIntent = intent;
    }

    public Intent getIntent() {
        return this.mIntent;
    }

    public void setFragment(String fragment) {
        this.mFragment = fragment;
    }

    public String getFragment() {
        return this.mFragment;
    }

    public Bundle getExtras() {
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        return this.mExtras;
    }

    public void setLayoutResource(int layoutResId) {
        this.mLayoutResId = layoutResId;
    }

    public final int getLayoutResource() {
        return this.mLayoutResId;
    }

    public void setWidgetLayoutResource(int widgetLayoutResId) {
        this.mWidgetLayoutResId = widgetLayoutResId;
    }

    public final int getWidgetLayoutResource() {
        return this.mWidgetLayoutResId;
    }

    public void onBindViewHolder(PreferenceViewHolder holder) {
        holder.itemView.setOnClickListener(this.mClickListener);
        holder.itemView.setId(this.mViewId);
        TextView titleView = (TextView) holder.findViewById(16908310);
        if (titleView != null) {
            CharSequence title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                titleView.setText(title);
                titleView.setVisibility(0);
            } else {
                titleView.setVisibility(8);
            }
        }
        TextView summaryView = (TextView) holder.findViewById(16908304);
        if (summaryView != null) {
            CharSequence summary = getSummary();
            if (!TextUtils.isEmpty(summary)) {
                summaryView.setText(summary);
                summaryView.setVisibility(0);
            } else {
                summaryView.setVisibility(8);
            }
        }
        ImageView imageView = (ImageView) holder.findViewById(16908294);
        if (imageView != null) {
            if (this.mIconResId != 0 || this.mIcon != null) {
                if (this.mIcon == null) {
                    this.mIcon = ContextCompat.getDrawable(getContext(), this.mIconResId);
                }
                if (this.mIcon != null) {
                    imageView.setImageDrawable(this.mIcon);
                }
            }
            imageView.setVisibility(this.mIcon != null ? 0 : 8);
        }
        View imageFrame = holder.findViewById(R$id.icon_frame);
        if (imageFrame == null) {
            imageFrame = holder.findViewById(16908350);
        }
        if (imageFrame != null) {
            imageFrame.setVisibility(this.mIcon == null ? 8 : 0);
        }
        if (this.mShouldDisableView) {
            setEnabledStateOnViews(holder.itemView, isEnabled());
        } else {
            setEnabledStateOnViews(holder.itemView, true);
        }
        boolean selectable = isSelectable();
        holder.itemView.setFocusable(selectable);
        holder.itemView.setClickable(selectable);
        holder.setDividerAllowedAbove(selectable);
        holder.setDividerAllowedBelow(selectable);
    }

    private void setEnabledStateOnViews(View v, boolean enabled) {
        v.setEnabled(enabled);
        if (!(v instanceof ViewGroup)) {
            return;
        }
        ViewGroup vg = (ViewGroup) v;
        for (int i = vg.getChildCount() - 1; i >= 0; i--) {
            setEnabledStateOnViews(vg.getChildAt(i), enabled);
        }
    }

    public void setOrder(int order) {
        if (order == this.mOrder) {
            return;
        }
        this.mOrder = order;
        notifyHierarchyChanged();
    }

    public int getOrder() {
        return this.mOrder;
    }

    public void setViewId(int viewId) {
        this.mViewId = viewId;
    }

    public void setTitle(CharSequence title) {
        if ((title != null || this.mTitle == null) && (title == null || title.equals(this.mTitle))) {
            return;
        }
        this.mTitle = title;
        notifyChanged();
    }

    public void setTitle(int titleResId) {
        setTitle(this.mContext.getString(titleResId));
    }

    public CharSequence getTitle() {
        return this.mTitle;
    }

    public void setIcon(Drawable icon) {
        if ((icon != null || this.mIcon == null) && (icon == null || this.mIcon == icon)) {
            return;
        }
        this.mIcon = icon;
        this.mIconResId = 0;
        notifyChanged();
    }

    public void setIcon(int iconResId) {
        setIcon(ContextCompat.getDrawable(this.mContext, iconResId));
        this.mIconResId = iconResId;
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    public CharSequence getSummary() {
        return this.mSummary;
    }

    public void setSummary(CharSequence summary) {
        if ((summary != null || this.mSummary == null) && (summary == null || summary.equals(this.mSummary))) {
            return;
        }
        this.mSummary = summary;
        notifyChanged();
    }

    public void setSummary(int summaryResId) {
        setSummary(this.mContext.getString(summaryResId));
    }

    public void setEnabled(boolean enabled) {
        if (this.mEnabled == enabled) {
            return;
        }
        this.mEnabled = enabled;
        notifyDependencyChange(shouldDisableDependents());
        notifyChanged();
    }

    public boolean isEnabled() {
        if (this.mEnabled && this.mDependencyMet) {
            return this.mParentDependencyMet;
        }
        return false;
    }

    public void setSelectable(boolean selectable) {
        if (this.mSelectable == selectable) {
            return;
        }
        this.mSelectable = selectable;
        notifyChanged();
    }

    public boolean isSelectable() {
        return this.mSelectable;
    }

    public void setShouldDisableView(boolean shouldDisableView) {
        this.mShouldDisableView = shouldDisableView;
        notifyChanged();
    }

    public final void setVisible(boolean visible) {
        if (this.mVisible == visible) {
            return;
        }
        this.mVisible = visible;
        if (this.mListener == null) {
            return;
        }
        this.mListener.onPreferenceVisibilityChange(this);
    }

    public final boolean isVisible() {
        return this.mVisible;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getId() {
        return this.mId;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onClick() {
    }

    public void setKey(String key) {
        this.mKey = key;
        if (!this.mRequiresKey || hasKey()) {
            return;
        }
        requireKey();
    }

    public String getKey() {
        return this.mKey;
    }

    void requireKey() {
        if (TextUtils.isEmpty(this.mKey)) {
            throw new IllegalStateException("Preference does not have a key assigned.");
        }
        this.mRequiresKey = true;
    }

    public boolean hasKey() {
        return !TextUtils.isEmpty(this.mKey);
    }

    public boolean isPersistent() {
        return this.mPersistent;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean shouldPersist() {
        if (this.mPreferenceManager == null || !isPersistent()) {
            return false;
        }
        return hasKey();
    }

    public void setPersistent(boolean persistent) {
        this.mPersistent = persistent;
    }

    public boolean callChangeListener(Object newValue) {
        if (this.mOnChangeListener != null) {
            return this.mOnChangeListener.onPreferenceChange(this, newValue);
        }
        return true;
    }

    public void setOnPreferenceChangeListener(OnPreferenceChangeListener onPreferenceChangeListener) {
        this.mOnChangeListener = onPreferenceChangeListener;
    }

    public void setOnPreferenceClickListener(OnPreferenceClickListener onPreferenceClickListener) {
        this.mOnClickListener = onPreferenceClickListener;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void performClick(View view) {
        performClick();
    }

    public void performClick() {
        PreferenceManager.OnPreferenceTreeClickListener listener;
        if (!isEnabled()) {
            return;
        }
        onClick();
        if (this.mOnClickListener != null && this.mOnClickListener.onPreferenceClick(this)) {
            return;
        }
        PreferenceManager preferenceManager = getPreferenceManager();
        if ((preferenceManager != null && (listener = preferenceManager.getOnPreferenceTreeClickListener()) != null && listener.onPreferenceTreeClick(this)) || this.mIntent == null) {
            return;
        }
        Context context = getContext();
        context.startActivity(this.mIntent);
    }

    public Context getContext() {
        return this.mContext;
    }

    public SharedPreferences getSharedPreferences() {
        if (this.mPreferenceManager == null) {
            return null;
        }
        return this.mPreferenceManager.getSharedPreferences();
    }

    @Override // java.lang.Comparable
    public int compareTo(@NonNull Preference another) {
        if (this.mOrder != another.mOrder) {
            return this.mOrder - another.mOrder;
        }
        if (this.mTitle == another.mTitle) {
            return 0;
        }
        if (this.mTitle == null) {
            return 1;
        }
        if (another.mTitle == null) {
            return -1;
        }
        return this.mTitle.toString().compareToIgnoreCase(another.mTitle.toString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void setOnPreferenceChangeInternalListener(OnPreferenceChangeInternalListener listener) {
        this.mListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyChanged() {
        if (this.mListener == null) {
            return;
        }
        this.mListener.onPreferenceChange(this);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyHierarchyChanged() {
        if (this.mListener == null) {
            return;
        }
        this.mListener.onPreferenceHierarchyChange(this);
    }

    public PreferenceManager getPreferenceManager() {
        return this.mPreferenceManager;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        this.mPreferenceManager = preferenceManager;
        if (!this.mHasId) {
            this.mId = preferenceManager.getNextId();
        }
        dispatchSetInitialValue();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onAttachedToHierarchy(PreferenceManager preferenceManager, long id) {
        this.mId = id;
        this.mHasId = true;
        try {
            onAttachedToHierarchy(preferenceManager);
        } finally {
            this.mHasId = false;
        }
    }

    public void onAttached() {
        registerDependency();
    }

    public void onDetached() {
        unregisterDependency();
    }

    private void registerDependency() {
        if (TextUtils.isEmpty(this.mDependencyKey)) {
            return;
        }
        Preference preference = findPreferenceInHierarchy(this.mDependencyKey);
        if (preference == null) {
            throw new IllegalStateException("Dependency \"" + this.mDependencyKey + "\" not found for preference \"" + this.mKey + "\" (title: \"" + this.mTitle + "\"");
        }
        preference.registerDependent(this);
    }

    private void unregisterDependency() {
        Preference oldDependency;
        if (this.mDependencyKey == null || (oldDependency = findPreferenceInHierarchy(this.mDependencyKey)) == null) {
            return;
        }
        oldDependency.unregisterDependent(this);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Preference findPreferenceInHierarchy(String key) {
        if (TextUtils.isEmpty(key) || this.mPreferenceManager == null) {
            return null;
        }
        return this.mPreferenceManager.findPreference(key);
    }

    private void registerDependent(Preference dependent) {
        if (this.mDependents == null) {
            this.mDependents = new ArrayList();
        }
        this.mDependents.add(dependent);
        dependent.onDependencyChanged(this, shouldDisableDependents());
    }

    private void unregisterDependent(Preference dependent) {
        if (this.mDependents == null) {
            return;
        }
        this.mDependents.remove(dependent);
    }

    public void notifyDependencyChange(boolean disableDependents) {
        List<Preference> dependents = this.mDependents;
        if (dependents == null) {
            return;
        }
        int dependentsCount = dependents.size();
        for (int i = 0; i < dependentsCount; i++) {
            dependents.get(i).onDependencyChanged(this, disableDependents);
        }
    }

    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        if (this.mDependencyMet != disableDependent) {
            return;
        }
        this.mDependencyMet = !disableDependent;
        notifyDependencyChange(shouldDisableDependents());
        notifyChanged();
    }

    public void onParentChanged(Preference parent, boolean disableChild) {
        if (this.mParentDependencyMet != disableChild) {
            return;
        }
        this.mParentDependencyMet = !disableChild;
        notifyDependencyChange(shouldDisableDependents());
        notifyChanged();
    }

    public boolean shouldDisableDependents() {
        return !isEnabled();
    }

    public void setDependency(String dependencyKey) {
        unregisterDependency();
        this.mDependencyKey = dependencyKey;
        registerDependency();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onPrepareForRemoval() {
        unregisterDependency();
    }

    private void dispatchSetInitialValue() {
        boolean shouldPersist = shouldPersist();
        if (!shouldPersist || !getSharedPreferences().contains(this.mKey)) {
            if (this.mDefaultValue == null) {
                return;
            }
            onSetInitialValue(false, this.mDefaultValue);
            return;
        }
        onSetInitialValue(true, null);
    }

    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    }

    private void tryCommit(@NonNull SharedPreferences.Editor editor) {
        if (!this.mPreferenceManager.shouldCommit()) {
            return;
        }
        SharedPreferencesCompat$EditorCompat.getInstance().apply(editor);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean persistString(String value) {
        if (shouldPersist()) {
            if (value == getPersistedString(null)) {
                return true;
            }
            SharedPreferences.Editor editor = this.mPreferenceManager.getEditor();
            editor.putString(this.mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        return this.mPreferenceManager.getSharedPreferences().getString(this.mKey, defaultReturnValue);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean persistInt(int value) {
        if (shouldPersist()) {
            if (value == getPersistedInt(~value)) {
                return true;
            }
            SharedPreferences.Editor editor = this.mPreferenceManager.getEditor();
            editor.putInt(this.mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getPersistedInt(int defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        return this.mPreferenceManager.getSharedPreferences().getInt(this.mKey, defaultReturnValue);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean persistBoolean(boolean value) {
        if (shouldPersist()) {
            if (value == getPersistedBoolean(value ? false : true)) {
                return true;
            }
            SharedPreferences.Editor editor = this.mPreferenceManager.getEditor();
            editor.putBoolean(this.mKey, value);
            tryCommit(editor);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean getPersistedBoolean(boolean defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        return this.mPreferenceManager.getSharedPreferences().getBoolean(this.mKey, defaultReturnValue);
    }

    public String toString() {
        return getFilterableStringBuilder().toString();
    }

    StringBuilder getFilterableStringBuilder() {
        StringBuilder sb = new StringBuilder();
        CharSequence title = getTitle();
        if (!TextUtils.isEmpty(title)) {
            sb.append(title).append(' ');
        }
        CharSequence summary = getSummary();
        if (!TextUtils.isEmpty(summary)) {
            sb.append(summary).append(' ');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb;
    }

    public void saveHierarchyState(Bundle container) {
        dispatchSaveInstanceState(container);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchSaveInstanceState(Bundle container) {
        if (!hasKey()) {
            return;
        }
        this.mBaseMethodCalled = false;
        Parcelable state = onSaveInstanceState();
        if (!this.mBaseMethodCalled) {
            throw new IllegalStateException("Derived class did not call super.onSaveInstanceState()");
        }
        if (state == null) {
            return;
        }
        container.putParcelable(this.mKey, state);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        this.mBaseMethodCalled = true;
        return BaseSavedState.EMPTY_STATE;
    }

    public void restoreHierarchyState(Bundle container) {
        dispatchRestoreInstanceState(container);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchRestoreInstanceState(Bundle container) {
        Parcelable state;
        if (!hasKey() || (state = container.getParcelable(this.mKey)) == null) {
            return;
        }
        this.mBaseMethodCalled = false;
        onRestoreInstanceState(state);
        if (this.mBaseMethodCalled) {
            return;
        }
        throw new IllegalStateException("Derived class did not call super.onRestoreInstanceState()");
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable state) {
        this.mBaseMethodCalled = true;
        if (state == BaseSavedState.EMPTY_STATE || state == null) {
            return;
        }
        throw new IllegalArgumentException("Wrong state class -- expecting Preference State");
    }

    @CallSuper
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfoCompat info) {
    }

    /* loaded from: classes.dex */
    public static class BaseSavedState extends AbsSavedState {
        public static final Parcelable.Creator<BaseSavedState> CREATOR = new Parcelable.Creator<BaseSavedState>() { // from class: android.support.v7.preference.Preference.BaseSavedState.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public BaseSavedState createFromParcel(Parcel in) {
                return new BaseSavedState(in);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public BaseSavedState[] newArray(int size) {
                return new BaseSavedState[size];
            }
        };

        public BaseSavedState(Parcel source) {
            super(source);
        }

        public BaseSavedState(Parcelable superState) {
            super(superState);
        }
    }
}
