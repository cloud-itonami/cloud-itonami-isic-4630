(ns provisiontrade.governor-contract-test
  "The governor contract as executable tests. The single invariant
  under test:

    ProvisionTradeAdvisor never dispatches food/beverage/tobacco
    product, or settles an invoice, the Provision Trading Governor
    would reject, `:delivery/dispatch`/`:invoice/settle` NEVER
    auto-commit at any phase, `:order/intake` (no direct capital risk)
    MAY auto-commit when clean, and every decision (commit OR hold)
    leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [provisiontrade.store :as store]
            [provisiontrade.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :trading-supervisor :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through regulatory verify -> approve, leaving a
  regulatory assessment on file. Uses distinct thread-ids per call site
  by suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :regulatory/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :order/intake :subject "po-1"
                   :patch {:id "po-1" :counterparty "Akita Provisions Wholesale Co"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Akita Provisions Wholesale Co" (:counterparty (store/provision-order db "po-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest regulatory-verify-always-needs-approval
  (testing "regulatory verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :regulatory/verify :subject "po-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "po-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a regulatory/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :regulatory/verify :subject "po-2"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "po-2")) "no assessment written"))))

(deftest delivery-without-assessment-is-held
  (testing "delivery/dispatch before any regulatory verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :delivery/dispatch :subject "po-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest credit-uncleared-is-held-and-unoverridable
  (testing "a counterparty whose credit has not been cleared -> HOLD, and never reaches request-approval -- the leasing collateral-coverage discipline applied to counterparty credit"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "po-3")
          res (exec-op actor "t5" {:op :delivery/dispatch :subject "po-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:credit-uncleared} (-> (store/ledger db) last :basis)))
      (is (empty? (store/delivery-history db))))))

(deftest contract-missing-is-held-and-unoverridable
  (testing "an order with no contract-terms on file -> HOLD, and never reaches request-approval"
    (let [[db actor] (fresh)
          _ (verify! actor "t6pre" "po-4")
          res (exec-op actor "t6" {:op :delivery/dispatch :subject "po-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:contract-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/delivery-history db))))))

(deftest counterparty-sanctions-flag-unresolved-is-held-and-unoverridable
  (testing "a counterparty that has not passed OFAC / equivalent sanctions screening -> HOLD, and never reaches request-approval (evaluated at both delivery and invoice)"
    (let [[db actor] (fresh)
          _ (verify! actor "t7pre" "po-5")
          res (exec-op actor "t7" {:op :delivery/dispatch :subject "po-5"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:counterparty-sanctions-flag-unresolved} (-> (store/ledger db) last :basis)))
      (is (empty? (store/delivery-history db))))))

(deftest food-safety-certificate-missing-is-held-and-unoverridable
  (testing "a :food consignment with no food-safety certificate on file -> HOLD, and never reaches request-approval"
    (let [[db actor] (fresh)
          _ (verify! actor "t8pre" "po-6")
          res (exec-op actor "t8" {:op :delivery/dispatch :subject "po-6"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:food-safety-certificate-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/delivery-history db))))))

(deftest alcohol-excise-license-missing-is-held-and-unoverridable
  (testing "a :beverage-alcoholic consignment with no alcohol excise licence on file -> HOLD, and never reaches request-approval"
    (let [[db actor] (fresh)
          _ (verify! actor "t9pre" "po-8")
          res (exec-op actor "t9" {:op :delivery/dispatch :subject "po-8"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:alcohol-excise-license-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/delivery-history db))))))

(deftest tobacco-excise-registration-missing-is-held-and-unoverridable
  (testing "a :tobacco consignment with no tobacco excise registration on file -> HOLD, and never reaches request-approval"
    (let [[db actor] (fresh)
          _ (verify! actor "t10pre" "po-10")
          res (exec-op actor "t10" {:op :delivery/dispatch :subject "po-10"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:tobacco-excise-age-verification-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/delivery-history db))))))

(deftest tobacco-age-verification-missing-is-held-and-unoverridable
  (testing "a :tobacco consignment with no age-verification record on file (but excise registration present) -> HOLD, and never reaches request-approval"
    (let [[db actor] (fresh)
          _ (verify! actor "t11pre" "po-11")
          res (exec-op actor "t11" {:op :delivery/dispatch :subject "po-11"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:tobacco-excise-age-verification-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/delivery-history db))))))

(deftest food-order-never-tripped-by-alcohol-or-tobacco-checks
  (testing "a :food consignment's delivery is never held by alcohol-excise-license-missing or tobacco-excise-age-verification-missing, even though it has no alcohol/tobacco facts set to true on purpose"
    (let [[db actor] (fresh)
          _ (verify! actor "t12pre" "po-1")
          res (exec-op actor "t12" {:op :delivery/dispatch :subject "po-1"} operator)]
      (is (= :interrupted (:status res)) "pauses for human approval -- governor is clean")
      (is (not (some #{:alcohol-excise-license-missing :tobacco-excise-age-verification-missing}
                     (-> (store/ledger db) last :basis)))))))

(deftest alcohol-order-never-tripped-by-food-safety-or-tobacco-checks
  (testing "a :beverage-alcoholic consignment's delivery is never held by food-safety-certificate-missing or tobacco-excise-age-verification-missing, even though it has no :food-safety-certificate? set"
    (let [[db actor] (fresh)
          _ (verify! actor "t13pre" "po-7")
          res (exec-op actor "t13" {:op :delivery/dispatch :subject "po-7"} operator)]
      (is (= :interrupted (:status res)) "pauses for human approval -- governor is clean")
      (is (not (some #{:food-safety-certificate-missing :tobacco-excise-age-verification-missing}
                     (-> (store/ledger db) last :basis)))))))

(deftest tobacco-order-never-tripped-by-food-safety-or-alcohol-checks
  (testing "a :tobacco consignment's delivery is never held by food-safety-certificate-missing or alcohol-excise-license-missing, even though it has no :food-safety-certificate?/:alcohol-excise-license? set"
    (let [[db actor] (fresh)
          _ (verify! actor "t14pre" "po-9")
          res (exec-op actor "t14" {:op :delivery/dispatch :subject "po-9"} operator)]
      (is (= :interrupted (:status res)) "pauses for human approval -- governor is clean")
      (is (not (some #{:food-safety-certificate-missing :alcohol-excise-license-missing}
                     (-> (store/ledger db) last :basis)))))))

(deftest non-alcoholic-beverage-order-shares-the-food-safety-check-not-a-separate-one
  (testing "a :beverage-non-alcoholic consignment's delivery is gated by the SAME food-safety-certificate-missing check as :food, never by alcohol/tobacco checks"
    (let [[db actor] (fresh)
          _ (verify! actor "t15pre" "po-12")
          res (exec-op actor "t15" {:op :delivery/dispatch :subject "po-12"} operator)]
      (is (= :interrupted (:status res)) "pauses for human approval -- governor is clean")
      (is (not (some #{:alcohol-excise-license-missing :tobacco-excise-age-verification-missing}
                     (-> (store/ledger db) last :basis)))))))

(deftest delivery-dispatch-always-escalates-then-human-decides
  (testing "a clean, fully-verified, credit-cleared, contract-on-file, certificate-on-file, sanctions-screened order still ALWAYS interrupts for human approval -- :delivery/dispatch is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t16pre" "po-1")
          r1 (exec-op actor "t16" {:op :delivery/dispatch :subject "po-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, delivery record drafted"
        (let [r2 (approve! actor "t16")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:dispatched? (store/provision-order db "po-1"))))
          (is (= 1 (count (store/delivery-history db))) "one draft delivery record"))))))

(deftest invoice-settle-always-escalates-then-human-decides
  (testing "a clean, fully-verified, already-dispatched order still ALWAYS interrupts for human approval -- :invoice/settle is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t17pre" "po-1")
          _ (exec-op actor "t17deliver" {:op :delivery/dispatch :subject "po-1"} operator)
          _ (approve! actor "t17deliver")
          r1 (exec-op actor "t17" {:op :invoice/settle :subject "po-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, invoice record drafted"
        (let [r2 (approve! actor "t17")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:invoiced? (store/provision-order db "po-1"))))
          (is (= 1 (count (store/invoice-history db))) "one draft invoice record"))))))

(deftest delivery-dispatch-double-dispatch-is-held
  (testing "dispatching the same provision-order twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t18pre" "po-1")
          _ (exec-op actor "t18a" {:op :delivery/dispatch :subject "po-1"} operator)
          _ (approve! actor "t18a")
          res (exec-op actor "t18" {:op :delivery/dispatch :subject "po-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-dispatched} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/delivery-history db))) "still only the one earlier delivery"))))

(deftest invoice-settle-double-invoice-is-held
  (testing "settling the same provision-order's invoice twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t19pre" "po-1")
          _ (exec-op actor "t19deliver" {:op :delivery/dispatch :subject "po-1"} operator)
          _ (approve! actor "t19deliver")
          _ (exec-op actor "t19a" {:op :invoice/settle :subject "po-1"} operator)
          _ (approve! actor "t19a")
          res (exec-op actor "t19" {:op :invoice/settle :subject "po-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-invoiced} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/invoice-history db))) "still only the one earlier invoice"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :order/intake :subject "po-1"
                          :patch {:id "po-1" :counterparty "Akita Provisions Wholesale Co"}} operator)
      (exec-op actor "b" {:op :regulatory/verify :subject "po-2"} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
