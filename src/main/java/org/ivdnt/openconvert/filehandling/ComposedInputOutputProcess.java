package org.ivdnt.openconvert.filehandling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class ComposedInputOutputProcess extends SimpleInputOutputProcess
{
	List<StreamInputOutputProcess> steps = new ArrayList<>();
	public ComposedInputOutputProcess(SimpleInputOutputProcess s1, SimpleInputOutputProcess s2)
	{
		steps.add(s1);
		steps.add(s2);
	}

	public List<StreamInputOutputProcess> getSteps()
	{
		return steps;
	}
	public ComposedInputOutputProcess(List<SimpleInputOutputProcess> l)
	{
		steps.addAll(l);
	}

        @Override
        public void handleFile(String inFilename, String outFilename)// throws ConversionException
        {
                // TODO Auto-generated method stub
                File previousOut = null;
                for (int i =0; i < steps.size(); i++)
                {
                        SimpleInputOutputProcess step = (SimpleInputOutputProcess) steps.get(i);
                        String in = i==0?inFilename:previousOut.getPath();
                        String out = null;
                        File x = null;
                        if (i==steps.size()-1)
                                out = outFilename;
                        else
                        {
                                try
                                {
                                        x = File.createTempFile("step.", "tmp");
                                        out = x.getPath();
                                        x.delete(); // this is ugly and wrong
                                } catch (Exception e)
                                {
                                        e.printStackTrace();
                                }
                        }
                        try
                        {
                           step.handleFile(in, out);
                         } catch (Exception e) { e.printStackTrace(); }
                        previousOut = x;
                }
        }

	@SuppressWarnings("deprecation")
    @Override
	public void handleStream(InputStream is, Charset ics, OutputStream os) throws IOException, SimpleProcessException {
		try {
			ByteArrayOutputStream _os = new ByteArrayOutputStream(); // can be reused between steps, close() is no-op

			for(int i = 0; i < steps.size(); ++i) {
				InputStream stepInput = i == 0 ? is : new ByteArrayInputStream(_os.toByteArray()); // use original input on first step
				OutputStream stepOutput = i == steps.size() -1 ? os : _os; // use original output on last step
				_os.reset();

				StreamInputOutputProcess proc = steps.get(i);
				proc.handleStream(stepInput, ics, stepOutput);
			}
		}
		finally {
			// TODO should be handled by calling code
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
		}
	}
}
