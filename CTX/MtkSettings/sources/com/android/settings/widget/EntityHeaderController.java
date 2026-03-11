package com.android.settings.widget;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class EntityHeaderController {
    private int mAction1;
    private int mAction2;
    private final Activity mActivity;
    private final Context mAppContext;
    private Intent mAppNotifPrefIntent;
    private View.OnClickListener mEditRuleNameOnClickListener;
    private final Fragment mFragment;
    private boolean mHasAppInfoLink;
    private final View mHeader;
    private Drawable mIcon;
    private String mIconContentDescription;
    private boolean mIsInstantApp;
    private CharSequence mLabel;
    private Lifecycle mLifecycle;
    private final int mMetricsCategory;
    private String mPackageName;
    private RecyclerView mRecyclerView;
    private CharSequence mSecondSummary;
    private CharSequence mSummary;
    private int mUid = -10000;

    public static EntityHeaderController newInstance(Activity activity, Fragment fragment, View view) {
        return new EntityHeaderController(activity, fragment, view);
    }

    private EntityHeaderController(Activity activity, Fragment fragment, View view) {
        this.mActivity = activity;
        this.mAppContext = activity.getApplicationContext();
        this.mFragment = fragment;
        this.mMetricsCategory = FeatureFactory.getFactory(this.mAppContext).getMetricsFeatureProvider().getMetricsCategory(fragment);
        if (view != null) {
            this.mHeader = view;
        } else {
            this.mHeader = LayoutInflater.from(fragment.getContext()).inflate(R.layout.settings_entity_header, (ViewGroup) null);
        }
    }

    public EntityHeaderController setRecyclerView(RecyclerView recyclerView, Lifecycle lifecycle) {
        this.mRecyclerView = recyclerView;
        this.mLifecycle = lifecycle;
        return this;
    }

    public EntityHeaderController setIcon(Drawable drawable) {
        if (drawable != null) {
            this.mIcon = drawable.getConstantState().newDrawable(this.mAppContext.getResources());
        }
        return this;
    }

    public EntityHeaderController setIcon(ApplicationsState.AppEntry appEntry) {
        this.mIcon = IconDrawableFactory.newInstance(this.mAppContext).getBadgedIcon(appEntry.info);
        return this;
    }

    public EntityHeaderController setIconContentDescription(String str) {
        this.mIconContentDescription = str;
        return this;
    }

    public EntityHeaderController setLabel(CharSequence charSequence) {
        this.mLabel = charSequence;
        return this;
    }

    public EntityHeaderController setLabel(ApplicationsState.AppEntry appEntry) {
        this.mLabel = appEntry.label;
        return this;
    }

    public EntityHeaderController setSummary(CharSequence charSequence) {
        this.mSummary = charSequence;
        return this;
    }

    public EntityHeaderController setSummary(PackageInfo packageInfo) {
        if (packageInfo != null) {
            this.mSummary = packageInfo.versionName;
        }
        return this;
    }

    public EntityHeaderController setSecondSummary(CharSequence charSequence) {
        this.mSecondSummary = charSequence;
        return this;
    }

    public EntityHeaderController setHasAppInfoLink(boolean z) {
        this.mHasAppInfoLink = z;
        return this;
    }

    public EntityHeaderController setButtonActions(int i, int i2) {
        this.mAction1 = i;
        this.mAction2 = i2;
        return this;
    }

    public EntityHeaderController setPackageName(String str) {
        this.mPackageName = str;
        return this;
    }

    public EntityHeaderController setUid(int i) {
        this.mUid = i;
        return this;
    }

    public EntityHeaderController setIsInstantApp(boolean z) {
        this.mIsInstantApp = z;
        return this;
    }

    public EntityHeaderController setEditZenRuleNameListener(View.OnClickListener onClickListener) {
        this.mEditRuleNameOnClickListener = onClickListener;
        return this;
    }

    public LayoutPreference done(Activity activity, Context context) {
        LayoutPreference layoutPreference = new LayoutPreference(context, done(activity));
        layoutPreference.setOrder(-1000);
        layoutPreference.setSelectable(false);
        layoutPreference.setKey("pref_app_header");
        return layoutPreference;
    }

    public View done(Activity activity, boolean z) {
        styleActionBar(activity);
        ImageView imageView = (ImageView) this.mHeader.findViewById(R.id.entity_header_icon);
        if (imageView != null) {
            imageView.setImageDrawable(this.mIcon);
            imageView.setContentDescription(this.mIconContentDescription);
        }
        setText(R.id.entity_header_title, this.mLabel);
        setText(R.id.entity_header_summary, this.mSummary);
        setText(R.id.entity_header_second_summary, this.mSecondSummary);
        if (this.mIsInstantApp) {
            setText(R.id.install_type, this.mHeader.getResources().getString(R.string.install_type_instant));
        }
        if (z) {
            bindHeaderButtons();
        }
        return this.mHeader;
    }

    public EntityHeaderController bindHeaderButtons() {
        View viewFindViewById = this.mHeader.findViewById(R.id.entity_header_content);
        ImageButton imageButton = (ImageButton) this.mHeader.findViewById(android.R.id.button1);
        ImageButton imageButton2 = (ImageButton) this.mHeader.findViewById(android.R.id.button2);
        bindAppInfoLink(viewFindViewById);
        bindButton(imageButton, this.mAction1);
        bindButton(imageButton2, this.mAction2);
        return this;
    }

    private void bindAppInfoLink(View view) {
        if (!this.mHasAppInfoLink) {
            return;
        }
        if (view == null || this.mPackageName == null || this.mPackageName.equals("os") || this.mUid == -10000) {
            Log.w("AppDetailFeature", "Missing ingredients to build app info link, skip");
        } else {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    AppInfoBase.startAppInfoFragment(AppInfoDashboardFragment.class, R.string.application_info_label, EntityHeaderController.this.mPackageName, EntityHeaderController.this.mUid, EntityHeaderController.this.mFragment, 0, EntityHeaderController.this.mMetricsCategory);
                }
            });
        }
    }

    public EntityHeaderController styleActionBar(Activity activity) {
        if (activity == null) {
            Log.w("AppDetailFeature", "No activity, cannot style actionbar.");
            return this;
        }
        ActionBar actionBar = activity.getActionBar();
        if (actionBar == null) {
            Log.w("AppDetailFeature", "No actionbar, cannot style actionbar.");
            return this;
        }
        actionBar.setBackgroundDrawable(new ColorDrawable(Utils.getColorAttr(activity, android.R.attr.colorPrimary)));
        actionBar.setElevation(0.0f);
        if (this.mRecyclerView != null && this.mLifecycle != null) {
            ActionBarShadowController.attachToRecyclerView(this.mActivity, this.mLifecycle, this.mRecyclerView);
        }
        return this;
    }

    View done(Activity activity) {
        return done(activity, true);
    }

    private void bindButton(ImageButton imageButton, int i) {
        if (imageButton == null) {
        }
        switch (i) {
            case 0:
                imageButton.setVisibility(8);
                break;
            case 1:
                if (this.mAppNotifPrefIntent == null) {
                    imageButton.setVisibility(8);
                } else {
                    imageButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            FeatureFactory.getFactory(EntityHeaderController.this.mAppContext).getMetricsFeatureProvider().actionWithSource(EntityHeaderController.this.mAppContext, EntityHeaderController.this.mMetricsCategory, 1016);
                            EntityHeaderController.this.mFragment.startActivity(EntityHeaderController.this.mAppNotifPrefIntent);
                        }
                    });
                    imageButton.setVisibility(0);
                }
                break;
            case 2:
                if (this.mEditRuleNameOnClickListener == null) {
                    imageButton.setVisibility(8);
                } else {
                    imageButton.setImageResource(R.drawable.ic_mode_edit);
                    imageButton.setVisibility(0);
                    imageButton.setOnClickListener(this.mEditRuleNameOnClickListener);
                }
                break;
        }
    }

    private void setText(int i, CharSequence charSequence) {
        TextView textView = (TextView) this.mHeader.findViewById(i);
        if (textView != null) {
            textView.setText(charSequence);
            textView.setVisibility(TextUtils.isEmpty(charSequence) ? 8 : 0);
        }
    }
}
