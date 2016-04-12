package com.yuwnloy.disconman.exceptions;
/**
 * 
 */
public class PersistenceException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public PersistenceException(Throwable throwable) {
        super(throwable);
    }

    public PersistenceException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public PersistenceException(String string) {
        super(string);
    }

    public PersistenceException() {
        super();
    }
}
