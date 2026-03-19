package javax.sql;

import java.sql.SQLException;
import java.util.EventObject;

public class ConnectionEvent extends EventObject {
    static final long serialVersionUID = -4843217645290030002L;
    private SQLException ex;

    public ConnectionEvent(PooledConnection pooledConnection) {
        super(pooledConnection);
        this.ex = null;
    }

    public ConnectionEvent(PooledConnection pooledConnection, SQLException sQLException) {
        super(pooledConnection);
        this.ex = null;
        this.ex = sQLException;
    }

    public SQLException getSQLException() {
        return this.ex;
    }
}
