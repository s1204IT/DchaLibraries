package com.mediatek.keyguard.ext;

import android.content.Context;

public class DefaultCarrierTextExt implements ICarrierTextExt {
    @Override
    public CharSequence customizeCarrierTextCapital(CharSequence charSequence) {
        if (charSequence != null) {
            return charSequence.toString().toUpperCase();
        }
        return null;
    }

    @Override
    public CharSequence customizeCarrierText(CharSequence charSequence, CharSequence charSequence2, int i) {
        return charSequence;
    }

    @Override
    public boolean showCarrierTextWhenSimMissing(boolean z, int i) {
        return z;
    }

    @Override
    public CharSequence customizeCarrierTextWhenCardTypeLocked(CharSequence charSequence, Context context, int i, boolean z) {
        return charSequence;
    }

    @Override
    public CharSequence customizeCarrierTextWhenSimMissing(CharSequence charSequence) {
        return charSequence;
    }

    @Override
    public String customizeCarrierTextDivider(String str) {
        return str;
    }
}
