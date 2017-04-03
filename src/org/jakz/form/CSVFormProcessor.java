package org.jakz.form;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CSVFormProcessor 
{
	
	public static CSVFormat csvFormatCSVSTD = CSVFormat.DEFAULT.withAllowMissingColumnNames(false);
	public static CSVFormat csvFormatCSVIMPROVED = CSVFormat.DEFAULT.withDelimiter(';').withAllowMissingColumnNames(false);
	public static CSVFormat csvFormatTSV = CSVFormat.DEFAULT.withDelimiter('\t').withAllowMissingColumnNames(false);
	
	
	private File sourceFile;
	private Form targetForm;
	private CSVFormat formatToUse;
	
	
	public CSVFormProcessor setSource(File nSource)
	{
		sourceFile=nSource;
		return this;
	}
	
	public CSVFormProcessor setTarget(Form nTarget)
	{
		targetForm=nTarget;
		return this;
	}
	
	public CSVFormProcessor setFormat(CSVFormat nFormat)
	{
		formatToUse=nFormat;
		return this;
	}
	
	public CSVFormProcessor produceFormFromFile() throws IOException
	{
		if(targetForm==null)
			targetForm=new Form("CSVForm");
		
		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		CSVParser parser = formatToUse.parse(reader);
		Iterator<CSVRecord> rowIt = parser.iterator();
		Iterator<String> cellIt;
		
		CSVRecord currentRow=null;
		String currentCell;
		while(rowIt.hasNext())
		{
			currentRow = rowIt.next();
			
			cellIt = currentRow.iterator();
			
			for(int iCell=0; cellIt.hasNext(); iCell++)
			{
				//TODO
				
			}
		}
		
		return this;
	}
}
