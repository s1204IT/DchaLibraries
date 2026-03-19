package com.android.statementservice.retriever;

import java.util.HashSet;
import java.util.Iterator;

final class AndroidAppAssetMatcher extends AbstractAssetMatcher {
    private final AndroidAppAsset mQuery;

    public AndroidAppAssetMatcher(AndroidAppAsset androidAppAsset) {
        this.mQuery = androidAppAsset;
    }

    @Override
    public boolean matches(AbstractAsset abstractAsset) {
        if (abstractAsset instanceof AndroidAppAsset) {
            AndroidAppAsset androidAppAsset = (AndroidAppAsset) abstractAsset;
            if (!androidAppAsset.getPackageName().equals(this.mQuery.getPackageName())) {
                return false;
            }
            HashSet hashSet = new HashSet(this.mQuery.getCertFingerprints());
            Iterator<String> it = androidAppAsset.getCertFingerprints().iterator();
            while (it.hasNext()) {
                if (hashSet.contains(it.next())) {
                    return true;
                }
            }
        }
        return false;
    }
}
