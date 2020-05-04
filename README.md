## README


#### Install

1.Enable auth using client certs
plugins/opendistro_security/securityconfig/config.yml
``` 
clientcert_auth_domain:
        http_enabled: true
```

2.Change elasticsearch.yml

```
#opendistro_security.authcz.admin_dn:
#  - CN=kirk,OU=client,O=client,L=test, C=de
opendistro_security.authcz.rest_impersonation_user.kirk:
  - ?*

```

#### Setup java truststore and keystore.

1. Truststore. CD to Elasticsearch/config directory.

```
keytool -import -trustcacerts -alias client -file  root-ca.pem -keystore odfe_keystore
passwd: keystore
```

1. Keystore setup:
```
openssl pkcs12 -export -in kirk.pem -inkey kirk-key.pem -chain -CAfile root-ca.pem -name "client" -out client.p12
```

####  Add demo data

1. Install sampleone and sampletwo plugins and start Elasticsearch and Kibana.
1. Login using admin and create the following:

    1. Users: CHIP, DALE
    1. Roles : Operations , Gov Clearance
    1. Mapping Role to Index : 
        Operations → iad_data (index)
        Gov Clearance → pdt_data (index)
    1. Mapping Role to User :
        CHIP → Operations
        DALE → Operations and Gov Clearance

#### Testing


