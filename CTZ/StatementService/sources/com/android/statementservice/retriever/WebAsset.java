package com.android.statementservice.retriever;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import org.json.JSONObject;

final class WebAsset extends AbstractAsset {
    private final URL mUrl;

    private WebAsset(URL url) {
        try {
            this.mUrl = new URL(url.getProtocol().toLowerCase(), url.getHost().toLowerCase(), url.getPort() != -1 ? url.getPort() : url.getDefaultPort(), "");
        } catch (MalformedURLException e) {
            throw new AssertionError("Url should always be validated before calling the constructor.");
        }
    }

    public String getDomain() {
        return this.mUrl.getHost();
    }

    public String getScheme() {
        return this.mUrl.getProtocol();
    }

    public int getPort() {
        return this.mUrl.getPort();
    }

    @Override
    public String toJson() {
        AssetJsonWriter assetJsonWriter = new AssetJsonWriter();
        assetJsonWriter.writeFieldLower("namespace", "web");
        assetJsonWriter.writeFieldLower("site", this.mUrl.toExternalForm());
        return assetJsonWriter.closeAndGetString();
    }

    public String toString() {
        return "WebAsset: " + toJson();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof WebAsset)) {
            return false;
        }
        return ((WebAsset) obj).toJson().equals(toJson());
    }

    public int hashCode() {
        return toJson().hashCode();
    }

    @Override
    public boolean followInsecureInclude() {
        return "http".equals(getScheme());
    }

    protected static WebAsset create(JSONObject jSONObject) throws AssociationServiceException {
        if (jSONObject.optString("site").equals("")) {
            throw new AssociationServiceException(String.format("Expected %s to be set.", "site"));
        }
        try {
            URL url = new URL(jSONObject.optString("site"));
            String lowerCase = url.getProtocol().toLowerCase(Locale.US);
            if (!lowerCase.equals("https") && !lowerCase.equals("http")) {
                throw new AssociationServiceException("Expected scheme to be http or https.");
            }
            if (url.getUserInfo() != null) {
                throw new AssociationServiceException("The url should not contain user info.");
            }
            String file = url.getFile();
            if (!file.equals("/") && !file.equals("")) {
                throw new AssociationServiceException("Site should only have scheme, domain, and port.");
            }
            return new WebAsset(url);
        } catch (MalformedURLException e) {
            throw new AssociationServiceException("Url is not well formatted.", e);
        }
    }
}
