package com.yuwnloy.disconman.exceptions;

public class DisconmanException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DisconmanException(Throwable throwable) {
        super(throwable);
    }

    public DisconmanException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public DisconmanException(String string) {
        super(string);
    }

    public DisconmanException() {
        super();
    }
}
