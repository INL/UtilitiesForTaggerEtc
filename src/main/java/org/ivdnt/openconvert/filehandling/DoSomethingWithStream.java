package org.ivdnt.openconvert.filehandling;

import java.io.InputStream;

public interface DoSomethingWithStream
{
	public void handleStream(InputStream stream) throws SimpleProcessException;
}
