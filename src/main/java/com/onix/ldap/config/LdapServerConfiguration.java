package com.onix.ldap.config;

import com.onix.ldap.interceptor.OperationInterceptor;
import com.onix.ldap.properties.EmbeddedLdapServerProperties;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldif.LDIFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.boot.autoconfigure.ldap.embedded.EmbeddedLdapProperties.Credential;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties({LdapProperties.class, EmbeddedLdapServerProperties.class})
@AutoConfigureBefore(LdapAutoConfiguration.class)
public class LdapServerConfiguration {

    private static final String PROPERTY_SOURCE_NAME = "ldap.ports";

    private final EmbeddedLdapServerProperties embeddedLdapServerProperties;

    private final LdapProperties properties;

    private final ConfigurableApplicationContext applicationContext;

    private final Environment environment;

    private final InMemoryListenerConfig inMemoryListenerConfig;

    private final OperationInterceptor operationInterceptor;

    private InMemoryDirectoryServer server;

    @Autowired
    public LdapServerConfiguration(EmbeddedLdapServerProperties embeddedLdapServerProperties,
                                   LdapProperties properties, ConfigurableApplicationContext applicationContext,
                                   Environment environment, InMemoryListenerConfig inMemoryListenerConfig,
                                   OperationInterceptor operationInterceptor
    ) {
        this.embeddedLdapServerProperties = embeddedLdapServerProperties;
        this.properties = properties;
        this.applicationContext = applicationContext;
        this.environment = environment;
        this.inMemoryListenerConfig = inMemoryListenerConfig;
        this.operationInterceptor = operationInterceptor;
    }

    @Bean
    @DependsOn("directoryServer")
    @ConditionalOnMissingBean
    public LdapContextSource ldapContextSource() {
        LdapContextSource source = new LdapContextSource();

        if (hasCredentials(this.embeddedLdapServerProperties.getCredential())) {
            source.setUserDn(this.embeddedLdapServerProperties.getCredential().getUsername());
            source.setPassword(this.embeddedLdapServerProperties.getCredential().getPassword());
        }

        source.setUrls(this.properties.determineUrls(this.environment));

        return source;
    }

    @Bean
    @DependsOn("inMemoryListenerConfig")
    public InMemoryDirectoryServer directoryServer() throws Exception {
        String[] baseDn = StringUtils.toStringArray(this.embeddedLdapServerProperties.getBaseDn());

        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDn);

        if (hasCredentials(this.embeddedLdapServerProperties.getCredential())) {
            config.addAdditionalBindCredentials(
                    this.embeddedLdapServerProperties.getCredential().getUsername(),
                    this.embeddedLdapServerProperties.getCredential().getPassword());
        }

        setSchema(config);

        config.setListenerConfigs(this.inMemoryListenerConfig);
        config.addInMemoryOperationInterceptor(this.operationInterceptor);

        this.server = new InMemoryDirectoryServer(config);

        this.importLDIF();

        this.server.startListening();

        setPortProperty(this.applicationContext, this.server.getListenPort());

        return this.server;
    }

    private void importLDIF() {
        String location = this.embeddedLdapServerProperties.getLdif();

        if (StringUtils.hasText(location)) {
            try {
                Resource resource = this.applicationContext.getResource(location);

                if (resource.exists()) {
                    try (InputStream inputStream = resource.getInputStream()) {
                        this.server.importFromLDIF(true, new LDIFReader(inputStream));
                    }
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to load LDIF " + location, ex);
            }
        }
    }

    private void setSchema(InMemoryDirectoryServerConfig config) {
        if (!this.embeddedLdapServerProperties.getValidation().isEnabled()) {
            config.setSchema(null);
            return;
        }

        Resource schema = this.embeddedLdapServerProperties.getValidation().getSchema();

        if (schema != null) {
            setSchema(config, schema);
        }
    }

    private void setSchema(InMemoryDirectoryServerConfig config, Resource resource) {
        try {
            Schema defaultSchema = Schema.getDefaultStandardSchema();
            Schema schema = Schema.getSchema(resource.getInputStream());

            config.setSchema(Schema.mergeSchemas(defaultSchema, schema));
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to load schema " + resource.getDescription(), ex);
        }
    }

    private boolean hasCredentials(Credential credential) {
        return StringUtils.hasText(credential.getUsername())
                && StringUtils.hasText(credential.getPassword());
    }

    private void setPortProperty(ApplicationContext context, int port) {
        if (context instanceof ConfigurableApplicationContext) {
            MutablePropertySources sources = ((ConfigurableApplicationContext) context)
                    .getEnvironment().getPropertySources();

            getLdapPorts(sources).put("local.ldap.port", port);
        }

        if (context.getParent() != null) {
            setPortProperty(context.getParent(), port);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getLdapPorts(MutablePropertySources sources) {
        PropertySource<?> propertySource = sources.get(PROPERTY_SOURCE_NAME);

        if (propertySource == null) {
            propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, new HashMap<>());
            sources.addFirst(propertySource);
        }

        return (Map<String, Object>) propertySource.getSource();
    }

    @PreDestroy
    public void close() {
        if (this.server != null) {
            this.server.shutDown(true);
        }
    }

}
