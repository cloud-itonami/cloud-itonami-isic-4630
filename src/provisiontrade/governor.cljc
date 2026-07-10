(ns provisiontrade.governor
  "Provision Trading Governor -- the independent compliance layer that
  earns the ProvisionTradeAdvisor the right to commit. The LLM has no
  notion of jurisdictional food-safety / alcohol-excise / tobacco-excise
  / sanctions law, whether a counterparty's credit has actually been
  cleared, whether contract terms are actually on file, whether a REAL
  food-safety certificate, alcohol excise licence, or tobacco excise
  registration + age-verification record has actually been issued for
  THIS consignment, whether OFAC / equivalent sanctions screening has
  actually been passed, or when an act stops being a draft and becomes a
  real dispatch of food/beverage/tobacco product or a real invoice
  settlement, so this MUST be a separate system able to *reject* a
  proposal and fall back to HOLD.

  Like the fuel-wholesale and agri-wholesale siblings' own governors,
  this food/beverage/tobacco wholesale vertical has NO pre-existing
  provision-trading capability library to delegate to -- so the domain
  checks (credit-clearance, contract-on-file, food-safety certification,
  alcohol excise licensing, tobacco excise/age-verification, sanctions-
  screening) are direct entity boolean reads off the `provision-order`
  record, evaluated directly here, NOT delegated to a separate library's
  validated function.

  `:itonami.blueprint/governor` is `:provision-trading-governor`, grep-
  verified UNIQUE among the actor fleet repos checked out at build time
  -- no naming-collision precedent question, a fresh independent build
  following the SAME governed-actor architecture (langgraph StateGraph +
  independent Governor + Phase 0->3 rollout) established by
  `cloud-itonami-isic-6511` and applied by the fuel-wholesale
  (`cloud-itonami-isic-4671`), general-trading (`cloud-itonami-isic-4690`),
  commission-brokerage (`cloud-itonami-isic-4610`) and agri-wholesale
  (`cloud-itonami-isic-4620`) siblings.

  CRITICAL STRUCTURAL DIFFERENCE from every sibling above: ISIC 4630
  covers FOUR consignment categories under ONE classification code --
  food (`:food`), non-alcoholic beverages (`:beverage-non-alcoholic`),
  alcoholic beverages (`:beverage-alcoholic`) and tobacco (`:tobacco`) --
  but they are governed by only THREE genuinely different regulatory
  regimes (`provisiontrade.facts/regulatory-class-for` is deliberately
  MANY-TO-ONE: `:food` and `:beverage-non-alcoholic` share the SAME
  food-safety regime, because neither has a separate excise/licensing
  regime the way alcohol and tobacco do). This EXTENDS the agri-
  wholesale sibling's two-way phytosanitary/animal-health split
  (Decision 4 in that sibling's ADR) to a three-way split: food-safety-
  certificate-missing, alcohol-excise-license-missing, and tobacco-
  excise-age-verification-missing are modeled as THREE separate HARD
  checks, each gated on `:consignment-category`, rather than one generic
  'certificate-missing' check -- a single generic rule keyword would be
  ambiguous on the audit ledger about WHICH regime actually failed
  (food safety vs. alcohol excise vs. tobacco excise/age-verification
  are enforced by different statutes and, in every seeded jurisdiction,
  different agencies). See `provisiontrade.facts` for the per-
  jurisdiction, per-regulatory-class spec-basis catalog this triple of
  checks is grounded in.

  Ten checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them. The confidence/actuation gate is SOFT: it asks a
  human to look (low confidence / actuation), and the human may approve
  -- but see `provisiontrade.phase`: for `:stake :delivery/dispatch`/
  `:invoice/settle` (a real dispatch or invoice settlement) NO phase
  ever allows auto-commit either. Two independent layers agree that
  actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source
                                       (`provisiontrade.facts`), or
                                       invent one?
    2. Evidence incomplete         -- for `:delivery/dispatch`/
                                       `:invoice/settle`, has the
                                       jurisdiction actually been
                                       verified with a full evidence
                                       checklist on file, FOR THIS
                                       CONSIGNMENT'S REGULATORY CLASS?
    3. Credit uncleared            -- for `:delivery/dispatch`, the
                                       counterparty's credit has NOT been
                                       cleared (the leasing collateral-
                                       coverage discipline, applied to
                                       counterparty credit). Evaluated
                                       before dispatch.
    4. Contract missing            -- for `:delivery/dispatch`, no
                                       contract-terms are on file for the
                                       order. Evaluated before dispatch.
    5. Food-safety certificate
       missing                       -- for `:delivery/dispatch`, WHEN
                                       `:consignment-category :food` OR
                                       `:beverage-non-alcoholic`, no
                                       food-safety certificate is on
                                       file for the consignment -- a
                                       processed food or non-alcoholic
                                       beverage cargo never leaves the
                                       warehouse without HACCP/FSMA-style
                                       clearance. NO analog in the fuel-
                                       wholesale, general-trading or
                                       commission-brokerage siblings.
    6. Alcohol excise licence
       missing                       -- for `:delivery/dispatch`, WHEN
                                       `:consignment-category
                                       :beverage-alcoholic`, no alcohol
                                       excise licence is on file for the
                                       consignment -- an alcoholic-
                                       beverage cargo never leaves the
                                       warehouse without excise/licensing
                                       clearance. Deliberately a SEPARATE
                                       check from #5, not a shared
                                       generic 'certificate-missing'
                                       rule -- see namespace docstring.
    7. Tobacco excise / age-
       verification missing          -- for `:delivery/dispatch`, WHEN
                                       `:consignment-category :tobacco`,
                                       either the tobacco excise
                                       registration OR the retailer/
                                       purchaser age-verification record
                                       is missing -- a tobacco cargo
                                       never leaves the warehouse without
                                       BOTH excise registration AND
                                       age-of-sale diligence. Deliberately
                                       a SEPARATE check from #5 and #6.
    8. Counterparty sanctions flag
       unresolved                    -- for `:delivery/dispatch` and
                                       `:invoice/settle`, the counterparty
                                       has NOT passed OFAC / equivalent
                                       sanctions screening -- a HARD,
                                       un-overridable hold. Evaluated
                                       UNCONDITIONALLY at both actuation
                                       ops.
    9. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:delivery/dispatch`/
                                       `:invoice/settle` (REAL acts)
                                       -> escalate.

  Two more guards, double-delivery/double-invoice prevention, are
  enforced but NOT listed as numbered HARD checks above because they
  need no upstream comparison at all -- `already-dispatched-violations`/
  `already-invoiced-violations` refuse to dispatch/invoice the SAME
  provision-order twice, off dedicated `:dispatched?`/`:invoiced?` facts
  (never a `:status` value) -- the SAME 'check a dedicated boolean, not
  status' discipline every prior governor's guards establish, informed
  by `cloud-itonami-isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [provisiontrade.facts :as facts]
            [provisiontrade.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Dispatching real food, beverages or tobacco product to a counterparty
  (product leaving the wholesale warehouse) and settling a real invoice
  (real money moving between counterparty and trader) are the two
  real-world actuation events this actor performs -- a two-member set,
  matching every sibling's own dual-actuation shape."
  #{:delivery/dispatch :invoice/settle})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:regulatory/verify` (or `:delivery/dispatch`/`:invoice/settle`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's food-safety / alcohol-excise / tobacco-excise
  / sanctions requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:regulatory/verify :delivery/dispatch :invoice/settle} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:delivery/dispatch`/`:invoice/settle`, the jurisdiction's
  required evidence (credit-clearance record, contract/PO, sanctions-
  screening record, PLUS the regulatory-class-specific certificate/
  licence/registration) must actually be satisfied -- do not trust the
  advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:delivery/dispatch :invoice/settle} op)
    (let [po (store/provision-order st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction po) (:consignment-category po) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域・品目区分の必要書類(信用審査記録/契約書またはPO/制裁スクリーニング記録/規制別証憑)が充足していない状態での提案"}]))))

(defn- credit-uncleared-violations
  "For `:delivery/dispatch`, refuses to dispatch to a counterparty whose
  credit has NOT been cleared -- counterparty credit not cleared (the
  leasing collateral-coverage discipline, applied to counterparty
  credit). Evaluated ahead of any physical warehouse pick/pack/palletize."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (let [po (store/provision-order st subject)]
      (when (not (true? (:credit-cleared? po)))
        [{:rule :credit-uncleared
          :detail (str subject " の取引先信用審査(credit-clearance)が未了 -- 出荷提案は進められない")}]))))

(defn- contract-missing-violations
  "For `:delivery/dispatch`, refuses to dispatch when no contract-terms
  are on file for the order."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (let [po (store/provision-order st subject)]
      (when (or (nil? (:contract-terms po)) (= "" (:contract-terms po)))
        [{:rule :contract-missing
          :detail (str subject " に契約条項(contract-terms)の記録が無い -- 出荷提案は進められない")}]))))

(defn- food-safety-certificate-missing-violations
  "For `:delivery/dispatch`, WHEN `:consignment-category :food` OR
  `:beverage-non-alcoholic`, refuses to dispatch a processed-food or
  non-alcoholic-beverage consignment with no food-safety certificate on
  file. This is the check with NO analog in the fuel-wholesale,
  general-trading or commission-brokerage siblings' governors -- it is
  this vertical's own defining food-safety content, deliberately
  SEPARATE from `alcohol-excise-license-missing-violations` and
  `tobacco-excise-age-verification-missing-violations` (see namespace
  docstring): processed food/non-alcoholic beverages, alcohol and
  tobacco are three genuinely different regulatory regimes, even though
  all four consignment categories arrive at the same ISIC 4630 wholesale
  order."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (let [po (store/provision-order st subject)]
      (when (and (contains? #{:food :beverage-non-alcoholic} (:consignment-category po))
                 (not (true? (:food-safety-certificate? po))))
        [{:rule :food-safety-certificate-missing
          :detail (str subject " (食品/清涼飲料水荷口)の食品衛生証明書(food-safety certificate)が未取得 -- 出荷提案は進められない")}]))))

(defn- alcohol-excise-license-missing-violations
  "For `:delivery/dispatch`, WHEN `:consignment-category
  :beverage-alcoholic`, refuses to dispatch an alcoholic-beverage
  consignment with no alcohol excise licence on file. This is the check
  with NO analog in the fuel-wholesale, general-trading or commission-
  brokerage siblings' governors -- it is this vertical's own defining
  alcohol-excise/licensing content, deliberately SEPARATE from
  `food-safety-certificate-missing-violations` and
  `tobacco-excise-age-verification-missing-violations` (see namespace
  docstring): an alcoholic-beverage consignment's regulatory gate is
  excise taxation and wholesale/retail licensing (酒税法-style regimes),
  not food-safety inspection."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (let [po (store/provision-order st subject)]
      (when (and (= :beverage-alcoholic (:consignment-category po))
                 (not (true? (:alcohol-excise-license? po))))
        [{:rule :alcohol-excise-license-missing
          :detail (str subject " (酒類荷口)の酒税免許(alcohol excise licence)が未取得 -- 出荷提案は進められない")}]))))

(defn- tobacco-excise-age-verification-missing-violations
  "For `:delivery/dispatch`, WHEN `:consignment-category :tobacco`,
  refuses to dispatch a tobacco consignment unless BOTH the tobacco
  excise registration AND the retailer/purchaser age-verification record
  are on file. This is the check with NO analog in the fuel-wholesale,
  general-trading or commission-brokerage siblings' governors -- it is
  this vertical's own defining tobacco-excise-and-age-verification
  content, deliberately SEPARATE from
  `food-safety-certificate-missing-violations` and
  `alcohol-excise-license-missing-violations` (see namespace docstring):
  a tobacco consignment's regulatory gate is BOTH excise taxation AND
  the retailer/purchaser age-of-sale diligence real tobacco-control law
  (e.g. the US Tobacco Control Act's age-of-sale provisions; the UK's
  Children and Young Persons (Protection from Tobacco) Act) demands --
  two distinct real-world requirements folded into one governor check
  because a tobacco dispatch is unsafe if EITHER is missing."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (let [po (store/provision-order st subject)]
      (when (= :tobacco (:consignment-category po))
        (let [excise-ok? (true? (:tobacco-excise-registration? po))
              age-ok? (true? (:age-verification-on-file? po))]
          (when-not (and excise-ok? age-ok?)
            [{:rule :tobacco-excise-age-verification-missing
              :detail (str subject " (たばこ荷口)の"
                          (cond
                            (and (not excise-ok?) (not age-ok?)) "たばこ税登録および年齢確認記録"
                            (not excise-ok?) "たばこ税登録"
                            :else "購入者/小売業者の年齢確認記録")
                          "が未取得 -- 出荷提案は進められない")}]))))))

(defn- counterparty-sanctions-flag-unresolved-violations
  "For `:delivery/dispatch` and `:invoice/settle`, an unresolved
  sanctions-screening flag -- the counterparty has NOT passed OFAC /
  equivalent sanctions screening -- is a HARD, un-overridable hold.
  Evaluated UNCONDITIONALLY at both actuation ops: neither product nor
  money moves against an unscreened counterparty, regardless of
  consignment category."
  [{:keys [op subject]} st]
  (when (contains? #{:delivery/dispatch :invoice/settle} op)
    (let [po (store/provision-order st subject)]
      (when (not (true? (:sanctions-screened? po)))
        [{:rule :counterparty-sanctions-flag-unresolved
          :detail (str subject " の取引先制裁スクリーニング(OFAC等)が未了 -- 出荷・請求提案は進められない")}]))))

(defn- already-dispatched-violations
  "For `:delivery/dispatch`, refuses to dispatch the SAME provision-order
  twice, off a dedicated `:dispatched?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (when (store/provision-order-already-dispatched? st subject)
      [{:rule :already-dispatched
        :detail (str subject " は既に出荷済み")}])))

(defn- already-invoiced-violations
  "For `:invoice/settle`, refuses to settle the SAME provision-order's
  invoice twice, off a dedicated `:invoiced?` fact (never a `:status`
  value)."
  [{:keys [op subject]} st]
  (when (= op :invoice/settle)
    (when (store/provision-order-already-invoiced? st subject)
      [{:rule :already-invoiced
        :detail (str subject " は既に請求済み")}])))

(defn check
  "Censors a ProvisionTradeAdvisor proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (credit-uncleared-violations request st)
                           (contract-missing-violations request st)
                           (food-safety-certificate-missing-violations request st)
                           (alcohol-excise-license-missing-violations request st)
                           (tobacco-excise-age-verification-missing-violations request st)
                           (counterparty-sanctions-flag-unresolved-violations request st)
                           (already-dispatched-violations request st)
                           (already-invoiced-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
