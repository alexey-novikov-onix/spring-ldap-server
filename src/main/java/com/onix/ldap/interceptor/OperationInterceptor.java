package com.onix.ldap.interceptor;

import com.onix.ldap.repository.EntryRepository;
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchEntry;
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchRequest;
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult;
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OperationInterceptor extends InMemoryOperationInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryOperationInterceptor.class);

    private final EntryRepository dataRepository;

    @Autowired
    public OperationInterceptor(EntryRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    public void processSearchEntry(final InMemoryInterceptedSearchEntry entry) {
        logger.debug("processSearchEntry [{}]", entry.getRequest().getBaseDN());
    }

    public void processSearchRequest(final InMemoryInterceptedSearchRequest request) throws LDAPException {
        logger.debug("processSearchRequest [{}]", request.getRequest().getBaseDN());

        if (request.getRequest().getBaseDN().equals("uid=jahn,ou=people,dc=ldap,dc=com")) {
            try {
                request.sendSearchEntry(this.dataRepository.findEntry("uid=jahn,ou=people,dc=ldap,dc=com"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            throw new LDAPException(ResultCode.SUCCESS);
        }
    }

    public void processSearchResult(final InMemoryInterceptedSearchResult result) {
        logger.debug("processSearchResult [{}]", result.getRequest().getBaseDN());
    }

}