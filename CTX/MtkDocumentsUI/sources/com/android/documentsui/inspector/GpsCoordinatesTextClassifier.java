package com.android.documentsui.inspector;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.LocaleList;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextSelection;
import java.util.regex.Pattern;

final class GpsCoordinatesTextClassifier implements TextClassifier {
    static final boolean $assertionsDisabled = false;
    private static final Pattern sGeoPattern = Pattern.compile("-?(90(\\.0*)?|[1-8]?[0-9](\\.[0-9]*)?), *-?(180(\\.0*)?|([1][0-7][0-9]|[0-9]?[0-9])(\\.[0-9]*)?)");
    private final PackageManager mPackageManager;
    private final TextClassifier mSystemClassifier;

    public GpsCoordinatesTextClassifier(PackageManager packageManager, TextClassifier textClassifier) {
        this.mPackageManager = packageManager;
        this.mSystemClassifier = textClassifier;
    }

    public static GpsCoordinatesTextClassifier create(Context context) {
        return new GpsCoordinatesTextClassifier(context.getPackageManager(), ((TextClassificationManager) context.getSystemService(TextClassificationManager.class)).getTextClassifier());
    }

    @Override
    public TextClassification classifyText(CharSequence charSequence, int i, int i2, LocaleList localeList) {
        ResolveInfo resolveInfoResolveActivity;
        CharSequence charSequenceSubSequence = charSequence.subSequence(i, i2);
        if (isGeoSequence(charSequenceSubSequence)) {
            Intent data = new Intent("android.intent.action.VIEW").setData(Uri.parse(String.format("geo:0,0?q=%s", charSequenceSubSequence)));
            if (BenesseExtension.getDchaState() == 0 && (resolveInfoResolveActivity = this.mPackageManager.resolveActivity(data, 0)) != null) {
                return new TextClassification.Builder().setText(charSequenceSubSequence.toString()).setEntityType("address", 1.0f).setIcon(resolveInfoResolveActivity.loadIcon(this.mPackageManager)).setLabel(resolveInfoResolveActivity.loadLabel(this.mPackageManager).toString()).setIntent(data).build();
            }
        }
        return this.mSystemClassifier.classifyText(charSequence, i, i2, localeList);
    }

    @Override
    public TextSelection suggestSelection(CharSequence charSequence, int i, int i2, LocaleList localeList) {
        int[] iArr = {0, charSequence.length()};
        return new TextSelection.Builder(iArr[0], iArr[1]).setEntityType("address", 1.0f).build();
    }

    private static boolean isGeoSequence(CharSequence charSequence) {
        return sGeoPattern.matcher(charSequence).matches();
    }
}
