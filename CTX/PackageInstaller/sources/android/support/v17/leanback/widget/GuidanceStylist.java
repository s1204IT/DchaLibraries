package android.support.v17.leanback.widget;

import android.animation.Animator;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.R;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class GuidanceStylist {
    private TextView mBreadcrumbView;
    private TextView mDescriptionView;
    private View mGuidanceContainer;
    private ImageView mIconView;
    private TextView mTitleView;

    public static class Guidance {
        private final String mBreadcrumb;
        private final String mDescription;
        private final Drawable mIconDrawable;
        private final String mTitle;

        public Guidance(String title, String description, String breadcrumb, Drawable icon) {
            this.mBreadcrumb = breadcrumb;
            this.mTitle = title;
            this.mDescription = description;
            this.mIconDrawable = icon;
        }

        public String getTitle() {
            return this.mTitle;
        }

        public String getDescription() {
            return this.mDescription;
        }

        public String getBreadcrumb() {
            return this.mBreadcrumb;
        }

        public Drawable getIconDrawable() {
            return this.mIconDrawable;
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Guidance guidance) {
        View guidanceView = inflater.inflate(onProvideLayoutId(), container, false);
        this.mTitleView = (TextView) guidanceView.findViewById(R.id.guidance_title);
        this.mBreadcrumbView = (TextView) guidanceView.findViewById(R.id.guidance_breadcrumb);
        this.mDescriptionView = (TextView) guidanceView.findViewById(R.id.guidance_description);
        this.mIconView = (ImageView) guidanceView.findViewById(R.id.guidance_icon);
        this.mGuidanceContainer = guidanceView.findViewById(R.id.guidance_container);
        if (this.mTitleView != null) {
            this.mTitleView.setText(guidance.getTitle());
        }
        if (this.mBreadcrumbView != null) {
            this.mBreadcrumbView.setText(guidance.getBreadcrumb());
        }
        if (this.mDescriptionView != null) {
            this.mDescriptionView.setText(guidance.getDescription());
        }
        if (this.mIconView != null) {
            if (guidance.getIconDrawable() != null) {
                this.mIconView.setImageDrawable(guidance.getIconDrawable());
            } else {
                this.mIconView.setVisibility(8);
            }
        }
        if (this.mGuidanceContainer != null) {
            CharSequence contentDescription = this.mGuidanceContainer.getContentDescription();
            if (TextUtils.isEmpty(contentDescription)) {
                StringBuilder builder = new StringBuilder();
                if (!TextUtils.isEmpty(guidance.getBreadcrumb())) {
                    builder.append(guidance.getBreadcrumb());
                    builder.append('\n');
                }
                if (!TextUtils.isEmpty(guidance.getTitle())) {
                    builder.append(guidance.getTitle());
                    builder.append('\n');
                }
                if (!TextUtils.isEmpty(guidance.getDescription())) {
                    builder.append(guidance.getDescription());
                    builder.append('\n');
                }
                this.mGuidanceContainer.setContentDescription(builder);
            }
        }
        return guidanceView;
    }

    public void onDestroyView() {
        this.mBreadcrumbView = null;
        this.mDescriptionView = null;
        this.mIconView = null;
        this.mTitleView = null;
    }

    public int onProvideLayoutId() {
        return R.layout.lb_guidance;
    }

    public void onImeAppearing(List<Animator> animators) {
    }

    public void onImeDisappearing(List<Animator> animators) {
    }
}
