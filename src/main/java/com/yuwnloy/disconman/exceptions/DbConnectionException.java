package com.yuwnloy.disconman.exceptions;
/**
 * This exception is raised ONLY when the error is due to a Database connection error.
 * When a Database connection error occurs [e.g. we cannot obtain a connection]
 * a special log filtering is put in effect and whoever catches this exception must log 
 * only in FINEST, as the log has already been taken care of earlier on.
 * 
 * @author xiaoguang.gao
 *
 * @date Sep 22, 2015
 */
public class DbConnectionException extends PersistenceException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public DbConnectionException(Throwable throwable) {
        super(throwable);
    }

    public DbConnectionException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public DbConnectionException(String string) {
        super(string);
    }

    public DbConnectionException() {
        super();
    }
}

