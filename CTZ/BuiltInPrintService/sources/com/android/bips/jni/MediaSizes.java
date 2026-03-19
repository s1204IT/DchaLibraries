package com.android.bips.jni;

import android.annotation.SuppressLint;
import android.content.Context;
import android.print.PrintAttributes;
import com.android.bips.R;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MediaSizes {
    static final String DEFAULT_MEDIA_NAME = "iso_a4_210x297mm";
    private static final String ISO_A3 = "iso_a3_297x420mm";
    private static final String ISO_A4 = "iso_a4_210x297mm";
    private static final String ISO_A5 = "iso_a5_148x210mm";
    private static final String JIS_B4 = "jis_b4_257x364mm";
    private static final String JIS_B5 = "jis_b5_182x257mm";
    private static final String JPN_HAGAKI = "jpn_hagaki_100x148mm";
    private static final String LEGAL = "na_legal_8.5x14in";
    private static final String LETTER = "na_letter_8.5x11in";
    private static final String NA_GOVT_LETTER = "na_govt-letter_8x10in";
    private static final String NA_LEDGER_11X17 = "na_ledger_11x17in";
    private static final String OE_PHOTO_L = "oe_photo-l_3.5x5in";
    private static final String OM_CARD = "om_card_54x86mm";
    private static final String OM_DSC_PHOTO = "om_dsc-photo_89x119mm";
    private static final String PHOTO_4x6in = "na_index-4x6_4x6in";
    private static final String PHOTO_5x7 = "na_5x7_5x7in";
    private static MediaSizes sInstance;
    private final Map<String, PrintAttributes.MediaSize> mNameToSizeMap = new HashMap();

    @SuppressLint({"UseSparseArrays"})
    private static final Map<Integer, String> sCodeToStringMap = new HashMap();
    static final Collection<String> DEFAULT_MEDIA_NAMES = new ArrayList();

    static {
        DEFAULT_MEDIA_NAMES.add("iso_a4_210x297mm");
        DEFAULT_MEDIA_NAMES.add(LETTER);
        DEFAULT_MEDIA_NAMES.add(PHOTO_4x6in);
        DEFAULT_MEDIA_NAMES.add(PHOTO_5x7);
        sCodeToStringMap.put(2, LETTER);
        sCodeToStringMap.put(3, LEGAL);
        sCodeToStringMap.put(7, NA_GOVT_LETTER);
        sCodeToStringMap.put(11, NA_LEDGER_11X17);
        sCodeToStringMap.put(25, ISO_A5);
        sCodeToStringMap.put(26, "iso_a4_210x297mm");
        sCodeToStringMap.put(27, ISO_A3);
        sCodeToStringMap.put(45, JIS_B5);
        sCodeToStringMap.put(46, JIS_B4);
        sCodeToStringMap.put(71, JPN_HAGAKI);
        sCodeToStringMap.put(74, PHOTO_4x6in);
        sCodeToStringMap.put(122, PHOTO_5x7);
        sCodeToStringMap.put(302, OM_DSC_PHOTO);
        sCodeToStringMap.put(303, OM_CARD);
        sCodeToStringMap.put(304, OE_PHOTO_L);
    }

    private MediaSizes(Context context) {
        this.mNameToSizeMap.put(LETTER, PrintAttributes.MediaSize.NA_LETTER);
        this.mNameToSizeMap.put(LEGAL, PrintAttributes.MediaSize.NA_LEGAL);
        this.mNameToSizeMap.put(ISO_A3, PrintAttributes.MediaSize.ISO_A3);
        this.mNameToSizeMap.put("iso_a4_210x297mm", PrintAttributes.MediaSize.ISO_A4);
        this.mNameToSizeMap.put(ISO_A5, PrintAttributes.MediaSize.ISO_A5);
        this.mNameToSizeMap.put(JPN_HAGAKI, PrintAttributes.MediaSize.JPN_HAGAKI);
        this.mNameToSizeMap.put(JIS_B4, PrintAttributes.MediaSize.JIS_B4);
        this.mNameToSizeMap.put(JIS_B5, PrintAttributes.MediaSize.JIS_B5);
        this.mNameToSizeMap.put(NA_LEDGER_11X17, PrintAttributes.MediaSize.NA_TABLOID);
        this.mNameToSizeMap.put(PHOTO_4x6in, new PrintAttributes.MediaSize(PHOTO_4x6in, context.getString(R.string.media_size_4x6in), 4000, 6000));
        this.mNameToSizeMap.put(NA_GOVT_LETTER, new PrintAttributes.MediaSize(NA_GOVT_LETTER, context.getString(R.string.media_size_8x10in), 8000, 10000));
        this.mNameToSizeMap.put(PHOTO_5x7, new PrintAttributes.MediaSize(PHOTO_5x7, context.getString(R.string.media_size_5x7in), 5000, 7000));
        this.mNameToSizeMap.put(OM_DSC_PHOTO, new PrintAttributes.MediaSize(OM_DSC_PHOTO, context.getString(R.string.media_size_89x119mm), 3504, 4685));
        this.mNameToSizeMap.put(OM_CARD, new PrintAttributes.MediaSize(OM_CARD, context.getString(R.string.media_size_54x86mm), 2126, 3386));
        this.mNameToSizeMap.put(OE_PHOTO_L, new PrintAttributes.MediaSize(OE_PHOTO_L, context.getString(R.string.media_size_l), 3500, 5000));
    }

    public static MediaSizes getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MediaSizes(context.getApplicationContext());
        }
        return sInstance;
    }

    private static int toMediaCode(String str) {
        for (Map.Entry<Integer, String> entry : sCodeToStringMap.entrySet()) {
            if (entry.getValue().equals(str)) {
                return entry.getKey().intValue();
            }
        }
        return 0;
    }

    static String toMediaName(int i) {
        return sCodeToStringMap.get(Integer.valueOf(i));
    }

    PrintAttributes.MediaSize toMediaSize(String str) {
        return this.mNameToSizeMap.get(str);
    }

    public int toMediaCode(PrintAttributes.MediaSize mediaSize) {
        for (Map.Entry<String, PrintAttributes.MediaSize> entry : this.mNameToSizeMap.entrySet()) {
            if (entry.getValue().getId().equals(mediaSize.getId())) {
                return toMediaCode(entry.getKey());
            }
        }
        return 0;
    }
}
