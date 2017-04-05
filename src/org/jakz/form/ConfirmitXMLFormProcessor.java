package org.jakz.form;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.jakz.form.Form.FieldType;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;

public class ConfirmitXMLFormProcessor 
{
	private File sourceFile;
	private Form targetForm, templateForm;

	private Integer languageCode;
	private Boolean fallbackToFirstLanguageFound;
	
	public ConfirmitXMLFormProcessor()
	{
		fallbackToFirstLanguageFound = true;
	}
	
	public ConfirmitXMLFormProcessor setSource(File nSource)
	{
		sourceFile=nSource;
		return this;
	}
	
	public ConfirmitXMLFormProcessor setTarget(Form nTarget)
	{
		targetForm=nTarget;
		return this;
	}
	
	public ConfirmitXMLFormProcessor setTemplate(Form nTemplate)
	{
		templateForm=nTemplate;
		return this;
	}
	
	public ConfirmitXMLFormProcessor setLanguageCode(Integer nLanguageCode)
	{
		languageCode=nLanguageCode;
		return this;
	}
	
	public ConfirmitXMLFormProcessor setFallbackToFirstLanguageFound(Boolean nFallbackToFirstLanguageFound)
	{
		fallbackToFirstLanguageFound=nFallbackToFirstLanguageFound;
		return this;
	}
	
	public ConfirmitXMLFormProcessor populateFormDataFromFile() throws FormException
	{
		try
		{
			Builder xmlParser = new Builder();
			Document doc = xmlParser.build(sourceFile);
			Element root = doc.getRootElement(); //Project
			Element questionnarieElement = root.getFirstChildElement("Questionnaire");
			Element routingNodes = questionnarieElement.getFirstChildElement("Routing").getFirstChildElement("Nodes");
			Element blocksNodes = questionnarieElement.getFirstChildElement("Blocks").getFirstChildElement("Nodes");
			Form templateQueryRow = null;
			if(targetForm.getHasContent())
				templateQueryRow=targetForm.content.getValueAt(0);
			else
			{
				templateQueryRow=new Form(targetForm.id, Form.FieldType.QRY);
				targetForm.add(templateQueryRow);
			}
			
			for(int i=0; i<routingNodes.getChildCount(); i++)
			{
				Form newVarForm = new Form("ConfirmitXMLFormProcessor_templateVariable", FieldType.VAR);
				populateFormDataFromQuestionElement(newVarForm,routingNodes.getChild(i));
				
			}
			
			for(int i=0; i<blocksNodes.getChildCount(); i++)
			{
				Node block = blocksNodes.getChild(i);
				for(int j=0; j<block.getChildCount(); j++)
				{
					Form newVarForm = new Form("ConfirmitXMLFormProcessor_templateVariable", FieldType.VAR);
					populateFormDataFromQuestionElement(newVarForm,block.getChild(j));
				}
			}
		}
		catch (Exception e)
		{
			throw new FormException("Could not populate Form from Confirmit xml",e);
		}
		return this;
	}
	
	//TODO
	protected void populateFormDataFromQuestionElement(Form targetVariable, Node nQuestionElement)
	{
		//basics
		targetVariable.type=FieldType.VAR;
		
		
		Element qe = (Element) nQuestionElement;
		String localName = qe.getLocalName();
		String questionId = null;
		
		Element nameElement = qe.getFirstChildElement("Name");
		if(nameElement!=null)
			questionId=nameElement.getValue();
		
		//attributes
		String entityId = qe.getAttributeValue("EntityId");
		String variableType = qe.getAttributeValue("VariableType");
		String questionCategory = qe.getAttributeValue("QuestionCategory");
		String sDefaultValue = qe.getAttributeValue("DefaultValue");
		String sPrecision = qe.getAttributeValue("Precision");
		String sRows = qe.getAttributeValue("Rows");
		String sNumeric = qe.getAttributeValue("Numeric");
		String lowerLimitType = qe.getAttributeValue("LowerLimitType");
		String upperLimitType = qe.getAttributeValue("UpperLimitType");
		
		String formTextTitle = null;
		String formTextText = null;
		String formTextInstruction = null;
		
		Element formTextsElement = qe.getFirstChildElement("FormTexts");
		if(formTextsElement!=null)
		{
			Elements formTexts = formTextsElement.getChildElements("FormText");
			for(int i=0; i<formTexts.size(); i++)
			{
				Element formText = formTexts.get(i);
				String language = formText.getAttributeValue("Language");
				if(
						(language!=null&&languageCode!=null&&languageCode==Integer.parseInt(language))
				||
						(languageCode==null&&fallbackToFirstLanguageFound!=null)
				)
				{
					formTextTitle=formText.getFirstChildElement("Title").getValue();
					formTextText=formText.getFirstChildElement("Text").getValue();
					formTextInstruction=formText.getFirstChildElement("Instruction").getValue();
					break;
				}
			}
		}
		
		if(localName.equals("Open"))
		{
			
		}
		else if(localName.equals("Single"))
		{
			
		}
		else if(localName.equals("Multi"))
		{
			
		}
		else if(localName.equals("Page"))
		{
			
		}
		else
		{
			//OTHER ELEMENTS
		}
		
		//String questionId = 
		
		//Form varForm = new Form(qe.getAttributeValue(name), FieldType.VAR);
	}
	
}
