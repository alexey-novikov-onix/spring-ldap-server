package com.onix.ldap.repository;

import com.unboundid.ldap.sdk.Entry;
import org.springframework.stereotype.Service;

@Service
public class EntryRepository {

    public Entry findEntry(String dn) {
        Entry entry = new Entry(dn);
        entry.addAttribute("objectclass", "top");
        entry.addAttribute("objectclass", "person");
        entry.addAttribute("objectclass", "organizationalPerson");
        entry.addAttribute("objectclass", "inetOrgPerson");
        entry.addAttribute("cn", "Jahn Dae");
        entry.addAttribute("sn", "Jahn");
        entry.addAttribute("uid", "jahn");
        entry.addAttribute("password", "secret");
        entry.addAttribute("telephone", "11111111");
        return entry;
    }

}
