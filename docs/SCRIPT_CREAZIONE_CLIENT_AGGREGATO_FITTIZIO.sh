#!/bin/bash

# Script per creare un client come soggetto aggregato fittizio per collaudo
# Utilizzo: ./script_creazione_client_aggregato_fittizio.sh

# Configurazione
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="${REALM:-spid-cie}"
CLIENT_ID="soggetto-aggregato-fittizio"

# Ottieni token admin (sostituisci con le tue credenziali)
echo "Ottengo token admin..."
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "Errore: Impossibile ottenere token. Verifica credenziali e URL Keycloak."
  exit 1
fi

echo "Token ottenuto con successo"

# Crea il client
echo "Creo client ${CLIENT_ID}..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "'"${CLIENT_ID}"'",
    "name": "Soggetto Aggregato Fittizio per Collaudo",
    "description": "Client di test per collaudo tecnico come soggetto aggregatore in modalità full",
    "enabled": true,
    "protocol": "saml",
    "attributes": {
      "saml.force.post.binding": "false",
      "saml.server.signature": "true",
      "saml.encrypt": "false",
      "saml.authnstatement": "true",
      "saml.signature.algorithm": "RSA_SHA256",
      "spid.entityId": "https://soggetto-fittizio.example.com/spid",
      "spid.organizationNames": "it|Soggetto Aggregato Fittizio S.r.l.",
      "spid.organizationDisplayNames": "it|Soggetto Fittizio",
      "spid.organizationUrls": "it|https://soggetto-fittizio.example.com",
      "spid.attributeConsumingServiceIndex": "0",
      "spid.attributeConsumingServiceName": "default",
      "spid.contact.other.isSpPrivate": "false",
      "spid.contact.other.ipaCode": "c_test001",
      "spid.contact.other.company": "Soggetto Aggregato Fittizio S.r.l.",
      "spid.contact.other.email": "contatto@soggetto-fittizio.example.com",
      "spid.contact.other.phone": "+39 02 12345678",
      "spid.aggregated.company": "Comune Fittizio",
      "spid.aggregated.ipaCode": "c_fittizio",
      "spid.aggregated.isPrivate": "false",
      "spid.requestedAttributes": "familyName,name,spidCode,fiscalNumber,email,dateOfBirth"
    },
    "defaultClientScopes": ["role_list", "profile", "email", "roles", "web-origins", "saml"],
    "fullScopeAllowed": true,
    "redirectUris": ["https://soggetto-fittizio.example.com/*"],
    "webOrigins": ["https://soggetto-fittizio.example.com"]
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "201" ]; then
  echo "✅ Client creato con successo!"
  echo ""
  echo "Metadata disponibile all'URL:"
  echo "${KEYCLOAK_URL}/realms/${REALM}/spid-sp-metadata/clients/${CLIENT_ID}"
  echo ""
  echo "Per verificare il metadata:"
  echo "curl -s '${KEYCLOAK_URL}/realms/${REALM}/spid-sp-metadata/clients/${CLIENT_ID}' | xmllint --format -"
else
  echo "❌ Errore nella creazione del client (HTTP $HTTP_CODE)"
  echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
  exit 1
fi
