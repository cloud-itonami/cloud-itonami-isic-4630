(ns provisiontrade.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [provisiontrade.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "JPN" (:jurisdiction (store/provision-order s "po-1"))))
      (is (= "Akita Provisions Wholesale Co" (:counterparty (store/provision-order s "po-1"))))
      (is (= :food (:consignment-category (store/provision-order s "po-1"))))
      (is (= "ATL" (:jurisdiction (store/provision-order s "po-2"))))
      (is (false? (:credit-cleared? (store/provision-order s "po-3"))) "po-3 credit not cleared")
      (is (nil? (:contract-terms (store/provision-order s "po-4"))) "po-4 no contract-terms")
      (is (false? (:sanctions-screened? (store/provision-order s "po-5"))) "po-5 sanctions not screened")
      (is (false? (:food-safety-certificate? (store/provision-order s "po-6"))) "po-6 no food-safety certificate")
      (is (= :beverage-alcoholic (:consignment-category (store/provision-order s "po-7"))) "po-7 is an alcoholic-beverage consignment")
      (is (false? (:alcohol-excise-license? (store/provision-order s "po-8"))) "po-8 no alcohol excise licence")
      (is (= :tobacco (:consignment-category (store/provision-order s "po-9"))) "po-9 is a tobacco consignment")
      (is (false? (:tobacco-excise-registration? (store/provision-order s "po-10"))) "po-10 no tobacco excise registration")
      (is (false? (:age-verification-on-file? (store/provision-order s "po-11"))) "po-11 no age-verification record")
      (is (= :beverage-non-alcoholic (:consignment-category (store/provision-order s "po-12"))) "po-12 is a non-alcoholic-beverage consignment")
      (is (false? (:dispatched? (store/provision-order s "po-1"))))
      (is (false? (:invoiced? (store/provision-order s "po-1"))))
      (is (= ["po-1" "po-10" "po-11" "po-12" "po-2" "po-3" "po-4" "po-5" "po-6" "po-7" "po-8" "po-9"]
             (mapv :id (store/all-provision-orders s))))
      (is (nil? (store/assessment-of s "po-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/delivery-history s)))
      (is (= [] (store/invoice-history s)))
      (is (zero? (store/next-delivery-sequence s "JPN")))
      (is (zero? (store/next-invoice-sequence s "JPN")))
      (is (false? (store/provision-order-already-dispatched? s "po-1")))
      (is (false? (store/provision-order-already-invoiced? s "po-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :order/upsert
                                 :value {:id "po-1" :counterparty "Akita Provisions Wholesale Co"}})
        (is (= "Akita Provisions Wholesale Co" (:counterparty (store/provision-order s "po-1"))))
        (is (= "JPN" (:jurisdiction (store/provision-order s "po-1"))) "unrelated field preserved"))
      (testing "regulatory-assessment payloads commit and read back"
        (store/commit-record! s {:effect :regulatory-assessment/set :path ["po-1"]
                                 :payload {:jurisdiction "JPN" :consignment-category :food :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :consignment-category :food :checklist ["a" "b"]} (store/assessment-of s "po-1"))))
      (testing "delivery drafts a record and advances the delivery sequence"
        (store/commit-record! s {:effect :order/mark-dispatched :path ["po-1"]})
        (is (= "JPN-DELIVERY-000000" (get (first (store/delivery-history s)) "record_id")))
        (is (= "provision-delivery-draft" (get (first (store/delivery-history s)) "kind")))
        (is (true? (:dispatched? (store/provision-order s "po-1"))))
        (is (= 1 (count (store/delivery-history s))))
        (is (= 1 (store/next-delivery-sequence s "JPN")))
        (is (true? (store/provision-order-already-dispatched? s "po-1"))))
      (testing "invoice settlement drafts a record and advances the invoice sequence"
        (store/commit-record! s {:effect :order/mark-invoiced :path ["po-1"]})
        (is (= "JPN-INVOICE-000000" (get (first (store/invoice-history s)) "record_id")))
        (is (= "provision-invoice-draft" (get (first (store/invoice-history s)) "kind")))
        (is (true? (:invoiced? (store/provision-order s "po-1"))))
        (is (= 1 (count (store/invoice-history s))))
        (is (= 1 (store/next-invoice-sequence s "JPN")))
        (is (true? (store/provision-order-already-invoiced? s "po-1"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/provision-order s "nope")))
    (is (= [] (store/all-provision-orders s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/delivery-history s)))
    (is (= [] (store/invoice-history s)))
    (is (zero? (store/next-delivery-sequence s "JPN")))
    (is (zero? (store/next-invoice-sequence s "JPN")))
    (store/with-provision-orders s {"x" {:id "x" :order-id "PO-X" :consignment-category :food
                                         :product-description "Shelf-stable canned vegetables (case pack)"
                                         :quantity 4000 :unit "cases"
                                         :counterparty "c" :price 18.75
                                         :contract-terms "FOB warehouse, net 30 days"
                                         :credit-cleared? true :sanctions-screened? true
                                         :food-safety-certificate? true
                                         :alcohol-excise-license? true
                                         :tobacco-excise-registration? true :age-verification-on-file? true
                                         :dispatched? false :invoiced? false
                                         :jurisdiction "JPN" :status :intake
                                         :dispatch-number nil :invoice-number nil}})
    (is (= "c" (:counterparty (store/provision-order s "x"))))))
