package org.apache.xpath.patterns;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.Axis;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.axes.SubContextList;
import org.apache.xpath.compiler.PsuedoNames;
import org.apache.xpath.objects.XObject;

public class StepPattern extends NodeTest implements SubContextList, ExpressionOwner {
    private static final boolean DEBUG_MATCHES = false;
    static final long serialVersionUID = 9071668960168152644L;
    protected int m_axis;
    Expression[] m_predicates;
    StepPattern m_relativePathPattern;
    String m_targetString;

    public StepPattern(int whatToShow, String namespace, String name, int axis, int axisForPredicate) {
        super(whatToShow, namespace, name);
        this.m_axis = axis;
    }

    public StepPattern(int whatToShow, int axis, int axisForPredicate) {
        super(whatToShow);
        this.m_axis = axis;
    }

    public void calcTargetString() {
        int whatToShow = getWhatToShow();
        switch (whatToShow) {
            case -1:
                this.m_targetString = "*";
                break;
            case 1:
                if ("*" == this.m_name) {
                    this.m_targetString = "*";
                } else {
                    this.m_targetString = this.m_name;
                }
                break;
            case 4:
            case 8:
            case 12:
                this.m_targetString = PsuedoNames.PSEUDONAME_TEXT;
                break;
            case 128:
                this.m_targetString = PsuedoNames.PSEUDONAME_COMMENT;
                break;
            case DTMFilter.SHOW_DOCUMENT:
            case 1280:
                this.m_targetString = PsuedoNames.PSEUDONAME_ROOT;
                break;
            default:
                this.m_targetString = "*";
                break;
        }
    }

    public String getTargetString() {
        return this.m_targetString;
    }

    @Override
    public void fixupVariables(Vector vars, int globalsSize) {
        super.fixupVariables(vars, globalsSize);
        if (this.m_predicates != null) {
            for (int i = 0; i < this.m_predicates.length; i++) {
                this.m_predicates[i].fixupVariables(vars, globalsSize);
            }
        }
        if (this.m_relativePathPattern == null) {
            return;
        }
        this.m_relativePathPattern.fixupVariables(vars, globalsSize);
    }

    public void setRelativePathPattern(StepPattern expr) {
        this.m_relativePathPattern = expr;
        expr.exprSetParent(this);
        calcScore();
    }

    public StepPattern getRelativePathPattern() {
        return this.m_relativePathPattern;
    }

    public Expression[] getPredicates() {
        return this.m_predicates;
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        int n = getPredicateCount();
        for (int i = 0; i < n; i++) {
            if (getPredicate(i).canTraverseOutsideSubtree()) {
                return true;
            }
        }
        return false;
    }

    public Expression getPredicate(int i) {
        return this.m_predicates[i];
    }

    public final int getPredicateCount() {
        if (this.m_predicates == null) {
            return 0;
        }
        return this.m_predicates.length;
    }

    public void setPredicates(Expression[] predicates) {
        this.m_predicates = predicates;
        if (predicates != null) {
            for (Expression expression : predicates) {
                expression.exprSetParent(this);
            }
        }
        calcScore();
    }

    @Override
    public void calcScore() {
        if (getPredicateCount() > 0 || this.m_relativePathPattern != null) {
            this.m_score = SCORE_OTHER;
        } else {
            super.calcScore();
        }
        if (this.m_targetString != null) {
            return;
        }
        calcTargetString();
    }

    @Override
    public XObject execute(XPathContext xctxt, int currentNode) throws TransformerException {
        DTM dtm = xctxt.getDTM(currentNode);
        if (dtm != null) {
            int expType = dtm.getExpandedTypeID(currentNode);
            return execute(xctxt, currentNode, dtm, expType);
        }
        return NodeTest.SCORE_NONE;
    }

    @Override
    public XObject execute(XPathContext xctxt) throws TransformerException {
        return execute(xctxt, xctxt.getCurrentNode());
    }

    @Override
    public XObject execute(XPathContext xctxt, int currentNode, DTM dtm, int expType) throws TransformerException {
        if (this.m_whatToShow == 65536) {
            if (this.m_relativePathPattern != null) {
                return this.m_relativePathPattern.execute(xctxt);
            }
            return NodeTest.SCORE_NONE;
        }
        XObject score = super.execute(xctxt, currentNode, dtm, expType);
        if (score == NodeTest.SCORE_NONE) {
            return NodeTest.SCORE_NONE;
        }
        if (getPredicateCount() != 0 && !executePredicates(xctxt, dtm, currentNode)) {
            return NodeTest.SCORE_NONE;
        }
        if (this.m_relativePathPattern != null) {
            return this.m_relativePathPattern.executeRelativePathPattern(xctxt, dtm, currentNode);
        }
        return score;
    }

    private final boolean checkProximityPosition(XPathContext xctxt, int predPos, DTM dtm, int context, int pos) {
        try {
            DTMAxisTraverser traverser = dtm.getAxisTraverser(12);
            int child = traverser.first(context);
            loop0: while (-1 != child) {
                try {
                    xctxt.pushCurrentNode(child);
                    if (NodeTest.SCORE_NONE != super.execute(xctxt, child)) {
                        boolean pass = true;
                        try {
                            xctxt.pushSubContextList(this);
                            int i = 0;
                            while (true) {
                                if (i >= predPos) {
                                    break;
                                }
                                xctxt.pushPredicatePos(i);
                                try {
                                    XObject pred = this.m_predicates[i].execute(xctxt);
                                    try {
                                        if (2 == pred.getType()) {
                                            break loop0;
                                        }
                                        if (pred.boolWithSideEffects()) {
                                            i++;
                                        } else {
                                            pass = false;
                                            break;
                                        }
                                    } finally {
                                        pred.detach();
                                    }
                                } finally {
                                    xctxt.popPredicatePos();
                                }
                            }
                            if (pass) {
                                pos--;
                            }
                            if (pos < 1) {
                                return false;
                            }
                        } finally {
                            xctxt.popSubContextList();
                        }
                    }
                    xctxt.popCurrentNode();
                    child = traverser.next(context, child);
                } finally {
                    xctxt.popCurrentNode();
                }
            }
            return pos == 1;
        } catch (TransformerException se) {
            throw new RuntimeException(se.getMessage());
        }
    }

    private final int getProximityPosition(XPathContext xctxt, int predPos, boolean findLast) {
        int pos = 0;
        int context = xctxt.getCurrentNode();
        DTM dtm = xctxt.getDTM(context);
        int parent = dtm.getParent(context);
        try {
            DTMAxisTraverser traverser = dtm.getAxisTraverser(3);
            for (int child = traverser.first(parent); -1 != child; child = traverser.next(parent, child)) {
                try {
                    xctxt.pushCurrentNode(child);
                    if (NodeTest.SCORE_NONE != super.execute(xctxt, child)) {
                        boolean pass = true;
                        try {
                            xctxt.pushSubContextList(this);
                            int i = 0;
                            while (true) {
                                if (i >= predPos) {
                                    break;
                                }
                                xctxt.pushPredicatePos(i);
                                try {
                                    XObject pred = this.m_predicates[i].execute(xctxt);
                                    try {
                                        if (2 == pred.getType()) {
                                            if (pos + 1 != ((int) pred.numWithSideEffects())) {
                                                pass = false;
                                                break;
                                            }
                                            i++;
                                        } else {
                                            if (!pred.boolWithSideEffects()) {
                                                pass = false;
                                                break;
                                            }
                                            i++;
                                        }
                                    } finally {
                                    }
                                } finally {
                                }
                            }
                            if (pass) {
                                pos++;
                            }
                            if (!findLast && child == context) {
                                return pos;
                            }
                        } finally {
                            xctxt.popSubContextList();
                        }
                    }
                    xctxt.popCurrentNode();
                } finally {
                    xctxt.popCurrentNode();
                }
            }
            return pos;
        } catch (TransformerException se) {
            throw new RuntimeException(se.getMessage());
        }
    }

    @Override
    public int getProximityPosition(XPathContext xctxt) {
        return getProximityPosition(xctxt, xctxt.getPredicatePos(), false);
    }

    @Override
    public int getLastPos(XPathContext xctxt) {
        return getProximityPosition(xctxt, xctxt.getPredicatePos(), true);
    }

    protected final XObject executeRelativePathPattern(XPathContext xctxt, DTM dtm, int currentNode) throws TransformerException {
        XObject score = NodeTest.SCORE_NONE;
        DTMAxisTraverser traverser = dtm.getAxisTraverser(this.m_axis);
        int relative = traverser.first(currentNode);
        while (true) {
            if (-1 == relative) {
                break;
            }
            try {
                xctxt.pushCurrentNode(relative);
                score = execute(xctxt);
                if (score != NodeTest.SCORE_NONE) {
                    break;
                }
                xctxt.popCurrentNode();
                relative = traverser.next(currentNode, relative);
            } finally {
                xctxt.popCurrentNode();
            }
        }
        return score;
    }

    protected final boolean executePredicates(XPathContext xctxt, DTM dtm, int currentNode) throws TransformerException {
        boolean result = true;
        boolean positionAlreadySeen = false;
        int n = getPredicateCount();
        try {
            xctxt.pushSubContextList(this);
            int i = 0;
            while (true) {
                if (i >= n) {
                    break;
                }
                xctxt.pushPredicatePos(i);
                try {
                    XObject pred = this.m_predicates[i].execute(xctxt);
                    try {
                        if (2 == pred.getType()) {
                            int pos = (int) pred.num();
                            if (positionAlreadySeen) {
                                result = pos == 1;
                            } else {
                                positionAlreadySeen = true;
                                if (!checkProximityPosition(xctxt, i, dtm, currentNode, pos)) {
                                    result = false;
                                    pred.detach();
                                    xctxt.popPredicatePos();
                                    break;
                                }
                                i++;
                            }
                        } else {
                            if (!pred.boolWithSideEffects()) {
                                result = false;
                                pred.detach();
                                xctxt.popPredicatePos();
                                break;
                            }
                            i++;
                        }
                    } finally {
                        pred.detach();
                    }
                } finally {
                    xctxt.popPredicatePos();
                }
            }
            return result;
        } finally {
            xctxt.popSubContextList();
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (StepPattern pat = this; pat != null; pat = pat.m_relativePathPattern) {
            if (pat != this) {
                buf.append(PsuedoNames.PSEUDONAME_ROOT);
            }
            buf.append(Axis.getNames(pat.m_axis));
            buf.append("::");
            if (20480 == pat.m_whatToShow) {
                buf.append("doc()");
            } else if (65536 == pat.m_whatToShow) {
                buf.append("function()");
            } else if (-1 == pat.m_whatToShow) {
                buf.append("node()");
            } else if (4 == pat.m_whatToShow) {
                buf.append("text()");
            } else if (64 == pat.m_whatToShow) {
                buf.append("processing-instruction(");
                if (pat.m_name != null) {
                    buf.append(pat.m_name);
                }
                buf.append(")");
            } else if (128 == pat.m_whatToShow) {
                buf.append("comment()");
            } else if (pat.m_name != null) {
                if (2 == pat.m_whatToShow) {
                    buf.append("@");
                }
                if (pat.m_namespace != null) {
                    buf.append("{");
                    buf.append(pat.m_namespace);
                    buf.append("}");
                }
                buf.append(pat.m_name);
            } else if (2 == pat.m_whatToShow) {
                buf.append("@");
            } else if (1280 == pat.m_whatToShow) {
                buf.append("doc-root()");
            } else {
                buf.append("?" + Integer.toHexString(pat.m_whatToShow));
            }
            if (pat.m_predicates != null) {
                for (int i = 0; i < pat.m_predicates.length; i++) {
                    buf.append("[");
                    buf.append(pat.m_predicates[i]);
                    buf.append("]");
                }
            }
        }
        return buf.toString();
    }

    public double getMatchScore(XPathContext xctxt, int context) throws TransformerException {
        xctxt.pushCurrentNode(context);
        xctxt.pushCurrentExpressionNode(context);
        try {
            XObject score = execute(xctxt);
            return score.num();
        } finally {
            xctxt.popCurrentNode();
            xctxt.popCurrentExpressionNode();
        }
    }

    public void setAxis(int axis) {
        this.m_axis = axis;
    }

    public int getAxis() {
        return this.m_axis;
    }

    class PredOwner implements ExpressionOwner {
        int m_index;

        PredOwner(int index) {
            this.m_index = index;
        }

        @Override
        public Expression getExpression() {
            return StepPattern.this.m_predicates[this.m_index];
        }

        @Override
        public void setExpression(Expression exp) {
            exp.exprSetParent(StepPattern.this);
            StepPattern.this.m_predicates[this.m_index] = exp;
        }
    }

    @Override
    public void callVisitors(ExpressionOwner owner, XPathVisitor visitor) {
        if (!visitor.visitMatchPattern(owner, this)) {
            return;
        }
        callSubtreeVisitors(visitor);
    }

    protected void callSubtreeVisitors(XPathVisitor visitor) {
        if (this.m_predicates != null) {
            int n = this.m_predicates.length;
            for (int i = 0; i < n; i++) {
                ExpressionOwner predOwner = new PredOwner(i);
                if (visitor.visitPredicate(predOwner, this.m_predicates[i])) {
                    this.m_predicates[i].callVisitors(predOwner, visitor);
                }
            }
        }
        if (this.m_relativePathPattern == null) {
            return;
        }
        this.m_relativePathPattern.callVisitors(this, visitor);
    }

    @Override
    public Expression getExpression() {
        return this.m_relativePathPattern;
    }

    @Override
    public void setExpression(Expression exp) {
        exp.exprSetParent(this);
        this.m_relativePathPattern = (StepPattern) exp;
    }

    @Override
    public boolean deepEquals(Expression expr) {
        if (!super.deepEquals(expr)) {
            return false;
        }
        StepPattern sp = (StepPattern) expr;
        if (this.m_predicates != null) {
            int n = this.m_predicates.length;
            if (sp.m_predicates == null || sp.m_predicates.length != n) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                if (!this.m_predicates[i].deepEquals(sp.m_predicates[i])) {
                    return false;
                }
            }
        } else if (sp.m_predicates != null) {
            return false;
        }
        return this.m_relativePathPattern != null ? this.m_relativePathPattern.deepEquals(sp.m_relativePathPattern) : sp.m_relativePathPattern == null;
    }
}
