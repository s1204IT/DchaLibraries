package com.android.statementservice.retriever;

import java.util.List;

final class ParsedStatement {
    private final List<String> mDelegates;
    private final List<Statement> mStatements;

    public ParsedStatement(List<Statement> list, List<String> list2) {
        this.mStatements = list;
        this.mDelegates = list2;
    }

    public List<Statement> getStatements() {
        return this.mStatements;
    }

    public List<String> getDelegates() {
        return this.mDelegates;
    }
}
