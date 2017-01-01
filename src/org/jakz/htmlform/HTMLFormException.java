package org.jakz.htmlform;

public class HTMLFormException extends Exception 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Throwable cause;

    public HTMLFormException(String message) 
    {
        super(message);
    }

    public HTMLFormException(Throwable cause) 
    {
        super(cause.getMessage());
        this.cause = cause;
    }
    
    public HTMLFormException(String message, Throwable cause) 
    {
        super(message);
        this.cause = cause;
    }
    
    public HTMLFormException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) 
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

    public Throwable getCause() 
    {
        return this.cause;
    }
}
