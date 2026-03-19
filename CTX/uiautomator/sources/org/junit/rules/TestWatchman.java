package org.junit.rules;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

@Deprecated
public class TestWatchman implements MethodRule {
    @Override
    public Statement apply(final Statement statement, final FrameworkMethod frameworkMethod, Object obj) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                TestWatchman.this.starting(frameworkMethod);
                try {
                    try {
                        statement.evaluate();
                        TestWatchman.this.succeeded(frameworkMethod);
                    } catch (AssumptionViolatedException e) {
                        throw e;
                    } catch (Throwable th) {
                        TestWatchman.this.failed(th, frameworkMethod);
                        throw th;
                    }
                } finally {
                    TestWatchman.this.finished(frameworkMethod);
                }
            }
        };
    }

    public void succeeded(FrameworkMethod frameworkMethod) {
    }

    public void failed(Throwable th, FrameworkMethod frameworkMethod) {
    }

    public void starting(FrameworkMethod frameworkMethod) {
    }

    public void finished(FrameworkMethod frameworkMethod) {
    }
}
