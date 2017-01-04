package org.jakz.htmlform;

import org.apache.commons.lang.StringEscapeUtils;
import org.jakz.common.Form;
import org.jakz.common.JSONObject;
import org.jakz.common.TypedValue;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

public class HTMLForm extends Form
{
	
	private Element workingElement;
	
	public HTMLForm(String nid, FieldType ntype)
	{
		super(nid,ntype);
		//workingElement = new Element(Tag.valueOf("div"), "");
		workingElement = null;
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
		Element formElement = generateHTMLForField(form,containerHead);
		
		if(form.getHasContent())
		{
			for(int i=0; i<form.content.size(); i++)
			{
				Form currentForm = form.content.getValueAt(i);
				HTMLForm.generateHTMLForForm(currentForm, formElement);
			}
		}
		
		return formElement;
	}
	
	protected static Element generateHTMLForField(Form field, Element containerHead) throws HTMLFormException
	{
		if(field.type==null)
			return containerHead;
		Element fieldElement;
		if(field.type==FieldType.FORM)
		{
			if(containerHead==null)
			{
				fieldElement=new Element(Tag.valueOf("form"), "");
				fieldElement=fieldElement.attr("name", field.getGlobalID()).attr("id", field.getGlobalID());
			}
			else
				fieldElement  = containerHead.appendElement("form").attr("name", field.getGlobalID()).attr("id", field.getGlobalID());
		}
		else if(field.type==FieldType.QUERY)
		{
			if(containerHead==null)
			{
				fieldElement=new Element(Tag.valueOf("fieldset"), "");
				fieldElement=fieldElement.attr("name", field.getGlobalID()).attr("id", field.getGlobalID());
			}
			else
				fieldElement = containerHead.appendElement("fieldset").attr("name", field.getGlobalID()).attr("id", field.getGlobalID());
			
			fieldElement.appendElement("legend").html(field.name);
			
		}
		else
			throw new HTMLFormException("Unrecognizable form type: "+field.type.toString());
		return fieldElement;
	}
	
	/**
	 * For regtesting
	 * @param args
	 * @throws HTMLFormException
	 */
	public static void main(String[] args) throws HTMLFormException
	{
		HTMLForm f = new HTMLForm("f1", FieldType.FORM);
		Form q;
		TypedValue val;
		q=new Form("q1", FieldType.QUERY);
		q.name="Question 1";
		q.text="What is your name?";
		val=new TypedValue(java.sql.Types.VARCHAR);
		q.value.add(val);
		f.addContent(q);
		
		System.out.println(f.generate().toString());
		String jsonString = f.toJSONObject().toString();
		System.out.println(jsonString);
		
		
		f = new HTMLForm("f2", FieldType.FORM);
		f.fromJSONObject(new JSONObject(jsonString));
		System.out.println(f.generate().toString());
		jsonString = f.toJSONObject().toString();
		System.out.println(jsonString);
		
	}
}
