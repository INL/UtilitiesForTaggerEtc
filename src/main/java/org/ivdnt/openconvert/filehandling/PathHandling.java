package org.ivdnt.openconvert.filehandling;

import java.io.BufferedInputStream;
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
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;


public class PathHandling
{
	static int max = Integer.MAX_VALUE;
	static FileSystem currentZipFS = null;
	static URI currentZipURI = null;
	static int currentZipCounter = 0;
	static int portionSize = 50;

	public static void traverseDirectory(StreamInputOutputProcess p, Path inputPath, Path outputPath, FileFilter fileFilter)
	{
		outputPath = possiblyReopen(outputPath);
		if (!Files.exists(inputPath))
		{
			System.err.println("bestaat NIET? "  + inputPath);
		}
		if (outputPath.toString().endsWith(".zip"))
		{
			Map<String,String> env = new HashMap<>();
			env.put("create", "true");
			URI uri = URI.create("jar:file:" + outputPath.toString().replaceAll("\\\\", "/"));
			currentZipURI = uri;

			// dit kan niet bij multithreaded toepassing, dan kan je pas achteraf sluiten
			try (FileSystem zipfs = FileSystems.newFileSystem(uri,env))
			{
				Path pathInZipfile = zipfs.getPath("/");
				Path x = Files.createDirectories(pathInZipfile);

				currentZipFS = zipfs;

				traverseDirectory(p, inputPath, x, fileFilter);
				currentZipFS = null;
				currentZipCounter = 0;
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		} else if  (Files.isRegularFile(inputPath) &&  (inputPath.toString().toLowerCase().endsWith(".tar.gz")  || inputPath.toString().toLowerCase().endsWith(".tar")))
		{
			boolean tgz = inputPath.toString().toLowerCase().endsWith(".tar.gz");
			try (
				InputStream i1 = new FileInputStream(inputPath.toString());
				InputStream i2 = new BufferedInputStream(i1);
				InputStream i3 = tgz ? new GzipCompressorInputStream(i2) : i2;
				ArchiveInputStream archive = new TarArchiveInputStream(i3);
			) {
				ArchiveEntry currentEntry;
				int k=0;
				while ((currentEntry = archive.getNextEntry()) != null)
				{
					// TODO all files inside .tar archives (except .gz) are ignored!
					// Though removing this check causes the entire gz to be exploded into /temp/ ....
					// Use streams in the future, also see if we can merge this with recursive archive/directory handling in BlackLab(-server)
					boolean compressed = currentEntry.getName().toLowerCase().endsWith(".gz");
					if (!compressed)
						continue;

					File f = File.createTempFile("", currentEntry.getName());
					try (OutputStream fout = new FileOutputStream(f)) {
						// only copyLarge allows providing the amount of bytes to copy
						IOUtils.copyLarge(archive, fout, 0, currentEntry.getSize());
					}

					traverseDirectory(p, f.toPath(), outputPath, fileFilter);
					f.delete();
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
			Map<String,String> env = new HashMap<>();
			URI uri = URI.create("jar:file:" + inputPath.toString().replaceAll("\\\\", "/"));
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
		}
		else if (Files.isRegularFile(inputPath) && (!Files.exists(outputPath)||Files.isRegularFile(outputPath) ) )
		{
			boolean gz = inputPath.toString().endsWith(".gz");
			try (
				InputStream i1 = Files.newInputStream(inputPath);
				InputStream inStream = gz ? new GZIPInputStream(i1) : i1;
			) {

				if (currentZipFS != null)
				{
					outputPath = possiblyReopen(outputPath);
					currentZipCounter++;
				}
				Files.createDirectories(outputPath.getParent());

				System.err.println("apply conversion " + p.getClass() + ":  "  + inputPath +   " -->  " + outputPath);
				try (OutputStream outStream = new BufferedOutputStream(Files.newOutputStream(outputPath)))
				{
					p.handleStream(inStream, null, outStream) ;
				} catch (Exception e)
				{
					System.err.println("Exception in conversion " + p.getClass());
					e.printStackTrace();
					try
					{
						Files.delete(outputPath);
					} catch (Exception e1)
					{
						System.err.println("could not delete "  + outputPath);
					}
				}
				inStream.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			return;
		} else if (Files.isRegularFile(inputPath) && Files.isDirectory(outputPath)) // boven behandeld...
		{
			Path pNew = outputPath.getFileSystem().getPath(inputPath.toString().replaceAll(".gz","").replaceAll("^/", "")); // nee dit werkt dus NIET

			traverseDirectory(p, inputPath, pNew, fileFilter);
		} else if (Files.isRegularFile(inputPath)) // boven behandeld...
		{
			System.err.println("Should not be here: "  + inputPath + " " + outputPath.toString() + ":" + outputPath.getClass().getName());
		} else if (Files.isDirectory(inputPath))
		{
			if (!Files.exists(outputPath))
			{
				try
				{
					Files.createDirectories(outputPath);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			try (DirectoryStream<Path> dir = Files.newDirectoryStream(inputPath))
			{
				for (Path entry: dir)
				{
					String lastPart = entry.getName(entry.getNameCount()-1).toString();
					Path oPath = outputPath.getFileSystem().getPath(outputPath.toString(), lastPart);
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

	@SuppressWarnings("restriction") // maybe do something about this?
	public static Path possiblyReopen(Path p)
	{
		FileSystem fs = p.getFileSystem();
		if (!(fs.provider() instanceof com.sun.nio.zipfs.ZipFileSystemProvider))
		{
			return p;
		}
		if (fs != currentZipFS )
		{
			String s = p.toString();
			Path p1 = currentZipFS.getPath(s);
			return p1;
		}

		try
		{
			if (currentZipCounter > 0 && currentZipCounter % portionSize == 0)
			{
				String s = p.toString();
				currentZipFS.close();
				// currentZipFS.
				System.err.println("Closing zip at path " + p.toString());
				Map<String,String> env = new HashMap<>();

				env.put("create", "true");

				FileSystem zipfs = FileSystems.newFileSystem(currentZipURI,env);
				Path pathInZipfile = zipfs.getPath("/");
				Files.createDirectories(pathInZipfile);

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
