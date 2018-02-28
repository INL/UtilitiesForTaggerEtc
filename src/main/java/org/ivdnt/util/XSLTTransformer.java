package org.ivdnt.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.ivdnt.openconvert.filehandling.DirectoryHandling;
import org.ivdnt.openconvert.filehandling.SimpleInputOutputProcess;
import org.ivdnt.openconvert.filehandling.SimpleProcessException;
import org.w3c.dom.Document;


/**
 * Wrapper voor de xsltc engine. Deze klasse zet met behulp van de XSLT engine (o.a. Transformer.java) een XML
 * artikel om in HTML.
 */

public class XSLTTransformer extends SimpleInputOutputProcess
{
	private Transformer transformer = null;
	private TransformerFactory tFactory;
	private String xslInUri = null;
	private boolean useSaxon = true;
	InputStream xslReader = null;
	int jobId=0;

	private synchronized void nextJob()
	{
		jobId++;
		setParameter("jobNumber","" + jobId);
	}

	public XSLTTransformer(String xslInUri)
	{
		String key = "javax.xml.transform.TransformerFactory";
		// N.B. Aangepast ivm nieuwe Javaversie (1.5 of 1.6), was vroeger:
		// com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl
		String value = "org.apache.xalan.processor.TransformerFactoryImpl";
		if (useSaxon)
		{
			value = "net.sf.saxon.TransformerFactoryImpl";
		}
		Properties props = System.getProperties();
		props.put(key, value);
		System.setProperties(props);
		tFactory = TransformerFactory.newInstance();
		this.xslInUri = xslInUri;
		loadStylesheet();
		if (this.transformer == null)
		{
			System.err.println("EEK!");
			System.exit(1);
		}
	}

	public XSLTTransformer(InputStream xslReader)
	{
		String key = "javax.xml.transform.TransformerFactory";
		// N.B. Aangepast ivm nieuwe Javaversie (1.5 of 1.6), was vroeger:
		// com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl
		String value = "org.apache.xalan.processor.TransformerFactoryImpl";
		if (useSaxon)
		{
			value = "net.sf.saxon.TransformerFactoryImpl";
		}
		Properties props = System.getProperties();
		props.put(key, value);
		System.setProperties(props);
		tFactory = TransformerFactory.newInstance();

		this.xslReader = xslReader;
		loadStylesheet();
		if (this.transformer == null)
		{
			System.err.println("EEK!");
			System.exit(1);
		}
	}

	private void loadStylesheet()
	{
		if (xslInUri != null)
		{
			try
			{
				this.transformer = tFactory.newTransformer(new StreamSource(this.xslInUri));
			} catch (TransformerConfigurationException e)
			{
				e.printStackTrace();
			}
		}
		if (xslReader != null)
		{
			try
			{
				this.transformer = tFactory.newTransformer(new StreamSource(xslReader));
			} catch (TransformerConfigurationException e)
			{
				e.printStackTrace();
			}
		}
	}

	public XSLTTransformer(Transformer transformer)
	{
		this.transformer = transformer;
	}

	public void setParameter(String name, String value)
	{
		transformer.setParameter(name, value);
	}
	/**
	 * Voert de transformatie uit. De input bestaat uit een String met de XML code.
	 * @param instring
	 *
	 * @param(STring  instring
	 *            De input string
	 * @param out
	 *            java.io.Writer object Het resultaat van het transformeren van de XML code.
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 * @throws IOException
	 */

	public void transformString(String instring, Writer out)
		throws TransformerConfigurationException, TransformerException, IOException
	{
		StreamSource source = new StreamSource(new StringReader(instring));
		StreamResult result = new StreamResult(out);

		transformer.transform(source, result);
		out.flush();
	}

	public Document transformDocument(Document inDocument)
	{
		try
		{
			DOMSource source = new DOMSource(inDocument);
			DOMResult result = new DOMResult();
			transformer.transform(source, result);
			return (Document) result.getNode();
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * Dit gaat niet goed als de input niet utf8 is
	 */
	public void transformFile(String inFileName, String outFileName)
	{
		try {
			handleFile(inFileName, outFileName);
		} catch (SimpleProcessException e) {
			e.printStackTrace();
		}
	}

	public void transformStream(Reader reader, Writer writer) {
		try {
			nextJob();
			transformer.transform(new StreamSource(reader), new StreamResult(writer));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handleStream(InputStream is, Charset ics, OutputStream os) throws SimpleProcessException, IOException {
		if (ics == null)
			ics = StandardCharsets.UTF_8;

		try (
			Reader reader = new InputStreamReader(is, ics);
			Writer writer = new OutputStreamWriter(os, ics);
		) {
			transformStream(reader, writer);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		XSLTTransformer p = new XSLTTransformer(args[0]);
		DirectoryHandling.tagAllFilesInDirectory(p, args[1], args[2]);
	}
}

