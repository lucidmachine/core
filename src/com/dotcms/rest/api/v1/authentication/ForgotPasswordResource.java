package com.dotcms.rest.api.v1.authentication;

import com.dotcms.company.CompanyAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.SendPasswordException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.ejb.UserManager;
import com.liferay.portal.ejb.UserManagerFactory;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

/**
 * This resource sends email with a link to recovery your password, if it is successfully returns the User email where the message is gonna be sent,
 * If there is a known error returns 500 and the error messages related.
 * Otherwise returns 500 and the exception as Json.
 * @author jsanca
 */
@Path("/v1/forgotpassword")
public class ForgotPasswordResource implements Serializable {

    private final UserLocalManager userLocalManager;
    private final UserManager userManager;
    private final CompanyAPI  companyAPI;
    private final AuthenticationHelper  authenticationHelper;
    private final SecurityLoggerServiceAPI securityLogger;

    public ForgotPasswordResource() {

        this (UserLocalManagerFactory.getManager(),
                UserManagerFactory.getManager(),
                APILocator.getCompanyAPI(),
                AuthenticationHelper.INSTANCE,
                APILocator.getSecurityLogger()
                );
    }

    @VisibleForTesting
    public ForgotPasswordResource(final UserLocalManager userLocalManager,
                                  final UserManager userManager,
                                  final CompanyAPI  companyAPI,
                                  final AuthenticationHelper  authenticationHelper,
                                  final SecurityLoggerServiceAPI securityLogger) {

        this.userLocalManager = userLocalManager;
        this.userManager      = userManager;
        this.companyAPI       = companyAPI;
        this.authenticationHelper = authenticationHelper;
        this.securityLogger   = securityLogger;
    }

    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response forgotPassword(@Context final HttpServletRequest request,
                                         @Context final HttpServletResponse response,
                                         final ForgotPasswordForm forgotPasswordForm) {

        Response res = null;
        String emailAddress = null;
        final Locale locale = LocaleUtil.getLocale(request);

        try {

            final Company company = this.companyAPI.getCompany(request);

            emailAddress = (Company.AUTH_TYPE_ID.equals(company.getAuthType()))?
                        this.userLocalManager.getUserById
                                (forgotPasswordForm.getUserId()).getEmailAddress():
                        forgotPasswordForm.getUserId();

            this.userManager.sendPassword(
                    this.companyAPI.getCompanyId(request), emailAddress, locale);

            res = Response.ok(new ResponseEntityView(emailAddress)).build(); // 200
            this.securityLogger.logInfo(this.getClass(),
                    "Email address " + emailAddress + " has request to reset his password from IP: "
                            + request.getRemoteAddr());
        } catch (NoSuchUserException e) {

            boolean displayNotSuchUserError =
                    Config.getBooleanProperty("si n ", false);

            if (displayNotSuchUserError) {

                res = this.authenticationHelper.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                        "the-email-address-you-requested-is-not-registered-in-our-database");
            } else {

                this.securityLogger.logInfo(this.getClass(),
                        "User does NOT exist in the Database, returning OK message for security reasons");

                try {

                    res = Response.status(Response.Status.UNAUTHORIZED).entity(new ResponseEntityView
                            (Arrays.asList(new ErrorEntity("a-new-password-has-been-sent-to-x",
                                    LanguageUtil.format(locale,
                                            "a-new-password-has-been-sent-to-x", emailAddress, false)
                            )))).build();
                } catch (LanguageException e1) {
                    // Quiet
                }
            }
        } catch (SendPasswordException e) {

            res = this.authenticationHelper.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                      "a-new-password-can-only-be-sent-to-an-external-email-address");
        } catch (UserEmailAddressException e) {

            res = this.authenticationHelper.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
                    "please-enter-a-valid-email-address");
        } catch (Exception e) {

            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    } // authentication.

} // E:O:F:ForgotPasswordResource.
