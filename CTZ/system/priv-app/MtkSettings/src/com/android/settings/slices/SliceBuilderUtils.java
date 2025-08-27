package com.android.settings.slices;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v4.util.Consumer;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/* loaded from: classes.dex */
public class SliceBuilderUtils {
    public static Slice buildSlice(Context context, SliceData sliceData) {
        Log.d("SliceBuilder", "Creating slice for: " + sliceData.getPreferenceController());
        BasePreferenceController preferenceController = getPreferenceController(context, sliceData);
        FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context, 1371, Pair.create(854, sliceData.getKey()));
        if (!preferenceController.isAvailable()) {
            return null;
        }
        if (preferenceController.getAvailabilityStatus() == 4) {
            return buildUnavailableSlice(context, sliceData);
        }
        switch (sliceData.getSliceType()) {
            case 0:
                return buildIntentSlice(context, sliceData, preferenceController);
            case 1:
                return buildToggleSlice(context, sliceData, preferenceController);
            case 2:
                return buildSliderSlice(context, sliceData, preferenceController);
            default:
                throw new IllegalArgumentException("Slice type passed was invalid: " + sliceData.getSliceType());
        }
    }

    public static int getSliceType(Context context, String str, String str2) {
        return getPreferenceController(context, str, str2).getSliceType();
    }

    public static Pair<Boolean, String> getPathData(Uri uri) {
        String[] strArrSplit = uri.getPath().split("/", 3);
        if (strArrSplit.length != 3) {
            return null;
        }
        return new Pair<>(Boolean.valueOf(TextUtils.equals("intent", strArrSplit[1])), strArrSplit[2]);
    }

    public static BasePreferenceController getPreferenceController(Context context, SliceData sliceData) {
        return getPreferenceController(context, sliceData.getPreferenceController(), sliceData.getKey());
    }

    public static PendingIntent getActionIntent(Context context, String str, SliceData sliceData) {
        Intent intent = new Intent(str);
        intent.setClass(context, SliceBroadcastReceiver.class);
        intent.putExtra("com.android.settings.slice.extra.key", sliceData.getKey());
        intent.putExtra("com.android.settings.slice.extra.platform", sliceData.isPlatformDefined());
        return PendingIntent.getBroadcast(context, 0, intent, 268435456);
    }

    public static PendingIntent getContentPendingIntent(Context context, SliceData sliceData) {
        return PendingIntent.getActivity(context, 0, getContentIntent(context, sliceData), 0);
    }

    public static CharSequence getSubtitleText(Context context, AbstractPreferenceController abstractPreferenceController, SliceData sliceData) {
        CharSequence screenTitle = sliceData.getScreenTitle();
        if (isValidSummary(context, screenTitle) && !TextUtils.equals(screenTitle, sliceData.getTitle())) {
            return screenTitle;
        }
        if (abstractPreferenceController != null) {
            CharSequence summary = abstractPreferenceController.getSummary();
            if (isValidSummary(context, summary)) {
                return summary;
            }
        }
        String summary2 = sliceData.getSummary();
        if (isValidSummary(context, summary2)) {
            return summary2;
        }
        return "";
    }

    public static Uri getUri(String str, boolean z) {
        String str2;
        if (z) {
            str2 = "android.settings.slices";
        } else {
            str2 = "com.android.settings.slices";
        }
        return new Uri.Builder().scheme("content").authority(str2).appendPath(str).build();
    }

    @VisibleForTesting
    static Intent getContentIntent(Context context, SliceData sliceData) {
        Uri uriBuild = new Uri.Builder().appendPath(sliceData.getKey()).build();
        Intent intentBuildSearchResultPageIntent = DatabaseIndexingUtils.buildSearchResultPageIntent(context, sliceData.getFragmentClassName(), sliceData.getKey(), sliceData.getScreenTitle().toString(), 0);
        intentBuildSearchResultPageIntent.setClassName(context.getPackageName(), SubSettings.class.getName());
        intentBuildSearchResultPageIntent.setData(uriBuild);
        return intentBuildSearchResultPageIntent;
    }

    private static Slice buildToggleSlice(Context context, final SliceData sliceData, BasePreferenceController basePreferenceController) {
        final PendingIntent contentPendingIntent = getContentPendingIntent(context, sliceData);
        final IconCompat iconCompatCreateWithResource = IconCompat.createWithResource(context, sliceData.getIconResource());
        final CharSequence subtitleText = getSubtitleText(context, basePreferenceController, sliceData);
        int colorAccent = Utils.getColorAccent(context);
        final SliceAction toggleAction = getToggleAction(context, sliceData, ((TogglePreferenceController) basePreferenceController).isChecked());
        return new ListBuilder(context, sliceData.getUri(), -1L).setAccentColor(colorAccent).addRow(new Consumer() { // from class: com.android.settings.slices.-$$Lambda$SliceBuilderUtils$-H4Orhnw7bHLhjHmJgSvCr6cWP8
            @Override // android.support.v4.util.Consumer
            public final void accept(Object obj) {
                SliceData sliceData2 = sliceData;
                ((ListBuilder.RowBuilder) obj).setTitle(sliceData2.getTitle()).setSubtitle(subtitleText).setPrimaryAction(new SliceAction(contentPendingIntent, iconCompatCreateWithResource, sliceData2.getTitle())).addEndItem(toggleAction);
            }
        }).setKeywords(buildSliceKeywords(sliceData)).build();
    }

    private static Slice buildIntentSlice(Context context, final SliceData sliceData, BasePreferenceController basePreferenceController) {
        final PendingIntent contentPendingIntent = getContentPendingIntent(context, sliceData);
        final IconCompat iconCompatCreateWithResource = IconCompat.createWithResource(context, sliceData.getIconResource());
        final CharSequence subtitleText = getSubtitleText(context, basePreferenceController, sliceData);
        int colorAccent = Utils.getColorAccent(context);
        return new ListBuilder(context, sliceData.getUri(), -1L).setAccentColor(colorAccent).addRow(new Consumer() { // from class: com.android.settings.slices.-$$Lambda$SliceBuilderUtils$NVqOOBEdIdirrSxUZFgCZXRQ1vA
            @Override // android.support.v4.util.Consumer
            public final void accept(Object obj) {
                SliceData sliceData2 = sliceData;
                ((ListBuilder.RowBuilder) obj).setTitle(sliceData2.getTitle()).setSubtitle(subtitleText).setPrimaryAction(new SliceAction(contentPendingIntent, iconCompatCreateWithResource, sliceData2.getTitle()));
            }
        }).setKeywords(buildSliceKeywords(sliceData)).build();
    }

    private static Slice buildSliderSlice(Context context, final SliceData sliceData, BasePreferenceController basePreferenceController) {
        final SliderPreferenceController sliderPreferenceController = (SliderPreferenceController) basePreferenceController;
        final PendingIntent sliderAction = getSliderAction(context, sliceData);
        PendingIntent contentPendingIntent = getContentPendingIntent(context, sliceData);
        IconCompat iconCompatCreateWithResource = IconCompat.createWithResource(context, sliceData.getIconResource());
        final CharSequence subtitleText = getSubtitleText(context, basePreferenceController, sliceData);
        int colorAccent = Utils.getColorAccent(context);
        final SliceAction sliceAction = new SliceAction(contentPendingIntent, iconCompatCreateWithResource, sliceData.getTitle());
        return new ListBuilder(context, sliceData.getUri(), -1L).setAccentColor(colorAccent).addInputRange(new Consumer() { // from class: com.android.settings.slices.-$$Lambda$SliceBuilderUtils$qRPBF1K1kbSIREThP22FAM_L1N0
            @Override // android.support.v4.util.Consumer
            public final void accept(Object obj) {
                SliceData sliceData2 = sliceData;
                CharSequence charSequence = subtitleText;
                SliceAction sliceAction2 = sliceAction;
                SliderPreferenceController sliderPreferenceController2 = sliderPreferenceController;
                ((ListBuilder.InputRangeBuilder) obj).setTitle(sliceData2.getTitle()).setSubtitle(charSequence).setPrimaryAction(sliceAction2).setMax(sliderPreferenceController2.getMaxSteps()).setValue(sliderPreferenceController2.getSliderPosition()).setInputAction(sliderAction);
            }
        }).setKeywords(buildSliceKeywords(sliceData)).build();
    }

    private static BasePreferenceController getPreferenceController(Context context, String str, String str2) {
        try {
            return BasePreferenceController.createInstance(context, str);
        } catch (IllegalStateException e) {
            return BasePreferenceController.createInstance(context, str, str2);
        }
    }

    private static SliceAction getToggleAction(Context context, SliceData sliceData, boolean z) {
        return new SliceAction(getActionIntent(context, "com.android.settings.slice.action.TOGGLE_CHANGED", sliceData), (CharSequence) null, z);
    }

    private static PendingIntent getSliderAction(Context context, SliceData sliceData) {
        return getActionIntent(context, "com.android.settings.slice.action.SLIDER_CHANGED", sliceData);
    }

    private static boolean isValidSummary(Context context, CharSequence charSequence) {
        if (charSequence == null || TextUtils.isEmpty(charSequence.toString().trim())) {
            return false;
        }
        return (TextUtils.equals(charSequence, context.getText(R.string.summary_placeholder)) || TextUtils.equals(charSequence, context.getText(R.string.summary_two_lines_placeholder))) ? false : true;
    }

    private static List<String> buildSliceKeywords(SliceData sliceData) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(sliceData.getTitle());
        if (!TextUtils.equals(sliceData.getTitle(), sliceData.getScreenTitle())) {
            arrayList.add(sliceData.getScreenTitle().toString());
        }
        String keywords = sliceData.getKeywords();
        if (keywords != null) {
            arrayList.addAll((List) Arrays.stream(keywords.split(",")).map(new Function() { // from class: com.android.settings.slices.-$$Lambda$SliceBuilderUtils$H4nQFDLpU9w8T-x-9Cq8nlH2grw
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ((String) obj).trim();
                }
            }).collect(Collectors.toList()));
        }
        return arrayList;
    }

    private static Slice buildUnavailableSlice(Context context, SliceData sliceData) {
        final String title = sliceData.getTitle();
        List<String> listBuildSliceKeywords = buildSliceKeywords(sliceData);
        int colorAccent = Utils.getColorAccent(context);
        final CharSequence text = context.getText(R.string.disabled_dependent_setting_summary);
        final IconCompat iconCompatCreateWithResource = IconCompat.createWithResource(context, sliceData.getIconResource());
        final SliceAction sliceAction = new SliceAction(getContentPendingIntent(context, sliceData), iconCompatCreateWithResource, title);
        return new ListBuilder(context, sliceData.getUri(), -1L).setAccentColor(colorAccent).addRow(new Consumer() { // from class: com.android.settings.slices.-$$Lambda$SliceBuilderUtils$JGXESizo03yh-FrnCdjYorH4I8Y
            @Override // android.support.v4.util.Consumer
            public final void accept(Object obj) {
                String str = title;
                IconCompat iconCompat = iconCompatCreateWithResource;
                ((ListBuilder.RowBuilder) obj).setTitle(str).setTitleItem(iconCompat).setSubtitle(text).setPrimaryAction(sliceAction);
            }
        }).setKeywords(listBuildSliceKeywords).build();
    }
}
