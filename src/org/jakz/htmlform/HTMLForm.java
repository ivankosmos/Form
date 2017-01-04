package org.jakz.htmlform;

import org.apache.commons.lang.StringEscapeUtils;
import org.jakz.common.Form;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

public class HTMLForm extends Form
{
	
	private Element workingElement;
	
	public HTMLForm(String nid, FieldType ntype)
	{
		super(nid,ntype);
		workingElement = new Element(Tag.valueOf("div"), "");
	}
	
	public HTMLForm setWorkingElement(Element nWorkingElement)
	{
		workingElement=nWorkingElement;
		return this;
	}
	
	public String getGlobalID()
	{
		return StringEscapeUtils.escapeHtml(super.getGlobalID());
	}
	
	public String toString()
	{
		return workingElement.toString();
	}
	
	public Element getWorkingElement()
	{
		return workingElement;
	}
	
	public HTMLForm generate() throws HTMLFormException
	{
		workingElement=generateHTMLForForm(this, workingElement);
		return this;
	}
	
	protected static Element generateHTMLForForm(Form form, Element containerHead) throws HTMLFormException
	{
		if(form.type==null)
			return containerHead;
		
		Element currentElement = generateHTMLForField(form,containerHead);
		
		if(form.getHasContent())
		{
			for(int i=0; i<form.content.size(); i++)
			{
				HTMLForm currentForm = (HTMLForm)form.content.getValueAt(i);
				HTMLForm.generateHTMLForForm(currentForm, currentElement);
			}
		}
		
		return containerHead;
	}
	
	protected static Element generateHTMLForField(Form field, Element containerHead) throws HTMLFormException
	{
		Element currentElement;
		if(field.type==FieldType.FORM)
		{
			currentElement  = containerHead.appendElement("form").attr("name", field.getGlobalID()).attr("id", field.getGlobalID());
		}
		else if(field.type==FieldType.QUERY)
		{
			currentElement = containerHead.appendElement("fieldset").attr("name", field.getGlobalID()).attr("id", field.getGlobalID());
			currentElement.appendElement("legend").html(field.name);
			
		}
		else
			throw new HTMLFormException("Unrecognizable form type: "+field.type.toString());
		return currentElement;
	}
}
