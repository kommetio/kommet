/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import kommet.data.KommetException;
import kommet.deployment.DeploymentException;

public class XMLUtil
{
	/**
	 * Append an attribute to XML tag.
	 * @param name Name of the attribute
	 * @param val Value of the attribute
	 * @param sb String builder containing the code of the XML element
	 */
	public static void attr (String name, String val, StringBuilder sb)
	{
		if (val != null && !"".equals(val))
		{
			sb.append(" ").append(name).append("=\"").append(val).append("\" ");
		}
	}
	
	public static void addStandardTagAttributes (StringBuilder code, String id, String name, String cssClass, String cssStyle)
	{
		if (StringUtils.hasText(id))
		{
			code.append(" id=\"").append(id).append("\"");
		}
		
		if (StringUtils.hasText(name))
		{
			code.append(" name=\"").append(name).append("\"");
		}
		
		if (StringUtils.hasText(cssClass))
		{
			code.append(" class=\"").append(cssClass).append("\"");
		}
		
		if (StringUtils.hasText(cssStyle))
		{
			code.append(" style=\"").append(cssStyle).append("\"");
		}
	}
	
	public static void addElement(Document doc, Element parent, String tagName, String elementValue)
	{
		Element elem = doc.createElement(tagName);
		elem.setTextContent(elementValue != null ? elementValue : "");
		parent.appendChild(elem);
	}
	
	public static void addElement(Document doc, Element parent, String tagName, Integer elementValue)
	{
		addElement(doc, parent, tagName, elementValue == null ? "" : elementValue.toString());
	}
	
	public static void addElement(Document doc, Element parent, String tagName, boolean elementValue)
	{
		addElement(doc, parent, tagName, elementValue ? "true" : "false");
	}
	
	public static String xmlNodeToString(Element elem) throws KommetException
	{
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try
		{
			transformer = transFactory.newTransformer();
		}
		catch (TransformerConfigurationException e)
		{
			throw new KommetException("Could not configure XML transformer: " + e.getMessage());
		}
		StringWriter buffer = new StringWriter();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		try
		{
			transformer.transform(new DOMSource(elem), new StreamResult(buffer));
		}
		catch (TransformerException e)
		{
			throw new KommetException("Could not transform XML to string: " + e.getMessage());
		}
		return buffer.toString();
	}
	
	public static String getSingleNodeValue(String nodeName, Element doc) throws KommetException
	{
		NodeList nList = doc.getElementsByTagName(nodeName);
		Element foundElem = null;
		
		if (nList.getLength() == 0)
		{
			throw new DeploymentException("No node with name " + nodeName + " found");
		}
		else if (nList.getLength() > 1)
		{
			// check only direct children
			NodeList nodes = doc.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++)
			{
				Node node = nodes.item(i);
				if (node instanceof Element)
				{
					if (((Element)node).getTagName().equals(nodeName))
					{
						if (foundElem == null)
						{
							foundElem = (Element)node;
						}
						else
						{
							throw new KommetException("More than one node with name " + nodeName + " found");
						}
					}
				}
			}
		}
		else
		{
			Node node = nList.item(0);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				foundElem = (Element)node;
			}
			else
			{
				throw new KommetException("Node with name " + nodeName + " is not an element");
			}
		}
		
		String value = foundElem.getTextContent();
		return StringUtils.hasText(value) ? value : null;
	}
}