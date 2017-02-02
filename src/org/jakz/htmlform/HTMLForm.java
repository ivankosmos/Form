package org.jakz.htmlform;

import java.text.SimpleDateFormat;
import java.util.Date;

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
	
	public HTMLForm(Form nform)
	{
		super(nform.id,nform.type);
		scopy(nform);
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
	
	public HTMLForm generateHTMLForm() throws HTMLFormException
	{
		workingElement=getFormHTML(this);
		return this;
	}
	
	public HTMLForm generateHTMLView() throws HTMLFormException
	{
		workingElement=getViewHTML(this);
		return this;
	}
	
	protected static Element getFormHTML(Form source) throws HTMLFormException
	{
		Element toreturn;
		if(source.type==Form.FieldType.QUERY)
		{
			toreturn = new Element(Tag.valueOf("fieldset"),"");
			toreturn.attr("name", source.id).attr("id",source.id);
			//toreturn.appendElement("legend").html(source.name).attr("name","name");
			toreturn.appendElement("span").html(source.name).attr("name","name");
			
			Form columnForm = source.content.getValueAt(0); //Only takes the first
			
			for(int vali=0; vali<source.value.size(); vali++)
			{
				TypedValue tv = source.value.get(vali);
				int type = tv.getType();
				
				Element inputElement;
				
				if(type==java.sql.Types.INTEGER)
					inputElement = toreturn.appendElement("input").attr("type", "number");
				else if(type==java.sql.Types.DOUBLE)
					inputElement = toreturn.appendElement("input").attr("type", "number");
				else if(type==java.sql.Types.BOOLEAN)
					inputElement = toreturn.appendElement("input").attr("type", "checkbox");
				else if(type==java.sql.Types.VARCHAR||type==java.sql.Types.NVARCHAR||type==-16) //TODO SQL Server returns -16 for nvarchar(max)
					inputElement = toreturn.appendElement("input").attr("type", "text");
				else if(type==java.sql.Types.TIMESTAMP)
					inputElement = toreturn.appendElement("input").attr("type", "datetime");
				else if(type==java.sql.Types.BIGINT)
					inputElement = toreturn.appendElement("input").attr("type", "number");
				else throw new HTMLFormException("Wrong value type for HTML generator. Type "+type+" form "+source.id);
				
				inputElement.attr("name",source.id+"_"+vali).attr("id",source.id+"_"+vali);
			}
			
		}
		else if(source.type==Form.FieldType.INFO)
		{
			toreturn = new Element(Tag.valueOf("div"),"").html(source.text);
		}
		else if(source.type==Form.FieldType.FORM)
		{
			toreturn = new Element(Tag.valueOf("form"),"");
			for(int icontent=0; icontent<source.content.size(); icontent++)
			{
				Element contentElement = getFormHTML(source.content.getAt(icontent).value);
				toreturn.appendChild(contentElement);
			}
		}
		else throw new HTMLFormException("Unrecognizable Form type "+source.type);
		
		return toreturn;
	}
	
	protected static Element getViewHTML(Form source) throws HTMLFormException
	{
		return getViewHTML(new HTMLForm(source));
	}
	
	protected static Element getViewHTML(HTMLForm source) throws HTMLFormException
	{
		Element toreturn;
		if(source.type==Form.FieldType.QUERY)
		{
			toreturn = new Element(Tag.valueOf("tr"),"").attr("name", source.getGlobalID()).attr("id",source.getGlobalID());

			for(int coli=0; coli<source.content.size(); coli++)
			{
				Form columnForm = source.content.getValueAt(coli);
				TypedValue tv = columnForm.value.get(0); //Only takes the first
				int type = tv.getType();
				
				Element tabledata = toreturn.appendElement("td");
				
				if(type==java.sql.Types.INTEGER)
					tabledata.attr("type", "number").html(""+tv.getValueInteger());
				else if(type==java.sql.Types.DOUBLE)
					tabledata.attr("type", "number").html(""+tv.getValueDouble());
				else if(type==java.sql.Types.BOOLEAN)
					tabledata.attr("type", "checkbox").html(""+tv.getValueBoolean());
				else if(type==java.sql.Types.VARCHAR||type==java.sql.Types.NVARCHAR||type==-16) //TODO SQL Server returns -16 for nvarchar(max)
					tabledata.attr("type", "text").html(""+tv.getValueVarchar());
				else if(type==java.sql.Types.TIMESTAMP)
				{
					Date d = new Date(tv.getValueTimestamp());
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
					tabledata.attr("type", "datetime").html(df.format(d));
				}
				else if(type==java.sql.Types.BIGINT)
					tabledata.attr("type", "number").html(""+tv.getValueBigint());
				else throw new HTMLFormException("Wrong value type for HTML generator. Type "+type+" form "+source.id+" column "+columnForm.id);
				
				tabledata.attr("name",columnForm.getGlobalID()).attr("id",columnForm.getGlobalID());
			}
		}
		else if(source.type==Form.FieldType.INFO)
		{
			toreturn = new Element(Tag.valueOf("div"),"").html(source.text);
		}
		else if(source.type==Form.FieldType.FORM)
		{
			toreturn = new Element(Tag.valueOf("table"),"").attr("id", source.getGlobalID()).attr("name", source.getGlobalID());
			for(int icontent=0; icontent<source.content.size(); icontent++)
			{
				Element contentElement = getViewHTML(source.content.getAt(icontent).value);
				toreturn.appendChild(contentElement);
			}
		}
		else throw new HTMLFormException("Unrecognizable Form type "+source.type);
		
		return toreturn;
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
		
		System.out.println(f.generateHTMLView().toString());
		String jsonString = f.toJSONObject().toString();
		System.out.println(jsonString);
		
		
		f = new HTMLForm("f2", FieldType.FORM);
		f.fromJSONObject(new JSONObject(jsonString));
		System.out.println(f.generateHTMLView().toString());
		jsonString = f.toJSONObject().toString();
		System.out.println(jsonString);
		
	}
}
