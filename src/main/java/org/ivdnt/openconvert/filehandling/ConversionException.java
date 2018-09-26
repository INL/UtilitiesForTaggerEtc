package org.ivdnt.openconvert.filehandling;

public class ConversionException extends java.lang.Exception 
{
  public ConversionException(java.lang.String message)
  {
    super(message);
  }

  public ConversionException(java.lang.Exception e)
  {
    super(e);
  }
}
