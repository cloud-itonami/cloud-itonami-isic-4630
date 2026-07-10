(ns provisiontrade.registry
  "Pure-function delivery + invoice record construction -- an
  append-only food/beverage/tobacco wholesale book-of-record draft.

  Like the fuel-wholesale and agri-wholesale siblings' own registries,
  this vertical's Provision Trading Governor needs NO registry range-
  check functions at all: its domain checks (credit-uncleared,
  contract-missing, food-safety-certificate-missing, alcohol-excise-
  license-missing, tobacco-excise-age-verification-missing,
  counterparty-sanctions-flag-unresolved) are direct entity boolean
  reads in `provisiontrade.governor`, off dedicated `:credit-cleared?` /
  `:contract-terms` / `:food-safety-certificate?` / `:alcohol-excise-
  license?` / `:tobacco-excise-registration?` / `:age-verification-on-
  file?` / `:sanctions-screened?` facts on the `provision-order` record.
  So this namespace is RECORD CONSTRUCTION ONLY -- no pure range checks
  to host here.

  Like every sibling actor's registry, there is no single international
  reference-number standard for a food/beverage/tobacco delivery or
  invoice record -- every operator/jurisdiction assigns its own
  reference format. This namespace does NOT invent one beyond a
  jurisdiction-scoped sequence number; it validates the record's
  required fields, the same honest, non-fabricating discipline
  `provisiontrade.facts` uses.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real warehouse-management/ERP system. It builds the
  RECORD an operator would keep, not the act of dispatching real food,
  beverage or tobacco product, or settling a real invoice itself (that
  is `provisiontrade.operation`'s `:delivery/dispatch`/
  `:invoice/settle`, always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the operator's act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

;; ----------------------------- record construction -----------------------------

(defn register-delivery-record
  "Validate + construct the PROVISION-DELIVERY registration DRAFT -- the
  operator's own legal act of dispatching real food, beverage or tobacco
  product to a counterparty (out of the wholesale warehouse). Pure
  function -- does not touch any real warehouse-management/ERP system;
  it builds the RECORD an operator would keep. `provisiontrade.governor`
  independently re-verifies the counterparty's credit-clearance,
  contract-on-file, regulatory-class-specific certificate/licence/
  registration and sanctions-screening ground truth, and blocks a
  double-delivery of the same provision-order, before this is ever
  allowed to commit."
  [provision-order-id jurisdiction sequence]
  (when-not (and provision-order-id (not= provision-order-id ""))
    (throw (ex-info "provision-delivery: provision_order_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "provision-delivery: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "provision-delivery: sequence must be >= 0" {})))
  (let [delivery-number (str (str/upper-case jurisdiction) "-DELIVERY-" (zero-pad sequence 6))
        record {"record_id" delivery-number
                "kind" "provision-delivery-draft"
                "provision_order_id" provision-order-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "delivery_number" delivery-number
     "certificate" (unsigned-certificate "ProvisionDelivery" delivery-number delivery-number)}))

(defn register-invoice-record
  "Validate + construct the PROVISION-INVOICE registration DRAFT -- the
  operator's own legal act of settling a real food/beverage/tobacco
  wholesale invoice (the money side of the trade, custody/financial
  transfer). Pure function -- does not touch any real billing or
  accounts-receivable system; it builds the RECORD an operator would
  keep. `provisiontrade.governor` independently re-verifies the
  sanctions-screening and evidence-completeness ground truth, and blocks
  a double-invoice of the same provision-order, before this is ever
  allowed to commit."
  [provision-order-id jurisdiction sequence]
  (when-not (and provision-order-id (not= provision-order-id ""))
    (throw (ex-info "provision-invoice: provision_order_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "provision-invoice: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "provision-invoice: sequence must be >= 0" {})))
  (let [invoice-number (str (str/upper-case jurisdiction) "-INVOICE-" (zero-pad sequence 6))
        record {"record_id" invoice-number
                "kind" "provision-invoice-draft"
                "provision_order_id" provision-order-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "invoice_number" invoice-number
     "certificate" (unsigned-certificate "ProvisionInvoice" invoice-number invoice-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
