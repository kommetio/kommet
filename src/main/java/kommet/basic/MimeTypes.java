/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.data.KommetException;

public class MimeTypes
{
	public static final String CSS_MIME_TYPE = "text/css";
	public static final String JAVASCRIPT_MIME_TYPE = "application/javascript";
	public static final String APPLICATION_EXCEL = "application/excel";
	public static final String PNG = "image/png";
	public static final String JPEG = "image/jpeg";
	public static final String BMP = "image/bmp";
	public static final String JPEG2 = "image/jpg";
	public static final String GIF = "image/gif";
	public static final String ICON = "image/x-icon";
	public static final String ICON2 = "image/ico";
	
	public static String getExtension(String mimeType) throws KommetException
	{
		if (CSS_MIME_TYPE.equals(mimeType))
		{
			return "css";
		}
		else if (JAVASCRIPT_MIME_TYPE.equals(mimeType))
		{
			return "js";
		}
		else if (APPLICATION_EXCEL.equals(mimeType))
		{
			return "xls";
		}
		else if (PNG.equals(mimeType))
		{
			return "png";
		}
		else if (JPEG.equals(mimeType))
		{
			return "jpeg";
		}
		else if (JPEG2.equals(mimeType))
		{
			return "jpg";
		}
		else if (GIF.equals(mimeType))
		{
			return "gif";
		}
		else if (ICON.equals(mimeType))
		{
			return "ico";
		}
		else if (ICON2.equals(mimeType))
		{
			return "ico";
		}
		else if (BMP.equals(mimeType))
		{
			return "bmp";
		}
		else
		{
			throw new KommetException("Cannot find extension for mime type " + mimeType);
		}
	}

	public static String getFromExtension(String ext) throws KommetException
	{
		if ("css".equals(ext))
		{
			return CSS_MIME_TYPE;
		}
		else if ("js".equals(ext))
		{
			return JAVASCRIPT_MIME_TYPE;
		}
		else if ("xls".equals(ext))
		{
			return APPLICATION_EXCEL;
		}
		else if ("png".equals(ext))
		{
			return PNG;
		}
		else if ("jpeg".equals(ext))
		{
			return JPEG;
		}
		else if ("jpg".equals(ext))
		{
			return JPEG;
		}
		else if ("gif".equals(ext))
		{
			return GIF;
		}
		else if ("ico".equals(ext))
		{
			return ICON;
		}
		else if ("bmp".equals(ext))
		{
			return BMP;
		}
		else
		{
			throw new KommetException("Cannot get mime type from extension " + ext);
		}
	}
}