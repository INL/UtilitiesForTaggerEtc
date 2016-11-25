package org.ivdnt.openconvert.filehandling;

public class ConversionException extends Exception
{
	public ConversionException(String message)
	{
		super(message);
	}
	public ConversionException(Exception  e)
	{
		super(e);
	}
}
