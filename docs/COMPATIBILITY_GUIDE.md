# Guida alla Compatibilità - Service Provider Base

## ✅ Compatibilità Retroattiva Garantita

L'implementazione **mantiene piena compatibilità** con l'approccio originale basato su identity provider. Tutti gli endpoint e le funzionalità esistenti continuano a funzionare esattamente come prima.

## Endpoint Disponibili

### 1. Metadata - Approccio Originale (Identity Provider)

**Endpoint originale (sempre disponibile):**
```
GET /realms/{realm}/spid-sp-metadata
```

**Comportamento:**
- Genera metadata aggregato per **tutti gli identity provider SPID** configurati nel realm
- Utilizza la configurazione dell'identity provider (come prima)
- **Nessuna modifica** rispetto all'implementazione originale
- **Completamente funzionante** e invariato

**Esempio:**
```bash
curl https://keycloak.example.com/realms/spid-realm/spid-sp-metadata
```

### 2. Metadata - Nuovo Approccio (Client)

**Nuovo endpoint (aggiuntivo):**
```
GET /realms/{realm}/spid-sp-metadata/clients/{client_id}
```

**Comportamento:**
- Genera metadata **specifico per un client** configurato come soggetto aggregato
- Utilizza la configurazione SPID memorizzata negli attributi del client
- **Non interferisce** con l'endpoint originale

**Esempio:**
```bash
curl https://keycloak.example.com/realms/spid-realm/spid-sp-metadata/clients/soggetto-aggregato-1
```

### 3. Endpoint SAML - Approccio Originale

**Endpoint originali (sempre disponibili):**
```
GET  /realms/{realm}/broker/{idp_alias}/endpoint
POST /realms/{realm}/broker/{idp_alias}/endpoint
```

**Comportamento:**
- Funzionano **esattamente come prima**
- Utilizzano la configurazione dell'identity provider
- **Nessuna modifica** rispetto all'implementazione originale
- **Completamente funzionanti** e invariati

**Esempio:**
```bash
# Redirect binding
GET /realms/spid-realm/broker/spid/endpoint?SAMLRequest=...

# Post binding
POST /realms/spid-realm/broker/spid/endpoint
Content-Type: application/x-www-form-urlencoded
SAMLResponse=...
```

### 4. Endpoint SAML - Nuovo Approccio (Client)

**Nuovi endpoint (aggiuntivi):**
```
GET  /realms/{realm}/broker/{idp_alias}/endpoint/clients/{client_id}
POST /realms/{realm}/broker/{idp_alias}/endpoint/clients/{client_id}
```

**Comportamento:**
- Supportano il parametro `client_id` per identificare il soggetto aggregato
- Utilizzano la configurazione SPID del client quando disponibile
- **Non interferiscono** con gli endpoint originali

**Esempio:**
```bash
# Redirect binding con client_id
GET /realms/spid-realm/broker/spid/endpoint/clients/soggetto-aggregato-1?SAMLRequest=...

# Post binding con client_id
POST /realms/spid-realm/broker/spid/endpoint/clients/soggetto-aggregato-1
Content-Type: application/x-www-form-urlencoded
SAMLResponse=...
```

## Tabella Comparativa

| Funzionalità | Approccio Originale (Identity Provider) | Nuovo Approccio (Client) |
|--------------|----------------------------------------|--------------------------|
| **Metadata URL** | `/realms/{realm}/spid-sp-metadata` | `/realms/{realm}/spid-sp-metadata/clients/{client_id}` |
| **Configurazione** | Identity Provider Config | Client Attributes |
| **Entity ID** | Da Identity Provider | Da Client Attribute `spid.entityId` |
| **Organization** | Da Identity Provider | Da Client Attributes |
| **Contact Info** | Da Identity Provider | Da Client Attributes |
| **Endpoint SAML** | `/broker/{idp}/endpoint` | `/broker/{idp}/endpoint/clients/{client_id}` |
| **Stato** | ✅ **Sempre disponibile** | ✅ **Aggiuntivo** |

## Scenari d'Uso

### Scenario 1: Service Provider Tradizionale

**Configurazione:**
- Un identity provider SPID configurato nel realm
- Nessun client configurato come soggetto aggregato

**Utilizzo:**
- ✅ Usa l'endpoint originale: `/realms/{realm}/spid-sp-metadata`
- ✅ Usa gli endpoint SAML originali: `/broker/{idp}/endpoint`
- ✅ **Funziona esattamente come prima**

### Scenario 2: Service Aggregator con Client

**Configurazione:**
- Un identity provider SPID configurato nel realm (condiviso)
- Uno o più client configurati come soggetti aggregati

**Utilizzo:**
- ✅ Usa il nuovo endpoint: `/realms/{realm}/spid-sp-metadata/clients/{client_id}`
- ✅ Usa i nuovi endpoint SAML: `/broker/{idp}/endpoint/clients/{client_id}`
- ✅ **Ogni client ha il proprio metadata e configurazione**

### Scenario 3: Configurazione Mista

**Configurazione:**
- Un identity provider SPID configurato nel realm
- Alcuni client configurati come soggetti aggregati

**Utilizzo:**
- ✅ Endpoint originale continua a funzionare (genera metadata aggregato per identity provider)
- ✅ Nuovi endpoint funzionano per i client configurati
- ✅ **Entrambi gli approcci coesistono**

## Verifica Compatibilità

### Test 1: Endpoint Metadata Originale

```bash
# Dovrebbe funzionare esattamente come prima
curl -v https://keycloak.example.com/realms/spid-realm/spid-sp-metadata

# Risposta attesa: XML metadata aggregato per identity provider
```

### Test 2: Endpoint SAML Originale

```bash
# Dovrebbe funzionare esattamente come prima
curl -v "https://keycloak.example.com/realms/spid-realm/broker/spid/endpoint?SAMLRequest=..."

# Risposta attesa: Comportamento identico a prima
```

### Test 3: Nuovo Endpoint Metadata Client

```bash
# Nuovo endpoint per client specifico
curl -v https://keycloak.example.com/realms/spid-realm/spid-sp-metadata/clients/soggetto-aggregato-1

# Risposta attesa: XML metadata specifico per il client
```

### Test 4: Nuovo Endpoint SAML Client

```bash
# Nuovo endpoint SAML con client_id
curl -v "https://keycloak.example.com/realms/spid-realm/broker/spid/endpoint/clients/soggetto-aggregato-1?SAMLRequest=..."

# Risposta attesa: Comportamento con configurazione client
```

## Migrazione Graduale

Se vuoi migrare da identity provider a client:

1. **Fase 1**: Mantieni la configurazione esistente (identity provider)
   - ✅ Tutto continua a funzionare
   - ✅ Nessuna interruzione del servizio

2. **Fase 2**: Configura i client come soggetti aggregati
   - ✅ Aggiungi attributi SPID ai client
   - ✅ Testa i nuovi endpoint

3. **Fase 3**: Migra gradualmente i servizi
   - ✅ Usa i nuovi endpoint per i nuovi servizi
   - ✅ Mantieni gli endpoint originali per i servizi esistenti

4. **Fase 4**: (Opzionale) Depreca l'approccio identity provider
   - ⚠️ Solo se necessario
   - ⚠️ Dopo aver migrato tutti i servizi

## Note Importanti

1. **Nessuna Breaking Change**: Tutti gli endpoint esistenti continuano a funzionare
2. **Aggiunte, Non Modifiche**: I nuovi endpoint sono aggiuntivi, non sostituiscono quelli esistenti
3. **Configurazione Indipendente**: Identity provider e client possono coesistere
4. **Backward Compatible**: Il codice esistente continua a funzionare senza modifiche

## Conclusione

✅ **Il service provider base è sempre disponibile e funzionante**

- Gli endpoint originali non sono stati modificati
- La logica originale è intatta
- Le nuove funzionalità sono aggiuntive, non sostitutive
- Puoi utilizzare entrambi gli approcci contemporaneamente

**Non c'è bisogno di modificare nulla nella configurazione esistente!**
