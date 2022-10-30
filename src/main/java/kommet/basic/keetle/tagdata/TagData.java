/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tagdata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.AppConfig;
import kommet.utils.PropertyUtilException;
import kommet.utils.XMLUtil;

public class TagData
{
	private List<Namespace> namespaces = new ArrayList<Namespace>();
	
	private static TagData tagData;
	
	public static TagData get(EnvData env, AppConfig config) throws KommetException
	{
		if (tagData == null)
		{
			initTagData(env, config);
		}
		
		return tagData;
	}

	private static void initTagData(EnvData env, AppConfig config) throws KommetException
	{
		File kmTLD = null;
		
		try
		{
			kmTLD = new File(config.getTldDir() + "/km-tags.tld");
		}
		catch (PropertyUtilException e)
		{
			e.printStackTrace();
			throw new KommetException("Error reading TLD directory property");
		}
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		
		try
		{
			// when there is no network access, the builder will fail to download DTD files
			// so we want to disabled the DTD validation altogether
			factory.setValidating(false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			
			// create the builder
			builder = factory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e)
		{
			throw new KommetException("Error configuring reader for XML file");
		}
		
		InputSource is;
		try
		{
			is = new InputSource(new FileReader(kmTLD));
		}
		catch (FileNotFoundException e)
		{
			throw new KommetException("File km-tags.tld not found");
		}
		
		Document doc = null;
		
		try
		{
			doc = builder.parse(is);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new KommetException("Error reading XML TLD file");
		}
		
		Namespace kmNamespace = new Namespace();
		kmNamespace.setName("km");
		
		doc.getDocumentElement().normalize();
		
		NodeList tags = doc.getElementsByTagName("tag");
		
		// find all tag elements
		for (int i = 0; i < tags.getLength(); i++)
		{
			Element tagNode = (Element)tags.item(i);
			kmNamespace.getTags().add(getTagFromNode(tagNode));
		}
		
		tagData = new TagData();
		tagData.getNamespaces().add(kmNamespace);
		
		initChildrenForRmTags(kmNamespace);
	}

	private static void initChildrenForRmTags(Namespace kmNamespace)
	{
		kmNamespace.getTagByName("dataTable").getChildren().add(kmNamespace.getTagByName("dataTableColumn"));
		kmNamespace.getTagByName("dataTable").getChildren().add(kmNamespace.getTagByName("dataTableSearch"));
		kmNamespace.getTagByName("dataTable").getChildren().add(kmNamespace.getTagByName("dataTableOption"));
		
		kmNamespace.getTagByName("dataTableSearch").getChildren().add(kmNamespace.getTagByName("dataTableSearchField"));
		
		kmNamespace.getTagByName("userMenu").getChildren().add(kmNamespace.getTagByName("menuItem"));
		
		kmNamespace.getTagByName("objectDetails").getChildren().add(kmNamespace.getTagByName("relatedList"));
		kmNamespace.getTagByName("objectDetails").getChildren().add(kmNamespace.getTagByName("files"));
		kmNamespace.getTagByName("objectDetails").getChildren().add(kmNamespace.getTagByName("comments"));
		kmNamespace.getTagByName("objectDetails").getChildren().add(kmNamespace.getTagByName("fieldHistory"));
		kmNamespace.getTagByName("objectDetails").getChildren().add(kmNamespace.getTagByName("recordSharing"));
		kmNamespace.getTagByName("objectDetails").getChildren().add(kmNamespace.getTagByName("inputField"));
		kmNamespace.getTagByName("objectDetails").getChildren().add(kmNamespace.getTagByName("outputField"));
		
		kmNamespace.getTagByName("fieldHistory").getChildren().add(kmNamespace.getTagByName("fieldHistoryField"));
		
		kmNamespace.getTagByName("tabs").getChildren().add(kmNamespace.getTagByName("tab"));
		
		kmNamespace.getTagByName("view").getChildren().add(kmNamespace.getTagByName("viewResource"));
		kmNamespace.getTagByName("view").getChildren().add(kmNamespace.getTagByName("tabs"));
		kmNamespace.getTagByName("view").getChildren().add(kmNamespace.getTagByName("breadcrumbs"));
	}

	private static Tag getTagFromNode(Element node) throws KommetException
	{
		Tag tag = new Tag();
		tag.setName(XMLUtil.getSingleNodeValue("name", node));
		tag.setDescription(XMLUtil.getSingleNodeValue("info", node));
		
		NodeList attrs = node.getElementsByTagName("attribute");
		for (int i = 0; i < attrs.getLength(); i++)
		{
			Element attrTag = (Element)attrs.item(i);
			Attribute attr = new Attribute();
			attr.setName(XMLUtil.getSingleNodeValue("name", attrTag));
			attr.setRequired("true".equals(XMLUtil.getSingleNodeValue("required", attrTag)));
			
			tag.getAttributes().add(attr);
		}
		
		return tag;
	}

	public List<Namespace> getNamespaces()
	{
		return namespaces;
	}

	public void setNamespaces(List<Namespace> namespaces)
	{
		this.namespaces = namespaces;
	}
}
