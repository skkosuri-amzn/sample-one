package com.amazon.opendistroforelasticsearch.sample.client;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.amazon.opendistroforelasticsearch.sample.actions.SearchErrorsAction;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;


/**
 *  Shows different ways to authenticate with SecurityPlugin using low-level REST client.
 *
 *  This class provides different way to create RestClient object.
 */
public class ElasticClient {

    private static final Logger log = LogManager.getLogger(SearchErrorsAction.class);

    private static final String LOCALHOST = "localhost";
    private static final int PORT = 9200;
    private static final String SCHEME = "https";

    //fixme: change to relative paths
    private static final String trustStore = "/home/skkosuri/elastic/1.2.0/opendistroforelasticsearch-1.2.0/config/odfe_keystore";
    private static final String keyStore = "/home/skkosuri/elastic/1.2.0/opendistroforelasticsearch-1.2.0/config/client.p12";



    /**
     * Creates RestClient using selfsigned certs, can be used to connect - only with valid auth headers or token.
     * @return
     * @throws IOException
     */
    public static RestClient createWithSelfSigned() throws IOException {
        final SSLContext sslContext;
        try {
            SSLContextBuilder sslbuilder = new SSLContextBuilder();
            sslbuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslContext = sslbuilder.build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new IOException(e);
        }
        return RestClient.builder(new HttpHost(LOCALHOST, PORT, SCHEME))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setSSLContext(sslContext);
                    }
                }).build();
    }

    /**
     * Creates RestClient using selfsigned certs and user/passwd creds.
     * Can be used for backgroup jobs, but needs passwd in the code. Not recommended.
     * @return
     * @throws IOException
     */
    public static RestClient createWithSelfSignedAndCreds() throws IOException {

        final SSLContext sslContext;
        try {
            SSLContextBuilder sslbuilder = new SSLContextBuilder();
            sslbuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslContext = sslbuilder.build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new IOException(e);
        }

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("admin", "admin"));

        return RestClient.builder(new HttpHost(LOCALHOST, PORT, SCHEME))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        httpClientBuilder.setSSLContext(sslContext);
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                }).build();
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public static RestClient createWithCertsAndCreds() throws IOException {
        Path trustStorePath = Paths.get(trustStore);
        final SSLContext sslContext;
        try {
            SSLContextBuilder sslBuilder = null;
            KeyStore truststore = KeyStore.getInstance("jks");
            try (InputStream is = Files.newInputStream(trustStorePath)) {
                truststore.load(is, "keystore".toCharArray());
            }
            sslBuilder = SSLContexts.custom().loadTrustMaterial(truststore, null);
            sslContext = sslBuilder.build();
        } catch (KeyManagementException | CertificateException | NoSuchAlgorithmException
                | KeyStoreException | IOException e) {
            throw new IOException(e);
        }

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("admin", "admin"));

        return RestClient.builder(new HttpHost(LOCALHOST, PORT, SCHEME))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        httpClientBuilder.setSSLContext(sslContext);
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                }).build();
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public static RestClient createWithCertsAndClientCerts() throws IOException {
        Path trustStorePath = Paths.get(trustStore);
        Path keyStorePath = Paths.get(keyStore);

        final SSLContext sslContext;
        try {
            KeyStore truststore = KeyStore.getInstance("jks");
            try (InputStream is = Files.newInputStream(trustStorePath)) {
                truststore.load(is, "keystore".toCharArray());
            }

            KeyStore keyStore = KeyStore.getInstance("jks");
            try (InputStream is = Files.newInputStream(keyStorePath)) {
                keyStore.load(is, "keystore".toCharArray());
            }

            sslContext = SSLContexts.custom()
                    .loadTrustMaterial(truststore, null)
                    .loadKeyMaterial(keyStore,"keystore".toCharArray())
                    .build();
        } catch (KeyManagementException | CertificateException | NoSuchAlgorithmException
                | KeyStoreException | UnrecoverableKeyException e) {
            throw new IOException(e);
        }

        return RestClient.builder(new HttpHost(LOCALHOST, PORT, SCHEME))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setSSLContext(sslContext);
                    }
                }).build();
    }
}
