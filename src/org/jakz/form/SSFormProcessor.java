package org.jakz.form;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.ibatis.jdbc.SQL;
import org.jakz.common.ApplicationException;
import org.jakz.common.OperationException;
import org.jakz.common.TypedValue;
import org.jakz.form.Form.FieldType;

public class SSFormProcessor
{
	static final String tableNamePrefix ="_SSFP";
	
	
	public static TypedValue setTypedValueFromSQLResultSet(TypedValue target, ResultSet r, String columnLabel) throws SQLException, OperationException
	{
		if(target.getType()==java.sql.Types.INTEGER)
			target.setInteger(r.getInt(columnLabel));
		else if(target.getType()==java.sql.Types.DOUBLE)
			target.setDouble(r.getDouble(columnLabel));
		else if(target.getType()==java.sql.Types.BOOLEAN)
			target.setBoolean(r.getBoolean(columnLabel));
		else if(target.getType()==java.sql.Types.VARCHAR)
			target.setVarchar(r.getString(columnLabel));
		else if(target.getType()==java.sql.Types.NVARCHAR)
			target.setNvarchar(r.getString(columnLabel));
		else if(target.getType()==java.sql.Types.TIMESTAMP)
			target.setTimestamp(r.getTimestamp(columnLabel).getTime());
		else if(target.getType()==java.sql.Types.BIGINT)
			target.setBigint(r.getLong(columnLabel));
		else
			throw new OperationException("SQL type unknown to TypedValue");
		
		return target;
	}
	
	public static void setPreparedStatementParameterFromTypedValue(PreparedStatement s, int psParameterNumber, TypedValue source) throws SQLException, OperationException
	{
		if(source.getType()==java.sql.Types.INTEGER)
			s.setInt(psParameterNumber, source.getValueInteger());
		else if(source.getType()==java.sql.Types.DOUBLE)
			s.setDouble(psParameterNumber, source.getValueDouble());
		else if(source.getType()==java.sql.Types.BOOLEAN)
			s.setBoolean(psParameterNumber, source.getValueBoolean());
		else if(source.getType()==java.sql.Types.VARCHAR)
			s.setString(psParameterNumber, source.getValueVarchar());
		else if(source.getType()==java.sql.Types.NVARCHAR)
			s.setNString(psParameterNumber, source.getValueNVarchar());
		else if(source.getType()==java.sql.Types.TIMESTAMP)
			s.setTimestamp(psParameterNumber, new java.sql.Timestamp(source.getValueTimestamp()));
		else if(source.getType()==java.sql.Types.BIGINT)
			s.setLong(psParameterNumber, source.getValueBigint());
		else
			throw new OperationException("SQL type unknown to TypedValue");
	}
	
	//TODO use modified DataEntry? Merge DataEntry and Form
	//TODO create offset & limit functionality
	/**
	 * Populates the form according to the template.
	 * Can only handle SQL Server paths for this connection
	 * @param toPopulate
	 * @param c
	 * @return
	 * @throws OperationException 
	 * @throws SQLException 
	 * @throws FormException 
	 */
	public static Form populateFormFromDB(Form toPopulate, Connection c, Form queryTemplate) throws SQLException, FormException
	{
		if(toPopulate.content.size()>1)
			throw new FormException("Form object is already populated");
		
		String mainTablePath = toPopulate.getEvaluatedDBPath();
		if(mainTablePath==null)
			throw new FormException("No main table path");
		
		Statement s = c.createStatement();
		String sqlQuery = SSFormProcessor.constructFormSelectQuery(toPopulate);
		ResultSet r = s.executeQuery(sqlQuery);
		for(long iRow = 0;r.next(); iRow++)
		{
			Form newQuery = queryTemplate.createNewDcopy();
			newQuery.id=""+iRow;
			
			for(int iColumn=0; iColumn<newQuery.content.size(); iColumn++)
			{
				Form colVar = newQuery.content.getValueAt(iColumn);
				try 
				{
					setTypedValueFromSQLResultSet(colVar.value,r, colVar.dataSourcePath);
				} catch (OperationException e) 
				{
					throw new FormException(e);
				}
			}
			
			toPopulate.add(newQuery);
		}
		
		return toPopulate;
	}
	
	public static void populateDBFromForm(Connection c, Form source) throws FormException, SQLException, OperationException
	{
		String mainTablePath = source.getEvaluatedDBPath();
		if(mainTablePath==null)
			throw new FormException("No main table path");
		
		if(source.content.size()>0&&source.content.getValueAt(0).content.size()>0) //if form has columns in first row
		{
			
			String sqlQuery = SSFormProcessor.constructFormInsertUpdatePreparedStatement(source);
			PreparedStatement s = c.prepareStatement(sqlQuery);
			
			for(int iRow=0; iRow<source.content.size(); iRow++)
			{
				Form q = source.content.getValueAt(iRow);
				s.clearParameters();
				for(int iColumn = 0; iColumn<q.content.size(); iColumn++)
				{
					Form var = q.content.getValueAt(iColumn);
					if(var.dataSourcePath!=null&&var.varMapForeignKey==null)
					{
						//main table
						SSFormProcessor.setPreparedStatementParameterFromTypedValue(s, iColumn+1, var.value);
					}
					else if(var.varMapForeignKey!=null)
					{
						//foreign table
						//TODO
					}
					else throw new FormException("Form variable "+iColumn+" has no dataSourcePath or is not a foreign key.");
				}
				
				s.execute();
			}
		}
	}
	
	/**
	 * Fetches everything in one query. Uses first query as template.
	 * @param toPopulate
	 * @return
	 * @throws FormException 
	 */
	private static String constructFormSelectQuery(Form toPopulate) throws FormException
	{	
		String mainTablePath = toPopulate.getEvaluatedDBPath();
		if(mainTablePath==null)
			throw new FormException("No main table path");
		
		HashSet<String> columnHash = new HashSet<String>();
		
		final StringBuilder columns = new StringBuilder();
		final StringBuilder fromTables = new StringBuilder();
		final StringBuilder whereCondition = new StringBuilder();
		boolean hasWhere =false;
		
		//add main table
		fromTables.append(mainTablePath+" "+tableNamePrefix+"M");
		Form query =null;
		if(toPopulate.type==FieldType.FRM)
		{
			if(toPopulate.content.size()>0)
				query = toPopulate.content.getValueAt(0);
			else
				throw new FormException("Form does not have any queries to use as template");
		}
		else if(toPopulate.type==FieldType.QRY)
			query = toPopulate;
		else
			throw new FormException("Must pass a form or query type Form object.");
		
		for(int iVar=0; iVar<query.content.size(); iVar++)
		{
			Form var = query.content.getValueAt(iVar);
			String fColumnNameEntry;
			
			if(var.varMapForeignKey!=null)
			{
				String foreignTableName =  Form.get1stLevelDBName(var.varMapForeignTable);
				String foreignSchemaName = Form.get2ndLevelDBName(var.varMapForeignTable);
				
				String fTableNameEntry = Form.constructDBPath(foreignSchemaName, foreignTableName, null);
				
				fromTables.append(","+fTableNameEntry+" "+tableNamePrefix+""+iVar);
				
				
				//foreign variable
				fColumnNameEntry = tableNamePrefix+iVar+"."+var.varMapForeignLabel+" AS "+var.dataSourcePath;
				if(!columnHash.contains(fColumnNameEntry))
				{
					if(columnHash.size()>0)
						columns.append(","+fColumnNameEntry);
					else
						columns.append(fColumnNameEntry);
					columnHash.add(fColumnNameEntry);
				}
				
				
				//where condition
				if(hasWhere)
				{
					whereCondition.append(" AND "+tableNamePrefix+"M."+var.dataSourcePath+"="+tableNamePrefix+iVar+"."+var.varMapForeignKey);
				}
				else
				{
					whereCondition.append(tableNamePrefix+"M."+var.dataSourcePath+"="+tableNamePrefix+iVar+"."+var.varMapForeignKey);
					hasWhere=true;
				}
				
			}
			else if(var.dataSourcePath!=null)
			{
				fColumnNameEntry = tableNamePrefix+"M."+var.dataSourcePath;
				if(!columnHash.contains(fColumnNameEntry))
				{
					if(columnHash.size()>0)
						columns.append(","+fColumnNameEntry);
					else
						columns.append(fColumnNameEntry);
					columnHash.add(fColumnNameEntry);
				}
			}
			else throw new FormException("Form variable "+iVar+" has no dataSourcePath or is not a foreign key.");
		}
		
		String q=new SQL()
		{
			{
				SELECT(columns.toString());
				FROM(fromTables.toString());
				WHERE(whereCondition.toString());
				//ORDER_BY("");
			}
		}.toString();
		
		return q;
		
	}
	
	public static String constructFormInsertUpdatePreparedStatement(Form source) throws FormException
	{
		String mainTablePath = source.getEvaluatedDBPath();
		if(mainTablePath==null)
			throw new FormException("No main table path");
		
		
		//insert
		StringBuilder insertColumnList = new StringBuilder();
		StringBuilder insertValueSelectQuery_columns = new StringBuilder();
		StringBuilder insertValueSelectQuery_fromTables = new StringBuilder();
		StringBuilder insertValueSelectQuery_whereCondition = new StringBuilder();
		
		//update
		//ArrayList<StringBuilder> updateColumnValueList = new ArrayList<StringBuilder>();
		//ArrayList<StringBuilder> updateWhereCondition = new ArrayList<StringBuilder>();
		boolean hasKey = false;
		
		
		Form query =null;
		if(source.type==FieldType.FRM)
		{
			if(source.content.size()>0)
				query = source.content.getValueAt(0);
			else
				throw new FormException("Form does not have any queries to use as template");
		}
		else if(source.type==FieldType.QRY)
			query = source;
		else
			throw new FormException("Must pass a form or query type Form object.");
		
		
		//table and var inventory
		for(int iVar=0; iVar<query.content.size(); iVar++)
		{
			Form var = query.content.getValueAt(iVar);
			if(var.tablekey)
				hasKey=true;
			
			String insertColumnListEntry,insertValueSelectQuery_columnsEntry,insertValueSelectQuery_fromTablesEntry,insertValueSelectQuery_whereConditionEntry;
			
			if(var.dataSourcePath!=null)
			{
				//insertColumnList
				insertColumnListEntry = Form.get1stLevelDBName(var.dataSourcePath);
				if(insertColumnList.length()>0)
					insertColumnList.append(",");
				
				insertColumnList.append(insertColumnListEntry);
			
				/*
				if(var.varMapForeignKey!=null)
				{
					//insertValueSelectQuery_columns
					insertValueSelectQuery_columnsEntry = tableNamePrefix+iVar+"."+var.varMapForeignKey;
					if(insertValueSelectQuery_columns.length()>0)
						insertValueSelectQuery_columns.append(",");
					
					insertValueSelectQuery_columns.append(insertValueSelectQuery_columnsEntry);
					
					//insertValueSelectQuery_fromTables
					insertValueSelectQuery_fromTablesEntry = var.varMapForeignTable;
					String foreignTableName =  Form.get1stLevelDBName(var.varMapForeignTable);
					String foreignSchemaName = Form.get2ndLevelDBName(var.varMapForeignTable);
					
					insertValueSelectQuery_fromTablesEntry = Form.constructDBPath(foreignSchemaName, foreignTableName, null)+" "+tableNamePrefix+""+iVar;
					if(insertValueSelectQuery_fromTables.length()>0)
						insertValueSelectQuery_fromTables.append(",");
					else
					{
						//append main table
						insertValueSelectQuery_fromTables.append(mainTablePath+" "+tableNamePrefix+"M,"); //includes comma
					}
					
					insertValueSelectQuery_fromTables.append(insertValueSelectQuery_fromTablesEntry);
					
					//insertValueSelectQuery_whereCondition
					insertValueSelectQuery_whereConditionEntry = tableNamePrefix+"M."+var.dataSourcePath+"="+tableNamePrefix+iVar+"."+var.varMapForeignKey;
					if(insertValueSelectQuery_whereCondition.length()>0)
						insertValueSelectQuery_whereCondition.append(",");
					
					insertValueSelectQuery_whereCondition.append(insertValueSelectQuery_whereConditionEntry);
					
				}
				*/
				//else
				//{
					//insertValueSelectQuery_columns
					insertValueSelectQuery_columnsEntry = "?";
					if(insertValueSelectQuery_columns.length()>0)
						insertValueSelectQuery_columns.append(",");
					
					insertValueSelectQuery_columns.append(insertValueSelectQuery_columnsEntry);
				//}
			}
			else throw new FormException("Form variable "+iVar+" has no dataSourcePath.");
		}
		

		String q ="INSERT INTO("+mainTablePath+")";
		q+="("+insertColumnList+")";
		q+=" SELECT "+insertValueSelectQuery_columns;
		
		/*
		if(insertValueSelectQuery_fromTables.length()>0)
			q+=" FROM "+insertValueSelectQuery_fromTables;
		
		if(insertValueSelectQuery_whereCondition.length()>0)
			q+=" WHERE ("+insertValueSelectQuery_whereCondition+")";
		 */
		
		return q;
	}
	
	/**
	 * Get the SQL Server type of the variable
	 * @param variable
	 * @return
	 * @throws FormException 
	 */
	public static String getTypeString(Form variable) throws FormException
	{
		if(variable.value!=null)
		{
			TypedValue value = variable.value;
			Integer type = value.getType();
			Integer sizeLimit = value.getSizeLimit();
			if(type==java.sql.Types.INTEGER)
				return "INT";
			else if(type==java.sql.Types.DOUBLE)
				return "DOUBLE PRECISION";
			else if(type==java.sql.Types.BOOLEAN)
				return "BIT";
			else if(type==java.sql.Types.VARCHAR)
			{
				if(sizeLimit!=null)
					return "VARCHAR("+sizeLimit+")";
				else return "VARCHAR(MAX)";
			}
			else if(type==java.sql.Types.NVARCHAR)
			{
				if(sizeLimit!=null)
					return "NVARCHAR("+sizeLimit+")";
				else return "NVARCHAR(MAX)";
			}
			else if(type==java.sql.Types.TIMESTAMP)
				return "DATETIME2";
			else if(type==java.sql.Types.BIGINT)
				return "BIGINT";
			else
				throw new FormException("SQL type unknown to TypedValue");
		}
		else throw new FormException("Variable value is null");
	}
	
	//TODO use modified DataEntry? Merge DataEntry and Form
	//TODO create offset functionality
	/**
	 * Produces a new form with columns that reflects the specified table path.
	 * @param tableName
	 * @param withData
	 * @param dataLimit
	 * @param c
	 * @return
	 * @throws SQLException
	 * @throws FormException
	 */
	public static Form produceFormFromDBTable(String tableName, boolean withData, int dataLimit, Connection c, HashSet<String> columnFilter) throws SQLException, FormException
	{
		Form masterForm = new Form(tableName,FieldType.FRM);
		
		Statement s = c.createStatement();
		ResultSet result;
		
		//TODO change for more secure handling of arguments. Maybe use the query building package from RegionAnnotator?
		if(withData)
			result =  s.executeQuery("SELECT TOP "+dataLimit+" * FROM "+tableName);
		else
			result =  s.executeQuery("SELECT TOP 1 * FROM "+tableName);
		
		ResultSetMetaData resultMeta = result.getMetaData();
		int colCount = resultMeta.getColumnCount();
		//System.out.println("meta columns="+resultMeta.getColumnCount());
		boolean hasRows = false;
		for(int rowi=0; result.next(); rowi++)
		{
			hasRows=true;
			Form rowForm = new Form(""+rowi,Form.FieldType.QRY);
			rowForm.name=tableName;
			
			for(int coli=1; coli<=colCount; coli++)  //index from 1
			{
				String columnName = resultMeta.getColumnName(coli);
				if(columnFilter!=null&&columnFilter.contains(columnName))
					continue;
				
				
				
				try 
				{
					Form columnForm = parseDBColumn(rowForm, coli, resultMeta, withData, result);
					rowForm.add(columnForm);
				} catch (OperationException e) 
				{
					throw new FormException("Could not parse Form value of table "+tableName+", column "+columnName+" at relative line index "+rowi);
				}
				
			}
			
			masterForm.add(rowForm);
		}
		
		if(!hasRows) //still add template columns if no row data
		{
			Form rowForm = new Form("0",Form.FieldType.QRY);
			rowForm.name=tableName;
			
			for(int coli=1; coli<colCount; coli++)
			{
				String columnName = resultMeta.getColumnName(coli);
				if(columnFilter!=null&&columnFilter.contains(columnName))
					continue;
				
				try 
				{
					Form columnForm = parseDBColumn(rowForm, coli, resultMeta, false, null);
					rowForm.add(columnForm);
				} catch (OperationException e) 
				{
					throw new FormException("Could not parse Form value of table "+tableName+", column "+columnName);
				}
			}
			
			masterForm.add(rowForm);
		}
		
		return masterForm;
	}
	
	private static Form parseDBColumn(Form queryRowForm, int columnIndex, ResultSetMetaData resultSetMeta, boolean withData, ResultSet resultSet) throws SQLException, FormException, OperationException
	{
		String columnName = resultSetMeta.getColumnName(columnIndex);
		
		Form columnForm = new Form(columnName, Form.FieldType.VAR);
		columnForm.name=columnName;
		
		int columnType = resultSetMeta.getColumnType(columnIndex);
		/* -16 nvarchar(max) bug conversion */
		if(columnType==-16)
			columnType=java.sql.Types.NVARCHAR;
		
		int columnPrecisionOrLength = resultSetMeta.getPrecision(columnIndex);
		//String columnTypeName = resultMeta.getColumnTypeName(coli);
		int resultSetNullability = resultSetMeta.isNullable(columnIndex);
		if(ResultSetMetaData.columnNoNulls==resultSetNullability)
			columnForm.nullable=false;
		else if(ResultSetMetaData.columnNullable==resultSetNullability)
			columnForm.nullable=true;
		else if(ResultSetMetaData.columnNullableUnknown==resultSetNullability)
			columnForm.nullable=true;
		else throw new FormException("Unrecognizeable nullability status when parsing Form");
		//int jdbcnvarcharordinal = JDBCType.NVARCHAR.ordinal();
		//int jdbcvarcharordinal = JDBCType.VARCHAR.ordinal();
		
		
		columnForm.writeable=resultSetMeta.isWritable(columnIndex);
		
		TypedValue tv = new TypedValue(columnType);
		if(columnPrecisionOrLength==0)
			tv.setSizeLimit(null);
		else
			tv.setSizeLimit(columnPrecisionOrLength);
		
		if(withData)
		{
			//dummy
			resultSet.getObject(columnIndex);
			if(!resultSet.wasNull())
			{	
				setTypedValueFromSQLResultSet(tv, resultSet, columnName);
			}
		}
		
		columnForm.setValue(tv);
		
		return columnForm;
	}
	
	public static String scriptDynamicSQLCreateColumnDefinitionColumnPart(String variableName, String typeString, String defaultValueString, boolean nullable, boolean unique, boolean identity) throws FormException
	{
		StringBuilder s = new StringBuilder();
		
		s.append(variableName+" "+typeString);
		
		if(identity)
			s.append(" IDENTITY(1,1)");
		
		if(!nullable&&!identity)
			s.append(" NOT NULL");
		
		if(unique)
			s.append(" UNIQUE");
		
		if(defaultValueString!=null)
			s.append(" DEFAULT "+defaultValueString);
		
		return s.toString();
	}
	
	public static String scriptDynamicSQLCreateColumnDefinition(Form source, boolean addIdentityColumn) throws FormException
	{
		
		Form query =null;
		if(source.type==FieldType.FRM)
		{
			if(source.content.size()>0)
				query = source.content.getValueAt(0);
			else
				throw new FormException("Form does not have any queries to use as template");
		}
		else if(source.type==FieldType.QRY)
			query = source;
		else
			throw new FormException("Must pass a form or query type Form object.");
		
		StringBuilder s = new StringBuilder();
		
		if(addIdentityColumn)
		{
			s.append(scriptDynamicSQLCreateColumnDefinitionColumnPart(query.getEvaluatedDBTable()+"_id", "INT", null,false,true,true));
		}
		
		for(int iColumn=0; iColumn<query.content.size(); iColumn++)
		{
			Form varColumn = query.content.getValueAt(iColumn);
			
			if(varColumn.getHasContent())
			{
				//has alternatives
				for(int iAlt=0; iAlt<varColumn.content.size(); iAlt++)
				{
					Form altForm = varColumn.content.getValueAt(iAlt);
					if(altForm.type==FieldType.ALT)
					{
						//alternative value placeholder
						//always nullable
						if(s.length()>0)
							s.append(",");
						s.append(scriptDynamicSQLCreateColumnDefinitionColumnPart(varColumn.dataSourcePath+"_"+altForm.getId(),SSFormProcessor.getTypeString(altForm),null,true,false,false));
						
						//alternative other value placeholder
						//always nullable
						if(altForm.alternativeHasOtherField)
						{
							altForm.getValue().setSizeLimit(null); //set size limit for the other-field
							if(s.length()>0)
								s.append(",");
							s.append(scriptDynamicSQLCreateColumnDefinitionColumnPart(varColumn.dataSourcePath+"_"+altForm.getId()+"_other",SSFormProcessor.getTypeString(altForm),null,true,false,false));
						}
					}
				}
			}
			
			if(s.length()>0)
				s.append(",");
			s.append(scriptDynamicSQLCreateColumnDefinitionColumnPart(varColumn.dataSourcePath, SSFormProcessor.getTypeString(varColumn),null,varColumn.nullable,false,false));
		}
			

		return s.toString();
	}
	
	public static String scriptDynamicSQLCreateVersionedTable(Form source, boolean addIdentityColumn) throws FormException
	{
		StringBuilder s = new StringBuilder();
		s.append("CREATE TABLE "+source.getEvaluatedDBPath()+"(");
		s.append(scriptDynamicSQLCreateColumnDefinition(source,addIdentityColumn));
		s.append(");");
		return s.toString();
	}
	
	public static void createVersionedTable(Form surveyForm, Connection c, boolean addIdentityColumn) throws FormException, SQLException
	{
		String q = SSFormProcessor.scriptDynamicSQLCreateVersionedTable(surveyForm,addIdentityColumn);
		Statement s = c.createStatement();
		s.execute(q);
	}

}
