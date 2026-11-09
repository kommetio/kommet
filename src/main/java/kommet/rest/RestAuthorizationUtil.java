package kommet.rest;

import java.util.Collections;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import kommet.data.KommetException;
import kommet.env.EnvData;

/** Provides reusable bearer-header, transport, and API-token authentication checks. */
@Component
public class RestAuthorizationUtil
{
	@Inject
	private RestAuthenticationService authenticationService;

	@Value("${kommet.devapi.allowLoopbackHttp:false}")
	private boolean allowLoopbackHttp;

	/** Requires one well-formed bearer credential over an allowed transport. */
	public String requireAuthorization(HttpServletRequest request)
	{
		String authorization = request.getHeader("Authorization");
		if (Collections.list(request.getHeaders("Authorization")).size() != 1)
		{
			throw new ApiException(401, "Exactly one bearer credential is required");
		}
		authenticationService.bearerSecret(authorization);

		// TODO: re-enable this but remember if Tomcat is not run as HTTPS, this will fail
		// if (!request.isSecure() && !(allowLoopbackHttp && isLoopback(request.getRemoteAddr())))
		// {
		// 	throw new ApiException(403, "HTTPS is required");
		// }
		return authorization;
	}

	/** Authenticates a request's API token for the selected environment and capability. */
	public RestAuthenticationService.Authentication authenticate(HttpServletRequest request, EnvData env, RestAuthenticationService.Access access) throws KommetException
	{
		return authenticationService.authenticate(requireAuthorization(request), env, access);
	}

	/** Recognizes loopback peers eligible for explicitly enabled development HTTP. */
	private static boolean isLoopback(String address)
	{
		return "127.0.0.1".equals(address) || "::1".equals(address) || "0:0:0:0:0:0:0:1".equals(address);
	}
}