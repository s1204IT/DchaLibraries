package org.apache.xpath.axes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.OpMap;

public class UnionPathIterator extends LocPathIterator implements Cloneable, DTMIterator, Serializable, PathComponent {
    static final long serialVersionUID = -3910351546843826781L;
    protected LocPathIterator[] m_exprs;
    protected DTMIterator[] m_iterators;

    public UnionPathIterator() {
        this.m_iterators = null;
        this.m_exprs = null;
    }

    @Override
    public void setRoot(int i, Object obj) {
        super.setRoot(i, obj);
        try {
            if (this.m_exprs != null) {
                int length = this.m_exprs.length;
                DTMIterator[] dTMIteratorArr = new DTMIterator[length];
                for (int i2 = 0; i2 < length; i2++) {
                    DTMIterator dTMIteratorAsIterator = this.m_exprs[i2].asIterator(this.m_execContext, i);
                    dTMIteratorArr[i2] = dTMIteratorAsIterator;
                    dTMIteratorAsIterator.nextNode();
                }
                this.m_iterators = dTMIteratorArr;
            }
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public void addIterator(DTMIterator dTMIterator) {
        if (this.m_iterators == null) {
            this.m_iterators = new DTMIterator[1];
            this.m_iterators[0] = dTMIterator;
        } else {
            DTMIterator[] dTMIteratorArr = this.m_iterators;
            int length = this.m_iterators.length;
            this.m_iterators = new DTMIterator[length + 1];
            System.arraycopy(dTMIteratorArr, 0, this.m_iterators, 0, length);
            this.m_iterators[length] = dTMIterator;
        }
        dTMIterator.nextNode();
        if (dTMIterator instanceof Expression) {
            ((Expression) dTMIterator).exprSetParent(this);
        }
    }

    @Override
    public void detach() {
        if (this.m_allowDetach && this.m_iterators != null) {
            int length = this.m_iterators.length;
            for (int i = 0; i < length; i++) {
                this.m_iterators[i].detach();
            }
            this.m_iterators = null;
        }
    }

    public UnionPathIterator(Compiler compiler, int i) throws TransformerException {
        loadLocationPaths(compiler, OpMap.getFirstChildPos(i), 0);
    }

    public static LocPathIterator createUnionIterator(Compiler compiler, int i) throws TransformerException {
        boolean z;
        UnionPathIterator unionPathIterator = new UnionPathIterator(compiler, i);
        int length = unionPathIterator.m_exprs.length;
        for (int i2 = 0; i2 < length; i2++) {
            LocPathIterator locPathIterator = unionPathIterator.m_exprs[i2];
            if (locPathIterator.getAxis() != 3 || HasPositionalPredChecker.check(locPathIterator)) {
                z = false;
                break;
            }
        }
        z = true;
        if (z) {
            UnionChildIterator unionChildIterator = new UnionChildIterator();
            for (int i3 = 0; i3 < length; i3++) {
                unionChildIterator.addNodeTest(unionPathIterator.m_exprs[i3]);
            }
            return unionChildIterator;
        }
        return unionPathIterator;
    }

    @Override
    public int getAnalysisBits() {
        if (this.m_exprs == null) {
            return 0;
        }
        int length = this.m_exprs.length;
        int analysisBits = 0;
        for (int i = 0; i < length; i++) {
            analysisBits |= this.m_exprs[i].getAnalysisBits();
        }
        return analysisBits;
    }

    private void readObject(ObjectInputStream objectInputStream) throws TransformerException, IOException {
        try {
            objectInputStream.defaultReadObject();
            this.m_clones = new IteratorPool(this);
        } catch (ClassNotFoundException e) {
            throw new TransformerException(e);
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        UnionPathIterator unionPathIterator = (UnionPathIterator) super.clone();
        if (this.m_iterators != null) {
            int length = this.m_iterators.length;
            unionPathIterator.m_iterators = new DTMIterator[length];
            for (int i = 0; i < length; i++) {
                unionPathIterator.m_iterators[i] = (DTMIterator) this.m_iterators[i].clone();
            }
        }
        return unionPathIterator;
    }

    protected LocPathIterator createDTMIterator(Compiler compiler, int i) throws TransformerException {
        return (LocPathIterator) WalkerFactory.newDTMIterator(compiler, i, compiler.getLocationPathDepth() <= 0);
    }

    protected void loadLocationPaths(Compiler compiler, int i, int i2) throws TransformerException {
        int op = compiler.getOp(i);
        if (op == 28) {
            loadLocationPaths(compiler, compiler.getNextOpPos(i), i2 + 1);
            this.m_exprs[i2] = createDTMIterator(compiler, i);
            this.m_exprs[i2].exprSetParent(this);
            return;
        }
        switch (op) {
            case 22:
            case 23:
            case 24:
            case 25:
                loadLocationPaths(compiler, compiler.getNextOpPos(i), i2 + 1);
                WalkingIterator walkingIterator = new WalkingIterator(compiler.getNamespaceContext());
                walkingIterator.exprSetParent(this);
                if (compiler.getLocationPathDepth() <= 0) {
                    walkingIterator.setIsTopLevel(true);
                }
                walkingIterator.m_firstWalker = new FilterExprWalker(walkingIterator);
                walkingIterator.m_firstWalker.init(compiler, i, op);
                this.m_exprs[i2] = walkingIterator;
                break;
            default:
                this.m_exprs = new LocPathIterator[i2];
                break;
        }
    }

    @Override
    public int nextNode() {
        int i = -1;
        if (this.m_foundLast) {
            return -1;
        }
        if (this.m_iterators != null) {
            int length = this.m_iterators.length;
            int i2 = -1;
            int i3 = -1;
            for (int i4 = 0; i4 < length; i4++) {
                int currentNode = this.m_iterators[i4].getCurrentNode();
                if (-1 != currentNode) {
                    if (-1 != i2) {
                        if (currentNode == i2) {
                            this.m_iterators[i4].nextNode();
                        } else if (getDTM(currentNode).isNodeAfter(currentNode, i2)) {
                            i3 = i4;
                            i2 = currentNode;
                        }
                    }
                }
            }
            if (-1 != i2) {
                this.m_iterators[i3].nextNode();
                incrementCurrentPos();
            } else {
                this.m_foundLast = true;
            }
            i = i2;
        }
        this.m_lastFetched = i;
        return i;
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        for (int i2 = 0; i2 < this.m_exprs.length; i2++) {
            this.m_exprs[i2].fixupVariables(vector, i);
        }
    }

    @Override
    public int getAxis() {
        return -1;
    }

    class iterOwner implements ExpressionOwner {
        int m_index;

        iterOwner(int i) {
            this.m_index = i;
        }

        @Override
        public Expression getExpression() {
            return UnionPathIterator.this.m_exprs[this.m_index];
        }

        @Override
        public void setExpression(Expression expression) {
            Object obj;
            if (!(expression instanceof LocPathIterator)) {
                WalkingIterator walkingIterator = new WalkingIterator(UnionPathIterator.this.getPrefixResolver());
                FilterExprWalker filterExprWalker = new FilterExprWalker(walkingIterator);
                walkingIterator.setFirstWalker(filterExprWalker);
                filterExprWalker.setInnerExpression(expression);
                walkingIterator.exprSetParent(UnionPathIterator.this);
                filterExprWalker.exprSetParent(walkingIterator);
                expression.exprSetParent(filterExprWalker);
                obj = walkingIterator;
            } else {
                expression.exprSetParent(UnionPathIterator.this);
                obj = expression;
            }
            UnionPathIterator.this.m_exprs[this.m_index] = (LocPathIterator) obj;
        }
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        if (xPathVisitor.visitUnionPath(expressionOwner, this) && this.m_exprs != null) {
            int length = this.m_exprs.length;
            for (int i = 0; i < length; i++) {
                this.m_exprs[i].callVisitors(new iterOwner(i), xPathVisitor);
            }
        }
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!super.deepEquals(expression)) {
            return false;
        }
        UnionPathIterator unionPathIterator = (UnionPathIterator) expression;
        if (this.m_exprs == null) {
            return unionPathIterator.m_exprs == null;
        }
        int length = this.m_exprs.length;
        if (unionPathIterator.m_exprs == null || unionPathIterator.m_exprs.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!this.m_exprs[i].deepEquals(unionPathIterator.m_exprs[i])) {
                return false;
            }
        }
        return true;
    }
}
