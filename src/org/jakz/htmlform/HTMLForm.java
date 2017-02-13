package org.jakz.htmlform;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
	public String settingHTMLFormSrc,settingHTMLFormArgumentNameId,settingHTMLFormArgumentFunction;
	//public ArrayList<String> settingViewLinkKeys;
	
	protected void init()
	{
		workingElement=null;
		settingGenerateViewLinks=true;
		settingHTMLFormSrc ="";
		settingHTMLFormArgumentNameId="HTMLFormArgument";
		//settingViewLinkKeys=new ArrayList<String>();
		settingHTMLFormArgumentFunction="HTMLFormSubmit";
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
	
	public HTMLForm initWorkingElement()
	{
		if(workingElement==null)
			workingElement = new Element(Tag.valueOf("div"),"").attr("name", id);
		
		return this;
	}
	
	public HTMLForm generateHTMLFormJSHTML() throws HTMLFormException
	{
		workingElement.appendChild(getHTMLFormJSHTML(this));
		return this;
	}
	
	public HTMLForm generateHTMLFormHTML() throws HTMLFormException
	{
		workingElement.appendChild(getHTMLFormHTML(this));
		return this;
	}
	
	public HTMLForm generateHTMLForm() throws HTMLFormException
	{
		workingElement.appendChild(getFormHTML(this,this));
		return this;
	}
	
	public HTMLForm generateHTMLView() throws HTMLFormException
	{
		workingElement.appendChild(getViewHTML(this,this));
		return this;
	}
	
	
	protected static Element getHTMLFormJSHTML(HTMLForm source)
	{
		Element script = new Element(Tag.valueOf("script"),"").attr("type", "text/javascript");
		
		String html = "";
		
		html+="function HTMLFormSubmit(var arg)";
		html+="{";
		html+="alert('KLICK!');";
		//html+="window.location = '"+settingViewLinkSrc+"?"+settingViewLinkKeyArgumentNameId+"='+arg;";
		html=html+"var formE = document.getElementById('"+source.id+"');";
		html=html+"var inputE = document.getElementById('"+source.settingHTMLFormArgumentNameId+"');";
		html+="inputE.value=arg;";
		html+="formE.submit();";
		html+="}";
		
		
		script.html(html);
		
		return script;
	}
	
	protected static Element getHTMLFormHTML(HTMLForm source)
	{
		Element formE = new Element(Tag.valueOf("form"),"").attr("action",source.settingHTMLFormSrc).attr("method", "post").attr("id", source.id);
		
		formE.appendElement("input").attr("type", "hidden").attr("name", source.settingHTMLFormArgumentNameId).attr("id", source.settingHTMLFormArgumentNameId);
		
		return formE;
	}
	
	protected static Element getFormHTML(HTMLForm source, HTMLForm masterForm) throws HTMLFormException
	{
		Element formElementToReturn;
		if(source.type==Form.FieldType.QUERY)
		{
			formElementToReturn = new Element(Tag.valueOf("fieldset"),"");
			formElementToReturn.attr("name", source.id).attr("id",source.getGlobalID()).attr("form",masterForm.id);
			//toreturn.appendElement("legend").html(source.name).attr("name","name");
			formElementToReturn.appendElement("div").html(source.name).attr("name","name");
			formElementToReturn.appendElement("div").html(source.text).attr("name","text");
			
			for(int coli=0; coli<source.content.size(); coli++)
			{
				
				Form columnForm = source.content.getValueAt(coli);
				if(!columnForm.writeable)
					continue;
				
				Element valcontainer = formElementToReturn.appendElement("div").attr("name","valcontainer");
				
				
				
				
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
				
				inputElement.attr("name",columnForm.id).attr("id",columnForm.getGlobalID()).attr("form",masterForm.id);
			}
			
		}
		else if(source.type==Form.FieldType.INFO)
		{
			formElementToReturn = new Element(Tag.valueOf("div"),"").attr("name", source.id).html(source.text);
		}
		else if(source.type==Form.FieldType.FORM)
		{
			//toreturn = new Element(Tag.valueOf("form"),"").attr("id", source.getGlobalID()).attr("name", source.getGlobalID());
			formElementToReturn = new Element(Tag.valueOf("div"),"").attr("name", source.id);

			
			for(int icontent=0; icontent<source.content.size(); icontent++)
			{
				Element contentElement = getFormHTML(new HTMLForm(source.content.getAt(icontent).value),masterForm);
				formElementToReturn.appendChild(contentElement);
			}
		}
		else throw new HTMLFormException("Unrecognizable Form type "+source.type);
		
		return formElementToReturn;
	}
	
	//TODO
	protected static Element getViewHTML(HTMLForm source, HTMLForm masterForm) throws HTMLFormException
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
			
			
			toreturn =  new Element(Tag.valueOf("tr"),"").attr("name", source.id).attr("id",source.getGlobalID());
			if(masterForm.settingGenerateViewLinks)
			{
				try
				{
				toreturn.attr("onclick", "HTMLFormEditRow('"+URLEncoder.encode(source.toJSONObject().toString(),"UTF-8") +"');");
				}
				catch (UnsupportedEncodingException e)
				{
					throw new HTMLFormException("Unsupported encoding", e);
				}
			}
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
			toreturn = new Element(Tag.valueOf("div"),"").attr("id", source.getGlobalID()).attr("name", source.id).html(source.text);
		}
		else if(source.type==Form.FieldType.FORM)
		{
			toreturn = new Element(Tag.valueOf("table"),"").attr("name", source.id);
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
	
	
	
	/**
	 * For regtesting
	 * @param args
	 * @throws HTMLFormException
	 * @throws  
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
