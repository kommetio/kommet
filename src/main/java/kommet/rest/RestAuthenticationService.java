package kommet.rest;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.basic.ApiToken;
import kommet.basic.User;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.services.ApiTokenService;
import kommet.uch.UserCascadeHierarchyService;

/** Authenticates environment-scoped API tokens without using a browser session. */
@Service
public class RestAuthenticationService
{
	private static final Logger log = LoggerFactory.getLogger(RestAuthenticationService.class);

	/** Identifies the token capability required by an endpoint. */
	public enum Access { READ, WRITE }

	@Inject
	private ApiTokenService apiTokenService;

	@Inject
	private UserService userService;

	@Inject
	private UserCascadeHierarchyService uchService;

	/** Extracts a secret from a well-formed bearer Authorization header. */
	public String bearerSecret(String authorization)
	{
		if (authorization == null || !authorization.matches("(?i)Bearer [^\\s]+"))
		{
			throw new ApiException(401, "A valid bearer token is required");
		}
		return authorization.substring(7);
	}

	/** Validates the token and initializes its owner's request-local permissions. */
	@Transactional(readOnly = true)
	public Authentication authenticate(String authorization, EnvData env, Access access) throws KommetException
	{
		log.debug("[APITOKEN] Authenticating request for environment " + (env != null ? env.getId() : null) + " with access " + access);
		ApiToken token = apiTokenService.findValidToken(bearerSecret(authorization), env);
		
		if (token == null || token.getSystemUser() == null)
		{
			log.debug("[APITOKEN] Token lookup failed or token has no system user");
			throw new ApiException(401, "Invalid or expired bearer token");
		}

		User user = userService.getUser(token.getSystemUser().getId(), env);
		if (user == null)
		{
			log.debug("[APITOKEN] Token owner could not be loaded for token " + token.getId());
			throw new ApiException(401, "Invalid or expired bearer token");
		}

		if (!Boolean.TRUE.equals(user.getIsActive()))
		{
			log.debug("[APITOKEN] Token owner is inactive for token " + token.getId());
			throw new ApiException(401, "User inactive");
		}

		if ((access == Access.READ && !token.isReadPermission()) || (access == Access.WRITE && !token.isWritePermission()))
		{
			log.debug("[APITOKEN] Token " + token.getId() + " does not permit access " + access);
			throw new ApiException(403, "API token does not permit this operation");
		}
		AuthData authData = userService.getAuthData(user, env);
		authData.initUserPermissions(env);
		authData.initUserCascadeSettings(uchService, env);
		log.debug("[APITOKEN] Authentication succeeded for token " + token.getId());
		return new Authentication(authData, env, token.getId());
	}

	/** Carries the authenticated identity and environment without exposing the secret. */
	public static final class Authentication
	{
		private final AuthData authData;
		private final EnvData env;
		private final KID tokenId;

		/** Captures the identity and token identifier for this request. */
		private Authentication(AuthData authData, EnvData env, KID tokenId)
		{
			this.authData = authData;
			this.env = env;
			this.tokenId = tokenId;
		}

		/** Returns the token owner's initialized permissions. */
		public AuthData getAuthData()
		{
			return authData;
		}

		/** Returns the environment in which the token was authenticated. */
		public EnvData getEnv()
		{
			return env;
		}

		/** Returns the stored token identifier for auditing. */
		public KID getTokenId()
		{
			return tokenId;
		}
	}
}