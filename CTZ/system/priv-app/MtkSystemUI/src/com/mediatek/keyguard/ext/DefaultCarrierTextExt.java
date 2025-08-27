package com.mediatek.keyguard.ext;

import android.content.Context;

/* loaded from: classes.dex */
public class DefaultCarrierTextExt implements ICarrierTextExt {
    @Override // com.mediatek.keyguard.ext.ICarrierTextExt
    public CharSequence customizeCarrierTextCapital(CharSequence charSequence) {
        if (charSequence != null) {
            return charSequence.toString().toUpperCase();
        }
        return null;
    }

    @Override // com.mediatek.keyguard.ext.ICarrierTextExt
    public CharSequence customizeCarrierText(CharSequence charSequence, CharSequence charSequence2, int i) {
        return charSequence;
    }

    @Override // com.mediatek.keyguard.ext.ICarrierTextExt
    public boolean showCarrierTextWhenSimMissing(boolean z, int i) {
        return z;
    }

    @Override // com.mediatek.keyguard.ext.ICarrierTextExt
    public CharSequence customizeCarrierTextWhenCardTypeLocked(CharSequence charSequence, Context context, int i, boolean z) {
        return charSequence;
    }

    @Override // com.mediatek.keyguard.ext.ICarrierTextExt
    public CharSequence customizeCarrierTextWhenSimMissing(CharSequence charSequence) {
        return charSequence;
    }

    @Override // com.mediatek.keyguard.ext.ICarrierTextExt
    public String customizeCarrierTextDivider(String str) {
        return str;
    }
}
