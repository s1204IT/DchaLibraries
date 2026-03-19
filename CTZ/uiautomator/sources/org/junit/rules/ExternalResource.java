package org.junit.rules;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public abstract class ExternalResource implements TestRule {
    @Override
    public Statement apply(Statement statement, Description description) {
        return statement(statement);
    }

    private Statement statement(final Statement statement) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ExternalResource.this.before();
                try {
                    statement.evaluate();
                } finally {
                    ExternalResource.this.after();
                }
            }
        };
    }

    protected void before() throws Throwable {
    }

    protected void after() {
    }
}
