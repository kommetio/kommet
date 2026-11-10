package kommet.web.controllers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Class;
import kommet.basic.View;
import kommet.basic.ViewResource;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.EnvFilter;
import kommet.koll.ClassFilter;
import kommet.ide.IDESourceSaveService;
import kommet.koll.ClassCompilationException;
import kommet.koll.ClassService;
import kommet.koll.KollUtil;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.ViewFilter;
import kommet.filters.ViewResourceFilter;
import kommet.rest.ApiException;
import kommet.rest.RestAuthenticationService;
import kommet.rest.RestAuthorizationUtil;
import kommet.rest.SchemaDataTypeParser;
import kommet.services.ViewResourceService;

/** Provides token-authenticated read and save endpoints for editable IDE sources. */
@Controller
public class IdeApiController
{
	public static final String CLASS_GET_PATH = "/api/dev/class/get";
	public static final String CLASS_SAVE_PATH = "/api/dev/class/save";
	public static final String VIEW_GET_PATH = "/api/dev/view/get";
	public static final String VIEW_SAVE_PATH = "/api/dev/view/save";
	public static final String JS_GET_PATH = "/api/dev/js/get";
	public static final String JS_SAVE_PATH = "/api/dev/js/save";
	public static final String CSS_GET_PATH = "/api/dev/css/get";
	public static final String CSS_SAVE_PATH = "/api/dev/css/save";
	public static final String CLASS_LIST_PATH = "/api/ide/class/list";
	public static final String VIEW_LIST_PATH = "/api/ide/view/list";
	public static final String JS_LIST_PATH = "/api/ide/js/list";
	public static final String CSS_LIST_PATH = "/api/ide/css/list";

	private static final Logger log = LoggerFactory.getLogger(IdeApiController.class);
	private static final String JS_MIME_TYPE = "application/javascript";
	private static final String CSS_MIME_TYPE = "text/css";

	@Inject
	private EnvService envService;

	@Inject
	private RestAuthorizationUtil authorizationUtil;

	@Inject
	private ClassService classService;

	@Inject
	private ViewService viewService;

	@Inject
	private ViewResourceService viewResourceService;

	@Inject
	private IDESourceSaveService sourceSaveService;

	@RequestMapping(value = CLASS_LIST_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Map<String, Object>>> listClasses(@RequestParam(value = "envId", required = false) String envId,
			HttpServletRequest request) throws KommetException
	{
		return listSources(envId, "class", request);
	}

	@RequestMapping(value = VIEW_LIST_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Map<String, Object>>> listViews(@RequestParam(value = "envId", required = false) String envId,
			HttpServletRequest request) throws KommetException
	{
		return listSources(envId, "view", request);
	}

	@RequestMapping(value = JS_LIST_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Map<String, Object>>> listJs(@RequestParam(value = "envId", required = false) String envId,
			HttpServletRequest request) throws KommetException
	{
		return listSources(envId, "js", request);
	}

	@RequestMapping(value = CSS_LIST_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Map<String, Object>>> listCss(@RequestParam(value = "envId", required = false) String envId,
			HttpServletRequest request) throws KommetException
	{
		return listSources(envId, "css", request);
	}

	@RequestMapping(value = CLASS_GET_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getClass(@RequestParam(value = "envId", required = false) String envId,
			@RequestParam(value = "qualifiedName", required = false) String qualifiedName, HttpServletRequest request) throws KommetException
	{
		EnvData env = readEnvironment(envId, request);
		requireAdministrator(authorizationUtil.authenticate(request, env, RestAuthenticationService.Access.READ).getAuthData());
		if (!StringUtils.hasText(qualifiedName))
		{
			throw new ApiException(400, "qualifiedName is required");
		}
		Class file = classService.getClass(qualifiedName, env);
		if (file == null)
		{
			throw new ApiException(404, "Class not found");
		}
		if (!Boolean.FALSE.equals(file.getIsSystem()))
		{
			throw new ApiException(403, "System classes cannot be accessed through the IDE API");
		}
		log.info("[IDEAPI] Read class {} in environment {}", qualifiedName, env.getId());
		return jsonSuccess(sourceData(file.getId().getId(), file.getQualifiedName(), file.getKollCode(), null));
	}

	@RequestMapping(value = VIEW_GET_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getView(@RequestParam(value = "envId", required = false) String envId,
			@RequestParam(value = "qualifiedName", required = false) String qualifiedName, HttpServletRequest request) throws KommetException
	{
		EnvData env = readEnvironment(envId, request);
		requireAdministrator(authorizationUtil.authenticate(request, env, RestAuthenticationService.Access.READ).getAuthData());
		if (!StringUtils.hasText(qualifiedName))
		{
			throw new ApiException(400, "qualifiedName is required");
		}
		View file = viewService.getView(qualifiedName, true, env);
		if (file == null)
		{
			throw new ApiException(404, "View not found");
		}
		if (!Boolean.FALSE.equals(file.getIsSystem()))
		{
			throw new ApiException(403, "System views cannot be accessed through the IDE API");
		}
		log.info("[IDEAPI] Read view {} in environment {}", qualifiedName, env.getId());
		return jsonSuccess(sourceData(file.getId().getId(), file.getQualifiedName(), file.getKeetleCode(), null));
	}

	@RequestMapping(value = JS_GET_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getJs(@RequestParam(value = "envId", required = false) String envId,
			@RequestParam(value = "name", required = false) String name, HttpServletRequest request) throws KommetException
	{
		return getResource(envId, name, "js", request);
	}

	@RequestMapping(value = CSS_GET_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getCss(@RequestParam(value = "envId", required = false) String envId,
			@RequestParam(value = "name", required = false) String name, HttpServletRequest request) throws KommetException
	{
		return getResource(envId, name, "css", request);
	}

	@RequestMapping(value = CLASS_SAVE_PATH, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> saveClass(@RequestParam(value = "envId", required = false) String envId,
			@RequestParam(value = "fileId", required = false) String fileId,
			@RequestParam(value = "file", required = false) MultipartFile sourceFile, HttpServletRequest request) throws KommetException, IOException
	{
		return saveSource(envId, fileId, null, sourceFile, request, "class");
	}

	@RequestMapping(value = VIEW_SAVE_PATH, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> saveView(@RequestParam(value = "envId", required = false) String envId,
			@RequestParam(value = "fileId", required = false) String fileId,
			@RequestParam(value = "file", required = false) MultipartFile sourceFile, HttpServletRequest request) throws KommetException, IOException
	{
		return saveSource(envId, fileId, null, sourceFile, request, "view");
	}

	@RequestMapping(value = JS_SAVE_PATH, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> saveJs(@RequestParam(value = "envId", required = false) String envId,
			@RequestParam(value = "fileId", required = false) String fileId,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "file", required = false) MultipartFile sourceFile, HttpServletRequest request) throws KommetException, IOException
	{
		return saveSource(envId, fileId, name, sourceFile, request, "js");
	}

	@RequestMapping(value = CSS_SAVE_PATH, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> saveCss(@RequestParam(value = "envId", required = false) String envId,
			@RequestParam(value = "fileId", required = false) String fileId,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "file", required = false) MultipartFile sourceFile, HttpServletRequest request) throws KommetException, IOException
	{
		return saveSource(envId, fileId, name, sourceFile, request, "css");
	}

	private ResponseEntity<Map<String, Object>> getResource(String envId, String name, String kind, HttpServletRequest request) throws KommetException
	{
		EnvData env = readEnvironment(envId, request);
		AuthData authData = authorizationUtil.authenticate(request, env, RestAuthenticationService.Access.READ).getAuthData();
		requireAdministrator(authData);
		if (!StringUtils.hasText(name))
		{
			throw new ApiException(400, "name is required");
		}
		ViewResourceFilter filter = new ViewResourceFilter();
		filter.setName(name);
		filter.setFetchContent(true);
		java.util.List<ViewResource> resources = viewResourceService.find(filter, env);
		ViewResource file = resources.isEmpty() ? null : resources.get(0);
		if (file == null || !isResourceKind(file, kind))
		{
			throw new ApiException(404, kind.toUpperCase() + " resource not found");
		}
		log.info("[IDEAPI] Read " + kind.toUpperCase() + " resource " + name + " in environment " + env.getId());
		return jsonSuccess(sourceData(file.getId().getId(), file.getName(), file.getContent(), file.getMimeType()));
	}

	private ResponseEntity<List<Map<String, Object>>> listSources(String envId, String kind, HttpServletRequest request) throws KommetException
	{
		EnvData env = readEnvironment(envId, request);
		requireAdministrator(authorizationUtil.authenticate(request, env, RestAuthenticationService.Access.READ).getAuthData());
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

		if ("class".equals(kind))
		{
			ClassFilter filter = new ClassFilter();
			filter.setSystemFile(false);
			for (Class file : classService.getClasses(filter, env))
			{
				if (Boolean.FALSE.equals(file.getIsSystem()))
				{
					result.add(sourceListItem(file.getName(), file.getPackageName(), file.getId().getId()));
				}
			}
		}
		else if ("view".equals(kind))
		{
			ViewFilter filter = new ViewFilter();
			filter.setSystemView(false);
			for (View file : viewService.getViews(filter, env))
			{
				if (Boolean.FALSE.equals(file.getIsSystem()))
				{
					result.add(sourceListItem(file.getName(), file.getPackageName(), file.getId().getId()));
				}
			}
		}
		else
		{
			for (ViewResource file : viewResourceService.find(null, env))
			{
				if (isResourceKind(file, kind))
				{
					result.add(resourceListItem(file, kind));
				}
			}
		}

		log.info("[IDEAPI] Listed " + kind.toUpperCase() + " files in environment " + env.getId() + ": " + result.size());
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
	}

	private ResponseEntity<Map<String, Object>> saveSource(String envId, String fileIdText, String name, MultipartFile sourceFile,
			HttpServletRequest request, String kind) throws KommetException, IOException
	{
		authorizationUtil.requireAuthorization(request);
		if (!isMultipartContentType(request.getContentType()))
		{
			throw new ApiException(400, "Content-Type must be multipart/form-data");
		}
		if (!StringUtils.hasText(envId))
		{
			throw new ApiException(400, "envId is required");
		}
		if (sourceFile == null)
		{
			throw new ApiException(400, "Multipart file part 'file' is required");
		}
		EnvData env = findEnvironment(envId);
		RestAuthenticationService.Authentication authentication = authorizationUtil.authenticate(request, env, RestAuthenticationService.Access.WRITE);
		AuthData authData = authentication.getAuthData();
		requireAdministrator(authData);

		String code = new String(sourceFile.getBytes(), StandardCharsets.UTF_8);
		fileIdText = StringUtils.hasText(fileIdText) ? fileIdText : null;
		name = StringUtils.hasText(name) ? name : null;

		try
		{
			Map<String, Object> data;
			if ("class".equals(kind))
			{
				requireNonBlankCode(code);
				Class file = fileIdText == null ? KollUtil.getClassFromCode(code, env) : loadClass(fileIdText, env);
				boolean created = fileIdText == null;
				if (created)
				{
					file.setIsSystem(false);
				}
				else if (!Boolean.FALSE.equals(file.getIsSystem()))
				{
					throw new ApiException(403, "System classes cannot be modified through the IDE API");
				}
				file = sourceSaveService.saveClass(file, code, authData, env, null);
				data = sourceData(file.getId().getId(), file.getQualifiedName(), null, null);
				data.put("operation", created ? "created" : "updated");
			}
			else if ("view".equals(kind))
			{
				requireNonBlankCode(code);
				boolean created = fileIdText == null;
				View file = created ? new View() : loadView(fileIdText, env);
				if (created)
				{
					file.setIsSystem(false);
				}
				else if (!Boolean.FALSE.equals(file.getIsSystem()))
				{
					throw new ApiException(403, "System views cannot be modified through the IDE API");
				}
				file = sourceSaveService.saveView(file, code, authData, env, null);
				data = sourceData(file.getId().getId(), file.getQualifiedName(), null, null);
				data.put("operation", created ? "created" : "updated");
			}
			else
			{
				ViewResource file;
				boolean created = fileIdText == null;
				if (created)
				{
					if (!StringUtils.hasText(name))
					{
						throw new ApiException(400, "name is required when creating a resource");
					}
					file = new ViewResource();
					file.setName(name);
					file.setMimeType("js".equals(kind) ? JS_MIME_TYPE : CSS_MIME_TYPE);
				}
				else
				{
					file = loadResource(fileIdText, env, kind);
					if (StringUtils.hasText(name) && !name.equals(file.getName()))
					{
						throw new ApiException(400, "Resource name cannot be changed");
					}
				}
				file.setContent(code);
				file = sourceSaveService.saveResource(file, authData, env, null);
				data = sourceData(file.getId().getId(), file.getName(), null, file.getMimeType());
				data.put("operation", created ? "created" : "updated");
			}

			log.info("[IDEAPI] Saved " + kind.toUpperCase() + " source " + data.get("name") + " in environment " + env.getId());
			return ResponseEntity.status("created".equals(data.get("operation")) ? 201 : 200)
					.contentType(MediaType.APPLICATION_JSON).body(envelope(data));
		}
		catch (ApiException exception)
		{
			throw exception;
		}
		catch (ClassCompilationException exception)
		{
			throw new ApiException(400, exception.getMessage());
		}
		catch (KommetException exception)
		{
			throw new ApiException(400, "Source could not be saved: " + exception.getMessage());
		}
	}

	private Class loadClass(String fileIdText, EnvData env) throws KommetException
	{
		KID id = SchemaDataTypeParser.id(fileIdText);
		requireType(id, KID.CLASS_PREFIX, env);
		Class file = classService.getClass(id, env);
		if (file == null)
		{
			throw new ApiException(404, "Class not found");
		}
		return file;
	}

	private View loadView(String fileIdText, EnvData env) throws KommetException
	{
		KID id = SchemaDataTypeParser.id(fileIdText);
		requireType(id, KID.VIEW_PREFIX, env);
		View file = viewService.getView(id, env);
		if (file == null)
		{
			throw new ApiException(404, "View not found");
		}
		return file;
	}

	private ViewResource loadResource(String fileIdText, EnvData env, String kind) throws KommetException
	{
		KID id = SchemaDataTypeParser.id(fileIdText);
		requireType(id, KID.VIEW_RESOURCE_PREFIX, env);
		ViewResource file = viewResourceService.get(id, env);
		if (file == null || !isResourceKind(file, kind))
		{
			throw new ApiException(404, kind.toUpperCase() + " resource not found");
		}
		return file;
	}

	private static void requireType(KID id, String expectedPrefix, EnvData env) throws KommetException
	{
		Type type = env.getTypeByRecordId(id);
		if (type == null || !expectedPrefix.equals(type.getKeyPrefix().getPrefix()))
		{
			throw new ApiException(404, "Source file not found");
		}
	}

	private static boolean isResourceKind(ViewResource file, String kind)
	{
		String mimeType = file.getMimeType();
		String name = file.getName() != null ? file.getName().toLowerCase() : "";
		if (name.endsWith(".css"))
		{
			return "css".equals(kind);
		}
		if (name.endsWith(".js"))
		{
			return "js".equals(kind);
		}
		if ("css".equals(kind))
		{
			return CSS_MIME_TYPE.equalsIgnoreCase(mimeType);
		}
		return "application/javascript".equalsIgnoreCase(mimeType)
				|| "text/javascript".equalsIgnoreCase(mimeType);
	}

	private EnvData readEnvironment(String envId, HttpServletRequest request) throws KommetException
	{
		authorizationUtil.requireAuthorization(request);
		if (!StringUtils.hasText(envId))
		{
			throw new ApiException(400, "envId is required");
		}
		return findEnvironment(envId);
	}

	private EnvData findEnvironment(String envId) throws KommetException
	{
		KID requestedEnvId = SchemaDataTypeParser.id(envId);
		EnvFilter filter = new EnvFilter();
		filter.setKID(requestedEnvId);
		if (envService.find(filter, envService.getMasterEnv()).isEmpty())
		{
			throw new ApiException(404, "Environment not found");
		}
		return envService.get(requestedEnvId);
	}

	private static void requireNonBlankCode(String code)
	{
		if (!StringUtils.hasText(code))
		{
			throw new ApiException(400, "code must not be blank");
		}
	}

	private static void requireAdministrator(AuthData authData)
	{
		if (!AuthUtil.isRoot(authData) && !AuthUtil.isSysAdmin(authData))
		{
			throw new ApiException(403, "IDE API requires a root or system-administrator profile");
		}
	}

	private static boolean isMultipartContentType(String contentType)
	{
		if (contentType == null)
		{
			return false;
		}
		try
		{
			return MediaType.MULTIPART_FORM_DATA.isCompatibleWith(MediaType.parseMediaType(contentType));
		}
		catch (IllegalArgumentException exception)
		{
			return false;
		}
	}

	private static Map<String, Object> sourceData(String fileId, String name, String code, String mimeType)
	{
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("fileId", fileId);
		data.put("name", name);
		if (code != null)
		{
			data.put("code", code);
		}
		if (mimeType != null)
		{
			data.put("mimeType", mimeType);
		}
		return data;
	}

	private static Map<String, Object> sourceListItem(String name, String packageName, String id)
	{
		Map<String, Object> item = new LinkedHashMap<String, Object>();
		item.put("name", name);
		item.put("package", packageName != null ? packageName : "");
		item.put("id", id);
		return item;
	}

	private static Map<String, Object> resourceListItem(ViewResource file, String kind)
	{
		String resourceName = file.getName() != null ? file.getName() : "";
		String extension = "";
		String lowerName = resourceName.toLowerCase();
		String expectedExtension = "." + kind;
		if (lowerName.endsWith(expectedExtension))
		{
			extension = resourceName.substring(resourceName.length() - expectedExtension.length());
			resourceName = resourceName.substring(0, resourceName.length() - expectedExtension.length());
		}
		int separator = resourceName.lastIndexOf('.');
		if (separator > 0)
		{
			return sourceListItem(resourceName.substring(separator + 1) + extension,
					resourceName.substring(0, separator), file.getId().getId());
		}
		return sourceListItem(resourceName + extension, "", file.getId().getId());
	}

	private static ResponseEntity<Map<String, Object>> jsonSuccess(Map<String, Object> data)
	{
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(envelope(data));
	}

	private static Map<String, Object> envelope(Object data)
	{
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put("success", true);
		response.put("data", data);
		return response;
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, Object>> error(ApiException exception)
	{
		log.warn("[IDEAPI] Request rejected with status {}: {}", exception.getStatus(), exception.getMessage());
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put("success", false);
		response.put("messages", Collections.singletonList(exception.getMessage()));
		ResponseEntity.BodyBuilder builder = ResponseEntity.status(exception.getStatus()).contentType(MediaType.APPLICATION_JSON);
		if (exception.getStatus() == 401)
		{
			builder.header("WWW-Authenticate", "Bearer");
		}
		return builder.body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> unexpectedError(Exception exception)
	{
		log.error("[IDEAPI] Unexpected IDE API failure", exception);
		Map<String, Object> response = new LinkedHashMap<String, Object>();
		response.put("success", false);
		response.put("messages", Collections.singletonList("Internal IDE API error"));
		return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON).body(response);
	}
}
