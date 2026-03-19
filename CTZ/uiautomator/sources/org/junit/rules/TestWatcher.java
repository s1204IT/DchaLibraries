package org.junit.rules;

import java.util.ArrayList;
import java.util.List;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

public abstract class TestWatcher implements TestRule {
    @Override
    public Statement apply(final Statement statement, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Exception {
                ArrayList arrayList = new ArrayList();
                TestWatcher.this.startingQuietly(description, arrayList);
                try {
                    try {
                        try {
                            statement.evaluate();
                            TestWatcher.this.succeededQuietly(description, arrayList);
                        } catch (Throwable th) {
                            arrayList.add(th);
                            TestWatcher.this.failedQuietly(th, description, arrayList);
                        }
                    } catch (AssumptionViolatedException e) {
                        arrayList.add(e);
                        TestWatcher.this.skippedQuietly(e, description, arrayList);
                    }
                    MultipleFailureException.assertEmpty(arrayList);
                } finally {
                    TestWatcher.this.finishedQuietly(description, arrayList);
                }
            }
        };
    }

    private void succeededQuietly(Description description, List<Throwable> list) {
        try {
            succeeded(description);
        } catch (Throwable th) {
            list.add(th);
        }
    }

    private void failedQuietly(Throwable th, Description description, List<Throwable> list) {
        try {
            failed(th, description);
        } catch (Throwable th2) {
            list.add(th2);
        }
    }

    private void skippedQuietly(AssumptionViolatedException assumptionViolatedException, Description description, List<Throwable> list) {
        try {
            if (assumptionViolatedException instanceof org.junit.AssumptionViolatedException) {
                skipped((org.junit.AssumptionViolatedException) assumptionViolatedException, description);
            } else {
                skipped(assumptionViolatedException, description);
            }
        } catch (Throwable th) {
            list.add(th);
        }
    }

    private void startingQuietly(Description description, List<Throwable> list) {
        try {
            starting(description);
        } catch (Throwable th) {
            list.add(th);
        }
    }

    private void finishedQuietly(Description description, List<Throwable> list) {
        try {
            finished(description);
        } catch (Throwable th) {
            list.add(th);
        }
    }

    protected void succeeded(Description description) {
    }

    protected void failed(Throwable th, Description description) {
    }

    protected void skipped(org.junit.AssumptionViolatedException assumptionViolatedException, Description description) {
        skipped((AssumptionViolatedException) assumptionViolatedException, description);
    }

    @Deprecated
    protected void skipped(AssumptionViolatedException assumptionViolatedException, Description description) {
    }

    protected void starting(Description description) {
    }

    protected void finished(Description description) {
    }
}
