package javax.sql;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.EventObject;

public class ConnectionEvent extends EventObject implements Serializable {
    private static final long serialVersionUID = -4843217645290030002L;
    private SQLException ex;

    public ConnectionEvent(PooledConnection theConnection) {
        super(theConnection);
    }

    public ConnectionEvent(PooledConnection theConnection, SQLException theException) {
        super(theConnection);
        this.ex = theException;
    }

    public SQLException getSQLException() {
        return this.ex;
    }
}
