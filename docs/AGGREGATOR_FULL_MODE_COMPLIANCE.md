# Verifica Conformità Provider per Modalità "Full" Soggetti Aggregatori

## Riferimenti Normativi

- **Procedura tecnica**: Documento "COME DIVENTARE SOGGETTI AGGREGATORI DI SERVIZI PUBBLICI"
- **Avvisi SPID**: N°6, N°19, N°22, N°23
- **Regole tecniche SPID**: Conformità SAML 2.0

## Requisiti per Modalità "Full"

Nella modalità "full", il Soggetto Aggregatore:
- Esegue l'autenticazione tramite la propria infrastruttura
- Riceve da AgID un certificato di sigillo elettronico
- Deve generare metadata conformi per ogni soggetto aggregato

## Verifica Elementi Metadata Richiesti

### ✅ 1. EntityDescriptor

**Requisito**: Ogni soggetto aggregato deve avere un proprio `EntityDescriptor` con `entityID` univoco.

**Implementazione**: ✅ **CONFORME**
- Il metodo `generateMetadataForClient()` genera un `EntityDescriptor` per ogni client
- L'`entityID` è configurabile tramite attributo client `spid.entityId`
- Se non configurato, viene generato automaticamente: `{baseUri}/realms/{realm}/clients/{clientId}`

**Codice**: `SpidSpMetadataResourceProvider.java:696-702`

### ✅ 2. SPSSODescriptor

**Requisito**: Deve contenere tutti gli elementi richiesti (AuthnRequestsSigned, WantAssertionsSigned, etc.)

**Implementazione**: ✅ **CONFORME**
- Viene creato tramite `SPMetadataDescriptor.buildSPDescriptor()`
- Configurazione ereditata dall'identity provider configurato
- Supporta binding POST e REDIRECT

**Codice**: `SpidSpMetadataResourceProvider.java:735-738`

### ✅ 3. AssertionConsumerService

**Requisito**: Deve includere endpoint per tutti gli identity provider SPID configurati nel realm.

**Implementazione**: ✅ **CONFORME**
- Il metodo genera endpoint per tutti gli identity provider SPID abilitati
- Ogni endpoint include il path specifico del client: `/clients/{clientId}`
- Supporta binding POST e REDIRECT in base alla configurazione

**Codice**: `SpidSpMetadataResourceProvider.java:807-825`

**Esempio generato**:
```xml
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" 
    Location="http://localhost:10001/realms/spid-cie/broker/demo-spid/endpoint/clients/soggetto-aggregato-3" 
    index="0" isDefault="true"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" 
    Location="http://localhost:10001/realms/spid-cie/broker/infocert/endpoint/clients/soggetto-aggregato-3" 
    index="1"/>
```

### ✅ 4. SingleLogoutService

**Requisito**: Deve includere endpoint per tutti gli identity provider SPID configurati.

**Implementazione**: ✅ **CONFORME**
- Genera endpoint per tutti gli identity provider SPID abilitati
- Ogni endpoint include il path specifico del client

**Codice**: `SpidSpMetadataResourceProvider.java:805-825`

### ✅ 5. AttributeConsumingService

**Requisito**: Deve contenere tutti i possibili `AttributeConsumingService` (index 0-30, 99-100) con i relativi `RequestedAttribute`.

**Implementazione**: ✅ **CONFORME**
- Include automaticamente tutti i 33 `AttributeConsumingService` predefiniti
- Ogni servizio contiene i `RequestedAttribute` corretti
- Supporta configurazione personalizzata via JSON (`spid.attributeConsumingServices`)
- Il primo servizio (index 0) ha `isDefault="true"`

**Codice**: `SpidSpMetadataResourceProvider.java:740-776`, `DefaultAttributeConsumingServices.java`

**Esempio generato**:
```xml
<md:AttributeConsumingService index="0" isDefault="true">
    <md:ServiceName xml:lang="it">default</md:ServiceName>
    <md:RequestedAttribute Name="familyName" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:basic"/>
    ...
</md:AttributeConsumingService>
```

### ✅ 6. Organization

**Requisito**: Deve contenere informazioni organizzative del soggetto aggregato.

**Implementazione**: ✅ **CONFORME**
- Configurabile tramite attributi client:
  - `spid.organizationNames`
  - `spid.organizationDisplayNames`
  - `spid.organizationUrls`
- Formato: `locale|Valore` (es. `it|Nome Organizzazione`)

**Codice**: `SpidClientConfig.java:74-84`, `SpidSpMetadataResourceProvider.java:789`

### ✅ 7. ContactPerson - Aggregator

**Requisito**: Deve contenere un `ContactPerson` con:
- `contactType="other"`
- `spid:entityType="spid:aggregator"`
- `spid:PublicServicesFullAggregator` (pubblico) o `spid:PrivateServicesFullAggregator` (privato)
- `spid:IPACode` o `spid:VATNumber` a seconda del tipo
- Dati organizzazione (Company, Email, Phone)

**Implementazione**: ✅ **CONFORME**
- Classe `SpidAggregatorContactType` crea il ContactPerson aggregator
- Usa dati dall'identity provider config (organizzazione principale)
- Aggiunge qualifier corretto in base a `spid.aggregated.isPrivate`
- Attributo `spid:entityType` aggiunto via post-processing XML

**Codice**: `SpidAggregatorContactType.java`, `SpidSpMetadataResourceProvider.java:791-793`

**Esempio generato**:
```xml
<md:ContactPerson contactType="other" spid:entityType="spid:aggregator">
    <md:Extensions>
        <spid:PublicServicesFullAggregator/>
        <spid:VATNumber>IT01805480512</spid:VATNumber>
    </md:Extensions>
    <md:Company>Lascaux S.r.l.</md:Company>
    <md:EmailAddress>info@lascaux.it</md:EmailAddress>
    <md:TelephoneNumber>+390575250983</md:TelephoneNumber>
</md:ContactPerson>
```

### ✅ 8. ContactPerson - Aggregated

**Requisito**: Deve contenere un `ContactPerson` con:
- `contactType="other"`
- `spid:entityType="spid:aggregated"`
- `spid:Public` o `spid:Private` in base al tipo
- `spid:IPACode` se disponibile
- Dati del soggetto aggregato (Company)

**Implementazione**: ✅ **CONFORME**
- Classe `SpidAggregatedContactType` crea il ContactPerson aggregated
- Usa dati dal client config (soggetto aggregato)
- Aggiunge qualifier `spid:Public` o `spid:Private` in base a `spid.aggregated.isPrivate`
- Attributo `spid:entityType` aggiunto via post-processing XML

**Codice**: `SpidAggregatedContactType.java`, `SpidSpMetadataResourceProvider.java:791-793`

**Esempio generato**:
```xml
<md:ContactPerson contactType="other" spid:entityType="spid:aggregated">
    <md:Extensions>
        <spid:Private xmlns:spid="https://spid.gov.it/saml-extensions"/>
        <spid:IPACode>c_a851</spid:IPACode>
    </md:Extensions>
    <md:Company>Comune di Bibbiena</md:Company>
</md:ContactPerson>
```

### ✅ 9. KeyDescriptor

**Requisito**: Deve contenere chiavi di firma e crittografia.

**Implementazione**: ✅ **CONFORME**
- Recupera chiavi dal realm Keycloak
- Aggiunge `KeyDescriptor` per firma e crittografia
- Usa certificati X.509 validi

**Codice**: `SpidSpMetadataResourceProvider.java:712-732`

### ✅ 10. NameIDFormat

**Requisito**: Deve specificare il formato NameID supportato.

**Implementazione**: ✅ **CONFORME**
- Configurabile tramite identity provider config
- Default: `urn:oasis:names:tc:SAML:2.0:nameid-format:transient`

**Codice**: `SpidSpMetadataResourceProvider.java:693`

### ✅ 11. AttributeConsumingServiceIndex nelle Request

**Requisito**: Nelle `AuthnRequest`, deve essere possibile specificare l'index dell'`AttributeConsumingService` desiderato.

**Implementazione**: ✅ **CONFORME**
- Il metodo `performLogin()` in `SpidIdentityProvider` legge `attributeConsumingServiceIndex` dal client
- Se non configurato nel client, usa quello dell'identity provider
- L'index viene incluso nella `AuthnRequest` SAML

**Codice**: `SpidIdentityProvider.java:157-165`

## Configurazione Client Richiesta

Per configurare un client come soggetto aggregato in modalità "full", sono necessari i seguenti attributi:

### Attributi Obbligatori

```json
{
  "spid.entityId": "https://soggetto1.example.com/spid",
  "spid.organizationNames": "it|Nome Organizzazione",
  "spid.organizationDisplayNames": "it|Nome Visualizzato",
  "spid.organizationUrls": "it|https://soggetto1.example.com"
}
```

### Attributi per ContactPerson Aggregator

I dati dell'aggregator vengono presi dalla configurazione dell'identity provider (organizzazione principale).

### Attributi per ContactPerson Aggregated

```json
{
  "spid.aggregated.company": "Comune di Bibbiena",
  "spid.aggregated.ipaCode": "c_a851",
  "spid.aggregated.isPrivate": "false"
}
```

### Attributi Opzionali

```json
{
  "spid.attributeConsumingServiceIndex": "0",
  "spid.attributeConsumingServiceName": "default",
  "spid.requestedAttributes": "familyName,name,spidCode,fiscalNumber",
  "spid.attributeConsumingServices": "[{...}]"
}
```

## Endpoint Metadata

Il metadata per un client specifico è disponibile all'endpoint:

```
GET /realms/{realm}/spid-sp-metadata/clients/{client_id}
```

**Esempio**:
```
GET /realms/spid-cie/spid-sp-metadata/clients/soggetto-aggregato-3
```

## Endpoint SAML

Gli endpoint SAML per un client specifico sono:

```
POST /realms/{realm}/broker/{idp_alias}/endpoint/clients/{client_id}
GET  /realms/{realm}/broker/{idp_alias}/endpoint/clients/{client_id}
```

## Verifica Conformità Avviso SPID N°19

Secondo l'Avviso SPID N°19, il metadata per soggetti aggregatori deve contenere:

### ✅ Elementi Richiesti

1. **EntityDescriptor con entityID univoco**: ✅ Implementato
2. **SPSSODescriptor con AuthnRequestsSigned="true"**: ✅ Implementato
3. **AssertionConsumerService per tutti gli IdP**: ✅ Implementato
4. **SingleLogoutService per tutti gli IdP**: ✅ Implementato
5. **AttributeConsumingService con tutti gli index (0-30, 99-100)**: ✅ Implementato
6. **Organization con dati del soggetto aggregato**: ✅ Implementato
7. **ContactPerson aggregator con spid:entityType**: ✅ Implementato
8. **ContactPerson aggregated con spid:entityType**: ✅ Implementato
9. **KeyDescriptor per firma e crittografia**: ✅ Implementato

### ⚠️ Elementi da Verificare Esternamente

1. **Certificato di sigillo elettronico**: 
   - Il metadata deve essere sigillato con certificato rilasciato da AgID
   - Questo è un processo esterno al provider (post-generazione)

2. **Validazione con SPID Validator**:
   - Il metadata generato deve essere validato con SPID Validator
   - Deve superare tutti i test del Quality Assessment Document

3. **Registrazione metadata su idp.spid.gov.it**:
   - Per test online, il metadata deve essere registrato su idp.spid.gov.it
   - Questo è un processo esterno

## Checklist Conformità

### Elementi Metadata
- [x] EntityDescriptor con entityID univoco
- [x] SPSSODescriptor con attributi corretti
- [x] AssertionConsumerService per tutti gli IdP
- [x] SingleLogoutService per tutti gli IdP
- [x] AttributeConsumingService completi (0-30, 99-100)
- [x] RequestedAttribute con NameFormat corretto
- [x] Organization con dati localizzati
- [x] ContactPerson aggregator con spid:entityType
- [x] ContactPerson aggregated con spid:entityType
- [x] KeyDescriptor per firma e crittografia
- [x] NameIDFormat configurato

### Configurazione
- [x] Attributi client per entity ID
- [x] Attributi client per organization
- [x] Attributi client per aggregated entity
- [x] Attributi client per AttributeConsumingService
- [x] Supporto per configurazione privato/pubblico

### Funzionalità
- [x] Generazione metadata per client specifico
- [x] Endpoint metadata dedicato
- [x] Endpoint SAML con path client-specific
- [x] Supporto per tutti gli identity provider SPID
- [x] Post-processing XML per attributi custom

## Conclusione

Il provider è **PREDISPOSTO** per generare metadata conformi per la procedura in modalità "full" per soggetti aggregatori.

Tutti gli elementi richiesti dalle regole tecniche e dagli Avvisi SPID sono implementati e configurabili tramite attributi client.

### Prossimi Passi

1. **Configurare i client** con gli attributi necessari
2. **Generare i metadata** per ogni soggetto aggregato
3. **Validare i metadata** con SPID Validator
4. **Sigillare i metadata** con certificato AgID (processo esterno)
5. **Inviare ad AgID** secondo la procedura documentata

## Note Importanti

1. **Certificato di sigillo elettronico**: Il provider genera il metadata XML, ma la sigillatura con certificato AgID deve essere fatta esternamente (post-generazione).

2. **Validazione**: Prima di inviare ad AgID, validare sempre il metadata con SPID Validator.

3. **EntityID**: L'entityID deve essere univoco e conforme all'Avviso SPID N°19. Non modificare l'entityID dopo la registrazione iniziale.

4. **Metadata URL**: Il metadata deve essere disponibile su URL HTTPS del dominio dell'aggregatore.
