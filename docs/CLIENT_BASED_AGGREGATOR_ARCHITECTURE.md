# Architettura Service Aggregator basata su Client Keycloak

## Panoramica

Questa proposta descrive come implementare un Service Aggregator SPID/CIE utilizzando **client Keycloak** per rappresentare ogni soggetto aggregato, invece di utilizzare identity provider separati.

## Architettura proposta

```
Keycloak Realm: "spid-aggregator"
├── Identity Provider SPID (singolo, condiviso)
│   └── Configurazione comune per tutti i soggetti aggregati
│
└── Client SAML (uno per ogni soggetto aggregato)
    ├── Client #1: "soggetto-aggregato-1"
    │   ├── Protocol: SAML
    │   ├── Entity ID: https://soggetto1.example.com/spid
    │   ├── ACS URL: /realms/spid-aggregator/broker/spid/endpoint/clients/soggetto-aggregato-1
    │   ├── Attributes: SPID-specific (organization, contact, etc.)
    │   └── Metadata URL: /realms/spid-aggregator/spid-sp-metadata/clients/soggetto-aggregato-1
    │
    ├── Client #2: "soggetto-aggregato-2"
    │   ├── Protocol: SAML
    │   ├── Entity ID: https://soggetto2.example.com/spid
    │   ├── ACS URL: /realms/spid-aggregator/broker/spid/endpoint/clients/soggetto-aggregato-2
    │   └── ...
    │
    └── Client #N: "soggetto-aggregato-N"
        └── ...
```

## Vantaggi di questa architettura

1. **Separazione logica**: Ogni soggetto aggregato è rappresentato da un client distinto
2. **Metadata indipendenti**: Ogni client può avere il proprio metadata SPID
3. **Configurazione flessibile**: Ogni client può avere entity ID, organization, contact info specifici
4. **Gestione centralizzata**: Un unico realm per tutti i soggetti aggregati
5. **Scalabilità**: Facile aggiungere/rimuovere soggetti aggregati

## Modifiche necessarie al provider

### 1. Estendere SpidSpMetadataResourceProvider

Aggiungere supporto per generare metadata basati su client:

```java
@GET
@Path("/clients/{client_id}")
@Produces("text/xml; charset=utf-8")
public Response getClientMetadata(@PathParam("client_id") String clientId) {
    // Genera metadata per un client specifico
}

@GET
@Produces("text/xml; charset=utf-8")
public Response get() {
    // Mantiene compatibilità: genera metadata aggregato per tutti i client SPID
}
```

### 2. Aggiungere configurazione SPID ai client

Utilizzare attributi client per memorizzare configurazione SPID-specifica:

- `spid.entityId`: Entity ID del soggetto aggregato
- `spid.organizationNames`: Nomi organizzazione
- `spid.organizationDisplayNames`: Nomi visualizzati
- `spid.organizationUrls`: URL organizzazione
- `spid.contact.other.*`: Informazioni contatto OTHER
- `spid.contact.billing.*`: Informazioni contatto BILLING
- `spid.attributeConsumingServiceIndex`: Indice AttributeConsumingService
- `spid.attributeConsumingServiceName`: Nome AttributeConsumingService

### 3. Modificare endpoint per supportare client_id

Gli endpoint già supportano `client_id` nel path (`/clients/{client_id}`), ma bisogna:
- Validare che il client esista e sia abilitato
- Utilizzare la configurazione SPID del client per generare le richieste SAML
- Reindirizzare correttamente dopo l'autenticazione

## Implementazione dettagliata

### Step 1: Creare helper per configurazione client SPID

```java
public class SpidClientConfig {
    private final ClientModel client;
    
    public static SpidClientConfig from(ClientModel client) {
        return new SpidClientConfig(client);
    }
    
    public String getEntityId() {
        return client.getAttribute("spid.entityId");
    }
    
    public String getOrganizationNames() {
        return client.getAttribute("spid.organizationNames");
    }
    
    // ... altri metodi getter per attributi SPID
}
```

### Step 2: Modificare SpidSpMetadataResourceProvider

Aggiungere metodo per generare metadata per un client specifico:

```java
@GET
@Path("/clients/{client_id}")
@Produces("text/xml; charset=utf-8")
public Response getClientMetadata(@PathParam("client_id") String clientId) {
    RealmModel realm = session.getContext().getRealm();
    ClientModel client = session.clients().getClientByClientId(realm, clientId);
    
    if (client == null || !client.isEnabled()) {
        return Response.status(Response.Status.NOT_FOUND).build();
    }
    
    
    SpidClientConfig clientConfig = SpidClientConfig.from(client);
    
    // Genera metadata per questo client specifico
    return generateMetadataForClient(client, clientConfig);
}
```

### Step 3: Modificare SpidSAMLEndpoint per utilizzare configurazione client

Quando viene specificato `client_id`, utilizzare la configurazione SPID del client invece di quella dell'identity provider:

```java
protected Response handleSamlResponse(String samlResponse, String relayState, String clientId) {
    // ...
    
    if (clientId != null) {
        ClientModel client = session.clients().getClientByClientId(realm, clientId);
        if (client != null) {
            SpidClientConfig clientConfig = SpidClientConfig.from(client);
            // Usa clientConfig invece di config per entity ID, organization, etc.
        }
    }
    
    // ...
}
```

### Step 4: Aggiornare generazione metadata aggregato

Il metodo `get()` esistente può essere modificato per:
- Opzione A: Generare metadata aggregato che include tutti i client SPID
- Opzione B: Generare metadata per il primo client (compatibilità retroattiva)

## Configurazione client esempio

Per configurare un client come soggetto aggregato SPID:

### Via Admin Console (manuale)
1. Creare un nuovo client
2. Impostare Protocol: `saml`
3. Aggiungere attributi client:
   ```
   spid.entityId = https://soggetto1.example.com/spid
   spid.organizationNames = it|Nome Organizzazione
   spid.organizationDisplayNames = it|Nome Visualizzato
   spid.organizationUrls = it|https://soggetto1.example.com
   spid.contact.other.company = Nome Azienda
   spid.contact.other.email = contatto@soggetto1.example.com
   spid.contact.other.phone = +39 123 456 7890
   spid.contact.other.isSpPrivate = false
   spid.contact.other.ipaCode = it/abc/xyz
   spid.attributeConsumingServiceIndex = 1
   spid.attributeConsumingServiceName = it|Servizi Online
   ```

### Via REST API (automatizzato)

```json
{
  "clientId": "soggetto-aggregato-1",
  "protocol": "saml",
  "enabled": true,
  "attributes": {
    "spid.entityId": "https://soggetto1.example.com/spid",
    "spid.organizationNames": "it|Nome Organizzazione",
    "spid.organizationDisplayNames": "it|Nome Visualizzato",
    "spid.organizationUrls": "it|https://soggetto1.example.com",
    "spid.contact.other.company": "Nome Azienda",
    "spid.contact.other.email": "contatto@soggetto1.example.com",
    "spid.contact.other.phone": "+39 123 456 7890",
    "spid.contact.other.isSpPrivate": "false",
    "spid.contact.other.ipaCode": "it/abc/xyz",
    "spid.attributeConsumingServiceIndex": "1",
    "spid.attributeConsumingServiceName": "it|Servizi Online"
  }
}
```

## Endpoint URL

### Metadata per client specifico
```
GET /realms/{realm}/spid-sp-metadata/clients/{client_id}
```

### Metadata aggregato (tutti i client)
```
GET /realms/{realm}/spid-sp-metadata
```

### Endpoint SAML per client specifico
```
POST /realms/{realm}/broker/{idp_alias}/endpoint/clients/{client_id}
GET  /realms/{realm}/broker/{idp_alias}/endpoint/clients/{client_id}
```

## Compatibilità retroattiva

Per mantenere compatibilità con l'implementazione attuale:

1. Se non viene specificato `client_id`, utilizzare il comportamento attuale (identity provider)
2. Se viene specificato `client_id`, utilizzare la configurazione del client
3. Il metadata aggregato può includere sia identity provider che client

## Considerazioni per AgID

Quando si comunica ad AgID l'elenco dei soggetti aggregati:

1. **Entity ID univoco**: Ogni client deve avere un entity ID univoco
2. **Metadata separati**: Ogni client può avere il proprio metadata URL
3. **Configurazione completa**: Ogni client deve avere organization, contact info, etc. completi
4. **Documentazione**: Ogni client deve essere documentato separatamente ad AgID

## Implementazione completata

✅ **Completato:**
1. ✅ Implementata classe `SpidClientConfig` helper
2. ✅ Modificato `SpidSpMetadataResourceProvider` per supportare client
3. ✅ Aggiunto endpoint `/clients/{client_id}` per metadata specifici
4. ✅ Creato adapter per compatibilità con codice esistente

## Utilizzo

### 1. Configurare un client come soggetto aggregato

#### Via REST API (Consigliato)

**Nota:** La tab "Attributes" per i client non è sempre visibile nell'Admin Console di Keycloak. Si consiglia di configurare gli attributi tramite REST API.

**Passo 1: Ottenere un token di accesso Admin**

```bash
# Sostituisci con le tue credenziali
export KEYCLOAK_URL="https://keycloak.example.com"
export REALM="master"
export ADMIN_USER="admin"
export ADMIN_PASSWORD="password"

# Ottieni token
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

echo "Token: $TOKEN"
```

**Passo 2: Creare il client con attributi SPID**

```bash
export TARGET_REALM="spid-aggregator"
export CLIENT_ID="soggetto-aggregato-1"

curl -X POST "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/clients" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "soggetto-aggregato-1",
    "protocol": "saml",
    "enabled": true,
    "attributes": {
      "saml.assertion.signature": "true",
      "saml.server.signature": "true",
      "spid.entityId": "https://soggetto1.example.com/spid",
      "spid.organizationNames": "it|Nome Organizzazione",
      "spid.organizationDisplayNames": "it|Nome Visualizzato",
      "spid.organizationUrls": "it|https://soggetto1.example.com",
      "spid.contact.other.company": "Nome Azienda",
      "spid.contact.other.email": "contatto@soggetto1.example.com",
      "spid.contact.other.phone": "+39 123 456 7890",
      "spid.contact.other.isSpPrivate": "false",
      "spid.contact.other.ipaCode": "it/abc/xyz",
      "spid.contact.other.vatNumber": "IT12345678901",
      "spid.contact.other.fiscalCode": "ABCDEF12G34H567I",
      "spid.attributeConsumingServiceIndex": "1",
      "spid.attributeConsumingServiceName": "it|Servizi Online"
    }
  }'
```

**Passo 3: Aggiornare attributi di un client esistente**

Se il client esiste già, puoi aggiornare solo gli attributi:

```bash
# Prima, ottieni l'UUID interno del client
CLIENT_UUID=$(curl -s "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/clients?clientId=${CLIENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" | jq -r '.[0].id')

echo "Client UUID: $CLIENT_UUID"

# Aggiorna gli attributi SPID
curl -X PUT "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/clients/${CLIENT_UUID}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "attributes": {
      "spid.entityId": "https://soggetto1.example.com/spid",
      "spid.organizationNames": "it|Nome Organizzazione",
      "spid.organizationDisplayNames": "it|Nome Visualizzato",
      "spid.organizationUrls": "it|https://soggetto1.example.com",
      "spid.contact.other.company": "Nome Azienda",
      "spid.contact.other.email": "contatto@soggetto1.example.com",
      "spid.contact.other.phone": "+39 123 456 7890",
      "spid.contact.other.isSpPrivate": "false",
      "spid.contact.other.ipaCode": "it/abc/xyz",
      "spid.attributeConsumingServiceIndex": "1",
      "spid.attributeConsumingServiceName": "it|Servizi Online"
    }
  }'
```

#### Via Admin Console (Limitato)

Se preferisci usare l'Admin Console:

1. Vai a **Clients** → **Create client** o seleziona un client esistente
2. Imposta:
   - **Client ID**: `soggetto-aggregato-1`
   - **Client Protocol**: `saml`
   - **Enabled**: `ON`
3. **Nota importante**: La tab "Attributes" potrebbe non essere visibile per i client in alcune versioni di Keycloak. In tal caso, usa la REST API come sopra.

4. Se la sezione "Attributes" è visibile (può essere in "Advanced" o "Advanced settings"), puoi aggiungere manualmente gli attributi:

   - Aggiungi ogni attributo come coppia chiave-valore
   - Chiave: `spid.entityId`, Valore: `https://soggetto1.example.com/spid`
   - Chiave: `spid.organizationNames`, Valore: `it|Nome Organizzazione`
   - E così via per tutti gli attributi necessari

#### Script di esempio completo

```bash
curl -X POST "https://keycloak.example.com/admin/realms/spid-aggregator/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "soggetto-aggregato-1",
    "protocol": "saml",
    "enabled": true,
    "attributes": {
      "spid.entityId": "https://soggetto1.example.com/spid",
      "spid.organizationNames": "it|Nome Organizzazione",
      "spid.organizationDisplayNames": "it|Nome Visualizzato",
      "spid.organizationUrls": "it|https://soggetto1.example.com",
      "spid.contact.other.company": "Nome Azienda",
      "spid.contact.other.email": "contatto@soggetto1.example.com",
      "spid.contact.other.phone": "+39 123 456 7890",
      "spid.contact.other.isSpPrivate": "false",
      "spid.contact.other.ipaCode": "it/abc/xyz",
      "spid.attributeConsumingServiceIndex": "1",
      "spid.attributeConsumingServiceName": "it|Servizi Online"
    }
  }'
```

### 2. Ottenere metadata per un client specifico

```bash
GET /realms/{realm}/spid-sp-metadata/clients/{client_id}
```

Esempio:
```bash
curl https://keycloak.example.com/realms/spid-aggregator/spid-sp-metadata/clients/soggetto-aggregato-1
```

### 3. Endpoint SAML per client specifico

Gli endpoint SAML supportano già il parametro `client_id`:

```
POST /realms/{realm}/broker/{idp_alias}/endpoint/clients/{client_id}
GET  /realms/{realm}/broker/{idp_alias}/endpoint/clients/{client_id}
```

## Attributi client supportati

### Entity ID e Organization
- `spid.entityId` - Entity ID univoco del soggetto aggregato (obbligatorio)
- `spid.organizationNames` - Nomi organizzazione (formato: `locale|Nome`)
- `spid.organizationDisplayNames` - Nomi visualizzati (formato: `locale|Nome`)
- `spid.organizationUrls` - URL organizzazione (formato: `locale|URL`)

### Contact OTHER
- `spid.contact.other.isSpPrivate` - `true`/`false`
- `spid.contact.other.ipaCode` - Codice IPA
- `spid.contact.other.vatNumber` - Partita IVA
- `spid.contact.other.fiscalCode` - Codice fiscale
- `spid.contact.other.company` - Nome azienda
- `spid.contact.other.phone` - Telefono
- `spid.contact.other.email` - Email

### Contact BILLING
- `spid.contact.billing.company` - Nome azienda
- `spid.contact.billing.phone` - Telefono
- `spid.contact.billing.email` - Email
- `spid.contact.billing.registryName` - Nome registro
- `spid.contact.billing.siteAddress` - Indirizzo
- `spid.contact.billing.siteNumber` - Numero civico
- `spid.contact.billing.siteCity` - Città
- `spid.contact.billing.siteZipCode` - CAP
- `spid.contact.billing.siteProvince` - Provincia
- `spid.contact.billing.siteCountry` - Codice paese

### AttributeConsumingService
- `spid.attributeConsumingServiceIndex` - Indice (default: 1)
- `spid.attributeConsumingServiceName` - Nome servizio (formato: `locale|Nome`)

## Compatibilità retroattiva

L'implementazione mantiene piena compatibilità con l'approccio esistente:

- ✅ Il metodo `get()` esistente continua a funzionare (genera metadata aggregato per identity provider)
- ✅ Gli endpoint SAML esistenti continuano a funzionare
- ✅ La configurazione esistente non viene modificata

## Prossimi passi (opzionali)

1. ⏳ Aggiornare `SpidSAMLEndpoint` per utilizzare configurazione client quando `client_id` è specificato
2. ⏳ Aggiungere validazione configurazione client (verificare che tutti gli attributi obbligatori siano presenti)
3. ⏳ Creare script/strumenti per configurazione automatica client
4. ⏳ Documentare processo di onboarding nuovi soggetti aggregati
5. ⏳ Aggiungere supporto per generare metadata aggregato che include tutti i client SPID

## Note importanti

- **Entity ID obbligatorio**: Ogni client deve avere `spid.entityId` configurato
- **Protocol SAML**: Il client deve usare il protocol `saml`
- **Identity Provider**: Deve esistere almeno un identity provider SPID abilitato nel realm
- **Chiavi di firma**: Vengono utilizzate le chiavi del realm (condivise tra tutti i client)
- **Metadata signing**: Utilizza la configurazione dell'identity provider per la firma

## Esempio completo

Vedi il file `CLIENT_BASED_AGGREGATOR_ARCHITECTURE.md` per un esempio completo di configurazione e utilizzo.
