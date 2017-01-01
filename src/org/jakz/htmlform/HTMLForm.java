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
		generate();
		return workingElement.toString();
	}
	
	private void generate()
	{
		//TODO
		//workingElement.appendElement("");
	}
	
	//TODO
	private Element generateHTMLForForm(HTMLForm f, Element containerHead, Element rootElement) throws HTMLFormException
	{
		if(f.type==FieldType.container)
		{
			Element form  = containerHead.appendElement("form").attr("name", f.getGlobalID()).attr("id", f.getGlobalID());
			return form;
		}
		else if(f.type==FieldType.single)
		{
			Element formElement;
			//if()
			formElement = containerHead.appendElement("form").attr("name", f.getGlobalID()).attr("id", f.getGlobalID());
			return formElement;
		}
		else
			throw new HTMLFormException("Unrecognizable form type: "+f.type.toString());
	}
}
