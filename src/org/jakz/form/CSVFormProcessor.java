package org.jakz.form;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jakz.common.TypedValue;
import org.jakz.form.Form.FieldType;

public class CSVFormProcessor 
{
	
	public static CSVFormat csvFormatCSVSTD = CSVFormat.DEFAULT.withAllowMissingColumnNames(false);
	public static CSVFormat csvFormatCSVIMPROVED = CSVFormat.DEFAULT.withDelimiter(';').withAllowMissingColumnNames(false);
	public static CSVFormat csvFormatTSV = CSVFormat.DEFAULT.withDelimiter('\t').withAllowMissingColumnNames(false);
	
	
	private File sourceFile;
	private Form targetForm, templateForm;
	private CSVFormat formatToUse;
	
	
	public CSVFormProcessor() 
	{
		formatToUse=csvFormatCSVSTD;
	}
	
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
	
	public CSVFormProcessor setTemplate(Form nTemplate)
	{
		templateForm=nTemplate;
		return this;
	}
	
	public CSVFormProcessor setFormat(CSVFormat nFormat)
	{
		formatToUse=nFormat;
		return this;
	}
	
	public CSVFormProcessor populateFormDataFromFile(boolean firstRowNames) throws IOException, FormException
	{
		if(targetForm==null)
			targetForm=new Form("CSVForm");
		
		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		CSVParser parser = formatToUse.parse(reader);
		Iterator<CSVRecord> rowIt = parser.iterator();
		Iterator<String> cellIt;
		
		ArrayList<String> fileColumns = new ArrayList<String>();
		
		CSVRecord currentRow=null;
		
		//names
		if(firstRowNames && rowIt.hasNext())
		{
			currentRow = rowIt.next();
			cellIt = currentRow.iterator();
			
			while(cellIt.hasNext())
			{
				String columnName = cellIt.next().trim();
				fileColumns.add(columnName);
			}
		}
		
		
		Form templateQuery = null;
		
		if(templateForm!=null&&templateForm.getHasContent())
			templateQuery=templateForm.content.getValueAt(0);
		
		for(int iRow=0; rowIt.hasNext(); iRow++)
		{
			currentRow = rowIt.next();
			cellIt = currentRow.iterator();
			
			for(int iCell=0; cellIt.hasNext(); iCell++)
			{
				String fileColumnName = fileColumns.get(iCell);
				String cellContent = cellIt.next();
				
				Form q = targetForm.addQuery(""+iRow);
				if(templateQuery!=null&&templateQuery.content.containsKey(fileColumnName))
				{
					Form templateVar = templateQuery.content.getValue(fileColumnName);
					Form newVar =templateVar.createNewDcopy();
					parseString(newVar.value.get(0),cellContent);
					q.add(newVar);
				}
				else
				{
					Form newVar = new Form(fileColumnName,FieldType.VAR);
					newVar.addValue(new TypedValue(Types.NVARCHAR));
					parseString(newVar.value.get(0),cellContent);
				}
			}
		}
		
		return this;
	}
	
	public static void parseString(TypedValue target, String stringToParse) throws FormException
	{
		if(target.getType()==java.sql.Types.INTEGER)
			target.setInteger(Integer.parseInt(stringToParse));
		else if(target.getType()==java.sql.Types.DOUBLE)
			target.setDouble(Double.parseDouble(stringToParse));
		else if(target.getType()==java.sql.Types.BOOLEAN)
			target.setBoolean(Boolean.parseBoolean(stringToParse));
		else if(target.getType()==java.sql.Types.VARCHAR)
			target.setVarchar(stringToParse);
		else if(target.getType()==java.sql.Types.NVARCHAR)
			target.setNvarchar(stringToParse);
		else if(target.getType()==java.sql.Types.TIMESTAMP)
			target.setTimestamp(Long.parseLong(stringToParse));
		else if(target.getType()==java.sql.Types.BIGINT)
			target.setBigint(Long.parseLong(stringToParse));
		else
			throw new FormException("SQL type unknown");
	}
}
