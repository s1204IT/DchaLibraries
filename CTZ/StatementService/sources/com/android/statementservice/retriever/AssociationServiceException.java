package com.android.statementservice.retriever;

public class AssociationServiceException extends Exception {
    public AssociationServiceException(String str) {
        super(str);
    }

    public AssociationServiceException(String str, Exception exc) {
        super(str, exc);
    }
}
