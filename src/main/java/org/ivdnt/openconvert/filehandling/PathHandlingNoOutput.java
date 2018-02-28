package org.ivdnt.openconvert.filehandling;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipError;

public class PathHandlingNoOutput
{
	private PathHandlingNoOutput() {}

	public static void traverseDirectory(DoSomethingWithStream p, Path inputPath) throws Exception
	{
		if (!Files.exists(inputPath))
		{
			System.err.println("bestaat NIET? "  + inputPath);
		} else if (Files.isRegularFile(inputPath) && inputPath.toString().endsWith(".zip"))
		{
			Map<String,String> env = new HashMap<>();
			URI uri = URI.create("jar:file:" + inputPath.toString().replaceAll("\\\\", "/"));
			System.err.println("Input zip with URI: "  + uri );
			try
			{
				try
				{
					FileSystem zipfs = FileSystems.newFileSystem(uri,env);
					for (Path r: zipfs.getRootDirectories())
					{
						traverseDirectory(p, r);
					}
				} catch (ZipError z)
				{
					System.err.println("Could not open zip!!! " + inputPath);
				}

			} catch (Exception e)
			{
				System.err.println("Could not open zip:" + inputPath);
				e.printStackTrace();
			}
		}  else if (Files.isRegularFile(inputPath) )
		{
			try (
				InputStream i1 = Files.newInputStream(inputPath);
				BufferedInputStream inStream = new BufferedInputStream(i1);
			) {
				 p.handleStream(inStream) ;
			} catch (Exception e)
			{
				System.err.println("Exception in conversion " + p.getClass());
				e.printStackTrace();
			}

			return;
		} else if (Files.isDirectory(inputPath))
		{
			try (DirectoryStream<Path> dir = Files.newDirectoryStream(inputPath))
			{
				for (Path entry: dir)
				{
					if (Files.isDirectory(entry))
					{
						// create output
						try
						{
							traverseDirectory(p, entry);
						} catch (Exception e)
						{
							e.printStackTrace();
						}
					} else
					{
						// make sure parent dir exists...
						traverseDirectory(p, entry);
					}
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
