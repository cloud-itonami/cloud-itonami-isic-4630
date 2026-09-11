# Operator Guide

## First Deployment
1. Register traders, wholesale warehouses, provision-orders, and
   trading supervisors.
2. Import provision-order, counterparty, credit, sanctions and trade
   history, tagging each consignment's `:consignment-category`
   (`:food`/`:beverage-non-alcoholic`/`:beverage-alcoholic`/`:tobacco`)
   correctly -- this is what routes which regulatory-class clearance
   check applies.
3. Seed the per-jurisdiction, per-regulatory-class spec-basis catalog
   (`provisiontrade.facts`) for the jurisdictions you actually trade
   in, citing real official sources only.
4. Run read-only spec-basis validation per jurisdiction and regulatory
   class.
5. Configure sanctions / credit escalation and accounts-receivable
   accounts.
6. Publish a dry-run delivery/invoice and audit export.

## Minimum Trading Controls
- spec-basis validation before any verification, delivery, or invoice
- full regulatory evidence (credit-clearance record, contract/PO,
  sanctions-screening record, food-safety certificate / alcohol excise
  licence / tobacco excise registration + age-verification record)
  before any delivery
- credit-clearance, contract-on-file and category-appropriate
  regulatory-clearance checks before any delivery; sanctions-screening
  before any delivery AND any invoice
- sanctions / credit escalation gate
- audit export for every delivery, invoice, and hold
- backup manual dispatch and invoicing process

## A Day in the Life: Intake → Verify → Dispatch → Settle → Audit

Wholesale of Food, Beverages and Tobacco (ISIC 4630,
`cloud-itonami-isic-4630`) runs on the same intake / advise / govern /
decide / commit-or-hold loop as every itonami blueprint, but here the
loop is concrete -- and it forks on WHAT is being traded. Walking
through four orders end to end, one food, one non-alcoholic beverage,
one alcoholic beverage, and one tobacco:

1. **Intake.** The trader books the provision-order through `:forms`:
   order-id, `:consignment-category` (`:food`/`:beverage-non-alcoholic`/
   `:beverage-alcoholic`/`:tobacco`), product-description, quantity,
   unit, counterparty, price, contract-terms, jurisdiction, and the
   order's own diligence record (credit-cleared?, sanctions-screened?).
   This creates a provision-order record at `:order/intake` status. The
   ProvisionTradeAdvisor only normalizes the patch; it does not invent
   the order-id, counterparty, jurisdiction, consignment category, or
   any commercial/diligence value.
2. **Verify.** The ProvisionTradeAdvisor drafts a per-jurisdiction,
   per-regulatory-class food-safety / alcohol-excise / tobacco-excise /
   sanctions evidence checklist (`:regulatory/verify`) from
   `provisiontrade.facts`, citing the jurisdiction's official
   spec-basis (owner authority, legal basis, provenance) FOR THIS
   CONSIGNMENT'S REGULATORY CLASS -- a food or non-alcoholic-beverage
   order gets the food-safety requirement set, an alcoholic-beverage
   order gets the alcohol-excise requirement set, a tobacco order gets
   the tobacco-excise-and-age-verification requirement set, never the
   wrong one. The `:provision-trading-governor` sign-off gate must
   clear: it checks the jurisdiction actually has an official
   spec-basis on file for that regulatory class (never invent one). A
   jurisdiction (or regulatory class) with no spec-basis is a HARD hold
   at the governor node -- it never even reaches a human. This
   verification always escalates to a human for approval; it is never
   auto.
3. **Dispatch.** Before a consignment can leave the wholesale
   warehouse, the `:provision-trading-governor` sign-off gate runs the
   full HARD check set against the order's own ground truth: the
   spec-basis exists, the evidence checklist is complete, the
   counterparty's credit has been cleared, contract-terms are on file,
   the CATEGORY-APPROPRIATE regulatory clearance is on file
   (food-safety certificate for food/non-alcoholic beverage, alcohol
   excise licence for alcoholic beverage, BOTH a tobacco excise
   registration AND an age-verification record for tobacco -- never
   the wrong one), the counterparty has passed sanctions screening, and
   the order has not already been dispatched. Any failure is a HARD
   hold that a human cannot override. If every check is clean, the
   proposal STILL always escalates to a human trading supervisor -- a
   `:delivery/dispatch` never auto-commits at any phase. On approval,
   the delivery record is drafted (`<JURISDICTION>-DELIVERY-000001`)
   and the order's `:dispatched?` flag is set.
4. **Settle.** Once the consignment has actually been dispatched, the
   invoice is settled (`:invoice/settle`): the money side of the trade,
   custody / financial transfer. The governor re-checks the spec-basis,
   the evidence completeness, the sanctions screening, and that this
   order's invoice has not already been settled. As with the delivery,
   a clean invoice STILL always escalates to a human trading supervisor
   -- `:invoice/settle` never auto-commits. On approval the invoice
   record is drafted (`<JURISDICTION>-INVOICE-000001`) and the order's
   `:invoiced?` flag is set.
5. **Audit.** The verification, the delivery sign-off, the delivery
   record, the invoice sign-off, and the invoice record are all
   appended to the `:audit-ledger` -- immutable and exportable, so a
   counterparty, food-safety inspector, excise authority or regulatory
   dispute can be traced back to the exact spec-basis citation,
   evidence checklist, and supervisor sign-off that authorized the
   delivery and invoice. If something is wrong with the counterparty or
   the consignment (a credit deterioration, a sanctions hit, an expired
   regulatory clearance), that gets raised as a flag and routed through
   the escalation gate instead of being silently suppressed -- a
   delivery for that order then waits on governor sign-off of the
   flag's resolution.

Any deviation from this loop is exactly what the Trust Controls in
`docs/business-model.md` exist to catch: an order verified against a
fabricated spec-basis, a delivery started with incomplete evidence, an
uncleared counterparty credit or a contract gap, a missing or
wrong-category regulatory clearance, a sanctions screening suppressed
to force a delivery through, or an invoice posted without a human
sign-off.

## Feel the Decision Gate: `kbb -M:dev:run`

This vertical has no companion playable prototype. The fastest hands-on
way to feel why the `:provision-trading-governor` gate exists is the
bundled demo, which walks one clean food order, one clean non-alcoholic
-beverage order, one clean alcoholic-beverage order, and one clean
tobacco order through intake → verify → dispatch → settle (each
dispatch/settle pausing for human approval) and then exercises every
HARD-hold failure mode in isolation:

- a jurisdiction with no official spec-basis → HOLD (`:no-spec-basis`),
- a counterparty whose credit has not been cleared → HOLD
  (`:credit-uncleared`),
- an order with no contract-terms on file → HOLD (`:contract-missing`),
- a food order with no food-safety certificate on file → HOLD
  (`:food-safety-certificate-missing`),
- an alcoholic-beverage order with no alcohol excise licence on file →
  HOLD (`:alcohol-excise-license-missing`),
- a tobacco order with no tobacco excise registration on file → HOLD
  (`:tobacco-excise-age-verification-missing`),
- a tobacco order with no age-verification record on file → HOLD
  (`:tobacco-excise-age-verification-missing`),
- a counterparty that has not passed sanctions screening → HOLD
  (`:counterparty-sanctions-flag-unresolved`),
- a double delivery of the same order → HOLD (`:already-dispatched`),
- a double invoice of the same order → HOLD (`:already-invoiced`).

Each HOLD settles at the governor node and never reaches a human
approver -- the same failure mode the audit ledger is built to catch and
the minimum trading controls above are built to prevent. It is not a
substitute for those controls, but it is the fastest way for a new
operator (or a reviewer) to feel, hands-on, why the gate exists before
touching a real deployment.

## Certification
Certified operators must prove spec-basis-grounded verification,
evidence-backed delivery readiness (credit-clearance, contract-on-file,
the correct category-appropriate regulatory clearance, sanctions-
screening), and human review for every delivery- and invoice-affecting
action.
