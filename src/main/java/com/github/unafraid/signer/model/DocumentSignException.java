package com.github.unafraid.signer.model;

/**
 * @author Svetlin Nakov on 10.7.2005 г.. Exception class used for document signing errors.
 */
public class DocumentSignException extends Exception
{
	private static final long serialVersionUID = 7500493051966576305L;
	
	public DocumentSignException(String aMessage)
	{
		super(aMessage);
	}
	
	public DocumentSignException(String aMessage, Throwable aCause)
	{
		super(aMessage, aCause);
	}
}