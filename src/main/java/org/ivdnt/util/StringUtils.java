package org.ivdnt.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	private StringUtils() {}

	public static String unescapeOctal(String s)
	{
		if (s == null)
			return null;
		Pattern p = Pattern.compile("\\\\([0-9]+)");
		Matcher m = p.matcher(s);
		int prevEnd = 0;

		List<Byte> bytes = new ArrayList<>();

		while (m.find())
		{
			int start = m.start();
			int end = m.end();
			byte[] xx = s.substring(prevEnd,start).getBytes();
			for (byte b: xx)
				bytes.add(b);
			byte o = (byte) Integer.parseInt(m.group(1),8);
			bytes.add(o);
			prevEnd = end;
		}
		byte[] xx = s.substring(prevEnd,s.length()).getBytes();
		for (byte b: xx)
			bytes.add(b);
		byte[] ba = new byte[bytes.size()];
		for (int i=0; i < bytes.size(); i++)
			ba[i] = bytes.get(i);
		String r = new String(ba);
		r = r.replaceAll("\\\\'", "'");
		return r;
	}

	// TODO precompile pattern
	private static final String punctuation = "\\p{P}+";

    public static String stripPunctuation(String s)
    {
    	s = s.replaceAll(punctuation, "");
    	return s;
    }
}
