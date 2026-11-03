# Payments in nl-portal — Resources

## Knowledge

- [Worldline Direct — Hosted Checkout](https://docs.direct.worldline-solutions.com/en/integration/basic-integration-methods/hostedcheckout/)
  Hoe het hosted-checkout-model werkt en waarom het PCI-scope bij de merchant (nl-portal) weghoudt. Gebruik voor: snappen waarom `DirectPaymentClient` nooit kaartgegevens ziet.
- [Worldline Direct — Webhooks](https://docs.direct.worldline-solutions.com/en/integration/webhooks)
  Hoe server-naar-server notificaties (de "postsale"-webhook) werken, inclusief signatuurvalidatie. Gebruik voor: beoordelen of een webhook-wijziging de beveiliging intact laat.
- Broncode: `backend/payment-direct/` en `frontend/packages/user-interface/src/hooks/usePayment*.tsx` in dit repo
  De daadwerkelijke integratie — altijd de uiteindelijke scheidsrechter bij twijfel over hoe nl-portal het protocol toepast.

## Wisdom (Communities)

- Geen community-voorkeur vastgelegd. Voor PR-review-vragen is intern overleg met het team waarschijnlijk hoger-signaal dan een publieke community — vraag de gebruiker of ze dit willen als er wisdom-behoefte ontstaat.

## Gaps

- Geen bron gevonden specifiek over "webhook-betrouwbaarheid / idempotentie-patronen" los van Worldline's eigen docs — relevant zodra een les over foutafhandeling/race-conditions in de webhook wordt gemaakt.
