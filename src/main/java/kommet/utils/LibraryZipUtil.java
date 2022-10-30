/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import kommet.data.FileExtension;
import kommet.data.KommetException;
import kommet.deployment.DeploymentProcess;

public class LibraryZipUtil
{
	public static byte[] createZip (Map<String, String> files) throws IOException, KommetException
	{
		return createZip(files, null);
	}
	
	public static byte[] createZip (Map<String, String> files, Map<String, String> byteFilePaths) throws IOException, KommetException
	{  
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();  
	    ZipOutputStream zipfile = new ZipOutputStream(bos);  
	    Iterator<?> i = files.keySet().iterator();  
	    String fileName = null;  
	    ZipEntry zipEntry = null;
	    
	    while (i.hasNext())
	    {  
	    	fileName = (String)i.next();
	    	
	    	if (!fileName.contains("."))
	    	{
	    		throw new KommetException("File name " + fileName + " contains no extension");
	    	}
	    	
	    	String[] nameParts = MiscUtils.splitFileName(fileName);
	    	String typeDir = typeDir(nameParts[1]);
	    	String fileInDir = DeploymentProcess.SRC_DIR + "\\" + (typeDir != null ? typeDir + "\\" : "") + nameParts[0].replace(".", "\\") + "." + nameParts[1];
	    	
	        zipEntry = new ZipEntry(fileInDir);  
	        zipfile.putNextEntry(zipEntry);  
	        zipfile.write(files.get(fileName).getBytes());  
	    }
	    
	    // now put byte files into zip
	    if (byteFilePaths != null)
	    {
	    	Iterator<?> byteFileIterator = byteFilePaths.keySet().iterator(); 
	    	while (byteFileIterator.hasNext())
	    	{
	    		fileName = (String)byteFileIterator.next();
	    		
	    		if (!fileName.contains("."))
		    	{
		    		throw new KommetException("File name " + fileName + " contains no extension");
		    	}
	    		
	    		// in case of byte files, the received file name should be its full name - with all dirs and extension
		        zipEntry = new ZipEntry(fileName);  
		        zipfile.putNextEntry(zipEntry);  
		        zipfile.write(IOUtils.toByteArray(new FileInputStream(new File(byteFilePaths.get(fileName)))));
	    	}
	    }
	    
	    zipfile.close();  
	    return bos.toByteArray();
	}

	public static String typeDir(String ext) throws KommetException
	{
		if (FileExtension.CLASS_EXT.equals(ext))
		{
			return DeploymentProcess.CLASS_DIR;
		}
		else if (FileExtension.VIEW_EXT.equals(ext))
		{
			return DeploymentProcess.VIEW_DIR;
		}
		else if (FileExtension.TYPE_EXT.equals(ext))
		{
			return DeploymentProcess.TYPE_DIR;
		}
		else if (FileExtension.FIELD_EXT.equals(ext))
		{
			return DeploymentProcess.FIELD_DIR;
		}
		else if (FileExtension.LAYOUT_EXT.equals(ext))
		{
			return DeploymentProcess.LAYOUT_DIR;
		}
		else if (FileExtension.VALIDATION_RULE_EXT.equals(ext))
		{
			return DeploymentProcess.VALIDATION_RULE_DIR;
		}
		else if (FileExtension.UNIQUE_CHECK_EXT.equals(ext))
		{
			return DeploymentProcess.UNIQUE_CHECK_DIR;
		}
		else if (FileExtension.APP_EXT.equals(ext))
		{
			return DeploymentProcess.APP_DIR;
		}
		else if (FileExtension.SCHEDULED_TASK_EXT.equals(ext))
		{
			return DeploymentProcess.SCHEDULED_TASK_DIR;
		}
		else if (FileExtension.PROFILE_EXT.equals(ext))
		{
			return DeploymentProcess.PROFILE_DIR;
		}
		else if (FileExtension.USER_GROUP_EXT.equals(ext))
		{
			return DeploymentProcess.USER_GROUP_DIR;
		}
		else if (FileExtension.WEB_RESOURCE_EXT.equals(ext))
		{
			return DeploymentProcess.WEB_RESOURCE_DIR;
		}
		else if (FileExtension.VIEW_RESOURCE_EXT.equals(ext))
		{
			return DeploymentProcess.VIEW_RESOURCE_DIR;
		}
		else if (FileExtension.ACTION_EXT.equals(ext))
		{
			return DeploymentProcess.ACTION_DIR;
		}
		else if (FileExtension.RECORD_COLLECTION_EXT.equals(ext))
		{
			return DeploymentProcess.RECORDS_DIR;
		}
		else if (ext.equals("xml"))
		{
			return null;
		}
		else
		{
			throw new KommetException("Cannot deduce directory by extension " + ext);
		}
	}
	
}