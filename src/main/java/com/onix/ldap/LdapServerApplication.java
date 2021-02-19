package com.onix.ldap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ldap.embedded.EmbeddedLdapAutoConfiguration;

@SpringBootApplication(
        exclude = {EmbeddedLdapAutoConfiguration.class}
)
public class LdapServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LdapServerApplication.class, args);
    }

}
