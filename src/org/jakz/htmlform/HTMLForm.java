package org.jakz.htmlform;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
	
	
	public boolean settingGenerateViewLinks;
	public String settingViewLinkSrc,settingViewLinkKeyArgumentName,settingViewLinkOnclickArgumentFunction;
	public ArrayList<String> settingViewLinkKeys;
	
	protected void init()
	{
		workingElement=null;
		settingGenerateViewLinks=true;
		settingViewLinkSrc ="";
		settingViewLinkKeyArgumentName="HTMLFormEditRow";
		settingViewLinkKeys=new ArrayList<String>();
		settingViewLinkOnclickArgumentFunction="HTMLFormEditRow";
	}
	
	public HTMLForm(String nid, FieldType ntype)
	{
		super(nid,ntype);
		//workingElement = new Element(Tag.valueOf("div"), "");
		init();
	}
	
	public HTMLForm(Form nform)
	{
		super(nform.id,nform.type);
		init();
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
		workingElement=getFormHTML(this,this);
		return this;
	}
	
	public HTMLForm generateHTMLView() throws HTMLFormException
	{
		workingElement=getViewHTML(this,this);
		return this;
	}
	
	protected Element getFormHTML(HTMLForm source, HTMLForm masterForm) throws HTMLFormException
	{
		Element toreturn;
		if(source.type==Form.FieldType.QUERY)
		{
			toreturn = new Element(Tag.valueOf("fieldset"),"");
			toreturn.attr("name", source.id).attr("id",source.getGlobalID());
			//toreturn.appendElement("legend").html(source.name).attr("name","name");
			toreturn.appendElement("div").html(source.name).attr("name","name");
			toreturn.appendElement("div").html(source.text).attr("name","text");
			
			for(int coli=0; coli<source.content.size(); coli++)
			{
				
				Form columnForm = source.content.getValueAt(coli);
				if(!columnForm.writeable)
					continue;
				
				Element valcontainer = toreturn.appendElement("div").attr("name","valcontainer");
				
				
				
				
				valcontainer.appendElement("span").html(columnForm.name);
				
				TypedValue tv = columnForm.value.get(0); //Only takes the first
				int type = tv.getType();
				
				Element inputElement;
				
				if(type==java.sql.Types.INTEGER)
					inputElement = valcontainer.appendElement("input").attr("type", "number");
				else if(type==java.sql.Types.DOUBLE)
					inputElement = valcontainer.appendElement("input").attr("type", "number");
				else if(type==java.sql.Types.BOOLEAN)
					inputElement = valcontainer.appendElement("input").attr("type", "checkbox");
				else if(type==java.sql.Types.VARCHAR||type==java.sql.Types.NVARCHAR||type==-16) //TODO SQL Server returns -16 for nvarchar(max)
					inputElement = valcontainer.appendElement("input").attr("type", "text");
				else if(type==java.sql.Types.TIMESTAMP)
					inputElement = valcontainer.appendElement("input").attr("type", "datetime");
				else if(type==java.sql.Types.BIGINT)
					inputElement = valcontainer.appendElement("input").attr("type", "number");
				else throw new HTMLFormException("Wrong value type for HTML generator. Type "+type+" form "+source.id);
				
				inputElement.attr("name",columnForm.getGlobalID()).attr("id",columnForm.getGlobalID());
			}
			
		}
		else if(source.type==Form.FieldType.INFO)
		{
			toreturn = new Element(Tag.valueOf("div"),"").attr("id", source.getGlobalID()).attr("name", source.getGlobalID()).html(source.text);
		}
		else if(source.type==Form.FieldType.FORM)
		{
			toreturn = new Element(Tag.valueOf("form"),"").attr("id", source.getGlobalID()).attr("name", source.getGlobalID());
			for(int icontent=0; icontent<source.content.size(); icontent++)
			{
				Element contentElement = getFormHTML(new HTMLForm(source.content.getAt(icontent).value),masterForm);
				toreturn.appendChild(contentElement);
			}
		}
		else throw new HTMLFormException("Unrecognizable Form type "+source.type);
		
		return toreturn;
	}
	
	protected Element getViewHTML(HTMLForm source, HTMLForm masterForm) throws HTMLFormException
	{
		Element toreturn;
		if(source.type==Form.FieldType.QUERY)
		{
			Element elementHead;
			//if(masterForm.settingGenerateViewLinks)
			//{
				//toreturn=new Element(Tag.valueOf("a"),"").attr("href",masterForm.settingViewLinkSrc).attr("target", "_blank");
				//toreturn = toreturn.attr("name", source.getGlobalID()).attr("id",source.getGlobalID());
				//elementHead=toreturn.appendElement("tr").attr("onclick", "alert('klick');");
			//}
			
			
			toreturn =  new Element(Tag.valueOf("tr"),"").attr("name", source.getGlobalID()).attr("id",source.getGlobalID());
			if(masterForm.settingGenerateViewLinks)
				toreturn.attr("onclick", "HTMLFormEditRow();");
			elementHead = toreturn;
			

			for(int coli=0; coli<source.content.size(); coli++)
			{
				Form columnForm = source.content.getValueAt(coli);
				TypedValue tv = columnForm.value.get(0); //Only takes the first
				int type = tv.getType();
				
				Element tableData = elementHead.appendElement("td");
				
				if(type==java.sql.Types.INTEGER)
					tableData.attr("type", "number").html(""+tv.getValueInteger());
				else if(type==java.sql.Types.DOUBLE)
					tableData.attr("type", "number").html(""+tv.getValueDouble());
				else if(type==java.sql.Types.BOOLEAN)
					tableData.attr("type", "checkbox").html(""+tv.getValueBoolean());
				else if(type==java.sql.Types.VARCHAR||type==java.sql.Types.NVARCHAR||type==-16) //TODO SQL Server returns -16 for nvarchar(max)
					tableData.attr("type", "text").html(""+tv.getValueVarchar());
				else if(type==java.sql.Types.TIMESTAMP)
				{
					Date d = new Date(tv.getValueTimestamp());
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
					tableData.attr("type", "datetime").html(df.format(d));
				}
				else if(type==java.sql.Types.BIGINT)
					tableData.attr("type", "number").html(""+tv.getValueBigint());
				else throw new HTMLFormException("Wrong value type for HTML generator. Type "+type+" form "+source.id+" column "+columnForm.id);
				
				tableData.attr("name",columnForm.getGlobalID()).attr("id",columnForm.getGlobalID());
			}
		}
		else if(source.type==Form.FieldType.INFO)
		{
			toreturn = new Element(Tag.valueOf("div"),"").attr("id", source.getGlobalID()).attr("name", source.getGlobalID()).html(source.text);
		}
		else if(source.type==Form.FieldType.FORM)
		{
			toreturn = new Element(Tag.valueOf("table"),"").attr("id", source.getGlobalID()).attr("name", source.getGlobalID());
			Element tbody = toreturn.appendElement("tbody");
			for(int icontent=0; icontent<source.content.size(); icontent++)
			{
				Element contentElement = getViewHTML(new HTMLForm(source.content.getAt(icontent).value),masterForm);
				tbody.appendChild(contentElement);
			}
		}
		else throw new HTMLFormException("Unrecognizable Form type "+source.type);
		
		return toreturn;
	}
	
	public Element getHTMLFormJS()
	{
		Element script = new Element(Tag.valueOf("script"),"").attr("type", "text/javascript");
		
		String html = "";
		
		html+="function HTMLFormEditRow()";
		html+="{";
		html+="alert('KLICK!');";
		html+="window.location = '"+settingViewLinkSrc+"?"+settingViewLinkKeyArgumentName+"=hej';";
		html+="}";
		
		
		script.html(html);
		
		return script;
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
