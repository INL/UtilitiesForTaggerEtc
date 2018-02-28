package org.ivdnt.openconvert.filehandling;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public interface StreamInputOutputProcess {
	/**
	 *
	 * @param is
	 * @param ics Optional:
	 * 		If the implementation is text-based, and this is provided, this implementation should use this Charset to read and write data from and to "is" and "os".
	 * 		Implementations should ignore this parameters if they use binary data, however if provided, and the implementation generates text as output, it should write the text in the provided encoding.
	 * @param os
	 * @throws SimpleProcessException
	 * @throws IOException
	 */
	public void handleStream(InputStream is, Charset ics, OutputStream os) throws SimpleProcessException, IOException;
}
