package org.ivdnt.openconvert.filehandling;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.ivdnt.util.XSLTTransformer;

// TODO compare with PathHandling and PathHandlingNoOutput and see which of them can go
public class DirectoryHandling
{
	public static FileFilter ff = null;
	public static boolean usePathHandler = true;

	public static void tagAllFilesInDirectory(FileInputOutputProcess p,
			String folderName, String outFolderName, boolean makeSubdirs)
	{
		if (makeSubdirs)
		{
			traverseDirectory(p,new File(folderName), new File(outFolderName), ff);
		} else
		{
			tagAllFilesInDirectory(p,folderName,outFolderName);
		}
	}

	public static  void createPath(String fileName)
	{
		String [] parts  = fileName.split(File.separator);
		String path = parts[0];
		for (int i=1; i < parts.length; i++)
		{
			File f = new File(path);
			if (!f.exists())
			{
				f.mkdir();
			}
			path = path + "/" + parts[i];
		}
	}

	static class Wrapper implements DoSomethingWithFile
	{
		FileInputOutputProcess p;
		String destinationFolder;

		public Wrapper(FileInputOutputProcess p, String destinationFolder)
		{
			this.p = p;
			this.destinationFolder = destinationFolder;
		}

		@Override
		public void handleFile(String fileName) throws SimpleProcessException
		{
			File f = new File(fileName);
			String n = f.getName();
			p.handleFile(fileName, destinationFolder + "/" + n);
		}
	}

	public static void handleZip(String filename, DoSomethingWithFile  siop)
	{
		try (
			FileInputStream fis = new FileInputStream(filename);
			ZipInputStream zipInputStream = new ZipInputStream(fis)
		) {
			File unzipTo = File.createTempFile("unzip.", ".dir");

			unzipTo.delete();
			unzipTo.mkdir();
			String destinationFolder = unzipTo.getPath();
			byte[] buf = new byte[1024];
			ZipEntry zipentry;

			zipentry = zipInputStream.getNextEntry();
			while (zipentry != null)
			{
				//for each entry to be extracted
				String entryName = zipentry.getName();
				System.out.println("entryname "+entryName);

				int n; FileOutputStream fileOutputStream;
				String f = destinationFolder + "/" + entryName;
				if (!zipentry.isDirectory())
				{
					createPath(f);
					fileOutputStream = new FileOutputStream(destinationFolder + "/" + entryName);

					while ((n = zipInputStream.read(buf, 0, 1024)) > -1)
						fileOutputStream.write(buf, 0, n);

					fileOutputStream.close();
				}
				zipInputStream.closeEntry();
				// again problems with multithreading ... cannot delete here
				siop.handleFile(f);
				File ff = new File(f);
				ff.delete(); ff.deleteOnExit();
				zipentry = zipInputStream.getNextEntry();
			}

			unzipTo.delete();
			unzipTo.deleteOnExit();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void tagAllFilesInDirectory(FileInputOutputProcess p,
			String folderName, String outFolderName)
	{
		if (usePathHandler )
		{
			traverseDirectory(p,folderName,outFolderName);
			return;
		}
		OutputToZip otz = null;
		if (outFolderName.endsWith(".zip") && ! (p instanceof org.ivdnt.openconvert.filehandling.OutputToZip))
		{
			otz = new OutputToZip(outFolderName, p);
			p = otz;
		}

		File f = new File(folderName);

		if (!f.exists())
		{
			try
			{
				File downloaded = DirectoryHandling.downloadURL(folderName);
				if (downloaded != null)
				{
					p.handleFile(downloaded.getCanonicalPath(), outFolderName);
					downloaded.delete();
				}
			} catch (Exception e)
			{

				e.printStackTrace();
			}
		}



		if (f.isFile())
		{
			if (folderName.toLowerCase().endsWith(".zip")) // ahem just a test
			{
				DoSomethingWithFile dswf = new Wrapper(p, outFolderName);
				handleZip(folderName, dswf);
			} else
			{
				File outFile = new File(outFolderName);

				if (!outFile.isDirectory())
				{
					try
					{
						p.handleFile(f.getCanonicalPath(), outFolderName);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		if (f.isDirectory())
		{
			File[] entries = f.listFiles();
			for (File x: entries)
			{
				String base = x.getName();
				System.err.println(base);
				if (x.isFile())
				{
					try
					{
						File outFile = new File( outFolderName + "/" + base);
						if (!outFile.exists())
						{
							p.handleFile(x.getCanonicalPath(), outFolderName + "/" + base);
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				} else
				{
					try
					{
						tagAllFilesInDirectory(p, x.getCanonicalPath(), outFolderName);
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		if (otz != null)
			otz.close();
	}

	/**
	 * Difference with previous: this creates subdirectories mirroring the source directory
	 * @param p
	 * @param currentDirectory
	 * @param outputDirectory
	 * @param fileFilter
	 */
	public static void traverseDirectory(FileInputOutputProcess p, File currentDirectory,
			File outputDirectory, FileFilter fileFilter)
	{
		if (usePathHandler)
		{
			try
			{
				Path pin = Paths.get(currentDirectory.getCanonicalPath());
				Path pout = Paths.get(outputDirectory.getCanonicalPath());
				// TODO cannot be correct: essential file name information is lost
				StreamInputOutputProcess s = new WrappedFileBasedConverter(p);
				PathHandling.traverseDirectory(s, pin, pout, fileFilter);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			return;
		}
		if (currentDirectory.isFile() && (!outputDirectory.exists() ||outputDirectory.isFile()) )
		{
			try
			{
				p.handleFile(currentDirectory.getCanonicalPath(),outputDirectory.getCanonicalPath()) ;
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			return;
		}
		File[] selectedFiles;
		if (currentDirectory.isFile())
		{
			selectedFiles = new File[1];
			selectedFiles[0]  = currentDirectory;
		}
		else
			selectedFiles = currentDirectory.listFiles(fileFilter); // what if null?
		if (selectedFiles != null)
		{
			Arrays.sort(selectedFiles);
			for (File f : selectedFiles)
			{
				if (f.isDirectory())
				{
					System.out.println(f.getPath());
					File outputSubdirectory = new File(outputDirectory, f.getName());
					outputSubdirectory.mkdirs();
					traverseDirectory(p, new File(currentDirectory, f.getName()),
							outputSubdirectory, fileFilter);
				} else
				{
					try
					{
						File outFile = new File( outputDirectory.getPath() + "/" + f.getName());
						if (!outFile.exists())
						{
							p.handleFile(f.getCanonicalPath(), outFile.getPath());
						}
					} catch (Exception ex)
					{
						System.err.println("Probleem met bestand " + f.getPath() + ": " + ex.toString());
					}
				}
			}
		}
	}

	public static void traverseDirectory(FileInputOutputProcess p, String folderName,
			String outputDirectory)
	{
		File f = new File(folderName);

		if (!f.exists())
		{
			try
			{
				File downloaded = DirectoryHandling.downloadURL(folderName);
				if (downloaded != null)
				{
					p.handleFile(downloaded.getCanonicalPath(), outputDirectory);
					downloaded.delete();
				}
			} catch (Exception e)
			{

				e.printStackTrace();
			}
		}

		if (f.isFile())
		{
			if (folderName.toLowerCase().endsWith(".zip")) // ahem just a test
			{
				DoSomethingWithFile dswf = new Wrapper(p, outputDirectory);
				handleZip(folderName, dswf);
			} else
			{
				File outFile = new File(outputDirectory);
				if (!outFile.isDirectory())
				{
					try
					{
						p.handleFile(f.getCanonicalPath(), outputDirectory);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (f.isDirectory())
			traverseDirectory(p, f, new File(outputDirectory), null);
	}

	public static void traverseDirectory(DoSomethingWithFile action, String folderName)
	{

		File f = new File(folderName);
		if (f.isFile())
		{
			try
			{
				action.handleFile(f.getCanonicalPath());
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		if (f.isDirectory())
		{
			File[] entries = f.listFiles();
			for (File x: entries)
			{
				if (x.isFile())
				{
					String entryName;
					try
					{
						entryName = x.getCanonicalPath();
						action.handleFile(entryName);
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				} else
				{
					try
					{
						traverseDirectory(action, x.getCanonicalPath());
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}

	private static File downloadURL(String url)
	{
		try
		{
			URL website = new URL(url);
			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			File savedURLContent = File.createTempFile("download", ".temp");

			try (FileOutputStream fos = new FileOutputStream(savedURLContent)) {
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			}
			return savedURLContent;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args)
	{
		XSLTTransformer x = new XSLTTransformer(args[0]);
		FileFilter fnf = new FileFilter()
		{
			@Override
			public boolean accept(File  file)
			{
				System.err.println("filtering: " + file);
				return file.isFile() && file.getName().endsWith(".xml");
			}
		};
		DirectoryHandling.ff =  fnf;
		DirectoryHandling.tagAllFilesInDirectory(x, args[1], args[2], true);
	}
}
