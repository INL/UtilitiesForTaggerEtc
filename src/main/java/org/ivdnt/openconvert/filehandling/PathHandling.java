package org.ivdnt.openconvert.filehandling;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;


public class PathHandling
{
	static int max = Integer.MAX_VALUE;
	static FileSystem currentZipFS = null;
	static URI currentZipURI = null;
	static int currentZipCounter = 0; 
	static int portionSize = 50;

	public static void traverseDirectory(StreamInputOutputProcess p, Path inputPath, 
			Path outputPath, FileFilter fileFilter) 
	{	
		outputPath = possiblyReopen(outputPath);
		//System.err.println(inputPath + " -->  " +  outputPath);
		if (!Files.exists(inputPath))
		{
			System.err.println("bestaat NIET? "  + inputPath);
		}
		if (outputPath.toString().endsWith(".zip"))
		{
			//System.err.println("Hah! output zip!!!" + outputPath);
			Map<String,String> env = new HashMap<String,String>();
			env.put("create", "true");
			URI uri = URI.create("jar:file:" + outputPath.toString().replaceAll("\\\\", "/"));
			currentZipURI = uri; 
			try
			{
				FileSystem zipfs = FileSystems.newFileSystem(uri,env);
				Path pathInZipfile = zipfs.getPath("/");
				Path x = Files.createDirectories(pathInZipfile);

				currentZipFS = zipfs;

				traverseDirectory(p, inputPath, x, fileFilter);
				currentZipFS = null;
				currentZipCounter = 0;
				zipfs.close(); // dit kan niet bij multithreaded toepassing, dan kan je pas achteraf sluiten
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		} else if  (Files.isRegularFile(inputPath) &&  (inputPath.toString().toLowerCase().endsWith(".tar.gz")  || inputPath.toString().toLowerCase().endsWith(".tar")))
		{
			boolean tgz = inputPath.toString().toLowerCase().endsWith(".tar.gz");
			try
			{
				TarInputStream tarInputStream;
				if (tgz)
					tarInputStream = new TarInputStream(new GZIPInputStream(new FileInputStream(inputPath.toString()))); 
				else
					tarInputStream = new TarInputStream(new FileInputStream(inputPath.toString())); 
				TarEntry currentEntry;
				int k=0;
				while ((currentEntry = tarInputStream.getNextEntry()) != null)
				{
					boolean compressed = currentEntry.getName().toLowerCase().endsWith(".gz");
					if (!compressed)
						continue;
					//System.err.println(currentEntry.getName());
					File tempFile = copyStreamToTempFile(tarInputStream, compressed);
					tempFile.deleteOnExit();
					Path path = tempFile.toPath();
					traverseDirectory(p, path, outputPath, fileFilter);
					tempFile.delete();
					k++;
					if (k > max)
						break;
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (Files.isRegularFile(inputPath) && inputPath.toString().endsWith(".zip"))
		{
			//System.err.println("Hah! input zip!!!");
			Map<String,String> env = new HashMap<String,String>();
			URI uri = URI.create("jar:file:" + inputPath.toString().replaceAll("\\\\", "/"));
			//System.err.println("URI: "  + uri);
			try
			{
				FileSystem zipfs = FileSystems.newFileSystem(uri,env);
				for (Path r: zipfs.getRootDirectories())	
				{
					traverseDirectory(p, r, outputPath, fileFilter);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}  else if (Files.isRegularFile(inputPath) && (!Files.exists(outputPath)||Files.isRegularFile(outputPath) ) )
		{
			try
			{
				boolean gz = inputPath.toString().endsWith(".gz");

				InputStream inStream = Files.newInputStream(inputPath);
				if (gz)
				{
					//System.err.println("compressed:" + inStream.getClass().getName() + " " + inputPath.toString());
					try
					{
						inStream = new GZIPInputStream(inStream);
					} catch (Exception e)
					{
						System.err.println("Could not open gzip input stream for " + inputPath.toString());
						return;
					}
				}
				//System.err.println("create new outputStream for:" + outputPath);
				if (currentZipFS != null)
				{
					outputPath = possiblyReopen(outputPath);
					currentZipCounter++;
				}
				//Path x = 
				Files.createDirectories(outputPath.getParent());
				//System.err.println("created Path " + x);
				OutputStream outStream = new BufferedOutputStream(Files.newOutputStream(outputPath));

			
				System.err.println("apply conversion " + p.getClass() + ":  "  + inputPath +   " -->  " + outputPath);
				try 
				{
					p.handleFile(inStream,outStream) ;
					outStream.close();
				} catch (Exception e)
				{
					System.err.println("Exception in conversion " + p.getClass());
					e.printStackTrace();
					outStream.close();
					try
					{
						Files.delete(outputPath);
					} catch (Exception e1)
					{
						System.err.println("could not delete "  + outputPath);
					}
				}
				//BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outStream));
				//bw.write("hallo heren, staat hier dan echt niks in???");
				//bw.close();
				inStream.close();
				//outStream.close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		} else if (Files.isRegularFile(inputPath) && Files.isDirectory(outputPath)) // boven behandeld...
		{
			///System.err.println("Output is directory... "  + inputPath + " " + outputPath.toString());
			Path pNew = outputPath.getFileSystem().getPath(inputPath.toString().replaceAll(".gz","").replaceAll("^/", "")); // nee dit werkt dus NIET

			traverseDirectory(p, inputPath, pNew, fileFilter);
		} else if (Files.isRegularFile(inputPath)) // boven behandeld...
		{
			System.err.println("Should not be here: "  + inputPath + " " + outputPath.toString() + ":" + outputPath.getClass().getName());
		} else if (Files.isDirectory(inputPath))
		{
			//System.err.println("directory : " + inputPath);
			if (!Files.exists(outputPath))
			{
				try
				{
					//System.err.println("creating directories for: " + outputPath);
					Files.createDirectories(outputPath);
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try
			{
				DirectoryStream<Path> dir = Files.newDirectoryStream(inputPath);
				for (Path entry: dir)
				{
					String lastPart = entry.getName(entry.getNameCount()-1).toString();
					Path oPath = outputPath.getFileSystem().getPath(outputPath.toString(), lastPart);
					//System.err.println("oPath: " + oPath);
					if (Files.isDirectory(entry))
					{
						// create output
						try
						{
							Files.createDirectories(oPath);
							traverseDirectory(p, entry, oPath, fileFilter);
						} catch (Exception e)
						{
							e.printStackTrace();
						}
					} else
					{
						// make sure parent dir exists...
						traverseDirectory(p, entry, oPath, fileFilter);
					}
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static File copyStreamToTempFile(InputStream fis, boolean compressed) throws Exception 
	{
		File f = File.createTempFile("temp_stream_", compressed?".txt.gz":".txt");
		FileOutputStream fos = new FileOutputStream(f);
		try 
		{
			byte[] buf = new byte[1024];
			int i = 0;
			while ((i = fis.read(buf)) != -1) 
			{
				fos.write(buf, 0, i);
			}
			return f;
		} 
		catch (Exception e) 
		{
			throw e;
		}
		finally 
		{
			//if (fis != null) fis.close();
			if (fos != null) fos.close();
		}
	}

	static public Path possiblyReopen(Path p)
	{
		FileSystem fs = p.getFileSystem();
		//System.err.println("PROVIDER:" + fs.provider());
		if (!(fs.provider() instanceof com.sun.nio.zipfs.ZipFileSystemProvider))
		{
			return p;
		}
		if (fs != currentZipFS )
		{
			String s = p.toString();
			Path p1 = currentZipFS.getPath(s);
			//System.err.println("Reopened old path for " + p1.toString());
			return p1;
		}
		//if (fs.provider()  instanceof )
		try
		{
			if (currentZipCounter > 0 && currentZipCounter % portionSize == 0)
			{
				String s = p.toString();
				currentZipFS.close();
				// currentZipFS.
				System.err.println("Closing zip at path " + p.toString());
				Map<String,String> env = new HashMap<String,String>();

				env.put("create", "true");

				FileSystem zipfs = FileSystems.newFileSystem(currentZipURI,env);
				Path pathInZipfile = zipfs.getPath("/");
				Path x = Files.createDirectories(pathInZipfile);

				currentZipFS = zipfs;
				currentZipCounter = 0;
				Path p1 = zipfs.getPath(s);
				System.err.println("Reopened path for " + p1.toString());
				return p1;
			} else
				return p;
		} catch (Exception e)
		{
			e.printStackTrace();
			return p;
		}
	}

}
