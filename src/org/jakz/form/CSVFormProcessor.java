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
	public boolean settingBlankCharactersNull = false;
	public boolean settingSkipVariableOnError = true;
	public boolean settingSkipRowOnError = true;
	public boolean settingSkipUnmappedColumns = true;
	
	
	protected InputStream sourceIS;
	protected Form targetForm, templateQuery;
	protected CSVFormat formatToUse;
	
	
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
	
	public CSVFormProcessor populateFormDataFromFile(boolean firstRowNames, Charset charset, ArrayList<String> messageList) throws IOException, FormException
	{
		boolean hasPopulationErrors = false;
		
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
				String fileColumnName = cellIt.next().trim();
				fileColumns.add(fileColumnName);
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
				
				if(settingSkipUnmappedColumns&&!templateQuery.content.getHasKey(fileColumnName))
					continue;
				
				if(cellContent.length()>0)
					rowIsEmpty=false;
				
				if(cellContent.trim().length()>0)
					rowIsBlank=false;
				
				Form newVar = new Form(fileColumnName,FieldType.VAR);
				try
				{
					
					if(templateQuery!=null&&templateQuery.content.containsKey(fileColumnName))
					{
						Form templateVar = templateQuery.content.getValue(fileColumnName);
						newVar =templateVar.createNewDcopy();
						parseFormValue(newVar,cellContent);
						q.add(newVar);
					}
					else if(!(settingSkipUnmappedColumns&&!templateQuery.content.getHasKey(fileColumnName)))
					{
						//newVar = new Form(fileColumnName,FieldType.VAR);
						newVar.setValue(Types.NVARCHAR);
						parseFormValue(newVar,cellContent);
						q.add(newVar);
					}
				}
				catch (Exception e)
				{
					hasPopulationErrors=true;
					newVar.errorFlag=true;
					String s = "Syntax error for variable "+fileColumnName+" ("+ iCell+","+iRow+"). Cell content ["+cellContent+"].";
					s=s+" Data type="+newVar.getValue().getTypeString()+", Nullable="+newVar.nullable+", Length="+newVar.getValue().getSizeLimit();
					
					messageList.add(s);
					newVar.errorMessage=s;
				}
			}
			
			if(
					(rowIsEmpty&&(settingSkipEmptyRows||settingSkipBlankRows))
					|| 
					(rowIsBlank&&settingSkipBlankRows)
				)
			{
				messageList.add("Skipping row index "+iRow+" (blank or empty)");
				continue;
			}
			
			targetForm.add(q);
		}
		
		if(hasPopulationErrors)
			throw new FormException("There were errors when populationg the form - see the error list");
		
		return this;
	}
	
	public Form parseFormValue(Form target, String stringToParse) throws FormException
	{
		
		if(target.getValueType()==java.sql.Types.INTEGER)
		{
			if(stringToParse.trim().length()==0)
				target.setValueInteger(null);
			else
				target.setValueInteger(Integer.parseInt(stringToParse.trim()));
		}
		else if(target.getValueType()==java.sql.Types.DOUBLE)
		{
			if(stringToParse.trim().length()==0)
				target.setValueDouble(null);
			else
				target.setValueDouble(Double.parseDouble(stringToParse.trim()));
		}
		else if(target.getValueType()==java.sql.Types.BOOLEAN)
		{
			if(stringToParse.trim().length()==0)
				target.setValueBoolean(null);
			else
				target.setValueBoolean(Boolean.parseBoolean(stringToParse.trim()));
		}
		else if(target.getValueType()==java.sql.Types.VARCHAR)
		{
			if(settingBlankCharactersNull && stringToParse.trim().length()==0)
				target.setValueVarchar(null);
			else
				target.setValueVarchar(stringToParse);
		}
		else if(target.getValueType()==java.sql.Types.NVARCHAR)
		{
			if(settingBlankCharactersNull && stringToParse.trim().length()==0)
				target.setValueNvarchar(null);
			else
				target.setValueNvarchar(stringToParse);
		}
		else if(target.getValueType()==java.sql.Types.TIMESTAMP)
		{	
			if(stringToParse.trim().length()==0)
				target.setValueTimestamp(null);
			else
				target.setValueTimestamp(Long.parseLong(stringToParse.trim()));
		}
		else if(target.getValueType()==java.sql.Types.BIGINT)
		{
			if(stringToParse.trim().length()==0)
				target.setValueBigint(null);
			else
				target.setValueBigint(Long.parseLong(stringToParse.trim()));
		}
		else
			throw new FormException("SQL type unknown");
		
		return target;
	}
}
