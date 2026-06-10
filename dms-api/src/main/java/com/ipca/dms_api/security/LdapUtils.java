package com.ipca.dms_api.security;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.springframework.stereotype.Component;

@Component
public class LdapUtils {

    public static LdapContext ldapCheck(String userid, String pwd) {
        LdapContext ctx;
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "Simple");
        env.put(Context.SECURITY_PRINCIPAL, userid + "@ipca.com");
        env.put(Context.SECURITY_CREDENTIALS, pwd);
        env.put(Context.PROVIDER_URL, "ldap://172.16.1.7:389");

       try {
    ctx = new InitialLdapContext(env, null);
    return ctx;
} catch (NamingException nex) {
    nex.printStackTrace();   // IMPORTANT
    return null;
}
    }
}
