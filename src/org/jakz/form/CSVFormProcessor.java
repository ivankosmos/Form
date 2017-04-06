package org.jakz.form;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
	
	public boolean settingSkipEmptyRows = true;
	public boolean settingSkipBlankRows = true;
	public boolean settingBlankCharactersNull = true;
	
	
	private InputStream sourceIS;
	private Form targetForm, templateQuery;
	private CSVFormat formatToUse;
	
	
	public CSVFormProcessor() 
	{
		formatToUse=csvFormatCSVSTD;
	}
	
	public CSVFormProcessor setSource(InputStream nSource)
	{
		sourceIS=nSource;
		return this;
	}
	
	public CSVFormProcessor setTargetForm(Form nTarget)
	{
		targetForm=nTarget;
		return this;
	}
	
	public CSVFormProcessor setTemplateQuery(Form nTemplate)
	{
		templateQuery=nTemplate;
		return this;
	}
	
	public CSVFormProcessor setFormat(CSVFormat nFormat)
	{
		formatToUse=nFormat;
		return this;
	}
	
	public CSVFormProcessor populateFormDataFromFile(boolean firstRowNames, Charset charset) throws IOException, FormException
	{
		if(targetForm==null)
			targetForm=new Form("CSVForm");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(sourceIS,charset));
		CSVParser parser = formatToUse.parse(reader);
		Iterator<CSVRecord> rowIt = parser.iterator();
		Iterator<String> cellIt;
		
		ArrayList<String> fileColumns = new ArrayList<String>();
		
		CSVRecord currentRow=null;
		
		boolean rowIsEmpty;
		boolean rowIsBlank;
		
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
		
		
		for(int iRow=0; rowIt.hasNext(); iRow++)
		{
			Form q = new Form(""+iRow,FieldType.QRY);
			currentRow = rowIt.next();
			cellIt = currentRow.iterator();
			rowIsEmpty=true;
			rowIsBlank=true;
			
			for(int iCell=0; cellIt.hasNext(); iCell++)
			{
				String fileColumnName = fileColumns.get(iCell);
				String cellContent = cellIt.next();
				
				if(cellContent.length()>0)
					rowIsEmpty=false;
				
				if(cellContent.trim().length()>0)
					rowIsBlank=false;
				
				
				if(templateQuery!=null&&templateQuery.content.containsKey(fileColumnName))
				{
					Form templateVar = templateQuery.content.getValue(fileColumnName);
					Form newVar =templateVar.createNewDcopy();
					parseTypedValue(newVar.value.get(0),cellContent);
					q.add(newVar);
				}
				else
				{
					Form newVar = new Form(fileColumnName,FieldType.VAR);
					newVar.addValue(new TypedValue(Types.NVARCHAR));
					parseTypedValue(newVar.value.get(0),cellContent);
					q.add(newVar);
				}
			}
			
			if(
					(rowIsEmpty&&(settingSkipEmptyRows||settingSkipBlankRows))
					|| 
					(rowIsBlank&&settingSkipBlankRows)
				)
				continue;
				
			targetForm.add(q);
		}
		
		return this;
	}
	
	public TypedValue parseTypedValue(TypedValue target, String stringToParse) throws FormException
	{
		if(target.getType()==java.sql.Types.INTEGER)
		{
			if(stringToParse.trim().length()==0)
				target.setInteger(null);
			else
				target.setInteger(Integer.parseInt(stringToParse));
		}
		else if(target.getType()==java.sql.Types.DOUBLE)
		{
			if(stringToParse.trim().length()==0)
				target.setDouble(null);
			else
				target.setDouble(Double.parseDouble(stringToParse));
		}
		else if(target.getType()==java.sql.Types.BOOLEAN)
		{
			if(stringToParse.trim().length()==0)
				target.setBoolean(null);
			else
				target.setBoolean(Boolean.parseBoolean(stringToParse));
		}
		else if(target.getType()==java.sql.Types.VARCHAR)
		{
			if(settingBlankCharactersNull && stringToParse.trim().length()==0)
				target.setVarchar(null);
			else
				target.setVarchar(stringToParse);
		}
		else if(target.getType()==java.sql.Types.NVARCHAR)
		{
			if(settingBlankCharactersNull && stringToParse.trim().length()==0)
				target.setNvarchar(null);
			else
				target.setNvarchar(stringToParse);
		}
		else if(target.getType()==java.sql.Types.TIMESTAMP)
		{	
			if(stringToParse.trim().length()==0)
				target.setTimestamp(null);
			else
				target.setTimestamp(Long.parseLong(stringToParse));
		}
		else if(target.getType()==java.sql.Types.BIGINT)
		{
			if(stringToParse.trim().length()==0)
				target.setBigint(null);
			else
				target.setBigint(Long.parseLong(stringToParse));
		}
		else
			throw new FormException("SQL type unknown");
		
		return target;
	}
}
