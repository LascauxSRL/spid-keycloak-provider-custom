# Client Scopes vs Client Attributes per SPID

## ⚠️ Differenza Importante

Per SPID abbiamo bisogno di **due tipi diversi di configurazione**:

### 1. **Client Attributes** (Metadata SPID del Service Provider)
- **Scopo**: Descrivere il service provider stesso (organization, contact, entity ID)
- **Dove**: Memorizzati in `client.attributes` 
- **Uso**: Generare metadata SAML SPID
- **Client Scopes**: ❌ **NON possono essere usati per questo**

### 2. **Protocol Mappers** (Attributi Utente nelle Assertion SAML)
- **Scopo**: Mappare attributi utente nelle assertion SAML (fiscalNumber, name, etc.)
- **Dove**: Configurati nei Protocol Mappers (possono essere in Client Scopes)
- **Uso**: Includere attributi utente nelle risposte SAML
- **Client Scopes**: ✅ **Possono essere usati per questo**

## Client Attributes (Metadata SPID) - NON possono usare Client Scopes

Gli attributi client SPID **devono** essere memorizzati direttamente negli `attributes` del client:

```json
{
  "clientId": "soggetto-aggregato-1",
  "attributes": {
    "spid.entityId": "https://soggetto1.example.com/spid",
    "spid.organizationNames": "it|Nome Organizzazione",
    "spid.contact.other.email": "contatto@example.com"
    // ... altri attributi metadata SPID
  }
}
```

**Perché non Client Scopes?**
- Client Scopes non hanno una mappa `attributes` che descrive il client stesso
- Il codice legge da `client.getAttribute("spid.entityId")`, non da Client Scope
- I metadata SPID devono essere specifici per ogni client (ogni soggetto aggregato)

## Protocol Mappers (Attributi Utente) - Possono usare Client Scopes

I Protocol Mappers che mappano attributi utente **possono** essere organizzati in Client Scopes:

### Esempio: Client Scope per Attributi SPID Utente

**Crea Client Scope:**
```
Nome: spid-user-attributes
Protocol: saml
```

**Aggiungi Protocol Mappers:**

1. **Mapper per fiscalNumber:**
   - Mapper Type: `User Attribute`
   - User Attribute: `fiscalNumber`
   - SAML Attribute Name: `fiscalNumber`
   - SAML Attribute NameFormat: `basic`

2. **Mapper per name:**
   - Mapper Type: `User Attribute`
   - User Attribute: `firstName`
   - SAML Attribute Name: `name`
   - SAML Attribute NameFormat: `basic`

3. **Mapper per familyName:**
   - Mapper Type: `User Attribute`
   - User Attribute: `lastName`
   - SAML Attribute Name: `familyName`
   - SAML Attribute NameFormat: `basic`

**Associa Client Scope al client:**
- Vai al client → **Client scopes** tab
- Aggiungi `spid-user-attributes` come **Default Client Scope**

## Architettura Completa

```
Client: soggetto-aggregato-1
├── Attributes (client.attributes) ← METADATA SPID DEL PROVIDER
│   ├── spid.entityId
│   ├── spid.organizationNames
│   ├── spid.contact.other.email
│   └── ...
│
└── Client Scopes ← PROTOCOL MAPPERS PER ATTRIBUTI UTENTE
    └── spid-user-attributes (Default)
        ├── Mapper: fiscalNumber → SAML attribute
        ├── Mapper: name → SAML attribute
        └── Mapper: familyName → SAML attribute
```

## Quando Usare Client Scopes

✅ **Usa Client Scopes per:**
- Condividere protocol mappers tra più client
- Organizzare mappature attributi utente
- Gestire AttributeConsumingService (attributi richiesti nel metadata)

❌ **NON usare Client Scopes per:**
- Entity ID del service provider
- Organization info (nome, display name, URL)
- Contact info (email, phone, company)
- Altre configurazioni metadata del service provider

## Esempio Pratico

### Scenario: Due Soggetti Aggregati

**Client 1: soggetto-aggregato-1**
```json
{
  "clientId": "soggetto-aggregato-1",
  "attributes": {
    // METADATA SPECIFICI DEL CLIENT (non in Client Scope)
    "spid.entityId": "https://soggetto1.example.com/spid",
    "spid.organizationNames": "it|Azienda 1",
    "spid.contact.other.email": "contatto1@example.com"
  },
  "defaultClientScopes": [
    // PROTOCOL MAPPERS CONDIVISI (possono essere in Client Scope)
    "spid-user-attributes"
  ]
}
```

**Client 2: soggetto-aggregato-2**
```json
{
  "clientId": "soggetto-aggregato-2",
  "attributes": {
    // METADATA SPECIFICI DEL CLIENT (diversi dal Client 1)
    "spid.entityId": "https://soggetto2.example.com/spid",
    "spid.organizationNames": "it|Azienda 2",
    "spid.contact.other.email": "contatto2@example.com"
  },
  "defaultClientScopes": [
    // STESSO CLIENT SCOPE (condiviso tra i due client)
    "spid-user-attributes"
  ]
}
```

**Client Scope: spid-user-attributes**
```
Protocol Mappers:
- fiscalNumber → fiscalNumber (basic)
- name → name (basic)
- familyName → familyName (basic)
```

## Riassunto

| Tipo Configurazione | Possono usare Client Scopes? | Dove memorizzare |
|---------------------|------------------------------|------------------|
| **Metadata SPID Provider** (entity ID, organization, contact) | ❌ NO | `client.attributes` |
| **Protocol Mappers** (attributi utente nelle assertion) | ✅ SÌ | Client Scopes (condivisi) o Client (specifici) |
| **AttributeConsumingService** (attributi richiesti) | ⚠️ Parziale | Client Scopes per organizzare, ma index nel `client.attributes` |

## Conclusione

- **Per i metadata SPID del service provider**: Usa **client.attributes** (via REST API)
- **Per i protocol mappers attributi utente**: Puoi usare **Client Scopes** per condividerli tra client

Entrambi gli approcci possono coesistere nella stessa configurazione!
