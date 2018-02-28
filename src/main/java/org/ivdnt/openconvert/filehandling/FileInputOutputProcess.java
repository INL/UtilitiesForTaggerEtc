package org.ivdnt.openconvert.filehandling;

public interface FileInputOutputProcess {
	void handleFile(String inFileName, String outFileName) throws SimpleProcessException;
}
