# Governance

`cloud-itonami-isic-4630` is an OSS open-business blueprint for a wholesaler
of food, beverages and tobacco -- processed/packaged food, non-alcoholic
beverage, alcoholic beverage and tobacco consignments, traded and dispatched
under one counterparty-diligence + regulatory-compliance governance loop
(ISIC 4630, "wholesale of food, beverages and tobacco").

## Maintainers
Maintainers may merge changes that preserve these invariants:
- a provision-order whose jurisdiction (and consignment category) has no
  official food-safety/alcohol-excise/tobacco-excise/sanctions spec-basis can
  never be verified, dispatched or invoiced.
- the Provision Trading Governor remains independent of the advisor.
- hard governor violations (a fabricated spec-basis, incomplete regulatory
  evidence, an uncleared counterparty credit, a missing contract, a missing
  food-safety certificate on a food/non-alcoholic-beverage consignment, a
  missing alcohol excise licence on an alcoholic-beverage consignment, a
  missing tobacco excise registration or age-verification record on a
  tobacco consignment, an unresolved sanctions-screening flag, a double
  delivery or a double invoice) cannot be overridden by human approval.
- every intake, verification, delivery, settlement and hold is auditable.
- counterparty, credit, food-safety/alcohol-excise/tobacco-excise
  certification and sanctions data stays outside Git.
- the food-safety, alcohol-excise and tobacco-excise checks stay three
  distinct, independently-named governor rules -- code must not collapse
  them into one generic 'certificate-missing' check that would blur which
  regime failed.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit and data-flow review.

Certified operators can lose certification for:
- bypassing delivery-dispatch or invoice-settlement policy checks
- mishandling counterparty, credit, food-safety/alcohol-excise/tobacco-excise
  certification or sanctions-screening data
- misrepresenting certification status
- failing to respond to security incidents
