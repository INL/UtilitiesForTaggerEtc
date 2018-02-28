package org.ivdnt.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Resource

{	public static String resourceFolder = "resources";
	public static String yetAnotherFolder =
			"N:/Impact/ImpactIR/OCRIRevaluatie/IREval/workspace/ImpactIR/resources";

	static String[] foldersToTry = {resourceFolder, yetAnotherFolder};

	private Resource() {}

	// TODO better name
	public static InputStream openStream(String s)
	{
		try
		{
			// first try to read file from local file system
			for (String f: foldersToTry)
			{
				File file = new File(f, s);
				if (file.exists())
				{
					return new FileInputStream(file);
				}
			}
			// next try for files included in jar
			try
			{
				InputStream is = Resource.class.getClass().getResourceAsStream("/"+ s);
				if (is != null)
				{
					System.err.println("found in jar!!");
					return is;
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}

			java.net.URL url = Resource.class.getResource(resourceFolder + "/" + s);
			System.err.println("jar url " + url);
			// or URL from web
			if (url == null) url = new java.net.URL(s);
			java.net.URLConnection site = url.openConnection();
			return site.getInputStream();
		} catch (IOException ioe)
		{
			System.err.println("Could not open " + s);
			return null;
		}
	}
}
