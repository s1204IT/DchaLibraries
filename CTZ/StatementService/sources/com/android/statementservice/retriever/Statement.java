package com.android.statementservice.retriever;

public final class Statement {
    private final Relation mRelation;
    private final AbstractAsset mSource;
    private final AbstractAsset mTarget;

    private Statement(AbstractAsset abstractAsset, AbstractAsset abstractAsset2, Relation relation) {
        this.mSource = abstractAsset;
        this.mTarget = abstractAsset2;
        this.mRelation = relation;
    }

    public AbstractAsset getTarget() {
        return this.mTarget;
    }

    public Relation getRelation() {
        return this.mRelation;
    }

    public static Statement create(AbstractAsset abstractAsset, AbstractAsset abstractAsset2, Relation relation) {
        return new Statement(abstractAsset, abstractAsset2, relation);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Statement statement = (Statement) obj;
        if (this.mRelation.equals(statement.mRelation) && this.mTarget.equals(statement.mTarget) && this.mSource.equals(statement.mSource)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((this.mTarget.hashCode() * 31) + this.mRelation.hashCode())) + this.mSource.hashCode();
    }

    public String toString() {
        return "Statement: " + this.mSource + ", " + this.mTarget + ", " + this.mRelation;
    }
}
