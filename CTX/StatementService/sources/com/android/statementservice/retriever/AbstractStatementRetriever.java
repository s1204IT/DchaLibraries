package com.android.statementservice.retriever;

import android.content.Context;
import java.util.List;

public abstract class AbstractStatementRetriever {

    public interface Result {
        List<Statement> getStatements();
    }

    public abstract Result retrieveStatements(AbstractAsset abstractAsset) throws AssociationServiceException;

    public static AbstractStatementRetriever createDirectRetriever(Context context) {
        return new DirectStatementRetriever(new URLFetcher(), new AndroidPackageInfoFetcher(context));
    }
}
