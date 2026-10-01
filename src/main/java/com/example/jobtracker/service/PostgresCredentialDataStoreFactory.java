package com.example.jobtracker.service;

import com.example.jobtracker.model.GmailOAuthCredential;
import com.example.jobtracker.repository.GmailOAuthCredentialRepository;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.AbstractDataStore;
import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.DataStore;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class PostgresCredentialDataStoreFactory
        extends AbstractDataStoreFactory {

    private final GmailOAuthCredentialRepository repository;

    public PostgresCredentialDataStoreFactory(
            GmailOAuthCredentialRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <V extends Serializable> DataStore<V> createDataStore(
            String id
    ) throws IOException {
        if (!StoredCredential.DEFAULT_DATA_STORE_ID.equals(id)) {
            throw new IOException("Unsupported credential data store: " + id);
        }

        return (DataStore<V>) new CredentialDataStore(
                this,
                id,
                repository
        );
    }

    private static final class CredentialDataStore
            extends AbstractDataStore<StoredCredential> {

        private final GmailOAuthCredentialRepository repository;

        private CredentialDataStore(
                PostgresCredentialDataStoreFactory factory,
                String id,
                GmailOAuthCredentialRepository repository
        ) {
            super(factory, id);
            this.repository = repository;
        }

        @Override
        public Set<String> keySet() {
            Set<String> keys = new LinkedHashSet<>();
            repository.findAll().forEach(
                    credential -> keys.add(credential.getCredentialKey())
            );
            return keys;
        }

        @Override
        public Collection<StoredCredential> values() {
            return repository.findAll().stream()
                    .map(CredentialDataStore::toStoredCredential)
                    .toList();
        }

        @Override
        public StoredCredential get(String key) {
            if (key == null) {
                return null;
            }

            return repository.findById(key)
                    .map(CredentialDataStore::toStoredCredential)
                    .orElse(null);
        }

        @Override
        public DataStore<StoredCredential> set(
                String key,
                StoredCredential value
        ) {
            Objects.requireNonNull(key, "Credential key is required");
            Objects.requireNonNull(value, "Credential value is required");

            repository.save(new GmailOAuthCredential(
                    key,
                    value.getAccessToken(),
                    value.getRefreshToken(),
                    value.getExpirationTimeMilliseconds()
            ));
            return this;
        }

        @Override
        public DataStore<StoredCredential> clear() {
            repository.deleteAll();
            return this;
        }

        @Override
        public DataStore<StoredCredential> delete(String key) {
            if (key != null) {
                repository.deleteById(key);
            }
            return this;
        }

        @Override
        public int size() {
            return Math.toIntExact(repository.count());
        }

        @Override
        public boolean containsKey(String key) {
            return key != null && repository.existsById(key);
        }

        @Override
        public boolean containsValue(StoredCredential value) {
            return value != null && values().contains(value);
        }

        private static StoredCredential toStoredCredential(
                GmailOAuthCredential credential
        ) {
            return new StoredCredential()
                    .setAccessToken(credential.getAccessToken())
                    .setRefreshToken(credential.getRefreshToken())
                    .setExpirationTimeMilliseconds(
                            credential.getExpirationTimeMilliseconds()
                    );
        }
    }
}
