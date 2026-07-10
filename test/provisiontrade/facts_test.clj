(ns provisiontrade.facts-test
  (:require [clojure.test :refer [deftest is]]
            [provisiontrade.facts :as facts]))

(deftest jpn-has-a-spec-basis-for-all-three-regulatory-classes
  (is (some? (facts/spec-basis "JPN" :food)))
  (is (some? (facts/spec-basis "JPN" :beverage-non-alcoholic)))
  (is (some? (facts/spec-basis "JPN" :beverage-alcoholic)))
  (is (some? (facts/spec-basis "JPN" :tobacco)))
  (is (string? (:provenance (facts/spec-basis "JPN" :food))))
  (is (string? (:provenance (facts/spec-basis "JPN" :beverage-alcoholic))))
  (is (string? (:provenance (facts/spec-basis "JPN" :tobacco)))))

(deftest food-and-non-alcoholic-beverage-share-the-same-regulatory-class
  (is (= (facts/spec-basis "JPN" :food) (facts/spec-basis "JPN" :beverage-non-alcoholic))
      "food and non-alcoholic beverages are both regulated under the SAME food-safety regime")
  (is (= :food-safety (get facts/regulatory-class-for :food)))
  (is (= :food-safety (get facts/regulatory-class-for :beverage-non-alcoholic))))

(deftest alcohol-and-tobacco-are-genuinely-different-entries-from-food-safety
  (is (not= (:legal-basis (facts/spec-basis "JPN" :food))
            (:legal-basis (facts/spec-basis "JPN" :beverage-alcoholic)))
      "food-safety law and alcohol-excise law are different statutes even in the same jurisdiction")
  (is (not= (:legal-basis (facts/spec-basis "JPN" :food))
            (:legal-basis (facts/spec-basis "JPN" :tobacco)))
      "food-safety law and tobacco-excise law are different statutes even in the same jurisdiction")
  (is (not= (:legal-basis (facts/spec-basis "JPN" :beverage-alcoholic))
            (:legal-basis (facts/spec-basis "JPN" :tobacco)))
      "alcohol-excise law and tobacco-excise law are different statutes even in the same jurisdiction")
  (is (not= (facts/spec-basis "USA" :food) (facts/spec-basis "USA" :beverage-alcoholic)))
  (is (not= (facts/spec-basis "USA" :beverage-alcoholic) (facts/spec-basis "USA" :tobacco))))

(deftest all-four-seeded-jurisdictions-have-required-evidence-for-every-category
  ;; every seeded food/beverage/tobacco wholesale jurisdiction actually
  ;; has a real required-evidence set reported honestly here, for EVERY
  ;; consignment category
  (doseq [iso3 ["JPN" "USA" "GBR" "DEU"]
          category [:food :beverage-non-alcoholic :beverage-alcoholic :tobacco]]
    (is (seq (facts/evidence-checklist iso3 category)) (str iso3 "/" category " required-evidence"))))

(deftest food-checklist-cites-food-safety-not-alcohol-or-tobacco-certificate
  (doseq [iso3 ["JPN" "USA" "GBR" "DEU"]]
    (is (some #(re-find #"(?i)food-safety" %) (facts/evidence-checklist iso3 :food))
        (str iso3 " :food checklist should require a food-safety certificate"))
    (is (not-any? #(re-find #"(?i)alcohol excise|tobacco excise" %) (facts/evidence-checklist iso3 :food))
        (str iso3 " :food checklist should not require alcohol/tobacco excise evidence"))))

(deftest alcohol-checklist-cites-alcohol-excise-not-food-safety-or-tobacco
  (doseq [iso3 ["JPN" "USA" "GBR" "DEU"]]
    (is (some #(re-find #"(?i)alcohol excise" %) (facts/evidence-checklist iso3 :beverage-alcoholic))
        (str iso3 " :beverage-alcoholic checklist should require an alcohol excise licence"))
    (is (not-any? #(re-find #"(?i)food-safety|tobacco excise" %) (facts/evidence-checklist iso3 :beverage-alcoholic))
        (str iso3 " :beverage-alcoholic checklist should not require food-safety/tobacco-excise evidence"))))

(deftest tobacco-checklist-cites-tobacco-excise-and-age-verification-not-food-or-alcohol
  (doseq [iso3 ["JPN" "USA" "GBR" "DEU"]]
    (is (some #(re-find #"(?i)tobacco excise" %) (facts/evidence-checklist iso3 :tobacco))
        (str iso3 " :tobacco checklist should require a tobacco excise registration"))
    (is (some #(re-find #"(?i)age-verification" %) (facts/evidence-checklist iso3 :tobacco))
        (str iso3 " :tobacco checklist should require an age-verification record"))
    (is (not-any? #(re-find #"(?i)food-safety|alcohol excise" %) (facts/evidence-checklist iso3 :tobacco))
        (str iso3 " :tobacco checklist should not require food-safety/alcohol-excise evidence"))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL" :food)))
  (is (nil? (facts/spec-basis "ATL" :beverage-alcoholic)))
  (is (nil? (facts/spec-basis "ATL" :tobacco))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "GBR"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["GBR" "JPN"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item-and-respects-category
  (let [food-all (facts/evidence-checklist "JPN" :food)
        alcohol-all (facts/evidence-checklist "JPN" :beverage-alcoholic)
        tobacco-all (facts/evidence-checklist "JPN" :tobacco)]
    (is (facts/required-evidence-satisfied? "JPN" :food food-all))
    (is (not (facts/required-evidence-satisfied? "JPN" :food (rest food-all))))
    (is (not (facts/required-evidence-satisfied? "JPN" :food alcohol-all))
        "the alcohol-excise checklist does not satisfy the food-safety checklist -- the certificates are not interchangeable")
    (is (not (facts/required-evidence-satisfied? "JPN" :food tobacco-all))
        "the tobacco-excise checklist does not satisfy the food-safety checklist either")
    (is (not (facts/required-evidence-satisfied? "ATL" :food food-all)) "no spec-basis -> never satisfied")))
