package com.android.statementservice.retriever;

import android.content.pm.PackageManager;
import android.util.Log;
import com.android.statementservice.retriever.AbstractStatementRetriever;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;

final class DirectStatementRetriever extends AbstractStatementRetriever {
    private final AndroidPackageInfoFetcher mAndroidFetcher;
    private final URLFetcher mUrlFetcher;

    public static class Result implements AbstractStatementRetriever.Result {
        private final Long mExpireMillis;
        private final List<Statement> mStatements;

        @Override
        public List<Statement> getStatements() {
            return this.mStatements;
        }

        private Result(List<Statement> list, Long l) {
            this.mStatements = list;
            this.mExpireMillis = l;
        }

        public static Result create(List<Statement> list, Long l) {
            return new Result(list, l);
        }

        public String toString() {
            return "Result: " + this.mStatements.toString() + ", mExpireMillis=" + this.mExpireMillis;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Result result = (Result) obj;
            if (this.mExpireMillis.equals(result.mExpireMillis) && this.mStatements.equals(result.mStatements)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.mStatements.hashCode()) + this.mExpireMillis.hashCode();
        }
    }

    public DirectStatementRetriever(URLFetcher uRLFetcher, AndroidPackageInfoFetcher androidPackageInfoFetcher) {
        this.mUrlFetcher = uRLFetcher;
        this.mAndroidFetcher = androidPackageInfoFetcher;
    }

    @Override
    public Result retrieveStatements(AbstractAsset abstractAsset) throws AssociationServiceException {
        if (abstractAsset instanceof AndroidAppAsset) {
            return retrieveFromAndroid((AndroidAppAsset) abstractAsset);
        }
        if (abstractAsset instanceof WebAsset) {
            return retrieveFromWeb((WebAsset) abstractAsset);
        }
        throw new AssociationServiceException("Namespace is not supported.");
    }

    private String computeAssociationJsonUrl(WebAsset webAsset) {
        try {
            return new URL(webAsset.getScheme(), webAsset.getDomain(), webAsset.getPort(), "/.well-known/assetlinks.json").toExternalForm();
        } catch (MalformedURLException e) {
            throw new AssertionError("Invalid domain name in database.");
        }
    }

    private Result retrieveStatementFromUrl(String str, int i, AbstractAsset abstractAsset) throws AssociationServiceException {
        ArrayList arrayList = new ArrayList();
        if (i < 0) {
            return Result.create(arrayList, 0L);
        }
        try {
            URL url = new URL(str);
            if (!abstractAsset.followInsecureInclude() && !url.getProtocol().toLowerCase().equals("https")) {
                return Result.create(arrayList, 0L);
            }
            WebContent webContentFromUrlWithRetry = this.mUrlFetcher.getWebContentFromUrlWithRetry(url, 1048576L, 5000, 3000, 3);
            try {
                ParsedStatement statementList = StatementParser.parseStatementList(webContentFromUrlWithRetry.getContent(), abstractAsset);
                arrayList.addAll(statementList.getStatements());
                Iterator<String> it = statementList.getDelegates().iterator();
                while (it.hasNext()) {
                    arrayList.addAll(retrieveStatementFromUrl(it.next(), i - 1, abstractAsset).getStatements());
                }
                return Result.create(arrayList, webContentFromUrlWithRetry.getExpireTimeMillis());
            } catch (IOException | JSONException e) {
                return Result.create(arrayList, 0L);
            }
        } catch (IOException | InterruptedException e2) {
            return Result.create(arrayList, 0L);
        }
    }

    private Result retrieveFromWeb(WebAsset webAsset) throws AssociationServiceException {
        return retrieveStatementFromUrl(computeAssociationJsonUrl(webAsset), 1, webAsset);
    }

    private Result retrieveFromAndroid(AndroidAppAsset androidAppAsset) throws AssociationServiceException {
        try {
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            List<String> certFingerprints = this.mAndroidFetcher.getCertFingerprints(androidAppAsset.getPackageName());
            if (!Utils.hasCommonString(certFingerprints, androidAppAsset.getCertFingerprints())) {
                throw new AssociationServiceException("Specified certs don't match the installed app.");
            }
            AndroidAppAsset androidAppAssetCreate = AndroidAppAsset.create(androidAppAsset.getPackageName(), certFingerprints);
            Iterator<String> it = this.mAndroidFetcher.getStatements(androidAppAsset.getPackageName()).iterator();
            while (it.hasNext()) {
                ParsedStatement statement = StatementParser.parseStatement(it.next(), androidAppAssetCreate);
                arrayList2.addAll(statement.getStatements());
                arrayList.addAll(statement.getDelegates());
            }
            Iterator it2 = arrayList.iterator();
            while (it2.hasNext()) {
                arrayList2.addAll(retrieveStatementFromUrl((String) it2.next(), 1, androidAppAssetCreate).getStatements());
            }
            return Result.create(arrayList2, 0L);
        } catch (PackageManager.NameNotFoundException | IOException | JSONException e) {
            Log.w(DirectStatementRetriever.class.getSimpleName(), e);
            return Result.create(Collections.emptyList(), 0L);
        }
    }
}
