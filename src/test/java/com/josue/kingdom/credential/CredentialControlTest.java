/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.josue.kingdom.credential;

import com.josue.kingdom.credential.entity.APICredential;
import com.josue.kingdom.credential.entity.CredentialStatus;
import com.josue.kingdom.credential.entity.Manager;
import com.josue.kingdom.credential.entity.ManagerCredential;
import com.josue.kingdom.domain.entity.APIDomainCredential;
import com.josue.kingdom.domain.entity.Domain;
import com.josue.kingdom.domain.entity.DomainPermission;
import com.josue.kingdom.domain.entity.ManagerDomainCredential;
import com.josue.kingdom.invitation.InvitationRepository;
import com.josue.kingdom.invitation.entity.Invitation;
import com.josue.kingdom.invitation.entity.InvitationStatus;
import com.josue.kingdom.rest.ListResource;
import com.josue.kingdom.rest.ex.InvalidResourceArgException;
import com.josue.kingdom.rest.ex.ResourceNotFoundException;
import com.josue.kingdom.rest.ex.RestException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class CredentialControlTest {

    private static final Integer DEFAULT_LIMIT = 100;
    private static final Integer DEFAULT_OFFSET = 0;

    @Mock
    CredentialRepository credentialRepository;

    @Mock
    CredentialService service;

    @Mock
    InvitationRepository invRepository;

    @Spy
    ManagerCredential currentCredential = new ManagerCredential();

    @Spy
    Manager manager = new Manager();

    @InjectMocks
    CredentialControl control = new CredentialControl();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        currentCredential.setManager(manager);
    }

    @Test
    public void testGetManagerByCredential() {
        String credUuid = "123";
        Manager mockManager = Mockito.mock(Manager.class);

        when(credentialRepository.getManagerByCredential(credUuid)).thenReturn(mockManager);
        Manager foundManager = control.getManagerByCredential(credUuid);

        assertEquals(mockManager, foundManager);
    }

    @Test
    public void testGetManagers() {
        long totalCount = 50;

        List<Manager> managers = Mockito.mock(List.class);
        when(credentialRepository.getManagers(DEFAULT_LIMIT, DEFAULT_OFFSET)).thenReturn(managers);
        when(credentialRepository.count(Manager.class)).thenReturn(totalCount);

        ListResource<Manager> foundManagers = control.getManagers(DEFAULT_LIMIT, DEFAULT_OFFSET);
        assertEquals(totalCount, foundManagers.getTotalCount());
        assertEquals(managers, foundManagers.getItems());

    }

    @Test
    public void testPasswordRecovery() throws Exception {
        String uuid = "uuid123";
        Manager manager = new Manager();
        manager.setEmail("josue@email.com");
        manager.setUuid("uuid");
        Manager spyManager = Mockito.spy(manager);
        when(spyManager.getUuid()).thenReturn(uuid);

        String login = "login";
        String password = "pass123";
        ManagerCredential manCred = new ManagerCredential(login, password);
        ManagerCredential spyManCred = Mockito.spy(manCred);

        when(credentialRepository.getManagerByLogin(login)).thenReturn(spyManager);
        when(credentialRepository.getManagerCredentialByManager(spyManager.getUuid())).thenReturn(spyManCred);

        control.passwordRecovery(login);

        verify(spyManCred, times(1)).setPassword(anyString());
        verify(service, times(1)).sendPasswordRecovery(spyManager.getEmail(), spyManCred.getPassword());

    }

    //TODO create specific ExceptionClass
    @Test(expected = RestException.class)
    public void testPasswordRecoveryManagerNotFound() throws Exception {
        String login = "login";
        when(credentialRepository.getManagerByLogin(login)).thenReturn(null);
        control.passwordRecovery(login);
        fail();
    }

    @Test
    public void testCreateCredential() throws RestException {
        String token = "token-123";

        Domain domain = new Domain();
        domain.setUuid("domain-uuid");
        DomainPermission permission = new DomainPermission(1);
        permission.setUuid("permission-uuid");

        Invitation invitation = new Invitation();
        invitation.setDomain(domain);
        invitation.setPermission(permission);
        invitation.setTargetEmail("target@email.com");
        Invitation spyInvitation = Mockito.spy(invitation);

        String login = "login1";
        ManagerCredential manCred = new ManagerCredential(login, "password");
        Manager manager = new Manager();
        manager.setEmail("anotherWrong@email.com");
        manCred.setManager(new Manager());

        ManagerCredential spyManCred = Mockito.spy(manCred);

        when(invRepository.getInvitationByToken(token)).thenReturn(spyInvitation);
        when(credentialRepository.find(Domain.class, spyInvitation.getDomain().getUuid())).thenReturn(domain);
        when(credentialRepository.find(DomainPermission.class, spyInvitation.getPermission().getUuid())).thenReturn(permission);
        when(credentialRepository.getManagerCredentialByLogin(login)).thenReturn(null);

        ManagerCredential createdCredential = control.createCredential(token, spyManCred);
        verify(spyManCred, times(1)).removeNonCreatable();
        verify(credentialRepository, times(1)).create(spyManCred);

        assertEquals(spyInvitation.getTargetEmail(), createdCredential.getManager().getEmail());
        assertEquals(CredentialStatus.ACTIVE, createdCredential.getStatus());

        //Verify if this object is being saved
        ManagerDomainCredential manDomCred = new ManagerDomainCredential();
        manDomCred.setCredential(spyManCred);
        manDomCred.setDomain(domain);
        manDomCred.setPermission(permission);

        verify(credentialRepository, times(1)).create(manDomCred);
    }

    @Test(expected = RestException.class)
    public void testCreateCredentialTokenNotFound() throws RestException {
        String token = "token-123";

        when(invRepository.getInvitationByToken(token)).thenReturn(null);
        control.createCredential(token, null);
        fail();
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testCreateCredentialLoginAlreadyExists() throws RestException {
        String token = "token-123";

        when(invRepository.getInvitationByToken(eq(token))).thenReturn(any(Invitation.class));
        when(credentialRepository.getManagerCredentialByLogin("login")).thenReturn("existentLogin");
        control.createCredential(token, null);
        fail();
    }

    @Test(expected = InvalidResourceArgException.class)
    public void testCreateCredentialCompletedStatus() throws RestException {
        String token = "token-123";

        Invitation invitation = new Invitation();
        invitation.setStatus(InvitationStatus.COMPLETED);

        Invitation spyInvitation = Mockito.spy(invitation);

        when(invRepository.getInvitationByToken(eq(token))).thenReturn(spyInvitation);
        control.createCredential(token, null);
        fail();
    }

    @Test(expected = InvalidResourceArgException.class)
    public void testCreateCredentialExpiredStatus() throws RestException {
        String token = "token-123";

        Invitation invitation = new Invitation();
        invitation.setStatus(InvitationStatus.EXPIRED);

        Invitation spyInvitation = Mockito.spy(invitation);

        when(invRepository.getInvitationByToken(eq(token))).thenReturn(spyInvitation);
        control.createCredential(token, null);
        fail();
    }

    @Test(expected = InvalidResourceArgException.class)
    public void testCreateCredentialFailedStatus() throws RestException {
        String token = "token-123";

        Invitation invitation = new Invitation();
        invitation.setStatus(InvitationStatus.FAILED);

        Invitation spyInvitation = Mockito.spy(invitation);

        when(invRepository.getInvitationByToken(eq(token))).thenReturn(spyInvitation);
        control.createCredential(token, null);
        fail();
    }

    @Test
    public void testGetApiCredentials() {
        APIDomainCredential apiCredMock = mock(APIDomainCredential.class, Mockito.RETURNS_DEEP_STUBS);
        List<APIDomainCredential> realList = Arrays.asList(apiCredMock, apiCredMock, apiCredMock);

        String domainUuid = "uuid-123";

        when(credentialRepository.getAPICredentials(currentCredential.getManager().getUuid(), domainUuid, DEFAULT_LIMIT, DEFAULT_OFFSET)).thenReturn(realList);

        APICredential credMock = mock(APICredential.class);
        when(apiCredMock.getCredential()).thenReturn(credMock);
        when(credMock.getApiKey()).thenReturn(UUID.randomUUID().toString());

        ListResource<APIDomainCredential> apiCredentials = control.getAPICredentials(domainUuid, DEFAULT_LIMIT, DEFAULT_OFFSET);
        verify(credentialRepository, times(1)).getAPICredentials(domainUuid, manager.getUuid(), DEFAULT_LIMIT, DEFAULT_OFFSET);
        assertEquals(realList.size(), apiCredentials.getItems().size());
        for (APIDomainCredential apicred : apiCredentials.getItems()) {
            assertNotNull(apicred);

            ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
            verify(credMock, times(3)).setApiKey(argument.capture());
            assertTrue(argument.getValue().contains("*******"));
        }
    }

    @Test
    public void testGetAPICredentials() {
        APIDomainCredential apiCredMock = mock(APIDomainCredential.class, Mockito.RETURNS_DEEP_STUBS);
        List<APIDomainCredential> realList = Arrays.asList(apiCredMock, apiCredMock, apiCredMock);

        when(credentialRepository.getAPICredentials(currentCredential.getManager().getUuid(), DEFAULT_LIMIT, DEFAULT_OFFSET)).thenReturn(realList);

        APICredential credMock = mock(APICredential.class);
        when(apiCredMock.getCredential()).thenReturn(credMock);
        when(credMock.getApiKey()).thenReturn(UUID.randomUUID().toString());

        ListResource<APIDomainCredential> apiCredentials = control.getAPICredentials(DEFAULT_LIMIT, DEFAULT_OFFSET);
        verify(credentialRepository, times(1)).getAPICredentials(manager.getUuid(), DEFAULT_LIMIT, DEFAULT_OFFSET);
        assertEquals(realList.size(), apiCredentials.getItems().size());
        for (APIDomainCredential apicred : apiCredentials.getItems()) {
            assertNotNull(apicred);

            ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
            verify(credMock, times(3)).setApiKey(argument.capture());
            assertTrue(argument.getValue().contains("*******"));
        }
    }

    @Test
    public void testGetAPICredential() {
        String apiKeyUuid = "apikey-uuid-123";
        String domainUuid = "domain-uuid-123";
        APIDomainCredential apiCredMock = mock(APIDomainCredential.class, Mockito.RETURNS_DEEP_STUBS);

        when(credentialRepository.getAPICredential(currentCredential.getManager().getUuid(), domainUuid, apiKeyUuid)).thenReturn(apiCredMock);

        APICredential credMock = mock(APICredential.class);
        when(apiCredMock.getCredential()).thenReturn(credMock);
        when(credMock.getApiKey()).thenReturn(UUID.randomUUID().toString());

        APIDomainCredential apiCredential = control.getAPICredential(domainUuid, apiKeyUuid);
        verify(credentialRepository, times(1)).getAPICredential(manager.getUuid(), domainUuid, apiKeyUuid);

        assertNotNull(apiCredential);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(credMock, times(1)).setApiKey(argument.capture());
        assertTrue(argument.getValue().contains("*******"));
    }
}
