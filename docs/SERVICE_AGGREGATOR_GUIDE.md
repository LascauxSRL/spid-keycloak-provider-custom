# Guida per diventare Service Aggregator SPID/CIE

## Panoramica

Questa guida descrive i passi necessari per diventare un **Soggetto Aggregatore** per SPID e CIE, partendo dalla condizione di essere già accreditati come Service Provider.

## Differenza tra Service Provider e Service Aggregator

- **Service Provider (SP)**: Fornisce servizi agli utenti finali utilizzando SPID/CIE per l'autenticazione
- **Service Aggregator (SA)**: Fornisce servizi di aggregazione, permettendo ad altri SP (soggetti aggregati) di utilizzare la propria infrastruttura SPID/CIE

## Modalità operative

Esistono due modalità operative per un Service Aggregator:

1. **Modalità "light"**: 
   - Sei aggregatore ma usi l'infrastruttura del soggetto aggregato
   - Installi soluzioni da te fornite presso il soggetto aggregato

2. **Modalità "full"**: 
   - Usi la tua infrastruttura per autenticazione/servizio del soggetto aggregato
   - Il soggetto aggregato si appoggia completamente alla tua infrastruttura

## Requisiti preliminari

- ✅ Essere già accreditati come Service Provider SPID/CIE
- ✅ Aver superato la procedura tecnica come SP
- ✅ Soddisfare i requisiti del D.lgs. 82/2005 (art. 2, comma 2) se privati
- ✅ Avere modelli organizzativi conformi

## Passi da seguire

### Fase 1: Preparazione tecnica

#### 1.1 Verifica conformità tecnica
- [ ] Verificare che la piattaforma sia conforme alle **regole tecniche SPID** aggiornate
- [ ] Verificare interoperabilità con CIE se prevista
- [ ] Verificare che i metadata generati siano conformi alle specifiche per aggregatori

#### 1.2 Definizione infrastruttura
- [ ] Definire se operare in modalità "light" o "full"
- [ ] Predisporre l'infrastruttura per autenticazione SPID/CIE che servirai ai soggetti aggregati
- [ ] Preparare metadata corretti e standard JSON che descrivano i soggetti aggregati

#### 1.3 Configurazione Keycloak
Il provider attuale (`spid-keycloak-provider`) genera metadata aggregati per tutti gli identity provider SPID nello stesso realm. 

**Considerazioni tecniche:**
- Il provider genera un unico metadata che aggrega tutti gli endpoint SPID configurati nel realm
- Ogni identity provider SPID configurato nel realm rappresenta potenzialmente un soggetto aggregato
- Il metadata include tutti gli `AssertionConsumerService` e `SingleLogoutService` endpoints per tutti i provider configurati

**Configurazione consigliata:**
- Utilizzare un realm dedicato per l'aggregazione
- Configurare un identity provider SPID per ogni soggetto aggregato
- Ogni identity provider deve avere:
  - Entity ID univoco
  - Organization info specifica (se richiesta)
  - Contact info specifica (OTHER e BILLING)
  - AttributeConsumingService configurato

### Fase 2: Preparazione amministrativa

#### 2.1 Definizione referenti
- [ ] Definire referenti tecnici e amministrativi
- [ ] Preparare documentazione organizzativa
- [ ] Predisporre modelli organizzativi conformi

#### 2.2 Documentazione privacy e sicurezza
- [ ] Preparare documentazione sulla protezione dei dati personali
- [ ] Definire misure di sicurezza
- [ ] Preparare documentazione su livelli di servizio e responsabilità

### Fase 3: Adesione formale

#### 3.1 Compilazione moduli
- [ ] Compilare il modulo di adesione come aggregatore
  - Distinguere se pubblico o privato
  - Includere tutti i dati richiesti
- [ ] Firmare digitalmente il modulo (PAdES)

#### 3.2 Convenzione con AgID
- [ ] Firmare la convenzione specifica per aggregatori:
  - **Pubblici**: Determina n. 80/2018
  - **Privati**: Determina n. 71/2022
- [ ] Inviare documentazione via PEC ad AgID
- [ ] Attendere controfirma da AgID

### Fase 4: Collaudo tecnico

#### 4.1 Validazione metadata
- [ ] Fornire ad AgID i metadata dei soggetti aggregati
- [ ] Verificare che i metadata siano conformi alle specifiche
- [ ] Risolvere eventuali problemi segnalati da AgID

#### 4.2 Test di autenticazione
- [ ] Eseguire test di autenticazione con gli Identity Provider SPID
- [ ] Verificare il flusso completo di autenticazione
- [ ] Testare il Single Logout (SLO)

#### 4.3 Compliance
- [ ] Verificare conformità alle regole tecniche vigenti
- [ ] Verificare conformità alle misure di sicurezza
- [ ] Completare la fase di collaudo con AgID

### Fase 5: Messa in produzione

#### 5.1 Pubblicazione metadata
- [ ] Pubblicare correttamente i metadata dei soggetti aggregati
- [ ] Configurare aggiornamenti periodici dei metadata
- [ ] Verificare accessibilità dei metadata

#### 5.2 Gestione soggetti aggregati
- [ ] Comunicare ad AgID l'elenco aggiornato dei soggetti aggregati
- [ ] Mantenere aggiornata la lista dei servizi abilitati
- [ ] Gestire onboarding di nuovi soggetti aggregati

#### 5.3 Monitoraggio e manutenzione
- [ ] Implementare monitoraggio dell'infrastruttura
- [ ] Definire procedure di manutenzione
- [ ] Preparare piani di disaster recovery

## Obblighi e responsabilità

### Obblighi verso AgID
- Comunicare l'elenco aggiornato dei soggetti aggregati
- Comunicare tutti i servizi che abilitano SPID
- Mantenere conformità alle regole tecniche vigenti
- Sottoporsi a controlli di vigilanza da parte di AgID

### Obblighi di sicurezza
- Protezione dei dati personali
- Conformità alle misure di sicurezza
- Gestione degli incidenti di sicurezza
- Audit e logging

### Obblighi verso i soggetti aggregati
- Fornire livelli di servizio definiti
- Gestire le richieste di supporto
- Mantenere disponibilità dell'infrastruttura
- Comunicare eventuali interruzioni o modifiche

## Considerazioni tecniche per Keycloak

### Architettura consigliata

```
Keycloak Realm: "spid-aggregator"
├── Identity Provider SPID #1 (Soggetto Aggregato 1)
│   ├── Entity ID: https://soggetto1.example.com/spid
│   ├── Organization: Info del soggetto aggregato 1
│   └── Contact: Info del soggetto aggregato 1
├── Identity Provider SPID #2 (Soggetto Aggregato 2)
│   ├── Entity ID: https://soggetto2.example.com/spid
│   ├── Organization: Info del soggetto aggregato 2
│   └── Contact: Info del soggetto aggregato 2
└── ...
```

### Metadata generati

Il provider genera un unico metadata XML che include:
- Un unico `EntityDescriptor` con l'entity ID del primo provider (in ordine alfabetico)
- Un `SPSSODescriptor` che contiene:
  - Tutti gli `AssertionConsumerService` endpoints per tutti i provider
  - Tutti i `SingleLogoutService` endpoints per tutti i provider
  - Le chiavi di firma e crittografia del realm
  - Gli `AttributeConsumingService` configurati

### Limitazioni attuali

Il provider attuale:
- ✅ Supporta la generazione di metadata aggregati
- ✅ Include tutti gli endpoint per tutti i provider configurati
- ⚠️ Utilizza l'entity ID del primo provider (potrebbe richiedere un entity ID dedicato per l'aggregatore)
- ⚠️ Utilizza le informazioni di Organization/Contact del primo provider (potrebbero servire info specifiche per l'aggregatore)

### Possibili modifiche necessarie

Se AgID richiede metadata con caratteristiche specifiche per aggregatori, potrebbe essere necessario:

1. **Entity ID dedicato per aggregatore**: Modificare la logica per utilizzare un entity ID specifico per l'aggregatore invece di quello del primo provider

2. **Organization/Contact dell'aggregatore**: Aggiungere configurazione specifica per le informazioni dell'aggregatore (diverse da quelle dei soggetti aggregati)

3. **Metadata separati per soggetto aggregato**: Se AgID richiede metadata separati per ogni soggetto aggregato, potrebbe essere necessario generare metadata multipli

## Risorse utili

- [AgID - Soggetti Aggregatori SPID](https://www.agid.gov.it/it/piattaforme/spid/soggetti-aggregatori)
- [SPID.gov.it - Diventa Soggetto Aggregatore](https://www.spid.gov.it/cos-e-spid/diventa-fornitore-di-servizi/diventa-soggetto-aggregatore/)
- [Regolamento AgID - Determinazione n. 75/2023](https://www.agid.gov.it/it/agenzia/stampa-e-comunicazione/notizie/2023/03/09/diventare-soggetto-aggregatore-spid-online-il-regolamento)
- [Convenzione Aggregatori Pubblici - Determina n. 80/2018](https://www.agid.gov.it/it/piattaforme/spid/soggetti-aggregatori)
- [Convenzione Aggregatori Privati - Determina n. 71/2022](https://www.agid.gov.it/it/piattaforme/spid/soggetti-aggregatori)

## Contatti AgID

Per domande specifiche sul processo di accreditamento:
- Email: spid@agid.gov.it
- PEC: protocollo@pec.agid.gov.it

## Note finali

Questa guida fornisce una panoramica generale del processo. I dettagli specifici possono variare in base alle esigenze di AgID e alle caratteristiche della vostra organizzazione. Si consiglia di:

1. Contattare AgID per chiarimenti specifici
2. Verificare le ultime versioni delle regole tecniche
3. Consultare la documentazione ufficiale di AgID
4. Valutare se sono necessarie modifiche al provider Keycloak in base ai requisiti specifici
