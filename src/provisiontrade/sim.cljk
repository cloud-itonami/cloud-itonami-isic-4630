(ns provisiontrade.sim
  "Demo driver -- `clojure -M:dev:run`. Walks FOUR clean provision-orders
  (one `:food` order, one `:beverage-non-alcoholic` order, one
  `:beverage-alcoholic` order, one `:tobacco` order) through intake ->
  regulatory verification -> delivery (escalate/approve/commit) ->
  invoice settlement (escalate/approve/commit), then shows every
  HARD-hold scenario: a jurisdiction with no spec-basis, a counterparty
  whose credit has not been cleared, an order with no contract-terms on
  file, a food order with no food-safety certificate on file, an
  alcoholic-beverage order with no alcohol excise licence on file, a
  tobacco order with no tobacco excise registration on file, a tobacco
  order with no age-verification record on file, a counterparty that has
  not passed sanctions screening, a double delivery, and a double
  invoice.

  Like every sibling actor's domain checks, this actor's checks
  (`credit-uncleared`, `contract-missing`, `food-safety-certificate-
  missing`, `alcohol-excise-license-missing`, `tobacco-excise-age-
  verification-missing`, `counterparty-sanctions-flag-unresolved`) are
  evaluated directly at `:delivery/dispatch` (and sanctions at
  `:invoice/settle` too) rather than via a separate screening op -- a
  real delivery decision validates counterparty credit, contract-on-
  file, regulatory clearance and sanctions screening at the point of the
  act itself, not as a discrete pre-screening ceremony. Each check is
  still exercised directly and independently below, one order per
  HARD-hold scenario, following the SAME 'exercise the failure mode
  directly, never only via a happy-path actuation' discipline
  `parksafety`'s ADR-2607071922 Decision 5 and every sibling since
  establish."
  (:require [langgraph.graph :as g]
            [provisiontrade.store :as store]
            [provisiontrade.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :trading-supervisor :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== order/intake po-1 (JPN, food, clean) ==")
    (println (exec-op actor "t1" {:op :order/intake :subject "po-1"
                                  :patch {:id "po-1" :counterparty "Akita Provisions Wholesale Co"}} operator))

    (println "== regulatory/verify po-1 (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :regulatory/verify :subject "po-1"} operator))
    (println (approve! actor "t2"))

    (println "== delivery/dispatch po-1 (always escalates -- :delivery/dispatch) ==")
    (let [r (exec-op actor "t3" {:op :delivery/dispatch :subject "po-1"} operator)]
      (println r)
      (println "-- human trading supervisor approves --")
      (println (approve! actor "t3")))

    (println "== invoice/settle po-1 (always escalates -- :invoice/settle) ==")
    (let [r (exec-op actor "t4" {:op :invoice/settle :subject "po-1"} operator)]
      (println r)
      (println "-- human trading supervisor approves --")
      (println (approve! actor "t4")))

    (println "== regulatory/verify po-2 (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t5" {:op :regulatory/verify :subject "po-2"} operator))

    (println "== regulatory/verify po-3 (escalates -- human approves; sets up the credit-uncleared test) ==")
    (println (exec-op actor "t6" {:op :regulatory/verify :subject "po-3"} operator))
    (println (approve! actor "t6"))

    (println "== delivery/dispatch po-3 (credit not cleared -> HARD hold) ==")
    (println (exec-op actor "t7" {:op :delivery/dispatch :subject "po-3"} operator))

    (println "== regulatory/verify po-4 (escalates -- human approves; sets up the contract-missing test) ==")
    (println (exec-op actor "t8" {:op :regulatory/verify :subject "po-4"} operator))
    (println (approve! actor "t8"))

    (println "== delivery/dispatch po-4 (no contract-terms on file -> HARD hold) ==")
    (println (exec-op actor "t9" {:op :delivery/dispatch :subject "po-4"} operator))

    (println "== regulatory/verify po-5 (escalates -- human approves; sets up the sanctions test) ==")
    (println (exec-op actor "t10" {:op :regulatory/verify :subject "po-5"} operator))
    (println (approve! actor "t10"))

    (println "== delivery/dispatch po-5 (sanctions screening not passed -> HARD hold) ==")
    (println (exec-op actor "t11" {:op :delivery/dispatch :subject "po-5"} operator))

    (println "== regulatory/verify po-6 (food; escalates -- human approves; sets up the food-safety-certificate-missing test) ==")
    (println (exec-op actor "t12" {:op :regulatory/verify :subject "po-6"} operator))
    (println (approve! actor "t12"))

    (println "== delivery/dispatch po-6 (no food-safety certificate on file -> HARD hold) ==")
    (println (exec-op actor "t13" {:op :delivery/dispatch :subject "po-6"} operator))

    (println "== order/intake po-7 (JPN, beverage-alcoholic, clean) ==")
    (println (exec-op actor "t14" {:op :order/intake :subject "po-7"
                                   :patch {:id "po-7" :counterparty "Golden Hills Spirits Traders"}} operator))

    (println "== regulatory/verify po-7 (alcoholic beverage; escalates -- human approves) ==")
    (println (exec-op actor "t15" {:op :regulatory/verify :subject "po-7"} operator))
    (println (approve! actor "t15"))

    (println "== delivery/dispatch po-7 (alcoholic beverage, clean -- always escalates) ==")
    (let [r (exec-op actor "t16" {:op :delivery/dispatch :subject "po-7"} operator)]
      (println r)
      (println "-- human trading supervisor approves --")
      (println (approve! actor "t16")))

    (println "== invoice/settle po-7 (alcoholic beverage, clean -- always escalates) ==")
    (let [r (exec-op actor "t17" {:op :invoice/settle :subject "po-7"} operator)]
      (println r)
      (println "-- human trading supervisor approves --")
      (println (approve! actor "t17")))

    (println "== regulatory/verify po-8 (alcoholic beverage; escalates -- human approves; sets up the alcohol-excise-license-missing test) ==")
    (println (exec-op actor "t18" {:op :regulatory/verify :subject "po-8"} operator))
    (println (approve! actor "t18"))

    (println "== delivery/dispatch po-8 (no alcohol excise licence on file -> HARD hold) ==")
    (println (exec-op actor "t19" {:op :delivery/dispatch :subject "po-8"} operator))

    (println "== order/intake po-9 (JPN, tobacco, clean) ==")
    (println (exec-op actor "t20" {:op :order/intake :subject "po-9"
                                   :patch {:id "po-9" :counterparty "Ironwood Tobacco Traders"}} operator))

    (println "== regulatory/verify po-9 (tobacco; escalates -- human approves) ==")
    (println (exec-op actor "t21" {:op :regulatory/verify :subject "po-9"} operator))
    (println (approve! actor "t21"))

    (println "== delivery/dispatch po-9 (tobacco, clean -- always escalates) ==")
    (let [r (exec-op actor "t22" {:op :delivery/dispatch :subject "po-9"} operator)]
      (println r)
      (println "-- human trading supervisor approves --")
      (println (approve! actor "t22")))

    (println "== invoice/settle po-9 (tobacco, clean -- always escalates) ==")
    (let [r (exec-op actor "t23" {:op :invoice/settle :subject "po-9"} operator)]
      (println r)
      (println "-- human trading supervisor approves --")
      (println (approve! actor "t23")))

    (println "== regulatory/verify po-10 (tobacco; escalates -- human approves; sets up the tobacco-excise-registration-missing test) ==")
    (println (exec-op actor "t24" {:op :regulatory/verify :subject "po-10"} operator))
    (println (approve! actor "t24"))

    (println "== delivery/dispatch po-10 (no tobacco excise registration on file -> HARD hold) ==")
    (println (exec-op actor "t25" {:op :delivery/dispatch :subject "po-10"} operator))

    (println "== regulatory/verify po-11 (tobacco; escalates -- human approves; sets up the age-verification-missing test) ==")
    (println (exec-op actor "t26" {:op :regulatory/verify :subject "po-11"} operator))
    (println (approve! actor "t26"))

    (println "== delivery/dispatch po-11 (no age-verification record on file -> HARD hold) ==")
    (println (exec-op actor "t27" {:op :delivery/dispatch :subject "po-11"} operator))

    (println "== order/intake po-12 (JPN, beverage-non-alcoholic, clean) ==")
    (println (exec-op actor "t28" {:op :order/intake :subject "po-12"
                                   :patch {:id "po-12" :counterparty "Lighthouse Beverage Traders"}} operator))

    (println "== regulatory/verify po-12 (non-alcoholic beverage; escalates -- human approves) ==")
    (println (exec-op actor "t29" {:op :regulatory/verify :subject "po-12"} operator))
    (println (approve! actor "t29"))

    (println "== delivery/dispatch po-12 (non-alcoholic beverage, clean -- shares the food-safety check, always escalates) ==")
    (let [r (exec-op actor "t30" {:op :delivery/dispatch :subject "po-12"} operator)]
      (println r)
      (println "-- human trading supervisor approves --")
      (println (approve! actor "t30")))

    (println "== invoice/settle po-12 (non-alcoholic beverage, clean -- always escalates) ==")
    (let [r (exec-op actor "t31" {:op :invoice/settle :subject "po-12"} operator)]
      (println r)
      (println "-- human trading supervisor approves --")
      (println (approve! actor "t31")))

    (println "== delivery/dispatch po-1 AGAIN (double-dispatch -> HARD hold) ==")
    (println (exec-op actor "t32" {:op :delivery/dispatch :subject "po-1"} operator))

    (println "== invoice/settle po-1 AGAIN (double-invoice -> HARD hold) ==")
    (println (exec-op actor "t33" {:op :invoice/settle :subject "po-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft provision-delivery records ==")
    (doseq [r (store/delivery-history db)] (println r))

    (println "== draft provision-invoice records ==")
    (doseq [r (store/invoice-history db)] (println r))))
