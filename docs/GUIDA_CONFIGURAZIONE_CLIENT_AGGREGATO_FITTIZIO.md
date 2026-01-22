# Guida Configurazione Client Aggregato Fittizio per Collaudo

## Panoramica

Questa guida fornisce esempi JSON completi per configurare un client Keycloak come soggetto aggregato fittizio, necessario per il collaudo tecnico secondo la procedura AGID per diventare soggetto aggregatore in modalità "full".

## File Disponibili

1. **`ESEMPIO_CLIENT_AGGREGATO_FITTIZIO.json`** - Configurazione base completa
2. **`ESEMPIO_CLIENT_AGGREGATO_FITTIZIO_PUBBLICO.json`** - Ente pubblico aggregato
3. **`ESEMPIO_CLIENT_AGGREGATO_FITTIZIO_PRIVATO.json`** - Azienda privata aggregata
4. **`ESEMPIO_CLIENT_AGGREGATO_FITTIZIO_COMPLETO.json`** - Configurazione completa con tutti gli attributi
5. **`SCRIPT_CREAZIONE_CLIENT_AGGREGATO_FITTIZIO.sh`** - Script bash per creazione automatica

## Configurazione Base (Minima)

Per un collaudo base, sono necessari almeno questi attributi:

```json
{
  "clientId": "soggetto-aggregato-fittizio",
  "enabled": true,
  "protocol": "saml",
  "attributes": {
    "spid.entityId": "https://soggetto-fittizio.example.com/spid",
    "spid.organizationNames": "it|Soggetto Aggregato Fittizio S.r.l.",
    "spid.organizationDisplayNames": "it|Soggetto Fittizio",
    "spid.organizationUrls": "it|https://soggetto-fittizio.example.com",
    "spid.aggregated.company": "Comune Fittizio",
    "spid.aggregated.ipaCode": "c_fittizio",
    "spid.aggregated.isPrivate": "false"
  }
}
```

## Attributi Obbligatori

### Entity ID e Organization
- `spid.entityId` - **OBBLIGATORIO** - Entity ID univoco del soggetto aggregato
- `spid.organizationNames` - **OBBLIGATORIO** - Nome organizzazione (formato: `locale|Nome`)
- `spid.organizationDisplayNames` - **OBBLIGATORIO** - Nome visualizzato (formato: `locale|Nome`)
- `spid.organizationUrls` - **OBBLIGATORIO** - URL organizzazione (formato: `locale|URL`)

### Aggregated Entity (per ContactPerson aggregated)
- `spid.aggregated.company` - **OBBLIGATORIO** - Nome del soggetto aggregato
- `spid.aggregated.ipaCode` - **Raccomandato** - Codice IPA (per enti pubblici)
- `spid.aggregated.isPrivate` - **OBBLIGATORIO** - `"true"` per privati, `"false"` per pubblici

## Attributi Opzionali ma Raccomandati

### ContactPerson OTHER (organizzazione principale)
- `spid.contact.other.isSpPrivate` - Tipo SP (privato/pubblico)
- `spid.contact.other.ipaCode` - Codice IPA (per pubblici)
- `spid.contact.other.vatNumber` - Partita IVA (per privati)
- `spid.contact.other.fiscalCode` - Codice fiscale (per privati)
- `spid.contact.other.company` - Nome azienda
- `spid.contact.other.email` - Email contatto
- `spid.contact.other.phone` - Telefono contatto

### ContactPerson BILLING
- `spid.contact.billing.company` - Nome azienda fatturazione
- `spid.contact.billing.email` - Email fatturazione
- `spid.contact.billing.phone` - Telefono fatturazione
- `spid.contact.billing.registryName` - Nome registro
- `spid.contact.billing.siteAddress` - Indirizzo sede
- `spid.contact.billing.siteNumber` - Numero civico
- `spid.contact.billing.siteCity` - Città
- `spid.contact.billing.siteZipCode` - CAP
- `spid.contact.billing.siteProvince` - Provincia
- `spid.contact.billing.siteCountry` - Nazione

### AttributeConsumingService
- `spid.attributeConsumingServiceIndex` - Index del servizio (default: `"0"`)
- `spid.attributeConsumingServiceName` - Nome del servizio (default: `"default"`)
- `spid.requestedAttributes` - Lista attributi richiesti (comma-separated)
- `spid.attributeConsumingServices` - JSON array per configurazione avanzata (vedi sotto)

## Configurazione Avanzata AttributeConsumingService

Se vuoi configurare manualmente gli `AttributeConsumingService`, puoi usare `spid.attributeConsumingServices` con un JSON array:

```json
{
  "spid.attributeConsumingServices": "[{\"index\":0,\"serviceName\":\"default\",\"serviceDescription\":\"\",\"requestedAttributes\":[\"familyName\",\"name\",\"spidCode\",\"fiscalNumber\"]},{\"index\":1,\"serviceName\":\"default,profile\",\"serviceDescription\":\"\",\"requestedAttributes\":[\"placeOfBirth\",\"gender\",\"idCard\",\"familyName\",\"name\",\"spidCode\",\"dateOfBirth\",\"countyOfBirth\",\"fiscalNumber\",\"expirationDate\"]}]"
}
```

**Nota**: Se non configurato, il provider include automaticamente tutti i 33 `AttributeConsumingService` predefiniti (index 0-30, 99-100).

## Utilizzo

### Opzione 1: Via REST API

```bash
# 1. Ottieni token admin
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

# 2. Crea client
curl -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d @docs/ESEMPIO_CLIENT_AGGREGATO_FITTIZIO.json
```

### Opzione 2: Script Automatico

```bash
chmod +x docs/SCRIPT_CREAZIONE_CLIENT_AGGREGATO_FITTIZIO.sh
./docs/SCRIPT_CREAZIONE_CLIENT_AGGREGATO_FITTIZIO.sh
```

### Opzione 3: Import Realm JSON

Puoi includere il client nella configurazione del realm e importarlo:

```json
{
  "realm": "spid-cie",
  "clients": [
    {
      "clientId": "soggetto-aggregato-fittizio",
      ...
    }
  ]
}
```

## Verifica Metadata Generato

Dopo aver creato il client, verifica il metadata generato:

```bash
# URL metadata
curl "http://localhost:8080/realms/spid-cie/spid-sp-metadata/clients/soggetto-aggregato-fittizio"

# Con formattazione XML
curl -s "http://localhost:8080/realms/spid-cie/spid-sp-metadata/clients/soggetto-aggregato-fittizio" | xmllint --format -

# Salva su file
curl -s "http://localhost:8080/realms/spid-cie/spid-sp-metadata/clients/soggetto-aggregato-fittizio" > metadata-fittizio.xml
```

## Validazione con SPID Validator

1. **Rendi disponibile il metadata su URL HTTPS**:
   ```bash
   # Esempio: disponibile su https://soggetto-fittizio.example.com/metadata.xml
   ```

2. **Aggiungi SPID Validator agli IdP**:
   - Metadata SPID Validator: `https://validator.spid.gov.it/metadata.xml`
   - Aggiungi questo IdP nella configurazione del realm

3. **Valida il metadata**:
   - Usa SPID Validator per verificare la conformità
   - Verifica tutti i test del Quality Assessment Document

## Elementi da Verificare nel Metadata

Il metadata generato deve contenere:

- ✅ `EntityDescriptor` con `entityID` univoco
- ✅ `SPSSODescriptor` con `AuthnRequestsSigned="true"`
- ✅ `AssertionConsumerService` per tutti gli IdP configurati
- ✅ `SingleLogoutService` per tutti gli IdP configurati
- ✅ `AttributeConsumingService` completi (index 0-30, 99-100)
- ✅ `Organization` con dati localizzati
- ✅ `ContactPerson` con `spid:entityType="spid:aggregator"`
- ✅ `ContactPerson` con `spid:entityType="spid:aggregated"`
- ✅ `KeyDescriptor` per firma e crittografia

## Note Importanti

1. **EntityID**: Deve essere univoco e conforme all'Avviso SPID N°19. Non modificarlo dopo la registrazione iniziale.

2. **Certificato di sigillo elettronico**: Il metadata generato deve essere sigillato con certificato rilasciato da AgID (processo esterno post-generazione).

3. **Validazione**: Prima di inviare ad AgID, validare sempre con SPID Validator.

4. **Metadata URL**: Il metadata deve essere disponibile su URL HTTPS del dominio dell'aggregatore.

5. **Formato Organization**: I valori per `organizationNames`, `organizationDisplayNames`, `organizationUrls` devono essere nel formato `locale|Valore` (es. `it|Nome Organizzazione`).

## Esempi per Diversi Scenari

### Ente Pubblico Aggregato
Vedi: `ESEMPIO_CLIENT_AGGREGATO_FITTIZIO_PUBBLICO.json`

Caratteristiche:
- `spid.contact.other.isSpPrivate`: `"false"`
- `spid.contact.other.ipaCode`: Codice IPA
- `spid.aggregated.isPrivate`: `"false"`

### Azienda Privata Aggregata
Vedi: `ESEMPIO_CLIENT_AGGREGATO_FITTIZIO_PRIVATO.json`

Caratteristiche:
- `spid.contact.other.isSpPrivate`: `"true"`
- `spid.contact.other.vatNumber`: Partita IVA
- `spid.contact.other.fiscalCode`: Codice fiscale
- `spid.aggregated.isPrivate`: `"true"`

## Troubleshooting

### Il metadata non viene generato
- Verifica che il client abbia `protocol: "saml"`
- Verifica che il client abbia `spid.entityId` configurato
- Controlla i log di Keycloak per errori

### ContactPerson aggregated mancante
- Verifica che `spid.aggregated.company` sia configurato
- Verifica che almeno `spid.aggregated.company` o `spid.aggregated.ipaCode` siano presenti

### AttributeConsumingService incompleti
- Se non configurato `spid.attributeConsumingServices`, vengono inclusi automaticamente tutti i 33 servizi predefiniti
- Verifica che il provider sia stato deployato correttamente

## Riferimenti

- [Procedura AGID per Soggetti Aggregatori](file://procedura_soggetti_aggregatori_0_0.pdf)
- [Avviso SPID N°19](https://www.agid.gov.it/it/agenzia/stampa-e-comunicazione/notizie/2020/03/16/avviso-spid-n19)
- [SPID Validator](https://validator.spid.gov.it)
- [Documentazione Conformità](docs/AGGREGATOR_FULL_MODE_COMPLIANCE.md)
