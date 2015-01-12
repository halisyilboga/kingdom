package com.josue.credential.manager.business.credential;

import com.josue.credential.manager.auth.credential.APICredential;
import com.josue.credential.manager.auth.credential.ManagerCredential;
import com.josue.credential.manager.auth.domain.APIDomainCredential;
import com.josue.credential.manager.auth.domain.Domain;
import com.josue.credential.manager.auth.manager.Manager;
import com.josue.credential.manager.auth.role.Role;
import com.josue.credential.manager.testutils.ArquillianTestBase;
import com.josue.credential.manager.testutils.InstanceHelper;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Josue
 */
@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class CredentialRepositoryIT {

    private static final Integer DEFAULT_LIMIT = 100;
    private static final Integer DEFAULT_OFFSET = 0;

    @Deployment
    @TargetsContainer("wildfly-managed")
    public static WebArchive createDeployment() {
        return ArquillianTestBase.createDefaultDeployment();
    }

    private static final Logger LOG = Logger.getLogger(CredentialRepositoryIT.class.getName());

    @PersistenceContext
    EntityManager em;

    @Inject
    CredentialRepository repository;

    @Test
    public void testGetApiCredentialsByManager() {
        APIDomainCredential domainCredential = InstanceHelper.createFullAPIDomainCredential(repository);
        Manager manager = domainCredential.getCredential().getManager();
        List<APIDomainCredential> foundDomainCredentials = repository.getApiCredentials(manager.getUuid(), DEFAULT_LIMIT, DEFAULT_OFFSET);
        assertEquals(1, foundDomainCredentials.size());
        assertEquals(domainCredential, foundDomainCredentials.get(0));

    }

    @Test
    public void testGetApiCredentialsByManagerDomain() {
        APIDomainCredential domainCredential = InstanceHelper.createFullAPIDomainCredential(repository);

        Manager manager = domainCredential.getCredential().getManager();
        Domain domain1 = domainCredential.getDomain();

        Domain domain2 = InstanceHelper.createDomain(manager);
        repository.create(domain2);

        Role simpleRole = domainCredential.getRole();

        APICredential apiCred1 = InstanceHelper.createAPICredential(manager);
        repository.create(apiCred1);
        APICredential apiCred2 = InstanceHelper.createAPICredential(manager);
        repository.create(apiCred2);
        APICredential apiCred3 = InstanceHelper.createAPICredential(manager);
        repository.create(apiCred3);

        APIDomainCredential apiDomainCred1 = InstanceHelper.createAPIDomainCredential(domain2, apiCred1, simpleRole);
        repository.create(apiDomainCred1);
        APIDomainCredential apiDomainCred2 = InstanceHelper.createAPIDomainCredential(domain2, apiCred2, simpleRole);
        repository.create(apiDomainCred2);
        APIDomainCredential apiDomainCred3 = InstanceHelper.createAPIDomainCredential(domain2, apiCred3, simpleRole);
        repository.create(apiDomainCred3);

        List<APIDomainCredential> foundDomainCredentials = repository.getApiCredentials(manager.getUuid(), domain1.getUuid(), DEFAULT_LIMIT, DEFAULT_OFFSET);
        assertEquals(1, foundDomainCredentials.size());
        assertEquals(domainCredential, foundDomainCredentials.get(0));

        //APIs credentials for Domain2
        List<APIDomainCredential> foundDomainCredentialsForDomain2 = repository.getApiCredentials(manager.getUuid(), domain2.getUuid(), DEFAULT_LIMIT, DEFAULT_OFFSET);
        assertEquals(3, foundDomainCredentialsForDomain2.size());
    }

    @Test
    public void testGetApiCredential() {
        APIDomainCredential apiDomCred = InstanceHelper.createFullAPIDomainCredential(repository);

        Manager mannager = apiDomCred.getCredential().getManager();
        APICredential apiCredential = apiDomCred.getCredential();
        Domain domain = apiDomCred.getDomain();

        APIDomainCredential foundapiDomCred = repository.getApiCredential(mannager.getUuid(), domain.getUuid(), apiCredential.getUuid());
        assertNotNull(foundapiDomCred);
        assertEquals(apiDomCred, foundapiDomCred);
    }

    @Test
    public void testGetManagerByCredential() {
        Manager manager = InstanceHelper.createManager();
        repository.create(manager);

        ManagerCredential credential = InstanceHelper.createManagerCredential(manager);
        repository.create(credential);

        Manager foundManager = repository.getManager(credential.getUuid());
        assertNotNull(foundManager);
        assertEquals(manager, foundManager);
    }

    @Test
    public void testCountAPICredential() {
        APIDomainCredential domainCredential = InstanceHelper.createFullAPIDomainCredential(repository);

        Manager manager = domainCredential.getCredential().getManager();
        Domain domain1 = domainCredential.getDomain();

        Role simpleRole = domainCredential.getRole();

        APICredential apiCredential1 = InstanceHelper.createAPICredential(manager);
        repository.create(apiCredential1);
        APICredential apiCredential2 = InstanceHelper.createAPICredential(manager);
        repository.create(apiCredential2);

        APIDomainCredential apiDomainCredential2 = InstanceHelper.createAPIDomainCredential(domain1, apiCredential1, simpleRole);
        repository.create(apiDomainCredential2);
        APIDomainCredential apiDomainCredential3 = InstanceHelper.createAPIDomainCredential(domain1, apiCredential2, simpleRole);
        repository.create(apiDomainCredential3);

        long countByManager = repository.countAPICredential(manager.getUuid());
        assertEquals(3, countByManager);

        long count = repository.countAPICredential(domain1.getUuid(), manager.getUuid());
        assertEquals(3, count);
    }

}
