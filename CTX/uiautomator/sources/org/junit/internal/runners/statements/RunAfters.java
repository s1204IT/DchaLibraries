package org.junit.internal.runners.statements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

public class RunAfters extends Statement {
    private final List<FrameworkMethod> afters;
    private final Statement next;
    private final Object target;

    public RunAfters(Statement statement, List<FrameworkMethod> list, Object obj) {
        this.next = statement;
        this.afters = list;
        this.target = obj;
    }

    @Override
    public void evaluate() throws Exception {
        ArrayList arrayList = new ArrayList();
        try {
            try {
                this.next.evaluate();
                Iterator<FrameworkMethod> it = this.afters.iterator();
                while (it.hasNext()) {
                    try {
                        it.next().invokeExplosively(this.target, new Object[0]);
                    } catch (Throwable th) {
                        arrayList.add(th);
                    }
                }
            } catch (Throwable th2) {
                arrayList.add(th2);
                Iterator<FrameworkMethod> it2 = this.afters.iterator();
                while (it2.hasNext()) {
                    try {
                        it2.next().invokeExplosively(this.target, new Object[0]);
                    } catch (Throwable th3) {
                        arrayList.add(th3);
                    }
                }
            }
            MultipleFailureException.assertEmpty(arrayList);
        } catch (Throwable th4) {
            Iterator<FrameworkMethod> it3 = this.afters.iterator();
            while (it3.hasNext()) {
                try {
                    it3.next().invokeExplosively(this.target, new Object[0]);
                } catch (Throwable th5) {
                    arrayList.add(th5);
                }
            }
            throw th4;
        }
    }
}
