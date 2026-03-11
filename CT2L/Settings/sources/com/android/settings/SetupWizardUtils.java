package com.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Window;
import android.widget.TextView;
import com.android.settings.widget.SetupWizardIllustration;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;

public class SetupWizardUtils {
    public static boolean isUsingWizardManager(Activity activity) {
        return activity.getIntent().hasExtra("scriptUri");
    }

    public static void sendResultsToSetupWizard(Activity activity, int resultCode) {
        Intent intent = activity.getIntent();
        Intent nextIntent = new Intent("com.android.wizard.NEXT");
        nextIntent.putExtra("scriptUri", intent.getStringExtra("scriptUri"));
        nextIntent.putExtra("actionId", intent.getStringExtra("actionId"));
        nextIntent.putExtra("theme", intent.getStringExtra("theme"));
        nextIntent.putExtra("com.android.setupwizard.ResultCode", resultCode);
        activity.startActivityForResult(nextIntent, 10000);
    }

    public static int getTheme(Intent intent, int defaultResId) {
        String themeName = intent.getStringExtra("theme");
        if ("holo_light".equalsIgnoreCase(themeName) || "material_light".equalsIgnoreCase(themeName)) {
            return R.style.SetupWizardTheme_Light;
        }
        if (!"holo".equalsIgnoreCase(themeName) && !"material".equalsIgnoreCase(themeName)) {
            return defaultResId;
        }
        return R.style.SetupWizardTheme;
    }

    public static void setImmersiveMode(Activity activity, SetupWizardNavBar navBar) {
        boolean useImmersiveMode = activity.getIntent().getBooleanExtra("useImmersiveMode", false);
        navBar.setUseImmersiveMode(useImmersiveMode);
        if (useImmersiveMode) {
            Window window = activity.getWindow();
            window.setNavigationBarColor(0);
            window.setStatusBarColor(0);
        }
    }

    public static TextView getHeader(Activity activity) {
        return (TextView) activity.findViewById(R.id.title);
    }

    public static void setHeaderText(Activity activity, int text) {
        getHeader(activity).setText(text);
    }

    public static void setHeaderText(Activity activity, CharSequence text) {
        getHeader(activity).setText(text);
    }

    public static void copySetupExtras(Intent fromIntent, Intent toIntent) {
        toIntent.putExtra("theme", fromIntent.getStringExtra("theme"));
        toIntent.putExtra("useImmersiveMode", fromIntent.getBooleanExtra("useImmersiveMode", false));
    }

    public static void setIllustration(Activity activity, int asset) {
        SetupWizardIllustration illustration = (SetupWizardIllustration) activity.findViewById(R.id.setup_illustration);
        if (illustration != null) {
            Drawable drawable = activity.getDrawable(R.drawable.setup_illustration);
            Drawable newIllustration = activity.getDrawable(asset);
            if (drawable instanceof LayerDrawable) {
                LayerDrawable layers = (LayerDrawable) drawable;
                Drawable oldIllustration = layers.findDrawableByLayerId(R.id.illustration_image);
                if ((newIllustration instanceof BitmapDrawable) && (oldIllustration instanceof BitmapDrawable)) {
                    int gravity = ((BitmapDrawable) oldIllustration).getGravity();
                    ((BitmapDrawable) newIllustration).setGravity(gravity);
                }
                layers.setDrawableByLayerId(R.id.illustration_image, newIllustration);
                illustration.setForeground(layers);
            }
        }
    }
}
