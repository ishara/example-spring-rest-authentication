package com.kevinpotgieter.web.security;

import com.kevinpotgieter.services.UserSecurityService;
import com.kevinpotgieter.web.security.tokens.RESTAuthenticationToken;
import com.kevinpotgieter.web.security.tokens.RESTCredentials;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.text.MessageFormat;

/**
 * Created by IntelliJ IDEA.
 * User: kevinpotgieter
 * Date: 27/04/2012
 * Time: 10:20
 * To change this template use File | Settings | File Templates.
 */
public class RESTDaoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {


    private UserSecurityService userSecurityService;
    private PasswordEncoder passwordEncoder;

    /**
     * This is the method which actually performs the check to see whether the user is indeed the correct user
     * @param userDetails
     * @param authentication
     * @throws AuthenticationException
     */
    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        // here we check if the details provided by the user actually stack up.

        //Get Credentials out of the Token...
        RESTAuthenticationToken token = (RESTAuthenticationToken)authentication;
        if(token != null){
            if (authentication.getCredentials() == null) {
                logger.debug("Authentication failed: no credentials provided");

                throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
            }

            RESTCredentials restCredentials = (RESTCredentials)authentication.getCredentials();

            if (!passwordEncoder.isPasswordValid(restCredentials.getSecureHash(), userDetails.getPassword(), restCredentials.getRequestSalt())) {
                logger.debug("Authentication failed: password does not match stored value");

                throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
            }

        }
        else{
            throw new AuthenticationCredentialsNotFoundException(MessageFormat.format("Expected Authentication Token object of type {0}, but instead received {1}",RESTAuthenticationToken.class.getSimpleName(), authentication.getClass().getSimpleName()));
        }
    }

    /**
     *
     * @param apiKey This is the API Key that was generated by the user. I guess you could just use the users's username here, but we
     * want something that's a little less "guessable"
     *
     * @param authentication The authentication request, which subclasses <em>may</em> need to perform a binding-based
     *        retrieval of the <code>UserDetails</code>
     *
     * @return the user information (never <code>null</code> - instead an exception should the thrown)
     *
     * @throws AuthenticationException if the credentials could not be validated (generally a
     *         <code>BadCredentialsException</code>, an <code>AuthenticationServiceException</code> or
     *         <code>UsernameNotFoundException</code>)
     */
    @Override
    protected UserDetails retrieveUser(String apiKey, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        UserDetails loadedUser;

        try {
            loadedUser = this.getUserSecurityService().getUserByApiKey(apiKey);
        } catch (UsernameNotFoundException notFound) {
            throw notFound;
        } catch (Exception repositoryProblem) {
            throw new AuthenticationServiceException(repositoryProblem.getMessage(), repositoryProblem);
        }

        if (loadedUser == null) {
            throw new AuthenticationServiceException(
                    "UserSecurityServiceImpl returned null, which is an interface contract violation");
        }
        return loadedUser;
    }

    @Override
    protected void doAfterPropertiesSet() throws Exception {
        Assert.notNull(this.userSecurityService, "A UserSecurityServiceImpl must be set");
        Assert.notNull(this.passwordEncoder, "A PasswordEncoder must be set");
    }


    public UserSecurityService getUserSecurityService() {
        return userSecurityService;
    }

    public void setUserSecurityService(UserSecurityService userSecurityService) {
        this.userSecurityService = userSecurityService;
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
}
