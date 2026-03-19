package com.android.statementservice.retriever;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class AndroidAppAsset extends AbstractAsset {
    private final List<String> mCertFingerprints;
    private final String mPackageName;

    public List<String> getCertFingerprints() {
        return Collections.unmodifiableList(this.mCertFingerprints);
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    @Override
    public String toJson() {
        AssetJsonWriter assetJsonWriter = new AssetJsonWriter();
        assetJsonWriter.writeFieldLower("namespace", "android_app");
        assetJsonWriter.writeFieldLower("package_name", this.mPackageName);
        assetJsonWriter.writeArrayUpper("sha256_cert_fingerprints", this.mCertFingerprints);
        return assetJsonWriter.closeAndGetString();
    }

    public String toString() {
        return "AndroidAppAsset: " + toJson();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AndroidAppAsset)) {
            return false;
        }
        return ((AndroidAppAsset) obj).toJson().equals(toJson());
    }

    public int hashCode() {
        return toJson().hashCode();
    }

    @Override
    public boolean followInsecureInclude() {
        return false;
    }

    public static AndroidAppAsset create(JSONObject jSONObject) throws AssociationServiceException {
        String strOptString = jSONObject.optString("package_name");
        if (strOptString.equals("")) {
            throw new AssociationServiceException(String.format("Expected %s to be set.", "package_name"));
        }
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("sha256_cert_fingerprints");
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
            throw new AssociationServiceException(String.format("Expected %s to be non-empty array.", "sha256_cert_fingerprints"));
        }
        ArrayList arrayList = new ArrayList(jSONArrayOptJSONArray.length());
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            try {
                arrayList.add(jSONArrayOptJSONArray.getString(i));
            } catch (JSONException e) {
                throw new AssociationServiceException(String.format("Expected all %s to be strings.", "sha256_cert_fingerprints"));
            }
        }
        return new AndroidAppAsset(strOptString, arrayList);
    }

    public static AndroidAppAsset create(String str, List<String> list) {
        if (str == null || str.equals("")) {
            throw new AssertionError("Expected packageName to be set.");
        }
        if (list == null || list.size() == 0) {
            throw new AssertionError("Expected certFingerprints to be set.");
        }
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().toUpperCase(Locale.US));
        }
        return new AndroidAppAsset(str, arrayList);
    }

    private AndroidAppAsset(String str, List<String> list) {
        if (str.equals("")) {
            this.mPackageName = null;
        } else {
            this.mPackageName = str;
        }
        if (list == null || list.size() == 0) {
            this.mCertFingerprints = null;
        } else {
            this.mCertFingerprints = Collections.unmodifiableList(sortAndDeDuplicate(list));
        }
    }

    private List<String> sortAndDeDuplicate(List<String> list) {
        if (list.size() <= 1) {
            return list;
        }
        ArrayList arrayList = new ArrayList(new HashSet(list));
        Collections.sort(arrayList);
        return arrayList;
    }
}
