package com.ib.urireg;

import com.ib.urireg.system.SystemData;
import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.system.ActiveUser;
import com.ib.system.exceptions.AuthenticationException;
import com.ib.system.exceptions.BaseException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public class LDAPTest {

    /* Lirex */
//    private static final Logger LOGGER = LoggerFactory.getLogger(LDAPTest.class);
//    public static final String LDAP_SERVER = "ldap://10.29.0.7:389";
//    public static final String USER = "krasi";
//    public static final String PASS = "123456";
//    public static final String DEFAULT_DOMAIN = "lirex";//"indexbg";


    /* Marad - Morska Administration */
    private static final Logger LOGGER = LoggerFactory.getLogger(LDAPTest.class);
    public static final String LDAP_SERVER = "ldap://10.56.1.30:389";
    public static final String USER = "indexbg";
    public static final String PASS = "in12De!B7";
    public static final String DEFAULT_DOMAIN = "marad";//"indexbg";

    @Test
    public void test() {
        InitialContext ldapContext = null;

        try{
        Hashtable<String, String> ldapContextProperties = new Hashtable<>();

        ldapContextProperties.put(Context.PROVIDER_URL, LDAP_SERVER);// url of the ldap server
        ldapContextProperties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");// initial context factory
        ldapContextProperties.put(Context.SECURITY_AUTHENTICATION, "simple");

        // username of the user
        ldapContextProperties.put(Context.SECURITY_PRINCIPAL, DEFAULT_DOMAIN +"\\"+ USER);

        // password of the user
        ldapContextProperties.put(Context.SECURITY_CREDENTIALS, PASS);

        // if we can create the context it means that user is present into the ldap directory
        ldapContext = new InitialDirContext(ldapContextProperties);

        LOGGER.info("End findUserLDAP ... done!");

        // TODO тука с грешките надолу не е добре
    } catch (javax.naming.AuthenticationException e) {
        LOGGER.error(e.getMessage());
        throw new AuthenticationException(AuthenticationException.CODE_USER_UNKNOWN, null);

    } catch (Exception e) {
        LOGGER.error("LDAPCredential login ERROR!", e);
        throw new AuthenticationException(e);

    } finally {
        if (ldapContext != null) {
            try {
                ldapContext.close();
            } catch (NamingException e) {
                // ignore
            }
        }
    }

}

@Test
public  void test2(){
    AdmUserDAO dao=new AdmUserDAO(ActiveUser.DEFAULT);
    try {

        String username = "indexbg\\krasi";
        AdmUser admUser = dao.validateUser(new SystemData(), null, username);
        System.out.printf(admUser.toString());
    } catch (BaseException e) {
        throw new RuntimeException(e);
    }
}
}
