package android.widget;

import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.TabWidget;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.List;

public class TabHost extends FrameLayout implements ViewTreeObserver.OnTouchModeChangeListener {
    private static final int TABWIDGET_LOCATION_BOTTOM = 3;
    private static final int TABWIDGET_LOCATION_LEFT = 0;
    private static final int TABWIDGET_LOCATION_RIGHT = 2;
    private static final int TABWIDGET_LOCATION_TOP = 1;
    protected int mCurrentTab;
    private View mCurrentView;
    protected LocalActivityManager mLocalActivityManager;
    private OnTabChangeListener mOnTabChangeListener;
    private FrameLayout mTabContent;
    private View.OnKeyListener mTabKeyListener;
    private int mTabLayoutId;
    private List<TabSpec> mTabSpecs;
    private TabWidget mTabWidget;

    private interface ContentStrategy {
        View getContentView();

        void tabClosed();
    }

    private interface IndicatorStrategy {
        View createIndicatorView();
    }

    public interface OnTabChangeListener {
        void onTabChanged(String str);
    }

    public interface TabContentFactory {
        View createTabContent(String str);
    }

    public TabHost(Context context) {
        super(context);
        this.mTabSpecs = new ArrayList(2);
        this.mCurrentTab = -1;
        this.mCurrentView = null;
        this.mLocalActivityManager = null;
        initTabHost();
    }

    public TabHost(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842883);
    }

    public TabHost(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TabHost(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet);
        this.mTabSpecs = new ArrayList(2);
        this.mCurrentTab = -1;
        this.mCurrentView = null;
        this.mLocalActivityManager = null;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.TabWidget, i, i2);
        this.mTabLayoutId = typedArrayObtainStyledAttributes.getResourceId(4, 0);
        typedArrayObtainStyledAttributes.recycle();
        if (this.mTabLayoutId == 0) {
            this.mTabLayoutId = R.layout.tab_indicator_holo;
        }
        initTabHost();
    }

    private void initTabHost() {
        setFocusableInTouchMode(true);
        setDescendantFocusability(262144);
        this.mCurrentTab = -1;
        this.mCurrentView = null;
    }

    public TabSpec newTabSpec(String str) {
        if (str == null) {
            throw new IllegalArgumentException("tag must be non-null");
        }
        return new TabSpec(str);
    }

    public void setup() {
        this.mTabWidget = (TabWidget) findViewById(16908307);
        if (this.mTabWidget == null) {
            throw new RuntimeException("Your TabHost must have a TabWidget whose id attribute is 'android.R.id.tabs'");
        }
        this.mTabKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (!KeyEvent.isModifierKey(i) && i != 66) {
                    switch (i) {
                        default:
                            switch (i) {
                                case 61:
                                case 62:
                                    break;
                                default:
                                    TabHost.this.mTabContent.requestFocus(2);
                                    return TabHost.this.mTabContent.dispatchKeyEvent(keyEvent);
                            }
                        case 19:
                        case 20:
                        case 21:
                        case 22:
                        case 23:
                            return false;
                    }
                }
                return false;
            }
        };
        this.mTabWidget.setTabSelectionListener(new TabWidget.OnTabSelectionChanged() {
            @Override
            public void onTabSelectionChanged(int i, boolean z) {
                TabHost.this.setCurrentTab(i);
                if (z) {
                    TabHost.this.mTabContent.requestFocus(2);
                }
            }
        });
        this.mTabContent = (FrameLayout) findViewById(16908305);
        if (this.mTabContent == null) {
            throw new RuntimeException("Your TabHost must have a FrameLayout whose id attribute is 'android.R.id.tabcontent'");
        }
    }

    @Override
    public void sendAccessibilityEventInternal(int i) {
    }

    public void setup(LocalActivityManager localActivityManager) {
        setup();
        this.mLocalActivityManager = localActivityManager;
    }

    @Override
    public void onTouchModeChanged(boolean z) {
    }

    public void addTab(TabSpec tabSpec) {
        if (tabSpec.mIndicatorStrategy == null) {
            throw new IllegalArgumentException("you must specify a way to create the tab indicator.");
        }
        if (tabSpec.mContentStrategy == null) {
            throw new IllegalArgumentException("you must specify a way to create the tab content");
        }
        View viewCreateIndicatorView = tabSpec.mIndicatorStrategy.createIndicatorView();
        viewCreateIndicatorView.setOnKeyListener(this.mTabKeyListener);
        if (tabSpec.mIndicatorStrategy instanceof ViewIndicatorStrategy) {
            this.mTabWidget.setStripEnabled(false);
        }
        this.mTabWidget.addView(viewCreateIndicatorView);
        this.mTabSpecs.add(tabSpec);
        if (this.mCurrentTab == -1) {
            setCurrentTab(0);
        }
    }

    public void clearAllTabs() {
        this.mTabWidget.removeAllViews();
        initTabHost();
        this.mTabContent.removeAllViews();
        this.mTabSpecs.clear();
        requestLayout();
        invalidate();
    }

    public TabWidget getTabWidget() {
        return this.mTabWidget;
    }

    public int getCurrentTab() {
        return this.mCurrentTab;
    }

    public String getCurrentTabTag() {
        if (this.mCurrentTab >= 0 && this.mCurrentTab < this.mTabSpecs.size()) {
            return this.mTabSpecs.get(this.mCurrentTab).getTag();
        }
        return null;
    }

    public View getCurrentTabView() {
        if (this.mCurrentTab >= 0 && this.mCurrentTab < this.mTabSpecs.size()) {
            return this.mTabWidget.getChildTabViewAt(this.mCurrentTab);
        }
        return null;
    }

    public View getCurrentView() {
        return this.mCurrentView;
    }

    public void setCurrentTabByTag(String str) {
        int size = this.mTabSpecs.size();
        for (int i = 0; i < size; i++) {
            if (this.mTabSpecs.get(i).getTag().equals(str)) {
                setCurrentTab(i);
                return;
            }
        }
    }

    public FrameLayout getTabContentView() {
        return this.mTabContent;
    }

    private int getTabWidgetLocation() {
        if (this.mTabWidget.getOrientation() != 1) {
            return this.mTabContent.getTop() < this.mTabWidget.getTop() ? 3 : 1;
        }
        return this.mTabContent.getLeft() < this.mTabWidget.getLeft() ? 2 : 0;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int i;
        int i2;
        int i3;
        boolean zDispatchKeyEvent = super.dispatchKeyEvent(keyEvent);
        if (!zDispatchKeyEvent && keyEvent.getAction() == 0 && this.mCurrentView != null && this.mCurrentView.isRootNamespace() && this.mCurrentView.hasFocus()) {
            int tabWidgetLocation = getTabWidgetLocation();
            if (tabWidgetLocation == 0) {
                i = 21;
                i2 = 17;
                i3 = 1;
            } else {
                switch (tabWidgetLocation) {
                    case 2:
                        i = 22;
                        i2 = 66;
                        i3 = 3;
                        break;
                    case 3:
                        i = 20;
                        i2 = 130;
                        i3 = 4;
                        break;
                    default:
                        i = 19;
                        i2 = 33;
                        i3 = 2;
                        break;
                }
            }
            if (keyEvent.getKeyCode() == i && this.mCurrentView.findFocus().focusSearch(i2) == null) {
                this.mTabWidget.getChildTabViewAt(this.mCurrentTab).requestFocus();
                playSoundEffect(i3);
                return true;
            }
        }
        return zDispatchKeyEvent;
    }

    @Override
    public void dispatchWindowFocusChanged(boolean z) {
        if (this.mCurrentView != null) {
            this.mCurrentView.dispatchWindowFocusChanged(z);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return TabHost.class.getName();
    }

    public void setCurrentTab(int i) {
        if (i < 0 || i >= this.mTabSpecs.size() || i == this.mCurrentTab) {
            return;
        }
        if (this.mCurrentTab != -1) {
            this.mTabSpecs.get(this.mCurrentTab).mContentStrategy.tabClosed();
        }
        this.mCurrentTab = i;
        TabSpec tabSpec = this.mTabSpecs.get(i);
        this.mTabWidget.focusCurrentTab(this.mCurrentTab);
        this.mCurrentView = tabSpec.mContentStrategy.getContentView();
        if (this.mCurrentView.getParent() == null) {
            this.mTabContent.addView(this.mCurrentView, new ViewGroup.LayoutParams(-1, -1));
        }
        if (!this.mTabWidget.hasFocus()) {
            this.mCurrentView.requestFocus();
        }
        invokeOnTabChangeListener();
    }

    public void setOnTabChangedListener(OnTabChangeListener onTabChangeListener) {
        this.mOnTabChangeListener = onTabChangeListener;
    }

    private void invokeOnTabChangeListener() {
        if (this.mOnTabChangeListener != null) {
            this.mOnTabChangeListener.onTabChanged(getCurrentTabTag());
        }
    }

    public class TabSpec {
        private ContentStrategy mContentStrategy;
        private IndicatorStrategy mIndicatorStrategy;
        private final String mTag;

        private TabSpec(String str) {
            this.mTag = str;
        }

        public TabSpec setIndicator(CharSequence charSequence) {
            this.mIndicatorStrategy = new LabelIndicatorStrategy(charSequence);
            return this;
        }

        public TabSpec setIndicator(CharSequence charSequence, Drawable drawable) {
            this.mIndicatorStrategy = new LabelAndIconIndicatorStrategy(charSequence, drawable);
            return this;
        }

        public TabSpec setIndicator(View view) {
            this.mIndicatorStrategy = new ViewIndicatorStrategy(view);
            return this;
        }

        public TabSpec setContent(int i) {
            this.mContentStrategy = new ViewIdContentStrategy(i);
            return this;
        }

        public TabSpec setContent(TabContentFactory tabContentFactory) {
            this.mContentStrategy = TabHost.this.new FactoryContentStrategy(this.mTag, tabContentFactory);
            return this;
        }

        public TabSpec setContent(Intent intent) {
            this.mContentStrategy = new IntentContentStrategy(this.mTag, intent);
            return this;
        }

        public String getTag() {
            return this.mTag;
        }
    }

    private class LabelIndicatorStrategy implements IndicatorStrategy {
        private final CharSequence mLabel;

        private LabelIndicatorStrategy(CharSequence charSequence) {
            this.mLabel = charSequence;
        }

        @Override
        public View createIndicatorView() {
            Context context = TabHost.this.getContext();
            View viewInflate = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(TabHost.this.mTabLayoutId, (ViewGroup) TabHost.this.mTabWidget, false);
            TextView textView = (TextView) viewInflate.findViewById(16908310);
            textView.setText(this.mLabel);
            if (context.getApplicationInfo().targetSdkVersion <= 4) {
                viewInflate.setBackgroundResource(R.drawable.tab_indicator_v4);
                textView.setTextColor(context.getColorStateList(R.color.tab_indicator_text_v4));
            }
            return viewInflate;
        }
    }

    private class LabelAndIconIndicatorStrategy implements IndicatorStrategy {
        private final Drawable mIcon;
        private final CharSequence mLabel;

        private LabelAndIconIndicatorStrategy(CharSequence charSequence, Drawable drawable) {
            this.mLabel = charSequence;
            this.mIcon = drawable;
        }

        @Override
        public View createIndicatorView() {
            Context context = TabHost.this.getContext();
            View viewInflate = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(TabHost.this.mTabLayoutId, (ViewGroup) TabHost.this.mTabWidget, false);
            TextView textView = (TextView) viewInflate.findViewById(16908310);
            ImageView imageView = (ImageView) viewInflate.findViewById(16908294);
            boolean z = true;
            if ((imageView.getVisibility() == 8) && !TextUtils.isEmpty(this.mLabel)) {
                z = false;
            }
            textView.setText(this.mLabel);
            if (z && this.mIcon != null) {
                imageView.setImageDrawable(this.mIcon);
                imageView.setVisibility(0);
            }
            if (context.getApplicationInfo().targetSdkVersion <= 4) {
                viewInflate.setBackgroundResource(R.drawable.tab_indicator_v4);
                textView.setTextColor(context.getColorStateList(R.color.tab_indicator_text_v4));
            }
            return viewInflate;
        }
    }

    private class ViewIndicatorStrategy implements IndicatorStrategy {
        private final View mView;

        private ViewIndicatorStrategy(View view) {
            this.mView = view;
        }

        @Override
        public View createIndicatorView() {
            return this.mView;
        }
    }

    private class ViewIdContentStrategy implements ContentStrategy {
        private final View mView;

        private ViewIdContentStrategy(int i) {
            this.mView = TabHost.this.mTabContent.findViewById(i);
            if (this.mView != null) {
                this.mView.setVisibility(8);
                return;
            }
            throw new RuntimeException("Could not create tab content because could not find view with id " + i);
        }

        @Override
        public View getContentView() {
            this.mView.setVisibility(0);
            return this.mView;
        }

        @Override
        public void tabClosed() {
            this.mView.setVisibility(8);
        }
    }

    private class FactoryContentStrategy implements ContentStrategy {
        private TabContentFactory mFactory;
        private View mTabContent;
        private final CharSequence mTag;

        public FactoryContentStrategy(CharSequence charSequence, TabContentFactory tabContentFactory) {
            this.mTag = charSequence;
            this.mFactory = tabContentFactory;
        }

        @Override
        public View getContentView() {
            if (this.mTabContent == null) {
                this.mTabContent = this.mFactory.createTabContent(this.mTag.toString());
            }
            this.mTabContent.setVisibility(0);
            return this.mTabContent;
        }

        @Override
        public void tabClosed() {
            this.mTabContent.setVisibility(8);
        }
    }

    private class IntentContentStrategy implements ContentStrategy {
        private final Intent mIntent;
        private View mLaunchedView;
        private final String mTag;

        private IntentContentStrategy(String str, Intent intent) {
            this.mTag = str;
            this.mIntent = intent;
        }

        @Override
        public View getContentView() {
            if (TabHost.this.mLocalActivityManager == null) {
                throw new IllegalStateException("Did you forget to call 'public void setup(LocalActivityManager activityGroup)'?");
            }
            Window windowStartActivity = TabHost.this.mLocalActivityManager.startActivity(this.mTag, this.mIntent);
            View decorView = windowStartActivity != null ? windowStartActivity.getDecorView() : null;
            if (this.mLaunchedView != decorView && this.mLaunchedView != null && this.mLaunchedView.getParent() != null) {
                TabHost.this.mTabContent.removeView(this.mLaunchedView);
            }
            this.mLaunchedView = decorView;
            if (this.mLaunchedView != null) {
                this.mLaunchedView.setVisibility(0);
                this.mLaunchedView.setFocusableInTouchMode(true);
                ((ViewGroup) this.mLaunchedView).setDescendantFocusability(262144);
            }
            return this.mLaunchedView;
        }

        @Override
        public void tabClosed() {
            if (this.mLaunchedView != null) {
                this.mLaunchedView.setVisibility(8);
            }
        }
    }
}
