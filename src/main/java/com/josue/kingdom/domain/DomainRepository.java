/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.josue.kingdom.domain;

import com.josue.kingdom.JpaRepository;
import com.josue.kingdom.domain.entity.Domain;
import com.josue.kingdom.domain.entity.DomainPermission;
import com.josue.kingdom.domain.entity.ManagerDomainCredential;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

/**
 *
 * @author Josue
 */
@ApplicationScoped
public class DomainRepository extends JpaRepository {

    @Transactional(Transactional.TxType.REQUIRED)
    public List<Domain> getDomainsByCredential(String credentialUuid) {
        Query query = em.createQuery("SELECT manCred.domain FROM ManagerDomainCredential manCred WHERE manCred.credential.uuid = :credentialUuid", Domain.class);
        query.setParameter("credentialUuid", credentialUuid);
        List<Domain> resultList = query.getResultList();
        return resultList;
    }

    //Control change some data, we dont want to update it on database
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public List<ManagerDomainCredential> getJoinedDomainsByManager(String managerUuid, Integer limit, Integer offset) {
        Query query = em.createQuery("SELECT manCred FROM ManagerDomainCredential manCred WHERE manCred.credential.manager.uuid = :managerUuid", ManagerDomainCredential.class);
        query.setParameter("managerUuid", managerUuid);
        List<ManagerDomainCredential> resultList = query.getResultList();
        return resultList;
    }

    public Domain getOwnedDomain(String domainUuid, String managerUuid) {
        Query query = em.createQuery("SELECT domain FROM Domain domain WHERE domain.owner.uuid = :managerUuid AND domain.uuid = :domainUuid", Domain.class);
        query.setParameter("domainUuid", domainUuid);
        query.setParameter("managerUuid", managerUuid);
        List<Domain> resultList = query.getResultList();
        return extractSingleResultFromList(resultList);
    }

    public List<Domain> getOwnedDomainsByManager(String managerUuid, Integer limit, Integer offset) {
        Query query = em.createQuery("SELECT domain FROM Domain domain WHERE domain.owner.uuid = :managerUuid", Domain.class);
        query.setParameter("managerUuid", managerUuid);
        query.setMaxResults(limit).setFirstResult(offset);
        List<Domain> domains = query.getResultList();
        return domains;
    }

    public Domain getDomainByName(String domainName) {
        Query query = em.createQuery("SELECT dom FROM Domain dom WHERE dom.name = :domainName", Domain.class);
        query.setParameter("domainName", domainName);
        List<Domain> domains = query.getResultList();
        Domain domain = extractSingleResultFromList(domains);
        return domain;
    }

    public Long countDomainCredentials(String managerUuid) {
        TypedQuery<Long> query = em.createQuery("SELECT COUNT(manCred.uuid) FROM ManagerDomainCredential manCred WHERE manCred.credential.manager.uuid = :credentialUuid", Long.class);
        query.setParameter("credentialUuid", managerUuid);
        Long count = query.getSingleResult();
        return count;
    }

    public Long countOwnedDomains(String managerUuid) {
        TypedQuery<Long> query = em.createQuery("SELECT COUNT(domain.uuid) FROM Domain domain WHERE domain.owner.uuid = :managerUuid", Long.class);
        query.setParameter("managerUuid", managerUuid);
        Long count = query.getSingleResult();
        return count;
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public DomainPermission getDomainPermission(String domainUuid, String permissionName) {
        TypedQuery<DomainPermission> query = em.createQuery("SELECT r FROM DomainPermission r WHERE r.domain.uuid = :domainUuid AND r.name = :permissionName", DomainPermission.class);
        query.setParameter("permissionName", permissionName);
        List<DomainPermission> permissions = query.getResultList();
        return extractSingleResultFromList(permissions);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public List<DomainPermission> getDomainPermissions(String domainUuid) {
        TypedQuery<DomainPermission> query = em.createQuery("SELECT r FROM DomainPermission r WHERE  r.domain.uuid = :domainUuid", DomainPermission.class);
        List<DomainPermission> permissions = query.getResultList();
        return permissions;
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public DomainPermission getDomainPermission(String domainUuid, int permissionLevel) {
        TypedQuery<DomainPermission> query = em.createQuery("SELECT r FROM DomainPermission r WHERE  r.domain.uuid = :domainUuid AND r.level = :permissionLevel", DomainPermission.class);
        query.setParameter("permissionLevel", permissionLevel);
        List<DomainPermission> permissions = query.getResultList();
        return extractSingleResultFromList(permissions);
    }

}
