package org.veupathdb.service.demo.container.middleware;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.accountdb.AccountManager;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.web.LoginCookieFactory;
import org.veupathdb.service.demo.config.InvalidConfigException;
import org.veupathdb.service.demo.config.Options;
import org.veupathdb.service.demo.container.Globals;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedSet;
import static java.util.Objects.isNull;
import static org.veupathdb.service.demo.container.Globals.DB_ACCOUNT_SCHEMA;
import static org.veupathdb.service.demo.container.utils.RequestKeys.AUTH_HEADER;

/**
 * Provides client authentication checks for resource classes or methods
 * annotated with @Authenticated.
 *
 * @see Authenticated
 */
@Provider
@Priority(3)
public class AuthFilter implements ContainerRequestFilter {

  private static final Logger LOG = LogManager.getLogger(AuthFilter.class);

  private static final Set<String> AUTH_CACHE = synchronizedSet(new HashSet<>());
  private static final String MESSAGE = "Users must be logged in to access this"
    + " resource.";

  private final Options opts;
  private final AccountManager acctMan;

  @Context
  private ResourceInfo resource;

  public AuthFilter(Options opts, DatabaseInstance acctDb) {
    this.opts = opts;
    this.acctMan = new AccountManager(acctDb, DB_ACCOUNT_SCHEMA, emptyList());

    // Only validate that the secret key is present if we actually need it.
    if (opts.getAuthSecretKey().isEmpty())
      throw new InvalidConfigException("Auth secret key is required for this "
        + "service");
  }

  @Override
  public void filter(ContainerRequestContext req) {
    LOG.trace("AuthFilter#filter");

    if (!isAuthRequired(resource))
      return;

    LOG.debug("Authenticating request");

    final var rawAuth = req.getCookies().get(AUTH_HEADER).getValue();

    if (isNull(rawAuth) || rawAuth.isEmpty()) {
      LOG.debug("Authentication failed: no auth cookie.");
      req.abortWith(build401());
      return;
    }

    final var auth = LoginCookieFactory.
      parseCookieValue(rawAuth);

    if (!new LoginCookieFactory(
      opts.getAuthSecretKey().orElseThrow()).isValidCookie(auth)) {
      LOG.debug("Authentication failed: bad cookie");
      req.abortWith(build401());
      return;
    }

    final var profile = acctMan.getUserProfile(auth.getUsername());
    if (isNull(profile)) {
      LOG.debug("Authentication failed: no such user");
      req.abortWith(build401());
      return;
    }

    req.setProperty(Globals.REQUEST_USER, profile);
    LOG.debug("Request authenticated");
  }

  static Response build401() {
    return Response.status(Status.UNAUTHORIZED).entity(MESSAGE).build();
  }

  static boolean isAuthRequired(ResourceInfo res) {
    LOG.trace("AuthFilter#isAuthRequired");

    final var meth = res.getResourceMethod();
    final var type = res.getResourceClass();

    if (AUTH_CACHE.contains(type.getName()))
      return true;

    final var methName = type.getName() + '#' + meth.getName();

    if (AUTH_CACHE.contains(methName))
      return true;

    if (methodHasAuth(meth)) {
      AUTH_CACHE.add(methName);
      return true;
    }
    if (classHasAuth(type)) {
      AUTH_CACHE.add(type.getName());
      return true;
    }

    return false;
  }

  static boolean methodHasAuth(Method meth) {
    LOG.trace("AuthFilter#methodHasAuth");
    return Arrays.stream(meth.getDeclaredAnnotations())
      .anyMatch(Authenticated.class::isInstance);
  }

  static boolean classHasAuth(Class<?> type) {
    LOG.trace("AuthFilter#classHasAuth");
    return Arrays.stream(type.getDeclaredAnnotations())
      .anyMatch(Authenticated.class::isInstance);
  }

  /**
   * Annotation that flags a resource as requiring a valid user auth token to
   * execute.
   */
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Authenticated {}
}
