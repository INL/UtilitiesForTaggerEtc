package org.ivdnt.openconvert.filehandling;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class WrappedFileBasedConverter implements StreamInputOutputProcess
{
    FileInputOutputProcess base = null;


	public WrappedFileBasedConverter(FileInputOutputProcess s)
	{
		this.base = s;
	}

	@Override
	public void handleStream(InputStream inStream, Charset ics, OutputStream outStream)  throws SimpleProcessException
	{
		try
		{
			File fin = File.createTempFile("bla", "in");
			File fout = File.createTempFile("bla", "out");

			fin.deleteOnExit();
			fout.deleteOnExit();
			fin.delete();
			fout.delete();

			Path pin = fin.toPath();
			Path pout = fout.toPath();

			Files.copy(inStream, pin);
			base.handleFile(fin.getCanonicalPath(), fout.getAbsolutePath());

			Files.copy(pout, outStream); // ahem?
			outStream.close();
			//outStream.flush();
			//outStream.close();
			//System.err.println(fin.getCanonicalPath());
			//System.err.println(fout.getCanonicalPath());
			fin.delete();
			fout.delete();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
