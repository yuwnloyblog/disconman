package com.yuwnloy.disconman.exceptions;
/**
 * 
 * @author xiaoguang.gao
 *
 * @date Apr 14, 2016
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
