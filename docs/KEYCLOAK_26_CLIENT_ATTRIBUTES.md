# Configurazione Attributi Client in Keycloak 26

## ⚠️ Limitazione dell'Interfaccia Grafica

In **Keycloak 26**, la tab "Attributes" per i client **non è disponibile** nell'Admin Console. Gli attributi client possono essere configurati solo tramite:

1. **REST API** (metodo consigliato)
2. **Import/Export JSON** dei client
3. **Script di configurazione**

## Soluzioni Alternative

### Opzione 1: REST API (Metodo Consigliato)

Vedi la sezione "Via REST API" nella documentazione principale per gli script completi.

### Opzione 2: Import/Export Client JSON

Puoi esportare un client, modificare il JSON e reimportarlo.

#### Passo 1: Esportare il client

```bash
# Ottieni token admin
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

# Ottieni UUID del client
CLIENT_UUID=$(curl -s "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" | jq -r '.[0].id')

# Esporta client
curl -s "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}" \
  -H "Authorization: Bearer ${TOKEN}" > client.json
```

#### Passo 2: Modificare il JSON

Apri `client.json` e aggiungi/modifica la sezione `attributes`:

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

#### Passo 3: Reimportare il client

```bash
# Rimuovi il campo "id" se presente (Keycloak lo genererà)
jq 'del(.id)' client.json > client_updated.json

# Aggiorna il client
curl -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d @client_updated.json
```

### Opzione 3: Script Bash Completo

Crea uno script `configure-spid-client.sh`:

```bash
#!/bin/bash

# Configurazione
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="${REALM:-spid-aggregator}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin}"
CLIENT_ID="${1:-soggetto-aggregato-1}"

# Colori per output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Configurazione client SPID: ${CLIENT_ID}${NC}"

# 1. Ottieni token
echo "Ottenimento token admin..."
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "Errore: Impossibile ottenere token. Verifica credenziali."
  exit 1
fi

echo -e "${GREEN}✓ Token ottenuto${NC}"

# 2. Verifica se il client esiste
echo "Verifica esistenza client..."
CLIENT_UUID=$(curl -s "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" | jq -r '.[0].id // empty')

if [ -z "$CLIENT_UUID" ]; then
  echo "Client non trovato. Creazione nuovo client..."
  
  # Crea nuovo client
  RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
      "clientId": "'"${CLIENT_ID}"'",
      "protocol": "saml",
      "enabled": true,
      "attributes": {
        "saml.assertion.signature": "true",
        "saml.server.signature": "true"
      }
    }')
  
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  if [ "$HTTP_CODE" != "201" ]; then
    echo "Errore nella creazione del client. HTTP Code: $HTTP_CODE"
    echo "$RESPONSE" | head -n-1
    exit 1
  fi
  
  # Ottieni UUID del client appena creato
  CLIENT_UUID=$(curl -s "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" \
    -H "Authorization: Bearer ${TOKEN}" | jq -r '.[0].id')
  
  echo -e "${GREEN}✓ Client creato${NC}"
else
  echo -e "${GREEN}✓ Client trovato${NC}"
fi

# 3. Leggi attributi da file di configurazione o usa valori di default
read -p "Inserisci Entity ID (default: https://${CLIENT_ID}.example.com/spid): " ENTITY_ID
ENTITY_ID=${ENTITY_ID:-"https://${CLIENT_ID}.example.com/spid"}

read -p "Inserisci Nome Organizzazione (default: Nome Organizzazione): " ORG_NAME
ORG_NAME=${ORG_NAME:-"Nome Organizzazione"}

read -p "Inserisci Email Contatto (default: contatto@example.com): " CONTACT_EMAIL
CONTACT_EMAIL=${CONTACT_EMAIL:-"contatto@example.com"}

# 4. Aggiorna attributi SPID
echo "Aggiornamento attributi SPID..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "attributes": {
      "spid.entityId": "'"${ENTITY_ID}"'",
      "spid.organizationNames": "it|'"${ORG_NAME}"'",
      "spid.organizationDisplayNames": "it|'"${ORG_NAME}"'",
      "spid.organizationUrls": "it|https://example.com",
      "spid.contact.other.company": "'"${ORG_NAME}"'",
      "spid.contact.other.email": "'"${CONTACT_EMAIL}"'",
      "spid.contact.other.phone": "+39 123 456 7890",
      "spid.contact.other.isSpPrivate": "false",
      "spid.attributeConsumingServiceIndex": "1",
      "spid.attributeConsumingServiceName": "it|Servizi Online"
    }
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" != "204" ]; then
  echo "Errore nell'aggiornamento. HTTP Code: $HTTP_CODE"
  echo "$RESPONSE" | head -n-1
  exit 1
fi

echo -e "${GREEN}✓ Attributi SPID configurati${NC}"
echo ""
echo "Client configurato con successo!"
echo "Metadata URL: ${KEYCLOAK_URL}/realms/${REALM}/spid-sp-metadata/clients/${CLIENT_ID}"
```

Rendi eseguibile e usa:

```bash
chmod +x configure-spid-client.sh
./configure-spid-client.sh soggetto-aggregato-1
```

## Verifica Configurazione

Dopo aver configurato gli attributi, verifica che siano stati salvati:

```bash
# Ottieni token (come sopra)
TOKEN="..."

# Ottieni UUID client
CLIENT_UUID=$(curl -s "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" | jq -r '.[0].id')

# Verifica attributi
curl -s "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.attributes | with_entries(select(.key | startswith("spid")))'
```

## Note per Keycloak 26

- **Nessuna UI per attributi client**: L'Admin Console non fornisce una tab dedicata
- **REST API obbligatoria**: È l'unico modo affidabile per configurare attributi client
- **Import/Export**: Puoi usare il JSON per modifiche batch
- **Script consigliati**: Per semplificare operazioni ripetitive

## Alternative Future

Se hai bisogno di una UI, potresti considerare:
1. Creare un'estensione personalizzata per Keycloak
2. Usare strumenti di terze parti che interfacciano con la REST API
3. Aspettare future versioni di Keycloak che potrebbero aggiungere questa funzionalità
