package kommet.ide;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.View;
import kommet.basic.ViewResource;
import kommet.basic.keetle.ViewService;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.services.ViewResourceService;

/** Shares the source-persistence path between the browser IDE and developer API. */
@Service
public class IDESourceSaveService
{
	@Inject
	private ClassService classService;

	@Inject
	private DataService dataService;

	@Inject
	private ViewService viewService;

	@Inject
	private ViewResourceService viewResourceService;

	public Class saveClass(Class file, String code, AuthData authData, EnvData env, HttpSession session) throws KommetException
	{
		file.setKollCode(code);
		Class saved = classService.fullSave(file, dataService, true, authData, env);
		refresh(new IDEFile(saved), session);
		return saved;
	}

	public View saveView(View file, String code, AuthData authData, EnvData env, HttpSession session) throws KommetException
	{
		View saved = viewService.fullSave(file, code, true, authData, env);
		refresh(new IDEFile(saved), session);
		return saved;
	}

	public ViewResource saveResource(ViewResource file, AuthData authData, EnvData env, HttpSession session) throws KommetException
	{
		ViewResource saved = viewResourceService.save(file, authData, env);
		refresh(new IDEFile(saved), session);
		return saved;
	}

	private static void refresh(IDEFile file, HttpSession session)
	{
		if (session != null)
		{
			IDEService.refreshFile(file, session);
		}
	}
}
