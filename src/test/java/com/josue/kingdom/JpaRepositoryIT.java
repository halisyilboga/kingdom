/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.josue.kingdom;

import com.josue.kingdom.account.entity.Manager;
import com.josue.kingdom.credential.AuthRepository;
import com.josue.kingdom.domain.entity.Domain;
import com.josue.kingdom.domain.entity.DomainPermission;
import com.josue.kingdom.testutils.ArquillianTestBase;
import com.josue.kingdom.testutils.InstanceHelper;
import com.josue.kingdom.testutils.Logged;
import java.util.List;
import java.util.Random;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Josue
 */
@Logged
@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class JpaRepositoryIT {

    @PersistenceContext
    EntityManager em;

    @Inject
    AuthRepository repo;

    @Deployment
    @TargetsContainer("wildfly-managed")
    public static WebArchive createDeployment() {
        return ArquillianTestBase.createDefaultDeployment();
    }

    private DomainPermission simpleCreate(JpaRepository repo) {
        Manager manager = InstanceHelper.createManager();
        repo.create(manager);
        Domain domain = InstanceHelper.createDomain(manager);
        repo.create(domain);

        DomainPermission permission = InstanceHelper.createRole(domain);
        repo.create(permission);
        assertNotNull(permission.getUuid());
        return permission;
    }

    @Test
    public void testCreate() {

        DomainPermission permission = simpleCreate(repo);

        DomainPermission foundRole = repo.find(DomainPermission.class, permission.getUuid());
        assertNotNull(foundRole);
    }

    @Test
    public void testUpdate() {

        DomainPermission permission = simpleCreate(repo);

        //Fail prone, if the random methos generate an existing Role.level
        //For this test purpose its enough
        permission.setLevel(new Random().nextInt(Integer.MAX_VALUE) + 1);
        DomainPermission editedEntity = repo.update(permission);
        assertEquals(permission.getUuid(), editedEntity.getUuid());
    }

    @Test
    public void testDelete() {

        DomainPermission permission = simpleCreate(repo);

        repo.delete(permission);
        DomainPermission foundRole = repo.find(DomainPermission.class, permission.getUuid());
        assertNull(foundRole);
    }

    @Test
    public void testFind() {

        DomainPermission permission = simpleCreate(repo);
        DomainPermission foundRole = repo.find(DomainPermission.class, permission.getUuid());
        assertEquals(permission, foundRole);
    }

    @Test
    public void testFindAll() {
        Manager manager = InstanceHelper.createManager();
        repo.create(manager);
        Domain domain = InstanceHelper.createDomain(manager);
        repo.create(domain);

        DomainPermission permission1 = InstanceHelper.createRole(domain);
        permission1.setLevel(5);
        repo.create(permission1);
        DomainPermission permission2 = InstanceHelper.createRole(domain);
        permission2.setLevel(3);
        repo.create(permission2);

        List<DomainPermission> foundRoles = repo.findAll(DomainPermission.class);
        assertTrue(foundRoles.size() >= 2);
        assertTrue(foundRoles.contains(permission1));
        assertTrue(foundRoles.contains(permission2));
    }

    @Test
    public void testFindRange() {
        int total = 100;

        for (int i = 0; i < total; i++) {
            simpleCreate(repo);
        }

        //All the tests below are not appropriate, and its not precise too
        // because the test environment pre-load some data
        int limit = 50;
        int offset = 50;
        List<DomainPermission> foundRoles1 = repo.findRange(DomainPermission.class, limit, offset);
        assertTrue(foundRoles1.size() >= 50);

        limit = 100;
        offset = 50;
        List<DomainPermission> foundRoles2 = repo.findRange(DomainPermission.class, limit, offset);
        assertTrue(foundRoles2.size() >= 50);

        limit = 100;
        offset = 0;
        List<DomainPermission> foundRoles3 = repo.findRange(DomainPermission.class, limit, offset);
        assertTrue(foundRoles3.size() >= limit);

        limit = 1;
        offset = 99;
        List<DomainPermission> foundRoles4 = repo.findRange(DomainPermission.class, limit, offset);
        assertTrue(foundRoles4.size() >= limit);

        limit = 10;
        offset = 95;
        List<DomainPermission> foundRoles5 = repo.findRange(DomainPermission.class, limit, offset);
        assertTrue(foundRoles5.size() >= 5);

    }

    @Test
    public void testCount() {
        int total = 15;

        for (int i = 0; i < total; i++) {
            simpleCreate(repo);
        }

        Long count = repo.count(DomainPermission.class);
        assertTrue(count >= total);
    }

    @Test
    public void testExtractSingleResultFromList() {
        int total = 5;

        DomainPermission someRole = null;
        for (int i = 0; i < total; i++) {
            someRole = simpleCreate(repo);
        }
        assertNotNull(someRole);

        TypedQuery<DomainPermission> query = em.createQuery("SELECT ro from DomainPermission ro where ro.uuid = :uuid", DomainPermission.class);
        query.setParameter("uuid", someRole.getUuid());
        List<DomainPermission> resultList = query.getResultList();
        DomainPermission foundRole = repo.extractSingleResultFromList(resultList);
        assertEquals(someRole, foundRole);
    }

}
