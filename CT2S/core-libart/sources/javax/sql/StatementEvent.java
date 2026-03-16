package javax.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.EventObject;

public class StatementEvent extends EventObject {
    private static final long serialVersionUID = -8089573731826608315L;
    private SQLException exception;
    private PreparedStatement statement;

    public StatementEvent(PooledConnection con, PreparedStatement statement, SQLException exception) {
        super(con);
        this.statement = statement;
        this.exception = exception;
    }

    public StatementEvent(PooledConnection con, PreparedStatement statement) {
        this(con, statement, null);
    }

    public PreparedStatement getStatement() {
        return this.statement;
    }

    public SQLException getSQLException() {
        return this.exception;
    }
}
