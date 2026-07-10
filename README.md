# cloud-itonami-isic-4630

Open Business Blueprint for **ISIC Rev.5 4630**: Wholesale of Food,
Beverages and Tobacco -- provision-order intake (processed food,
non-alcoholic beverage, alcoholic beverage and tobacco consignments),
per-jurisdiction counterparty-diligence / food-safety / alcohol-excise
/ tobacco-excise / sanctions regulatory verification, delivery
dispatch, and invoice settlement for a wholesaler of food, beverages
and tobacco.

This repository publishes a food/beverage/tobacco wholesale actor --
provision-order intake, per-jurisdiction contract / food-safety /
alcohol-excise / tobacco-excise / sanctions regulatory verification,
delivery and invoice settlement -- as an OSS business that any
qualified operator can fork, deploy, run, improve and sell, so a
regional food, beverage or tobacco wholesaler never surrenders
counterparty, credit, regulatory-certification and trade data to a
closed provision-trading / ERP SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **ProvisionTradeAdvisor ⊣
Provision Trading Governor**. This blueprint's own
`:itonami.blueprint/governor` keyword, `:provision-trading-governor`,
is grep-verified UNIQUE among the actor fleet repos checked out at
build time -- a fresh, independent build.

**Like the fuel-wholesale (`cloud-itonami-isic-4671`), general-trading
(`cloud-itonami-isic-4690`), commission-brokerage
(`cloud-itonami-isic-4610`) and agri-wholesale
(`cloud-itonami-isic-4620`) siblings, this vertical is
SELF-CONTAINED**: there is no `kotoba-lang/provisiontrade` to delegate
provision-trading validation to, so the credit-clearance / contract-
on-file / food-safety-certificate / alcohol-excise-license / tobacco-
excise-registration / age-verification / sanctions-screening checks
live as direct entity boolean reads in `provisiontrade.governor` (off
dedicated `:credit-cleared?` / `:contract-terms` / `:food-safety-
certificate?` / `:alcohol-excise-license?` / `:tobacco-excise-
registration?` / `:age-verification-on-file?` / `:sanctions-screened?`
facts on the `provision-order` record), rather than wrapping an
external capability library's own validated function.

**What makes ISIC 4630 structurally different from every wholesale
sibling above**: it is downstream of the agri-wholesale sibling
(trading PROCESSED, PACKAGED goods for onward retail distribution, not
RAW agricultural inputs), and it is the only one where a single ISIC
code spans FOUR consignment categories mapped many-to-one onto THREE
genuinely different regulatory regimes -- food-safety (shared by
processed food AND non-alcoholic beverages), alcohol excise/licensing
(alcoholic beverages only), and tobacco excise/age-verification
(tobacco only) -- extending the agri-wholesale sibling's two-way
phytosanitary/animal-health split to a three-way split. See
`src/provisiontrade/facts.cljc` and `src/provisiontrade/governor.cljc`
for how this is modeled: a `:consignment-category` (`:food` |
`:beverage-non-alcoholic` | `:beverage-alcoholic` | `:tobacco`) on
every `provision-order`, a jurisdiction catalog keyed by BOTH
jurisdiction and regulatory class (via `regulatory-class-for`'s
many-to-one mapping), and THREE separate certificate/licence/
registration-missing HARD checks
(`food-safety-certificate-missing`, `alcohol-excise-license-missing`,
`tobacco-excise-age-verification-missing`) rather than one generic
check.

> **Why an actor layer at all?** An LLM is great at drafting an order
> summary, normalizing records, and reading a credit file -- but it
> has **no notion of which jurisdiction's food-safety / alcohol-excise
> / tobacco-excise / sanctions law is official, no license to dispatch
> real food, beverage or tobacco product to a counterparty or settle a
> real invoice, and no way to know on its own whether the
> counterparty's credit has actually been cleared, whether contract
> terms are actually on file, or whether a REAL food-safety
> certificate, alcohol excise licence, or tobacco excise registration +
> age-verification record has actually been issued for THIS
> consignment**. Letting it dispatch a consignment or settle an invoice
> directly invites fabricated regulatory citations, product leaving the
> warehouse to an uncreditworthy or unscreened counterparty without a
> real regulatory clearance, and an invoice settling against a
> sanctioned party -- exposing the operator to real regulatory-
> enforcement and financial liability, for whoever runs it. This
> project seals the ProvisionTradeAdvisor into a single node and wraps
> it with an independent **Provision Trading Governor**, a human
> **approval workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers provision-order intake through contract / food-safety
/ alcohol-excise / tobacco-excise / sanctions regulatory verification,
delivery dispatch (food, beverage or tobacco product leaving the
wholesale warehouse for a counterparty) and invoice settlement (the
money side of the trade, custody / financial transfer) for a
wholesaler of food, beverages and tobacco. It does **not**, by itself,
hold any food/beverage/tobacco wholesale licence, food-safety
inspection authority, liquor licence or tobacco retail/wholesale
authority required to run a food/beverage/tobacco wholesale business in
a given jurisdiction, and it does not claim to. It also does not
perform the actual physical warehouse pick/pack/palletize, the
long-haul carrier leg to the retailer, or route optimization itself, or
judge trading-book economics -- warehouse-slotting/route optimization
(the blueprint's own `:optimization` technology) is a follow-up slice,
not in this R0. Whoever deploys and operates a live instance (a
qualified trading supervisor / warehouse operator) supplies any
jurisdiction-specific operating authority, the real warehouse-
automation/ERP integrations, and bears that jurisdiction's liability --
the software supplies the governed, spec-cited, audited execution
scaffold so that operator does not have to build the compliance layer
from scratch.

### Actuation

**Dispatching a real food/beverage/tobacco consignment to a
counterparty and settling a real invoice are never autonomous, at any
phase, by construction.** Two independent layers enforce this
(`provisiontrade.governor`'s `:delivery/dispatch`/`:invoice/settle`
high-stakes gate and `provisiontrade.phase`'s phase table, which never
puts either op in any phase's `:auto` set) -- see
`provisiontrade.phase`'s docstring and
`test/provisiontrade/phase_test.clj`'s
`delivery-dispatch-never-auto-at-any-phase`/
`invoice-settle-never-auto-at-any-phase`. The actor may draft, check
and recommend; a human trading supervisor is always the one who
actually dispatches a delivery or settles an invoice. Grounded in
provision-trading and food-safety/excise doctrine (the same discipline
every regulator in `provisiontrade.facts` codifies: a real delivery and
a real invoice settlement are human sign-off acts) -- a genuine
DUAL-actuation shape, applied SEQUENTIALLY to the SAME provision-order
(delivery first, invoice settlement later), the same sequential shape
the fuel-wholesale and agri-wholesale siblings use.

## The core contract

```
provision-order intake + jurisdiction/regulatory-class facts (provisiontrade.facts, spec-cited)
        |
        v
   ┌────────────────────────┐   proposal      ┌─────────────────────────┐
   │ ProvisionTradeAdvisor   │ ─────────────▶ │ Provision Trading         │  (independent system)
   │ (sealed)                │  + citations    │ Governor                 │
   └────────────────────────┘                 │ spec-basis · evidence-   │
          │                 commit ◀┼ incomplete · credit-uncleared ·   │
          │                         │ contract-missing · food-safety-   │
    record + ledger        escalate ┼ certificate-missing · alcohol-    │
          │              (ALWAYS for│ excise-license-missing ·          │
          │       :delivery/        │ tobacco-excise-age-verification-  │
          │       dispatch/         │ missing · counterparty-sanctions- │
          │       :invoice/         │ flag-unresolved · already-        │
          │       settle)           │ dispatched · already-invoiced     │
          │                         └─────────────────────────┘
          ▼
      human approval
```

**The ProvisionTradeAdvisor never dispatches a consignment to a
counterparty or settles an invoice the Provision Trading Governor would
reject.** Hard violations (fabricated regulatory requirements;
unsupported evidence; an uncleared counterparty credit; no
contract-terms on file; a missing food-safety certificate on a food/
non-alcoholic-beverage consignment; a missing alcohol excise licence on
an alcoholic-beverage consignment; a missing tobacco excise
registration or age-verification record on a tobacco consignment; an
unresolved sanctions-screening flag; a double delivery/invoice) force
**hold** and *cannot* be approved past; a clean delivery/invoice
proposal still always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk four clean lifecycles (food, non-alcoholic beverage, alcoholic beverage, tobacco) plus every HARD-hold case, through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here that premise is exercised as
warehouse automation, not a rack/elevator dispatch act: real modern
food/beverage/tobacco distribution centers already run automated
storage-and-retrieval systems (AS/RS), robotic case-picking/palletizing
and AGV-class robotic forklifts (well-known real-world precedents
include Symbotic-powered distribution centers used by major grocery
retailers, and Ocado's automated grocery fulfillment centers). An
autonomous warehouse pallet-picking/staging robot performs the physical
pick/pack/palletize/staging act at the wholesaler's own warehouse --
the point at which this actor's `:delivery/dispatch` occurs -- under
the actor, gated by the independent **Provision Trading Governor**. The
robot's role ends at the wholesaler's own dock; the long-haul carrier
leg to the retailer is explicitly out of scope for this actor, the same
"hand off to a carrier" disclaimer every sibling's scope section makes
about the leg beyond its own physical dispatch point. Either way, the
governor never dispatches hardware itself: a delivery-clearing action
must have cleared the same sign-off a human trading supervisor would
need. This restates the fleet-wide robotics premise three ways
(ADR-2607011000): the blueprint declares `:robotics true`, the README
names the robot that performs the physical act, and the Provision
Trading Governor is the independent gate that robot's command must
pass.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Provision Trading Governor, delivery/invoice draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`4630`). Like the fuel-wholesale and agri-wholesale siblings, this
vertical is NOT backed by a separate bespoke domain capability lib: the
provision-trading checks (credit-clearance, contract-on-file,
food-safety certification, alcohol excise licensing, tobacco excise
registration/age-verification, sanctions-screening) are direct entity
boolean reads in `provisiontrade.governor`, on top of the generic
robotics/identity/forms/dmn/bpmn/audit-ledger stack.

## Layout

| File | Role |
|---|---|
| `src/provisiontrade/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + delivery AND invoice history (dual history). The double-actuation guard checks dedicated `:dispatched?`/`:invoiced?` booleans rather than a `:status` value |
| `src/provisiontrade/registry.cljc` | Delivery/invoice draft records (record construction only -- the Provision Trading Governor's checks are direct entity booleans, so there are no pure range-check functions to host here) |
| `src/provisiontrade/facts.cljc` | Per-jurisdiction, per-regulatory-class (`:food-safety`/`:alcohol-excise`/`:tobacco-excise`) catalog with an official spec-basis citation per entry, honest coverage reporting, and the `:consignment-category` -> `:regulatory-class` many-to-one mapping |
| `src/provisiontrade/provisiontradeadvisor.cljc` | **ProvisionTradeAdvisor** -- `mock-advisor` ‖ `llm-advisor`; intake/regulatory-verification/delivery/invoice proposals |
| `src/provisiontrade/governor.cljc` | **Provision Trading Governor** -- 10 HARD checks (spec-basis · evidence-incomplete · credit-uncleared · contract-missing · food-safety-certificate-missing · alcohol-excise-license-missing · tobacco-excise-age-verification-missing · counterparty-sanctions-flag-unresolved · already-dispatched · already-invoiced) + 1 soft (confidence/actuation gate) |
| `src/provisiontrade/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (delivery/invoice always human; order intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/provisiontrade/operation.cljc` | **OperationActor** -- langgraph StateGraph |
| `src/provisiontrade/sim.cljc` | demo driver |
| `test/provisiontrade/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers provision-order intake through contract / food-safety
/ alcohol-excise / tobacco-excise / sanctions regulatory verification,
delivery and invoice settlement -- the core governed lifecycle:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Provision-order intake + per-jurisdiction, per-regulatory-class evidence checklisting, HARD-gated on an official spec-basis citation (`:order/intake`/`:regulatory/verify`) | Real warehouse-automation/ERP integration, long-haul carrier routing and trading-book economics |
| Delivery, HARD-gated on full evidence, a credit-cleared counterparty, contract-terms on file, the category-appropriate regulatory clearance on file and a passed sanctions screen (`:delivery/dispatch`) | |
| Invoice settlement, HARD-gated on full evidence, a passed sanctions screen and no double-invoice (`:invoice/settle`) | |
| Immutable audit ledger for every intake/verification/delivery/invoice decision | |

Extending coverage is additive: add the next gate (e.g. a lot-level
recall/traceability reconciliation check) as its own governed op with
its own HARD checks and tests, following the SAME "an independent
governor re-verifies against the actor's own records before any
real-world act" pattern this repo's flagship ops already establish.

## Jurisdiction coverage (honest)

`provisiontrade.facts/coverage` reports how many requested
jurisdictions actually have an official spec-basis (for ALL THREE
regulatory classes) in `provisiontrade.facts/catalog` -- currently 4
seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions worldwide. This is
a starting catalog to prove the governor contract end-to-end, not a
claim of global coverage. I do not have live web access; the headline
statute/regulation names and owner-authority names (食品衛生法/酒税法/
たばこ事業法/MHLW/NTA/MOF for Japan; the FDA Food Safety Modernization
Act/Federal Alcohol Administration Act/Family Smoking Prevention and
Tobacco Control Act/FDA/TTB for the US; the Food Safety Act 1990/
Alcoholic Liquor Duties Act 1979/Tobacco Products Duty Act 1979/FSA/
HMRC for the UK; Regulation (EC) No 178/2002/Directive 92/83/EEC/
Directive 2011/64/EU/BVL/Zoll for Germany) are cited from training-time
knowledge with reasonable confidence for the headline instruments and
agencies, but the exact US Code pin-cite for FSMA, the exact UK Act
titles, and the exact German national implementing-act names (as
distinct from the EU directives themselves, which I am confident about)
should be independently verified before this catalog is relied on
operationally -- see `docs/business-model.md` 'Jurisdiction coverage
(honest)'. Adding a jurisdiction (or a missing regulatory class for an
already-seeded jurisdiction) is additive: one map entry in
`provisiontrade.facts/catalog`, citing a real official source -- never
fabricate a jurisdiction's requirements to make coverage look bigger.

## Maturity

`:implemented` -- `ProvisionTradeAdvisor` + `Provision Trading
Governor` run as real, tested code (see `Run` above), following the
SAME governed-actor architecture as the other prior actors across this
fleet, with its own distinct, independently-named governor and its own
direct-entity-boolean provision-trading checks, including the fleet's
first three-way regulatory-class split via a many-to-one consignment-
category mapping. See `docs/adr/0001-architecture.md` for the history
and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
