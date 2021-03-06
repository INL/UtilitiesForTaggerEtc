package org.ivdnt.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TabSeparatedFile
{
	BufferedReader b =null;
	public String[] fieldNames;
	public String[] currentRow = null;
	String separator = "\t";
	Map<String,Integer> fieldNumbers = new HashMap<>();
	List<String[]> rows = new ArrayList<>();
	public boolean munched = false;

	public TabSeparatedFile(String fileName, String[] fields)
	{
		init(fileName, fields);
	}

	public TabSeparatedFile(BufferedReader b, String[] fields)
	{
		this.b = b;
		this.fieldNames = fields;
		for (int i=0; i < fieldNames.length; i++)
			fieldNumbers.put(fieldNames[i],i);
	}

	public void init(InputStream s, String[] fields)
	{
		try {
			this.b = new BufferedReader(new InputStreamReader(s, "utf8" ));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.fieldNames = fields;
		for (int i=0; i < fieldNames.length; i++)
			fieldNumbers.put(fieldNames[i],i);
	}

	public TabSeparatedFile(String fileName, String[] fields, boolean fromJar)
	{
		if (!fromJar)
			init(fileName, fields);
		else
		{
			InputStream s = Resource.openStream(fileName);
			init(s,fields);
		}
	}

	public void init(String fileName, String[] fields)
	{

		{
			try {
				b = new BufferedReader(new FileReader(fileName));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.fieldNames = fields;
			for (int i=0; i < fieldNames.length; i++)
				fieldNumbers.put(fieldNames[i],i);
		}
	}

	public void munch() throws IOException
	{
		String s;

		while  (( (s = b.readLine()) != null))
		{
			currentRow = s.split(separator);
			rows.add(currentRow);
		}

		munched = true;
	}

	public int size() throws IOException
	{
		if (!munched)
			munch();
		return rows.size();
	}

	public String[] getRow(int i) throws IOException
	{
		if (!munched)
			munch();
		return rows.get(i);
	}

	public String[] getLine()
	{
		String s;
		try {
			if  (( (s = b.readLine()) != null))
			{
				currentRow = s.split(separator);
				return currentRow;
			}
		} catch (IOException e)
		{

			e.printStackTrace();
		}
		currentRow = null;
		return null;
	}


	public String getField(String s)
	{
		if (currentRow != null)
		{
			try
			{
				return currentRow[fieldNumbers.get(s)];
			} catch (Exception e)
			{

			}
		}
		return null;
	}

	public String getField(String s, int k) throws IOException
	{
		String[] row = getRow(k);
		if (row != null)
		{
			try
			{
				return row[fieldNumbers.get(s)];
			} catch (Exception e)
			{

			}
		}
		return null;
	}

	public List<String> getColumn(String colName) throws IOException
	{
		List<String> r = new ArrayList<>();
		if (!munched)
			munch();
		for (int i=0; i < size(); i++)
		{
			r.add(getField(colName,i));
		}
		return r;
	}
}
