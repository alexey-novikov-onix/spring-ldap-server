package com.onix.ldap.config;

import com.onix.ldap.properties.EmbeddedLdapServerProperties;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.util.Objects;

@Configuration
@EnableConfigurationProperties({EmbeddedLdapServerProperties.class})
public class LdapServerListenerConfiguration {

    private static final String LISTENER_SSL_NAME = "LDAPS";

    private static final String LISTENER_NAME = "LDAP";

    private final EmbeddedLdapServerProperties embeddedLdapServerProperties;

    @Autowired
    public LdapServerListenerConfiguration(EmbeddedLdapServerProperties embeddedLdapServerProperties) {
        this.embeddedLdapServerProperties = embeddedLdapServerProperties;
    }

    @Bean
    public InMemoryListenerConfig inMemoryListenerConfig() throws Exception {
        String keystore = this.embeddedLdapServerProperties.getSsl().getKeystore();

        if (Objects.isNull(keystore)) {
            return this.createListenerConfig();
        } else {
            return this.createSslListenerConfig();
        }
    }

    private InMemoryListenerConfig createSslListenerConfig() throws Exception {
        final String keystoreFile = ResourceUtils.getFile(this.embeddedLdapServerProperties.getSsl().getKeystore())
                .getCanonicalPath();

        final SSLUtil serverSSLUtil = new SSLUtil(
                new KeyStoreKeyManager(
                        keystoreFile,
                        this.embeddedLdapServerProperties.getSsl().getPassword().toCharArray()
                ),
                new TrustStoreTrustManager(keystoreFile)
        );

        final SSLUtil clientSSLUtil = new SSLUtil(new TrustStoreTrustManager(keystoreFile));

        return InMemoryListenerConfig
                .createLDAPSConfig(
                        LISTENER_SSL_NAME,
                        null,
                        this.embeddedLdapServerProperties.getPort(),
                        serverSSLUtil.createSSLServerSocketFactory(),
                        clientSSLUtil.createSSLSocketFactory()
                );
    }

    private InMemoryListenerConfig createListenerConfig() throws Exception {
        return InMemoryListenerConfig.createLDAPConfig(LISTENER_NAME, this.embeddedLdapServerProperties.getPort());
    }

}
