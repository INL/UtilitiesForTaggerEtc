package org.ivdnt.openconvert.filehandling;

import java.io.InputStream;
import java.nio.charset.Charset;

public interface DoSomethingWithStream
{
	public void handleStream(InputStream stream, Charset ics) throws SimpleProcessException;
}
