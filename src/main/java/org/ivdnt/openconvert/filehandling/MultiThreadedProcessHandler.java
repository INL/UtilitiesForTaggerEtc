package org.ivdnt.openconvert.filehandling;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MultiThreadedProcessHandler extends SimpleInputOutputProcess implements DoSomethingWithFile, DoSomethingWithStream
{
	private static final ExecutorService pool = Executors.newFixedThreadPool(4);

	DoSomethingWithFile unaryFileHandler;
	DoSomethingWithStream unaryStreamHandler;

	FileInputOutputProcess binaryFileHandler;
	StreamInputOutputProcess binaryStreamHandler;

	public MultiThreadedProcessHandler(DoSomethingWithFile handler) { this.unaryFileHandler = handler; }
	public MultiThreadedProcessHandler(DoSomethingWithStream handler) { this.unaryStreamHandler = handler; }
	public MultiThreadedProcessHandler(FileInputOutputProcess handler) { this.binaryFileHandler = handler; }
	public MultiThreadedProcessHandler(StreamInputOutputProcess handler) { this.binaryStreamHandler = handler; }

	@Override
	public void handleFile(String fileName)	{
		handleFile(this.unaryFileHandler, fileName);
	}

	public static Future<Void> handleFile(final DoSomethingWithFile handler, final String fileName) {
		if (handler == null || fileName == null)
			throw new NullPointerException();

		return pool.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				handler.handleFile(fileName);
				return null;
			}
		});
	}

	@Override
	public void handleStream(InputStream stream) {
		handleStream(this.unaryStreamHandler, stream);
	}

	public static Future<Void> handleStream(final DoSomethingWithStream handler, final InputStream stream) {
		if (handler == null || stream == null)
			throw new NullPointerException();

		return pool.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				handler.handleStream(stream);
				return null;
			}
		});
	}

	@Override
	public void handleFile(String inFile, String outFile) {
		handleFile(this.binaryFileHandler, inFile, outFile);
	}

	public static Future<Void> handleFile(final FileInputOutputProcess handler, final String inFile, final String outFile) {
		if (handler == null || inFile == null || outFile == null)
			throw new NullPointerException();

		return pool.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				handler.handleFile(inFile, outFile);
				return null;
			}
		});
	}

	@Override
	public void handleStream(InputStream is, Charset ics, OutputStream os) {
		handleStream(this.binaryStreamHandler, is, ics, os);
	}

	public static Future<Void> handleStream(final StreamInputOutputProcess handler, final InputStream is, final Charset ics, final OutputStream os) {
		if (handler == null || is == null || os == null) // ics may be null
			throw new NullPointerException();

		return pool.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				handler.handleStream(is, ics, os);
				return null;
			}
		});
	}

	public static void shutdown() throws InterruptedException
	{
		pool.shutdown();
		while (!pool.isTerminated()) {
			Thread.sleep(1000);
		}
	}
}
