# Business Model: Wholesale of Food, Beverages and Tobacco

## Classification
- Repository: `cloud-itonami-isic-4630`
- ISIC Rev.5: `4630` — wholesale of food, beverages and tobacco
- Domain: `downstream/fbt-wholesale`
- Social impact: food safety, public health, transparency
- Governor: `:provision-trading-governor`
- License: AGPL-3.0-or-later

## Scope
This actor covers provision-order intake through per-jurisdiction,
per-regulatory-class contract / food-safety / alcohol-excise /
tobacco-excise / sanctions regulatory verification, delivery dispatch
(processed food, non-alcoholic beverage, alcoholic beverage or tobacco
product leaving the wholesale warehouse for a counterparty), and
invoice settlement (the money side of the trade, custody / financial
transfer) for a wholesaler of food, beverages and tobacco. It is
downstream of the agri-wholesale sibling `cloud-itonami-isic-4620`
(which trades RAW agricultural inputs and live animals): this vertical
trades PROCESSED, PACKAGED goods for onward wholesale-to-retail
distribution, not raw commodity inputs. It does **not**, by itself,
hold any food/beverage/tobacco wholesale licence, food-safety
inspection authority, liquor licence or tobacco retail/wholesale
authority required to run a food/beverage/tobacco wholesale business in
a given jurisdiction, perform the actual physical warehouse pick/pack/
palletize, or judge trading-book economics (inventory and route
optimization is a follow-up slice, not this R0). Whoever deploys a
live instance supplies the jurisdiction-specific operating authority,
the real warehouse-automation/ERP integrations, and bears that
jurisdiction's liability -- the software supplies the governed,
spec-cited, audited execution scaffold so the operator does not have to
build the compliance layer from scratch.

## Customer
- regional and independent food, beverage and tobacco wholesalers and
  distribution-center operators
- grocery, convenience-store and hospitality distributors leaving
  closed provision-trading / ERP SaaS
- liquor and tobacco wholesalers who need licence/excise-aware
  dispatch controls their generic warehouse-management system does not
  enforce
- counterparties, banks and regulators who need an auditable,
  spec-cited trade record

## Offer
- provision-order intake and directory management, across all four
  consignment categories (food, non-alcoholic beverage, alcoholic
  beverage, tobacco) in one system
- per-jurisdiction, per-regulatory-class contract / food-safety /
  alcohol-excise / tobacco-excise / sanctions regulatory verification
  with an official spec-basis citation
- delivery (warehouse dispatch) gated on full evidence, a credit-
  cleared counterparty, contract-terms on file, the category-
  appropriate regulatory clearance (food-safety certificate for food/
  non-alcoholic beverage, alcohol excise licence for alcoholic
  beverage, tobacco excise registration AND age-verification record
  for tobacco) and a passed sanctions screen
- invoice settlement (custody / financial transfer) with double-invoice
  prevention
- evidence checklisting (credit-clearance record, contract/PO,
  sanctions-screening record, category-specific certificate/licence/
  registration)
- sanctions and credit exception workflows
- role-based access and immutable audit ledger

## Revenue
- self-host setup fee
- managed hosting subscription per trader / warehouse
- support retainer with SLA
- ERP and accounts-receivable integration

| Package | Customer | Price shape |
|---|---|---|
| Self-host starter | wholesaler IT/operations lead | setup fee + optional support retainer |
| Managed Starter | one regional food/beverage/tobacco wholesaler, 1 拠点, 取引先 50–200 社 | ¥35,000/月 flat |

**Market-anchored (2026-08-10)**: benchmarked against 6 real products a
mid-size Japanese food/liquor wholesaler would actually evaluate, at an
assumed size of one distribution centre, 50–200 trading counterparties,
5–10 internal users and roughly ¥10M monthly trade volume. **4 of the 6
publish real numbers; 2 disclose nothing.**

Published:

- **Bカート** (Dai) — ライトプラン ¥9,800/月 (商品 500 / 会員 50), プラン10
  ¥19,800/月, プラン30 ¥29,800/月, プラン50 ¥39,800/月, プラン100 ¥49,800/月,
  プラン300 ¥79,800/月, 初期費用 ¥80,000 全プラン共通 (税別) —
  <https://bcart.jp/plan/>. At the assumed size this lands at
  **¥19,800–29,800/月**.
- **BtoBプラットフォーム受発注** (インフォマート), seller side — the pricing
  page itself shows no figures, but the 2024 料金改定 notice discloses the
  seller-side scheme as 「4段階従量制（上限は15万円）」 with a worked example of
  月間取引額 1,000万円 → 「月額使用料（従量_1 + 従量_2）＝57,500円（税別）」 —
  <https://corp.infomart.co.jp/news/20240215_5330/>. At the assumed size:
  **~¥57,500/月**.
- **Cin7 Core** — Standard $349/month (5 users, 6,000 sale orders/year),
  Pro $599/month, Advanced $1,199/month; Cin7 Omni is contact-sales —
  <https://www.cin7.com/pricing/>. At ~¥150/$: **¥52,350–89,850/月**.
- **FoodDocs** (food-safety / HACCP evidence) — Lite $99/site/月,
  Standard $199/site/月, Professional $299/site/月, Enterprise contact-sales —
  <https://www.fooddocs.com/pricing>. One site at Standard: **~¥29,850/月**.

Not disclosed (an observation, not a gap):

- **CO-NECT** (受注側) — 非公開. The plan page states only 「基本料金は取引先数、
  月間受注数に応じて変動します」 and 「ご契約期間は1年間、年間でのご一括でのお支払い」;
  the figures are an image and an individual quote —
  <https://biz.conct.jp/supplier/lp/plan/>.
- **TANOMU** (インフォマート) — 非公開. 「利用料金は初期費用と月額費用で構成されます」
  「月額料金は月の受注件数に応じて変動いたします」「詳しくはお問い合わせください」 —
  <https://www.infomart.co.jp/tanomu/price/index.asp>.

The measured band at the assumed size is therefore **¥19,800–89,850/月**.
**¥35,000/月 sits in the lower third of that band, and deliberately so.**
The top of the band belongs to products whose core value this actor does not
supply: Bカート's is a buyer-facing product catalogue, Cin7's is inventory and
purchasing, and インフォマート's seller-side fee is metered on trade value. This
actor holds no inventory ledger, no EC catalogue, no accounts posting, and
charges nothing on trade value — pricing at those levels would bill for
functions that are not shipped. It does, however, carry two things the
order-intake floor (¥19,800) does not: a per-jurisdiction check that branches
across three separate regulatory regimes by consignment category (food-safety
certificate, alcohol excise licence, tobacco excise registration +
age-verification, plus sanctions and credit clearance), and an immutable audit
ledger whose HOLDs a human approver cannot override. That is a superset of
what FoodDocs charges ~¥29,850/月/site for on food safety alone, which is why
¥35,000/月 sits just above it rather than at the ¥19,800 portal floor.

No portfolio-uniform ¥50,000–150,000/月 figure was inherited into this file,
and none was introduced: that range was anchored on HR/recruiting/CRM per-seat
SaaS and has no evidenced relationship to wholesale order-intake pricing.

**Subscribe (2026-08-10)**: a live Stripe Payment Link for the Managed Starter
tier (¥35,000/月 flat) is available now —
[**subscribe to Managed Starter**](https://buy.stripe.com/8x214o1IbgZW8PubaaeEo0b).
This is a no-code Stripe-hosted checkout; nothing in this repo's actor code
changed. **No wholesaler has claimed or subscribed to this tier yet — this is
a live, working checkout with zero paid tenants, not a claim of existing
revenue.**

## The `:provision-trading-governor` Decision Rule

This blueprint's `:itonami.blueprint/governor` is `:provision-trading-
governor`. It is the single authority that stands between "a
consignment could be dispatched to a counterparty" and "it is allowed
to leave the wholesale warehouse," and between "an invoice could be
settled" and "it is allowed to settle." Every rule it enforces is
traceable to the domain (Wholesale of Food, Beverages and Tobacco,
ISIC 4630) and to the three `:social-impact` tags in `blueprint.edn`
(`:food-safety`, `:public-health`, `:transparency`).

This is the rule the companion contract test
(`test/provisiontrade/governor_contract_test.clj`) encodes end-to-end:
the ProvisionTradeAdvisor never dispatches a consignment to a
counterparty or settles an invoice the Provision Trading Governor would
reject, `:delivery/dispatch` and `:invoice/settle` NEVER auto-commit at
any phase, `:order/intake` (no direct capital risk) MAY auto-commit
when clean, and every decision (commit OR hold) leaves exactly one
ledger fact.

**Authorizes a delivery (`:delivery/dispatch`) or invoice settlement
(`:invoice/settle`) only when ALL of the following hold:**

1. **An official spec-basis citation exists for the jurisdiction AND
   regulatory class** -- the governor will not authorize any
   `:regulatory/verify`, `:delivery/dispatch`, or `:invoice/settle`
   proposal whose jurisdiction has no entry in the `provisiontrade.facts`
   catalog (`:no-spec-basis`). This is the direct enforcement of
   `:transparency`: a jurisdiction whose food-safety/alcohol-excise/
   tobacco-excise/sanctions requirements cannot be traced to an
   OFFICIAL public source is never guessed. The advisor must not
   fabricate a jurisdiction's requirements.
2. **The jurisdiction's required evidence is fully on file, for THIS
   consignment's regulatory class** -- for a delivery or invoice the
   order's jurisdiction must have been verified with a complete
   evidence checklist on record: the credit-clearance record, the
   contract / purchase order, the sanctions-screening (OFAC /
   equivalent) record, and the regulatory-class-specific certificate/
   licence/registration (`:evidence-incomplete`). This protects
   `:food-safety` and `:public-health`: a consignment that cannot prove
   regulatory diligence never dispatches.
3. **The counterparty's credit has been cleared** -- the governor reads
   the dedicated `:credit-cleared?` fact on the order and refuses to
   dispatch when credit has NOT been cleared (the leasing collateral-
   coverage discipline, applied to counterparty credit)
   (`:credit-uncleared`). Evaluated at `:delivery/dispatch`.
4. **Contract-terms are on file** -- the governor refuses to dispatch
   when no `:contract-terms` are recorded for the order
   (`:contract-missing`). No food, beverage or tobacco consignment
   leaves the wholesale warehouse against an undocumented trade.
   Evaluated at `:delivery/dispatch`.
5. **A food-safety certificate is on file, for `:food` AND
   `:beverage-non-alcoholic` consignments** -- the governor reads the
   dedicated `:food-safety-certificate?` fact and refuses to dispatch a
   processed-food or non-alcoholic-beverage consignment with no
   HACCP/FSMA-style clearance (`:food-safety-certificate-missing`).
   This check is a NO-OP for `:beverage-alcoholic`/`:tobacco`
   consignments -- it is specific to the general food-safety regime.
   Evaluated at `:delivery/dispatch`.
6. **An alcohol excise licence is on file, for `:beverage-alcoholic`
   consignments** -- the governor reads the dedicated
   `:alcohol-excise-license?` fact and refuses to dispatch an
   alcoholic-beverage consignment with no excise/licensing clearance
   (`:alcohol-excise-license-missing`). This check is a NO-OP for
   `:food`/`:beverage-non-alcoholic`/`:tobacco` consignments --
   deliberately a SEPARATE check from #5 rather than one generic
   'certificate-missing' rule (see `docs/adr/0001-architecture.md`
   Decision 4). Evaluated at `:delivery/dispatch`.
7. **A tobacco excise registration AND an age-verification record are
   BOTH on file, for `:tobacco` consignments** -- the governor reads
   the dedicated `:tobacco-excise-registration?` AND
   `:age-verification-on-file?` facts and refuses to dispatch a tobacco
   consignment missing EITHER (`:tobacco-excise-age-verification-
   missing`). This check is a NO-OP for
   `:food`/`:beverage-non-alcoholic`/`:beverage-alcoholic`
   consignments -- deliberately a SEPARATE check from #5 and #6,
   folding two distinct real-world requirements (excise tax
   registration and retailer/purchaser age-of-sale diligence) into one
   named rule because a tobacco dispatch is unsafe if EITHER is
   missing. Evaluated at `:delivery/dispatch`.
8. **The counterparty has passed OFAC / equivalent sanctions screening**
   -- the governor reads the dedicated `:sanctions-screened?` fact and
   treats an unresolved sanctions-screening flag as a HARD, un-
   overridable hold (`:counterparty-sanctions-flag-unresolved`). Neither
   product nor money moves against an unscreened counterparty,
   regardless of consignment category. Evaluated UNCONDITIONALLY at
   both `:delivery/dispatch` and `:invoice/settle`.
9. **The order has not already been dispatched, and the invoice has not
   already been settled** -- a double delivery of the same order is
   refused off a dedicated `:dispatched?` fact, and a double invoice
   off a dedicated `:invoiced?` fact (never a `:status` value), the
   double-actuation guard every sibling actor in this fleet enforces
   (`:already-dispatched` / `:already-invoiced`).

**Rejects (HOLD, un-overridable, never even reaches a human) when any
of the above fail.** A proposal with no spec-basis, incomplete
evidence, an uncleared counterparty credit, no contract-terms on file,
a missing category-appropriate regulatory clearance, an unresolved
sanctions-screening flag, or a double delivery/invoice is held at the
governor node -- a human approver cannot override these, by
construction.

**Always escalates to a human (never auto-commits) for `:delivery/
dispatch` and `:invoice/settle`**, even when every check above is
clean. Dispatching a real consignment to a counterparty and settling a
real invoice (real money moving between counterparty and trader) are
the two real-world actuation events this actor performs; both are
always a human trading supervisor's call. This is enforced by TWO
independent layers that agree on purpose: the governor's confidence /
actuation SOFT gate (a `:delivery/dispatch` / `:invoice/settle` stake
always escalates) and `provisiontrade.phase`'s phase table, which never
puts either op in any phase's `:auto` set. The `:public-health` tag is
enforced upstream of the governor, in the regulatory-verification
evidence step and the Robotics Premise below -- the governor's job is
delivery/invoice authorization integrity, not trading-book
optimization.

## Required Technologies

`blueprint.edn`'s `:itonami.blueprint/required-technologies` for this
business, and what each one is actually load-bearing for here (not a
generic capability list):

| Technology | What it is FOR in Wholesale of Food, Beverages and Tobacco |
|---|---|
| `:robotics` | The autonomous warehouse pallet-picking/staging robot (AS/RS -- automated storage-and-retrieval system -- or AGV-class robotic forklift/case-picker) that performs the physical pick/pack/palletize/staging act at the wholesale warehouse, the point at which this actor's `:delivery/dispatch` occurs. The governor never dispatches hardware itself: a delivery-clearing action must have cleared the same sign-off a human trading supervisor would need (see Robotics Premise). |
| `:identity` | Trader, trading-supervisor, warehouse-operator and counterparty identity plus role-based access, so the governor's sign-off is tied to *who* authorized a delivery or invoice, not just *that* someone did. |
| `:forms` | Structured intake for provision-order booking, per-jurisdiction/per-regulatory-class evidence capture (credit-clearance record, contract/PO, sanctions-screening record, food-safety certificate / alcohol excise licence / tobacco excise registration + age-verification record), and sanctions / credit exception submission -- the data the Decision Rule above actually evaluates comes in through these forms. |
| `:dmn` | Encodes the `:provision-trading-governor` Decision Rule itself (spec-basis, evidence completeness, credit-clearance, contract-on-file, food-safety-certificate, alcohol-excise-license, tobacco-excise-age-verification, sanctions-screening, the double-actuation guards, the actuation gate) as an evaluable decision table rather than code buried in application logic -- this is what makes the governor auditable and swappable per-deployment. |
| `:bpmn` | Orchestrates the intake -> verify -> dispatch -> settle -> audit loop end-to-end (see `docs/operator-guide.md`) across provision-order intake, regulatory verification, delivery, and invoice settlement, including the sanctions / credit escalation gate. |
| `:audit-ledger` | The immutable record of every verification, delivery, invoice, sanctions flag, and hold -- this is what "an auditable, spec-cited trade record for every delivery and invoice" (Trust Controls, below) actually means in practice, and the evidence an operator needs if a delivery or an invoice is later disputed by a counterparty or regulator. |
| `:optimization` | Warehouse-slotting, inventory and route optimization -- selects the profitable fulfillment strategy for a distribution center. This R0 build deliberately scopes optimization OUT (see README `Business-process coverage`); the capability is correctly marked required, the integration is a follow-up slice. |

There is NO bespoke `:provisiontrade` capability library in this stack
(unlike the freight sibling's `:logistics`): the provision-trading
checks (credit-clearance, contract-on-file, food-safety certification,
alcohol excise licensing, tobacco excise registration/age-verification,
sanctions-screening) are direct entity boolean reads in
`provisiontrade.governor`, on top of the generic robotics/identity/
forms/dmn/bpmn/audit-ledger stack (see Capability layer).

## Trust Controls
- a jurisdiction (or regulatory class) with no official spec-basis can
  never be verified, dispatched, or invoiced against
- a delivery never starts with incomplete regulatory-diligence evidence
- a delivery never starts with an uncleared counterparty credit, no
  contract-terms on file, or a missing category-appropriate regulatory
  clearance (food-safety certificate for food/non-alcoholic beverage,
  alcohol excise licence for alcoholic beverage, tobacco excise
  registration AND age-verification record for tobacco)
- a delivery or invoice never settles against an unresolved
  sanctions-screening flag
- sanctions / credit / regulatory-clearance flags cannot be silently
  suppressed
- the same order can never be delivered or invoiced twice
- a delivery or invoice never auto-commits; both always need a human
  trading supervisor
- every delivery and invoice (commit OR hold) leaves exactly one
  immutable ledger fact
- counterparty, credit, regulatory-clearance and sanctions data stays
  outside Git

## Implementation notes (`:implemented`)

The Decision Rule above is implemented faithfully by
`provisiontrade.governor` as ten HARD checks (a human approver cannot
override them) plus one SOFT gate:

- `spec-basis-violations` -- the spec-basis check above, evaluated on
  every `:regulatory/verify`, `:delivery/dispatch`, and
  `:invoice/settle`.
- `evidence-incomplete-violations` -- the evidence-completeness check
  above, for `:delivery/dispatch` / `:invoice/settle`.
- `credit-uncleared-violations` -- the counterparty-credit check above
  (the leasing collateral-coverage discipline applied to counterparty
  credit); evaluated on every `:delivery/dispatch`.
- `contract-missing-violations` -- the contract-on-file check above;
  evaluated on every `:delivery/dispatch`.
- `food-safety-certificate-missing-violations` -- the food-safety
  certificate check above, gated on `:consignment-category`
  `#{:food :beverage-non-alcoholic}`; evaluated on every
  `:delivery/dispatch`. NO analog in the fuel-wholesale, general-
  trading or commission-brokerage siblings.
- `alcohol-excise-license-missing-violations` -- the alcohol excise
  licence check above, gated on `:consignment-category
  :beverage-alcoholic`; evaluated on every `:delivery/dispatch`.
  Deliberately a SEPARATE check from the one above -- see `docs/adr/
  0001-architecture.md` Decision 4 for why.
- `tobacco-excise-age-verification-missing-violations` -- the tobacco
  excise registration AND age-verification check above, gated on
  `:consignment-category :tobacco`; evaluated on every
  `:delivery/dispatch`. Deliberately a SEPARATE check from the two
  above, and folds two distinct real-world sub-requirements (excise
  registration, age-of-sale diligence) into one named rule.
- `counterparty-sanctions-flag-unresolved-violations` -- the sanctions-
  screening check above (the same open-flag-unresolved discipline the
  freight sibling's delivery-exception-unresolved check establishes);
  evaluated unconditionally on both `:delivery/dispatch` and
  `:invoice/settle`.
- `already-dispatched-violations` / `already-invoiced-violations` --
  the double-actuation guards above, off dedicated `:dispatched?` /
  `:invoiced?` booleans (never a `:status` value), the same discipline
  every sibling governor's guards establish.
- the confidence floor / actuation SOFT gate -- low confidence, OR a
  `:delivery/dispatch` / `:invoice/settle` stake, escalates to a human;
  and `provisiontrade.phase` independently never auto-commits either
  op at any phase.

Unlike the crude-extraction sibling's governor (which calls pure
physical range-check functions in its registry), this governor needs no
range-check functions at all: its domain checks read the
`provision-order` record's own dedicated booleans directly.
`:delivery/dispatch` and `:invoice/settle` are the two real-world
actuation events (`#{:delivery/dispatch :invoice/settle}`), applied
SEQUENTIALLY to the SAME provision-order (delivery first, invoice
settlement later), the same sequential dual-actuation shape the
fuel-wholesale, agri-wholesale, repair-shop, quarrying and crude-
extraction clusters use. Neither ever auto-commits at any phase.
Warehouse-slotting/route and trading-book optimization (the
`:optimization` line above) is a follow-up slice, not in this R0 build
-- see README `Business-process coverage`.

## Capability layer

Like the fuel-wholesale (`cloud-itonami-isic-4671`), general-trading
(`cloud-itonami-isic-4690`), commission-brokerage
(`cloud-itonami-isic-4610`) and agri-wholesale
(`cloud-itonami-isic-4620`) siblings, this vertical is SELF-CONTAINED:
there is no `kotoba-lang/provisiontrade` to delegate provision-trading
validation to. The credit-clearance / contract-on-file / food-safety-
certificate / alcohol-excise-license / tobacco-excise-registration /
age-verification / sanctions-screening checks live as direct entity
boolean reads in `provisiontrade.governor` (off dedicated
`:credit-cleared?` / `:contract-terms` / `:food-safety-certificate?` /
`:alcohol-excise-license?` / `:tobacco-excise-registration?` / `:age-
verification-on-file?` / `:sanctions-screened?` facts on the
`provision-order` record) -- this vertical's governor needs no pure
range-check functions at all, because its domain checks ARE direct
boolean reads.

## Jurisdiction coverage (honest)

`provisiontrade.facts/catalog` currently seeds 4 jurisdictions with an
official spec-basis FOR ALL THREE regulatory classes (food-safety,
alcohol-excise, tobacco-excise), each a REAL regime:

- **Japan (JPN)** -- 食品衛生法 (Food Sanitation Act) for food-safety,
  administered by 厚生労働省 (MHLW) with labeling functions at 消費者庁
  (Consumer Affairs Agency); 酒税法 (Liquor Tax Act) for alcohol
  excise/licensing, administered by 国税庁 (National Tax Agency, NTA);
  たばこ事業法 (Tobacco Business Act) plus 地方税法 (Local Tax Act) for
  tobacco excise, administered by 財務省 (Ministry of Finance).
- **United States (USA)** -- the FDA Food Safety Modernization Act
  (Pub. L. 111-353), amending the Federal Food, Drug, and Cosmetic Act
  (21 U.S.C. Chapter 9), for food-safety, administered by the FDA; the
  Federal Alcohol Administration Act (27 U.S.C. §201 et seq.) plus
  alcohol excise tax (26 U.S.C. §5001 et seq.) for alcohol excise/
  licensing, administered by the Alcohol and Tobacco Tax and Trade
  Bureau (TTB); the Family Smoking Prevention and Tobacco Control Act
  (Pub. L. 111-31; 21 U.S.C. §387 et seq.) plus tobacco excise tax (26
  U.S.C. §5701 et seq.) for tobacco excise/age-verification, jointly
  administered by the FDA Center for Tobacco Products and TTB.
- **United Kingdom (GBR)** -- the Food Safety Act 1990 for food-safety,
  administered by the Food Standards Agency (FSA); the Alcoholic Liquor
  Duties Act 1979 for alcohol excise, administered by HM Revenue &
  Customs (HMRC); the Tobacco Products Duty Act 1979 (tax) plus the
  Children and Young Persons (Protection from Tobacco) Act 1991 as
  amended (age-of-sale) for tobacco excise/age-verification, jointly
  administered by HMRC and Trading Standards.
- **Germany (DEU)**, representing the EU regime -- Regulation (EC) No
  178/2002 (General Food Law) for food-safety, enforced by the
  Bundesamt für Verbraucherschutz und Lebensmittelsicherheit (BVL)
  under BMEL; Council Directive 92/83/EEC (alcohol excise structures)
  as implemented by Germany's Alkoholsteuergesetz for alcohol excise;
  Directive 2011/64/EU (tobacco excise) as implemented by Germany's
  Tabaksteuergesetz for tobacco excise -- both German excise statutes
  administered by German Customs (Generalzolldirektion / Zoll) under
  BMF.

This is a starting catalog to prove the governor contract end-to-end,
not a claim of global coverage (4 of ~194 jurisdictions worldwide, each
covering 3 regulatory classes). I do not have live web access; the
statute/regulation names and owner-authority names are cited from
training-time knowledge with reasonable confidence for Japan (食品衛生法
/酒税法/たばこ事業法, MHLW/NTA/MOF), the EU regulations/directives
themselves (Regulation (EC) No 178/2002; Directives 92/83/EEC and
2011/64/EU), and the US agency structure (FDA; TTB; FDA Center for
Tobacco Products) -- but the exact US Code pin-cite for FSMA (cited
here only as amending 21 U.S.C. Chapter 9, deliberately not pinned to a
specific section number I am not confident about), the exact UK Act
titles (Alcoholic Liquor Duties Act 1979; Tobacco Products Duty Act
1979; Children and Young Persons (Protection from Tobacco) Act 1991),
and the exact German national implementing-act names (Alkoholsteuer-
gesetz; Tabaksteuergesetz, as distinct from the EU directives
themselves, which I am confident about) should be independently
verified before this catalog is relied on operationally. Adding a
jurisdiction (or a missing regulatory class for an already-seeded
jurisdiction) is additive: one map entry in
`provisiontrade.facts/catalog`, citing a real official source -- never
fabricate a jurisdiction's requirements to make coverage look bigger.

## Maturity

`:implemented` -- `ProvisionTradeAdvisor` + `Provision Trading
Governor` run as real, tested code, following the SAME governed-actor
architecture as the other prior actors across this fleet, with its own
distinct, independently-named governor and its own direct-entity-
boolean provision-trading checks -- including the fleet's first
THREE-way split of a single domain-defining check into category-gated
variants (extending the agri-wholesale sibling's two-way phytosanitary/
animal-health split). See `docs/adr/0001-architecture.md` for the
history and design.

## Robotics Premise

`blueprint.edn` sets `:itonami.blueprint/robotics true`. This is a
reasoned call, not a default carried over from a sibling: real modern
food/beverage/tobacco distribution centers already run substantial
warehouse automation -- automated storage-and-retrieval systems
(AS/RS), robotic case-picking and palletizing systems, and AGV-class
robotic forklifts are widely deployed in grocery and consumer-packaged-
goods 3PL/wholesale distribution (well-known real-world examples
include Symbotic-powered distribution centers used by major grocery
retailers, and Ocado's automated grocery fulfillment centers). An
autonomous warehouse pallet-picking/staging robot performs the physical
pick/pack/palletize/staging act at the wholesale warehouse -- the point
at which this actor's `:delivery/dispatch` occurs -- under the actor,
gated by the independent Provision Trading Governor. This is directly
analogous to the fuel-wholesale sibling's loading-rack/valve robot and
the agri-wholesale sibling's elevator-loadout robot: the robot's role
ends at the wholesaler's own dock/staging area, not at the final retail
destination. The long-haul carrier leg (the truck that actually drives
the consignment to the retailer) is explicitly OUT of scope for this
actor -- "hand off to a carrier" -- the same disclaimer every sibling's
scope section makes about the leg beyond its own physical dispatch
point.

Either way, the governor never dispatches hardware itself: a delivery-
clearing action must have cleared the same sign-off a human trading
supervisor would need. A robot may stage a pallet or close out a
loading-dock door, but only after the governor (every HARD check clean)
and a human supervisor both agree it is safe to -- the same
operating-state-machine-gated-by-governor premise every cloud-itonami
vertical restates (ADR-2607011000): the blueprint declares `:robotics
true`, the README names the robot that performs the physical act, and
the Provision Trading Governor is the independent gate that robot's
command must pass.
