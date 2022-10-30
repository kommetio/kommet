/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.UserService;
import kommet.basic.keetle.LayoutService;
import kommet.comments.CommentService;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.files.FileService;
import kommet.i18n.InternationalizationService;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.FieldHistoryService;
import kommet.services.ViewResourceService;
import kommet.services.WebResourceService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;

public class ViewWrapperTag extends KommetTag
{
	private static final long serialVersionUID = -6532673453031574498L;
	protected DataService dataService;
	private FileService fileService;
	private WebResourceService webResourceService;
	private InternationalizationService i18n;
	private CommentService commentService;
	private UserService userService;
	private ViewResourceService viewResourceService;
	protected AppConfig appConfig;
	protected EnvData env;
	protected EnvService envService;
	protected LayoutService layoutService;
	private SharingService sharingService;
	private KommetCompiler compiler;
	private AuthData authData;
	private FieldHistoryService fieldHistoryService;
	private StringBuilder postViewCode;
	private StringBuilder preViewCode;
	private UserCascadeHierarchyService uchService;

	public ViewWrapperTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		this.postViewCode = null;
		this.preViewCode = null;
		
		initBean();
		try
		{
			this.env = this.envService.getCurrentEnv(this.pageContext.getSession());
		}
		catch (KommetException e)
		{
			throw new JspException("Could not initialized environment in view tag: " + e.getMessage(), e);
		}
		
		if (this.dataService == null)
		{
			throw new JspException("Data service not injected into view tag");
		}
		
		this.authData = AuthUtil.getAuthData(this.pageContext.getSession());
		
		// add breadcrumb to session
		// it is done here, not in the request filter, because here we already know the name of the view
		/*if (this.pageContext.getRequest() instanceof HttpServletRequest)
		{
			try
			{
				Breadcrumbs.add(((HttpServletRequest)this.pageContext.getRequest()).getRequestURI(), "some page", appConfig.getBreadcrumbMax(), this.pageContext.getSession());
			}
			catch (PropertyUtilException e)
			{
				throw new JspException("Could not render breadcrumbs due to an error in configuration");
			}
		}*/
		
		if (this.preViewCode != null && this.preViewCode.length() > 0)
		{
			writeToPage(this.preViewCode.toString());
		}
		
		
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		if (this.postViewCode != null && this.postViewCode.length() > 0)
		{
			writeToPage(this.postViewCode.toString());
		}
		
		return EVAL_PAGE;
    }
	
	protected void initBean() throws JspException
	{
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(this.pageContext.getServletContext());
        AutowireCapableBeanFactory factory = wac.getAutowireCapableBeanFactory();
        
        if (factory == null)
        {
        	throw new JspException("Bean factory is null");
        }
        
        this.envService = factory.getBean(EnvService.class);
        this.dataService = factory.getBean(DataService.class);
        this.fileService = factory.getBean(FileService.class);
        this.userService = factory.getBean(UserService.class);
        this.layoutService = factory.getBean(LayoutService.class);
        this.commentService = factory.getBean(CommentService.class);
        this.i18n = factory.getBean(InternationalizationService.class);
        this.appConfig = factory.getBean(AppConfig.class);
        this.fieldHistoryService = factory.getBean(FieldHistoryService.class);
        this.sharingService = factory.getBean(SharingService.class);
        this.webResourceService = factory.getBean(WebResourceService.class);
        this.viewResourceService = factory.getBean(ViewResourceService.class);
        this.compiler = factory.getBean(KommetCompiler.class);
        this.uchService = factory.getBean(UserCascadeHierarchyService.class);
	}
	
	public DataService getDataService()
	{
		return this.dataService;
	}
	
	public EnvService getEnvService()
	{
		return this.envService;
	}

	public void setEnv(EnvData env)
	{
		this.env = env;
	}

	public EnvData getEnv()
	{
		return env;
	}

	public FileService getFileService()
	{
		return this.fileService;
	}

	public AuthData getAuthData()
	{
		return authData;
	}

	public UserService getUserService()
	{
		return userService;
	}
	
	public WebResourceService getWebResourceService()
	{
		return this.webResourceService;
	}

	public LayoutService getLayoutService()
	{
		return this.layoutService;
	}

	public CommentService getCommentService()
	{
		return commentService;
	}

	public InternationalizationService getI18n()
	{
		return i18n;
	}

	public FieldHistoryService getFieldHistoryService()
	{
		return fieldHistoryService;
	}
	
	public AppConfig getAppConfig()
	{
		return this.appConfig;
	}

	public SharingService getSharingService()
	{
		return sharingService;
	}

	public void addPostViewCode(String code)
	{
		if (this.postViewCode == null)
		{
			this.postViewCode = new StringBuilder();
		}
		this.postViewCode.append(code);	
	}
	
	public void addPreViewCode(String code)
	{
		if (this.preViewCode == null)
		{
			this.preViewCode = new StringBuilder();
		}
		this.preViewCode.append(code);	
	}

	public ViewResourceService getViewResourceService()
	{
		return viewResourceService;
	}

	public KommetCompiler getCompiler()
	{
		return compiler;
	}
	
	public UserCascadeHierarchyService getUserCascadeHierarchyService()
	{
		return this.uchService;
	}
}
