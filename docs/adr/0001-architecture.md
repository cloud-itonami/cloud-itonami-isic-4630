# ADR-0001: ProvisionTradeAdvisor ⊣ Provision Trading Governor architecture

## Status

Accepted. `cloud-itonami-isic-4630` published directly as `:implemented`
in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-4630` publishes an OSS business blueprint for
wholesale of food, beverages and tobacco (provision-order intake,
per-jurisdiction, per-regulatory-class contract / food-safety /
alcohol-excise / tobacco-excise / sanctions regulatory verification,
delivery, and invoice settlement). Like every prior actor in this
fleet, the blueprint alone is not an implementation: this ADR records
the governed-actor architecture that establishes it as real, tested
code, following the same langgraph StateGraph + independent Governor +
Phase 0->3 rollout pattern established by `cloud-itonami-isic-6511`
(life insurance) and applied across many prior siblings, most directly
the four PRINCIPAL/AGENCY wholesale-trading siblings:
`cloud-itonami-isic-4671` (fuel wholesale, PRINCIPAL, single-commodity
excise/sanctions focus), `cloud-itonami-isic-4690` (general/diversified
wholesale trading, PRINCIPAL, multi-commodity export-control/sanctions
focus), `cloud-itonami-isic-4610` (commission brokerage, AGENCY, never
takes title, dual-agency conflict-of-interest focus), and
`cloud-itonami-isic-4620` (agri-wholesale, PRINCIPAL, RAW agricultural
inputs and live animals, biosecurity focus, the fleet's first two-way
kind-gated certificate split).

ISIC 4630 is downstream of ISIC 4620: it trades PROCESSED, PACKAGED
food, beverage and tobacco goods for onward wholesale-to-retail
distribution, not raw commodity inputs. Its defining regulatory
exposure is genuinely THREE-WAY -- food-safety compliance (HACCP/
FSMA-style, shared by processed food AND non-alcoholic beverages),
alcohol excise/licensing (specific to alcoholic beverages), and tobacco
excise/age-verification (specific to tobacco products) -- extending the
agri-wholesale sibling's two-way phytosanitary/animal-health split to a
three-way split gated on FOUR consignment categories mapped
many-to-one onto three regulatory classes.

Like those four siblings, this vertical has NO bespoke domain
capability library in `kotoba-lang` to wrap (verified: no
`kotoba-lang/provisiontrade`-style repo exists, and `kotoba-lang/
robotics` is the generic cross-cutting robotics contract every
cloud-itonami vertical already uses, not a domain-specific library for
this vertical). This build therefore uses self-contained domain logic.
The provision-trading checks (credit-clearance, contract-on-file,
food-safety certification, alcohol excise licensing, tobacco excise
registration/age-verification, sanctions-screening) are direct entity
boolean reads in `provisiontrade.governor`, off dedicated
`:credit-cleared?` / `:contract-terms` / `:food-safety-certificate?` /
`:alcohol-excise-license?` / `:tobacco-excise-registration?` /
`:age-verification-on-file?` / `:sanctions-screened?` facts on the
`provision-order` record -- NO pure range-check functions are needed
(contrast the crude sibling, whose registry hosts its reservoir/
annular/water-cut/H2S range checks).

This blueprint's own `:itonami.blueprint/governor` keyword,
`:provision-trading-governor`, is grep-verified UNIQUE among the actor
fleet repos checked out at build time -- no naming-collision precedent
question, a fresh independent build.

## Decision

### Decision 1: fresh governor identity, no reuse precedent needed

`:provision-trading-governor` is grep-verified unique across every
`blueprint.edn` checked out locally at build time. This build follows
the SAME governed-actor architecture as every prior actor, but with its
own distinct governor identity.

### Decision 2: self-contained domain logic, direct entity booleans (no `kotoba-lang/provisiontrade` to wrap, and no range-check functions to host)

Like the fuel-wholesale, general-trading, commission-brokerage and
agri-wholesale siblings (and unlike the crude-extraction sibling, which
hosts pure physical range-check functions in its registry because its
governor re-verifies measured physical values), this food/beverage/
tobacco wholesale vertical needs no range-check functions: there is no
pre-existing provision-trading capability library to delegate to, AND
the governor's domain checks (credit-clearance, contract-on-file,
food-safety certification, alcohol excise licensing, tobacco excise
registration/age-verification, sanctions-screening) are direct entity
boolean reads off the `provision-order` record's own dedicated facts --
not measured-value-vs-limit range comparisons. So
`provisiontrade.registry` is RECORD CONSTRUCTION ONLY (no range-check
functions), and `provisiontrade.governor` reads the order's booleans
directly.

### Decision 3: dual-actuation shape, SEQUENTIAL on the SAME `provision-order` entity

Like the fuel-wholesale sibling's `fuel-order` entity and the
agri-wholesale sibling's `agri-order` entity, this vertical's `dispatch`
and `settle` actuation events apply SEQUENTIALLY to the SAME
`provision-order` -- a delivery happens first (food/beverage/tobacco
product leaves the wholesale warehouse), invoice settlement happens
later (the money side of the trade, custody / financial transfer), on
the same order record. `high-stakes` is
`#{:delivery/dispatch :invoice/settle}`; neither ever auto-commits at
any phase.

### Decision 4: THREE regulatory-class checks, category-gated via a many-to-one mapping, not four category-specific checks nor one generic 'certificate-missing' check -- the defining design decision of this build

This is the decision that most distinguishes this vertical from its
four PRINCIPAL/AGENCY wholesale-trading siblings, and extends the
agri-wholesale sibling's Decision 4 (the fleet's first kind-gated
certificate split) from two ways to three -- with a wrinkle the agri
sibling did not have to handle: FOUR consignment categories, not a
clean one-to-one mapping onto regulatory regimes.

ISIC 4630 spans FOUR consignment categories under ONE classification
code -- food (`:food`), non-alcoholic beverages
(`:beverage-non-alcoholic`), alcoholic beverages
(`:beverage-alcoholic`) and tobacco (`:tobacco`) -- but only THREE
genuinely different regulatory regimes govern them:

- processed food AND non-alcoholic beverages are BOTH governed by
  general food-safety law (e.g. the US Federal Food, Drug, and
  Cosmetic Act's own statutory definition of 'food', 21 U.S.C. §321(f),
  explicitly includes 'articles used for food or drink' -- a
  non-alcoholic beverage has no separate excise/licensing regime the
  way alcohol and tobacco do, in any of the four seeded jurisdictions).
- alcoholic beverages are governed by excise taxation and wholesale/
  retail licensing law entirely separate from general food safety
  (酒税法-style regimes: 酒税法 in Japan, the Federal Alcohol
  Administration Act in the US, the Alcoholic Liquor Duties Act 1979 in
  the UK, Council Directive 92/83/EEC in the EU).
- tobacco is governed by its own excise taxation AND retailer/
  purchaser age-verification law, again entirely separate from general
  food safety (たばこ事業法-style regimes plus age-of-sale law: たばこ
  事業法 in Japan, the Family Smoking Prevention and Tobacco Control
  Act in the US, the Tobacco Products Duty Act 1979 plus the Children
  and Young Persons (Protection from Tobacco) Act 1991 in the UK,
  Directive 2011/64/EU in the EU).

Three design options were considered:

- **Option A (rejected): one generic `:certificate-missing` check**,
  reading a single `:regulatory-certificate?` fact regardless of
  consignment category. Rejected for the SAME reason the agri-
  wholesale sibling's ADR rejects the analogous option: it would blur
  which regulatory regime actually failed on the audit ledger -- a
  `:certificate-missing` hold on a food order and a
  `:certificate-missing` hold on a tobacco order would look identical
  in the ledger even though they are governed by completely different
  statutes, inspected by different agencies in every seeded
  jurisdiction, and remediated by completely different processes (a
  food-safety inspection vs. an excise/age-verification review). This
  would make the audit trail less useful to exactly the regulator or
  counterparty it exists to serve.
- **Option B (rejected): FOUR category-specific checks**, one per
  `:consignment-category` (a dedicated
  `non-alcoholic-beverage-certificate-missing-violations` distinct from
  `food-certificate-missing-violations`). Rejected: this would
  misrepresent the domain by implying non-alcoholic beverages sit under
  a DIFFERENT regulatory regime from food, when in every seeded
  jurisdiction they are governed by the exact SAME food-safety statute
  and the exact SAME administering agency. Modeling them as two
  checks reading two different facts would also force the operator to
  populate two functionally-identical certificate fields for what is,
  regulatorily, one clearance.
- **Option C (chosen): THREE regulatory-class checks, with
  `regulatory-class-for` doing the many-to-one mapping BEFORE the
  governor check ever runs** --
  `food-safety-certificate-missing-violations` (reads
  `:food-safety-certificate?`, fires when `:consignment-category`
  is `:food` OR `:beverage-non-alcoholic`),
  `alcohol-excise-license-missing-violations` (reads
  `:alcohol-excise-license?`, fires only when `:consignment-category
  :beverage-alcoholic`), and
  `tobacco-excise-age-verification-missing-violations` (reads BOTH
  `:tobacco-excise-registration?` AND `:age-verification-on-file?`,
  fires only when `:consignment-category :tobacco` and EITHER
  sub-fact is false). Each produces its own distinctly-named rule
  keyword on the audit ledger, and the `:food`/`:beverage-non-
  alcoholic` many-to-one mapping is itself declared as data
  (`provisiontrade.facts/regulatory-class-for`) rather than duplicated
  `case` branches, so `provisiontrade.facts/spec-basis` and the
  governor's category-gate share ONE source of truth for "which regime
  applies to this category." This mirrors real regulatory practice
  honestly (see `provisiontrade.facts` catalog, which is likewise keyed
  by BOTH jurisdiction AND regulatory class, since e.g. Japan's 食品
  衛生法, 酒税法 and たばこ事業法 are three different statutes under
  three different administering agencies even though all four
  consignment categories arrive at the same ISIC 4630 order), and it is
  the SAME kind of domain-specific-check-gets-its-own-name discipline
  the agri-wholesale sibling's Decision 4 and the commission-brokerage
  sibling's `conflict-of-interest-undisclosed-violations` establish as
  precedent.

The tobacco check additionally folds TWO distinct real-world
sub-requirements (excise registration, retailer/purchaser age-of-sale
diligence) into ONE named governor rule rather than two, because both
are part of the SAME real-world tobacco-control regulatory concern (see
e.g. the US Tobacco Control Act's age-of-sale provisions and the UK's
Children and Young Persons (Protection from Tobacco) Act, which treat
excise registration and age-of-sale enforcement as two arms of one
tobacco-control regime, not two separate regimes the way food-safety
and alcohol-excise are from each other) -- and a tobacco dispatch is
equally unsafe whether the excise registration or the age-verification
record is the one missing, so a human reviewing the ledger needs to see
"tobacco compliance failed," with the `:detail` string naming which
sub-requirement specifically, rather than two independently-gated rule
keywords that could each individually mislead a reviewer into thinking
the OTHER sub-requirement was satisfied.

This makes ISIC 4630 the first vertical in this fleet's wholesale-
trading cluster whose governor runs TEN HARD checks instead of five
(fuel-wholesale), six (general-trading) or nine (agri-wholesale), and
whose `provision-order` record's regulatory fields are
consignment-category-conditional via a many-to-one mapping rather than
a one-to-one kind split.

### Decision 5: `counterparty-sanctions-flag-unresolved?` -- the open-flag-unresolved discipline (reapplied, not new)

An unresolved sanctions-screening flag -- the counterparty has not
passed OFAC / equivalent sanctions screening -- is a HARD,
un-overridable hold. This reuses the SAME open-flag-unresolved
discipline the freight sibling's `delivery-exception-unresolved?` check
(and the fuel-wholesale/general-trading/commission-brokerage/agri-
wholesale siblings' own sanctions checks) establish -- an open concern
cannot be silently suppressed to force a delivery or invoice through.
Evaluated UNCONDITIONALLY at both `:delivery/dispatch` and
`:invoice/settle`, and UNCONDITIONALLY regardless of consignment
category (unlike the certificate checks in Decision 4, sanctions
screening applies uniformly to all four categories -- there is no
regulatory reason to differentiate it by category, only the
certificate/licence/registration itself differs).

### Decision 6: dedicated double-actuation-guard booleans

`:dispatched?` / `:invoiced?` are dedicated booleans on the
`provision-order` record, never a single `:status` value -- the same
discipline every prior governor's guards establish, informed by
`cloud-itonami-isic-6492`'s real status-lifecycle bug
(ADR-2607071320).

### Decision 7: Store protocol, MemStore + DatomicStore parity

`provisiontrade.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.db`-
backed), proven to satisfy the same contract in
`test/provisiontrade/store_contract_test.clj`. The ledger stays
append-only on every backend: which provision-order was verified for a
jurisdiction/regulatory-class with no official spec-basis, which
counterparty had credit-uncleared / no contract / a missing food-safety
certificate / alcohol excise licence / tobacco excise registration or
age-verification record / an unresolved sanctions-screening flag, which
order was dispatched, which invoice was settled, on what jurisdictional
and regulatory basis, approved by whom -- always a query over an
immutable log.

### Decision 8: Phase 0->3 with `:delivery/dispatch`/`:invoice/settle` NEVER auto

`provisiontrade.phase`'s phase table puts `:order/intake` (no direct
capital risk) in phase 3's `:auto` set as its only member;
`:delivery/dispatch` and `:invoice/settle` are deliberately ABSENT from
every phase's `:auto` set, including phase 3 -- a permanent structural
fact. `provisiontrade.governor`'s high-stakes gate enforces the same
invariant independently: two layers agree that actuation is always a
human trading supervisor's call.

### Decision 9: mock + LLM advisor pair

`provisiontrade.provisiontradeadvisor` provides a deterministic
`mock-advisor` (default, runs offline) and an `llm-advisor` backed by a
`langchain.model/ChatModel`. The LLM advisor's EDN proposal is parsed
defensively: any parse/shape failure yields a safe low-confidence noop
so the governor escalates/holds -- an LLM hiccup can never auto-dispatch
a consignment or auto-settle an invoice.

### Decision 10: `:robotics true`, reasoned for warehouse automation distinct from a rack/elevator dispatch act

`:itonami.blueprint/robotics` is `true`, a deliberate call reasoned
specifically for this vertical rather than copied from a sibling
default. Modern food/beverage/tobacco distribution centers already
deploy substantial warehouse automation -- automated storage-and-
retrieval systems (AS/RS), robotic case-picking/palletizing, and
AGV-class robotic forklifts (e.g. Symbotic-powered distribution centers
used by major grocery retailers, and Ocado's automated grocery
fulfillment centers, are well-known real-world precedents). This
vertical's `:delivery/dispatch` is the physical pick/pack/palletize/
staging act at the wholesaler's own warehouse -- directly analogous to
the fuel-wholesale sibling's rack dispatch and the agri-wholesale
sibling's elevator loadout, in that the robot's role ends at the
wholesaler's own dock, NOT at the final retail destination (the
long-haul carrier leg is explicitly out of scope, "hand off to a
carrier," the same disclaimer every sibling's scope section makes about
the leg beyond its own physical dispatch point). This is a materially
different physical claim from a pure intermediation/brokerage vertical
(the general-trading and commission-brokerage siblings both correctly
set `:robotics false`, having no analogous physical dispatch act at
all) -- so `:robotics true` here was reasoned on this vertical's own
terms (warehouse automation is real and load-bearing for THIS actor's
`:delivery/dispatch`), not defaulted from either extreme precedent.

## Alternatives considered

- **Wrapping a bespoke `kotoba-lang/provisiontrade` capability
  library.** Considered and explicitly ruled out: no such library
  exists, and `kotoba-lang/robotics` is generic, not provision-trading-
  specific. Forcing a false capability-library integration would be
  dishonest; this build correctly uses self-contained domain logic
  instead.
- **Hosting pure range-check functions in the registry (as the crude
  sibling does).** Considered and ruled out: the provision-trading
  domain checks are direct entity booleans (credit cleared? contract on
  file? regulatory clearance on file? sanctions screened?), not
  measured-value-vs-limit range comparisons, so there are no range
  checks to host. `provisiontrade.registry` is record construction
  only.
- **One generic `:certificate-missing` check instead of three
  regulatory-class-gated checks.** Considered and rejected -- see
  Decision 4 above for the full reasoning: it would blur three
  genuinely different regulatory regimes on the audit ledger.
- **FOUR category-specific checks (one per `:consignment-category`)
  instead of three regulatory-class-gated checks.** Considered and
  rejected -- see Decision 4 Option B: it would misrepresent food and
  non-alcoholic beverages as separately-regulated when, in every
  seeded jurisdiction, they share the exact same statute and agency.
- **Splitting the tobacco check into two separate rules** (`tobacco-
  excise-registration-missing` and `tobacco-age-verification-missing`).
  Considered and rejected in favor of one combined
  `tobacco-excise-age-verification-missing` rule -- see Decision 4:
  both sub-requirements are arms of the SAME real-world tobacco-control
  regime, and a tobacco dispatch is equally unsafe if either is
  missing; the combined rule's `:detail` string still names which
  sub-requirement specifically failed, so no audit-ledger precision is
  lost.
- **A `:kind`-distinguished entity for delivery vs. invoice** (matching
  the retail sibling's `order` shape). Rejected: delivery and invoice
  settlement happen SEQUENTIALLY on the SAME provision-order in this
  domain, not as alternative actions -- the fuel-wholesale and agri-
  wholesale siblings' sequential shape is the honest match here. (Note
  this is a DIFFERENT `:kind` question from `:consignment-category`,
  which distinguishes food/beverage-non-alcoholic/beverage-alcoholic/
  tobacco and is orthogonal to the dispatch/invoice sequencing
  question.)
- **Defaulting `:robotics` to `false`** (matching the general-trading
  and commission-brokerage siblings, which are non-physical
  intermediation/brokerage verticals with no analogous physical
  dispatch act). Considered and rejected: this vertical's `:delivery/
  dispatch` is a genuine physical act (food/beverage/tobacco product
  actually leaving a wholesale warehouse via automated pick/pack/
  palletize), closer in kind to the fuel-wholesale sibling's rack
  dispatch and the agri-wholesale sibling's elevator loadout than to
  either of the pure-intermediation siblings -- see Decision 10.
- **Building warehouse-slotting/route and trading-book optimization in
  this R0.** Rejected in favor of a scoped R0 slice (the
  `:optimization` capability is correctly marked required, the
  integration is a follow-up), consistent with this fleet's 'extending
  coverage is additive' convention.

## Consequences

- Fresh independent actor in this fleet, following the SAME
  governed-actor architecture as every prior sibling.
- Establishes the provision-trading checks as direct entity boolean
  reads (no pure range-check functions needed), an honest structural
  differentiator from the crude-extraction sibling's registry-hosted
  physical range checks.
- Establishes the fleet's first THREE-way regulatory-class split via a
  many-to-one category mapping (Decision 4) -- a template for any
  future vertical whose ISIC code spans more consignment/product
  categories than genuinely distinct regulatory regimes.
- `MemStore` || `DatomicStore` parity is proven by
  `test/provisiontrade/store_contract_test.clj`.
- The demo (`clojure -M:dev:run`) walks FOUR clean lifecycles (one food
  order, one non-alcoholic-beverage order, one alcoholic-beverage
  order, one tobacco order) end-to-end, plus every HARD-hold scenario
  (no spec-basis, credit-uncleared, contract-missing, food-safety-
  certificate-missing, alcohol-excise-license-missing, tobacco-excise-
  registration-missing, tobacco-age-verification-missing, sanctions,
  double delivery, double invoice).
- `blueprint.edn`'s `:robotics true` is a reasoned, vertical-specific
  call, documented in README and `docs/business-model.md`, not a
  default carried over from either extreme sibling precedent.

## References

- `cloud-itonami-isic-6511/docs/adr/0001-architecture.md` (origin of the
  general governed-actor architecture pattern)
- `cloud-itonami-isic-4671/docs/adr/0001-architecture.md` (fuel-
  wholesale sibling; origin of the sequential dual-actuation shape and
  the self-contained-domain-logic pattern this build follows most
  closely)
- `cloud-itonami-isic-4690/docs/adr/0001-architecture.md` (general-
  trading sibling; contrast: one uniform export-control check across
  UNRELATED commodity categories, vs. this vertical's three
  regulatory-class-gated checks across ONE ISIC code)
- `cloud-itonami-isic-4610/docs/adr/0001-architecture.md` (commission-
  brokerage sibling; origin of the 'a genuinely new regulatory concern
  gets its own named check' precedent this build's Decision 4 follows)
- `cloud-itonami-isic-4620/docs/adr/0001-architecture.md` (agri-
  wholesale sibling; origin of the fleet's first kind-gated certificate
  split -- this build's Decision 4 extends that two-way split to a
  three-way split with a many-to-one category mapping)
- `cloud-itonami-isic-0610/docs/adr/0001-architecture.md` (crude-
  extraction sibling; contrast: hosts pure physical range-check
  functions in its registry, which this vertical does NOT need)
- 食品衛生法 (Food Sanitation Act); 酒税法 (Liquor Tax Act); たばこ事業法
  (Tobacco Business Act) (Japan, MHLW/NTA/MOF)
- FDA Food Safety Modernization Act (Pub. L. 111-353); Federal Alcohol
  Administration Act (27 U.S.C. §201 et seq.); Family Smoking
  Prevention and Tobacco Control Act (Pub. L. 111-31) (US, FDA/TTB)
- Food Safety Act 1990; Alcoholic Liquor Duties Act 1979; Tobacco
  Products Duty Act 1979 (UK, FSA/HMRC)
- Regulation (EC) No 178/2002 (General Food Law); Council Directive
  92/83/EEC (alcohol excise structures); Directive 2011/64/EU (tobacco
  excise) (EU; Germany, BVL/Zoll/BMF)
