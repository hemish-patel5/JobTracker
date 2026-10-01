package com.example.jobtracker.service;

import com.example.jobtracker.model.GmailOAuthCredential;
import com.example.jobtracker.repository.GmailOAuthCredentialRepository;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresCredentialDataStoreFactoryTest {

    @Mock
    private GmailOAuthCredentialRepository repository;

    @Test
    void storesLoadsAndDeletesCredential() throws Exception {
        PostgresCredentialDataStoreFactory factory =
                new PostgresCredentialDataStoreFactory(repository);
        DataStore<StoredCredential> dataStore =
                StoredCredential.getDefaultDataStore(factory);
        StoredCredential storedCredential = new StoredCredential()
                .setAccessToken("access-token")
                .setRefreshToken("refresh-token")
                .setExpirationTimeMilliseconds(123456789L);

        dataStore.set("jobtracker-user", storedCredential);

        ArgumentCaptor<GmailOAuthCredential> captor =
                ArgumentCaptor.forClass(GmailOAuthCredential.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCredentialKey())
                .isEqualTo("jobtracker-user");
        assertThat(captor.getValue().getRefreshToken())
                .isEqualTo("refresh-token");

        when(repository.findById("jobtracker-user"))
                .thenReturn(Optional.of(captor.getValue()));

        assertThat(dataStore.get("jobtracker-user"))
                .isEqualTo(storedCredential);

        dataStore.delete("jobtracker-user");
        verify(repository).deleteById("jobtracker-user");
    }
}
