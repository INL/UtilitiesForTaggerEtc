package org.ivdnt.openconvert.filehandling;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadedStreamHandler implements DoSomethingWithStream, StreamInputOutputProcess
{
	ExecutorService pool;
	DoSomethingWithStream baseHandler1;
	StreamInputOutputProcess baseHandler2;
	
	public class UnaryTask implements Runnable
	{
		InputStream  fileName = null;
		
		public UnaryTask(InputStream  fileName)
		{
			this.fileName = fileName;
		}
		//@Override
		public void run() 
		{
			if (fileName != null && baseHandler1 != null)
				try
				{
					baseHandler1.handleFile(fileName);
				} catch (ConversionException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
	
	public class BinaryTask implements Runnable
	{
		InputStream  inFile = null;
		OutputStream outFile = null;
		public  BinaryTask(InputStream inFile, OutputStream outFile)
		{
			this.inFile = inFile;
			this.outFile = outFile;
		}
		//@Override
		public void run() 
		{
			if (inFile != null && baseHandler2 != null)
				try
				{
					baseHandler2.handleFile(inFile,outFile);
				} catch (ConversionException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
	public MultiThreadedStreamHandler(DoSomethingWithStream h, int nThreads)
	{
		this.baseHandler1 = h;
		this.pool =  Executors.newFixedThreadPool(nThreads);
	}

	public MultiThreadedStreamHandler(StreamInputOutputProcess h, int nThreads)
	{
		this.baseHandler2 = h;
		this.pool =  Executors.newFixedThreadPool(nThreads);
	}
	
	//@Override
	public void handleFile(InputStream fileName) 
	{
		// TODO Auto-generated method stub
		UnaryTask t = new UnaryTask(fileName);
		pool.execute(t);
	}

	//@Override
	public void handleFile(InputStream in, OutputStream out) 
	{
		// TODO Auto-generated method stub
		BinaryTask t = new BinaryTask(in,out);
		pool.execute(t);
	}
	
	public void shutdown()
	{
		this.pool.shutdown();
		while (!this.pool.isTerminated())
		{
			try 
			{
				Thread.sleep(100);
			} catch (InterruptedException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	static class ExampleHandler implements DoSomethingWithStream
	{
		long totalSpace = 0;
		synchronized void increment(long x)
		{
			totalSpace += x;
		}
		//@Override
		public void handleFile(InputStream fileName) 
		{
			//System.err.println(fileName + " " + Thread.currentThread().getId());
			//File f = new File(fileName);
		
		}
	}
	

	public static void main(String args[])
	{
		ExampleHandler e = new ExampleHandler();
		MultiThreadedStreamHandler m = new MultiThreadedStreamHandler(e,100);
		//PathHandlingNoOutput.traverseDirectory(m, "D:/");
		m.pool.shutdown();
	}

	//@Override
	public void setProperties(Properties properties)  throws ConversionException
	{
		// TODO Auto-generated method stub
		if (this.baseHandler2 != null)
			this.baseHandler2.setProperties(properties);
	}

	//@Override
	public void close()
	{
		// TODO Auto-generated method stub
		
	}
}
