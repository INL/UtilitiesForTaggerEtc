package org.ivdnt.openconvert.filehandling;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Some operations require filenames, while others are much more efficient if they chain input and outputstreams
 * Most actions can handle both files and streams and extend this class.
 * Others that only work with filenames should instead implement FileInputOutputProcess
 */
public abstract class SimpleInputOutputProcess implements FileInputOutputProcess, StreamInputOutputProcess
{
     java.util.Properties properties;
	/**
	 * Delegate to {@link SimpleInputOutputProcess#handleStream(InputStream, Charset, OutputStream)}, the streams are buffered, and closed after usage.
	 *
	 * @param inFileName
	 * @param outFileName
	 * @throws SimpleProcessException
	 */

	@Override
	public void handleFile(String inFileName, String outFileName) throws SimpleProcessException {
		try (
			FileInputStream _is = new FileInputStream(inFileName);
			BufferedInputStream is = new BufferedInputStream(_is);

			FileOutputStream _os = new FileOutputStream(outFileName);
			BufferedOutputStream os = new BufferedOutputStream(_os);
		) {
			handleStream(is, null, os);
		} catch (IOException e) {
			System.err.println("Error when opening file(s) " + inFileName + " and " + outFileName);
			e.printStackTrace();
		}
	}

        public void setProperties(java.util.Properties properties)
        {
                // TODO Auto-generated method stub
                this.properties = properties;
        }

	
      public void close()
      {  
      }
}
