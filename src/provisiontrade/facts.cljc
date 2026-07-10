(ns provisiontrade.facts
  "Per-jurisdiction, per-regulatory-class catalog -- the G2-style
  spec-basis table the Provision Trading Governor checks every
  `:regulatory/verify` proposal against ('did the advisor cite an
  OFFICIAL public source for this jurisdiction's food-safety /
  alcohol-excise / tobacco-excise requirements, or did it invent one?').

  Wholesale of food, beverages and tobacco (ISIC 4630) is downstream of
  the agri-wholesale sibling `cloud-itonami-isic-4620` (which trades RAW
  agricultural inputs and live animals): this vertical trades PROCESSED,
  PACKAGED goods -- food, beverages (alcoholic and non-alcoholic) and
  tobacco products -- for onward wholesale-to-retail distribution. Its
  defining regulatory exposure is genuinely THREE-WAY, not one uniform
  regime: food-safety compliance for processed/packaged food and
  non-alcoholic beverages (HACCP/FSMA-style), alcohol excise/licensing
  for alcoholic beverages, and tobacco excise/age-verification for
  tobacco products -- three different statutes, and in every seeded
  jurisdiction below, three different administering agencies. See
  `provisiontrade.governor`'s `food-safety-certificate-missing-
  violations` / `alcohol-excise-license-missing-violations` /
  `tobacco-excise-age-verification-missing-violations` for why this is
  modeled as THREE distinct governor checks (extending the two-way split
  the agri-wholesale sibling's phytosanitary/animal-health checks
  established as precedent) rather than one generic 'certificate-missing'
  check.

  `regulatory-class-for` maps a `:consignment-category` (`:food` |
  `:beverage-alcoholic` | `:beverage-non-alcoholic` | `:tobacco`) to a
  `:regulatory-class` (`:food-safety` | `:alcohol-excise` |
  `:tobacco-excise`). This mapping is deliberately MANY-TO-ONE: `:food`
  AND `:beverage-non-alcoholic` both map to `:food-safety` because, in
  every jurisdiction below, non-alcoholic beverages are regulated under
  the SAME general food-safety statute as food (e.g. the US Federal
  Food, Drug, and Cosmetic Act's own definition of 'food', 21 U.S.C.
  §321(f), explicitly includes 'articles used for food or drink' -- a
  non-alcoholic beverage has no separate excise/licensing regime the way
  alcohol and tobacco do). `:beverage-alcoholic` and `:tobacco` each get
  their OWN excise-specific regulatory class because alcohol and tobacco
  are, in every seeded jurisdiction, taxed and licensed under statutes
  and agencies entirely separate from general food safety.

  Each entry below is a REAL jurisdiction with REAL food-safety /
  alcohol-excise / tobacco-excise regimes:

    - Japan (JPN): 食品衛生法 (Food Sanitation Act) for food-safety,
      administered by 厚生労働省 (MHLW) with food-labeling functions at
      消費者庁 (Consumer Affairs Agency); 酒税法 (Liquor Tax Act) for
      alcohol excise/licensing, administered by 国税庁 (National Tax
      Agency, NTA) -- a liquor wholesaler needs a 酒類卸売業免許
      (liquor wholesale licence) from the NTA; たばこ事業法 (Tobacco
      Business Act) plus 地方税法 (Local Tax Act) for tobacco tax,
      administered by 財務省 (Ministry of Finance), which retains
      regulatory authority over tobacco retail/wholesale licensing since
      JT's privatization.
    - United States (USA): the FDA Food Safety Modernization Act (Pub.
      L. 111-353, 2011), which amended the Federal Food, Drug, and
      Cosmetic Act (21 U.S.C. Chapter 9), for food-safety, administered
      by the FDA; the Federal Alcohol Administration Act (27 U.S.C.
      §201 et seq.) plus alcohol excise tax (26 U.S.C. §5001 et seq.)
      for alcohol excise/licensing, administered by the Alcohol and
      Tobacco Tax and Trade Bureau (TTB), U.S. Treasury; the Family
      Smoking Prevention and Tobacco Control Act (Pub. L. 111-31; 21
      U.S.C. §387 et seq.) plus tobacco excise tax (26 U.S.C. §5701 et
      seq.) for tobacco excise/age-verification, jointly administered by
      the FDA Center for Tobacco Products (product/marketing/age-of-sale
      authority) and TTB (excise tax).
    - United Kingdom (GBR): the Food Safety Act 1990 for food-safety,
      administered by the Food Standards Agency (FSA); the Alcoholic
      Liquor Duties Act 1979 for alcohol excise, administered by HM
      Revenue & Customs (HMRC); the Tobacco Products Duty Act 1979 (tax)
      plus the Children and Young Persons (Protection from Tobacco) Act
      1991 as amended (retailer/purchaser age-of-sale) for tobacco
      excise/age-verification, jointly administered by HMRC (excise) and
      Trading Standards (age-of-sale enforcement).
    - Germany (DEU), representing the EU regime (directly applicable in
      every member state): Regulation (EC) No 178/2002 (the General Food
      Law) for food-safety, enforced in Germany by the Bundesamt für
      Verbraucherschutz und Lebensmittelsicherheit (BVL) under the
      Bundesministerium für Ernährung und Landwirtschaft (BMEL); Council
      Directive 92/83/EEC (structures of excise duty on alcohol) as
      implemented by Germany's Alkoholsteuergesetz for alcohol excise,
      and Directive 2011/64/EU (tobacco excise) as implemented by
      Germany's Tabaksteuergesetz for tobacco excise -- both German
      excise statutes administered by German Customs (Generalzolldirektion
      / Zoll) under the Bundesministerium der Finanzen (BMF).

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction (or
  regulatory class) not in this table has NO spec-basis, full stop --
  the advisor must not fabricate one, and the governor holds if it
  tries. I do not have live web access; the headline statute names and
  owner-authority names above are cited from training-time knowledge
  with reasonable confidence for Japan (食品衛生法/酒税法/たばこ事業法,
  MHLW/NTA/MOF), the EU regulations/directives themselves (Regulation
  (EC) No 178/2002; Directives 92/83/EEC and 2011/64/EU), and the US
  agency structure (FDA; TTB; FDA Center for Tobacco Products) -- but
  the EXACT US Code pin-cite for FSMA (cited here only as amending 21
  U.S.C. Chapter 9, deliberately NOT pinned to a specific section
  number I am not confident about), the exact UK Act titles (Alcoholic
  Liquor Duties Act 1979; Tobacco Products Duty Act 1979; Children and
  Young Persons (Protection from Tobacco) Act 1991), and the exact
  German national implementing-act names (Alkoholsteuergesetz;
  Tabaksteuergesetz, as distinct from the EU directives themselves,
  which I am confident about) should be independently verified before
  this catalog is relied on operationally -- see `docs/business-model.md`
  'Jurisdiction coverage (honest)'.")

(def regulatory-class-for
  "`:consignment-category` -> `:regulatory-class`. Deliberately
  many-to-one: `:food` and `:beverage-non-alcoholic` share
  `:food-safety` (no separate excise/licensing regime for either in any
  seeded jurisdiction); `:beverage-alcoholic` and `:tobacco` each get
  their own excise-specific class."
  {:food                    :food-safety
   :beverage-non-alcoholic  :food-safety
   :beverage-alcoholic      :alcohol-excise
   :tobacco                 :tobacco-excise})

(def catalog
  "iso3 -> {:food-safety {..} :alcohol-excise {..} :tobacco-excise {..}}
  -- THREE requirement maps per jurisdiction, one per `:regulatory-class`.
  `:required-evidence` is the counterparty-diligence + regulatory
  evidence set (credit-clearance record, contract/PO, sanctions-
  screening record, PLUS the class-specific certificate/licence/
  registration); `:legal-basis` / `:owner-authority` / `:provenance` are
  the G2 citation the governor requires before any `:regulatory/verify`
  proposal can commit."
  {"JPN"
   {:food-safety
    {:name "JPN" :regulatory-class :food-safety
     :owner-authority "厚生労働省 (MHLW) / 消費者庁 (Consumer Affairs Agency, labeling)"
     :legal-basis "食品衛生法 (Food Sanitation Act)"
     :provenance "https://www.mhlw.go.jp/"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "food-safety certificate (HACCP-style processed-food safety compliance)"]}
    :alcohol-excise
    {:name "JPN" :regulatory-class :alcohol-excise
     :owner-authority "国税庁 (National Tax Agency, NTA) 酒税課"
     :legal-basis "酒税法 (Liquor Tax Act)"
     :provenance "https://www.nta.go.jp/"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "alcohol excise licence (酒類卸売業免許 / liquor wholesale licence)"]}
    :tobacco-excise
    {:name "JPN" :regulatory-class :tobacco-excise
     :owner-authority "財務省 (Ministry of Finance) 理財局"
     :legal-basis "たばこ事業法 (Tobacco Business Act); 地方税法 (Local Tax Act, たばこ税)"
     :provenance "https://www.mof.go.jp/"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "tobacco excise registration"
                         "retailer/purchaser age-verification record"]}}
   "USA"
   {:food-safety
    {:name "USA" :regulatory-class :food-safety
     :owner-authority "U.S. Food and Drug Administration (FDA)"
     :legal-basis "FDA Food Safety Modernization Act (Pub. L. 111-353), amending the Federal Food, Drug, and Cosmetic Act (21 U.S.C. Chapter 9)"
     :provenance "https://www.fda.gov/food/food-safety-modernization-act-fsma"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "food-safety certificate (FSMA preventive-controls compliance)"]}
    :alcohol-excise
    {:name "USA" :regulatory-class :alcohol-excise
     :owner-authority "Alcohol and Tobacco Tax and Trade Bureau (TTB), U.S. Treasury"
     :legal-basis "Federal Alcohol Administration Act (27 U.S.C. §201 et seq.); alcohol excise tax (26 U.S.C. §5001 et seq.)"
     :provenance "https://www.ttb.gov/"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "alcohol excise licence (TTB basic permit / wholesaler licence)"]}
    :tobacco-excise
    {:name "USA" :regulatory-class :tobacco-excise
     :owner-authority "FDA Center for Tobacco Products (CTP) / Alcohol and Tobacco Tax and Trade Bureau (TTB)"
     :legal-basis "Family Smoking Prevention and Tobacco Control Act (Pub. L. 111-31; 21 U.S.C. §387 et seq.); tobacco excise tax (26 U.S.C. §5701 et seq.)"
     :provenance "https://www.fda.gov/tobacco-products"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "tobacco excise registration"
                         "retailer/purchaser age-verification record"]}}
   "GBR"
   {:food-safety
    {:name "GBR" :regulatory-class :food-safety
     :owner-authority "Food Standards Agency (FSA)"
     :legal-basis "Food Safety Act 1990"
     :provenance "https://www.food.gov.uk/"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "food-safety certificate (HACCP-style processed-food safety compliance)"]}
    :alcohol-excise
    {:name "GBR" :regulatory-class :alcohol-excise
     :owner-authority "HM Revenue & Customs (HMRC)"
     :legal-basis "Alcoholic Liquor Duties Act 1979"
     :provenance "https://www.gov.uk/government/organisations/hm-revenue-customs"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "alcohol excise licence (AWRS wholesaler approval)"]}
    :tobacco-excise
    {:name "GBR" :regulatory-class :tobacco-excise
     :owner-authority "HM Revenue & Customs (HMRC) / Trading Standards"
     :legal-basis "Tobacco Products Duty Act 1979; Children and Young Persons (Protection from Tobacco) Act 1991 (as amended)"
     :provenance "https://www.gov.uk/government/organisations/hm-revenue-customs"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "tobacco excise registration"
                         "retailer/purchaser age-verification record"]}}
   "DEU"
   {:food-safety
    {:name "DEU" :regulatory-class :food-safety
     :owner-authority "Bundesamt für Verbraucherschutz und Lebensmittelsicherheit (BVL), under Bundesministerium für Ernährung und Landwirtschaft (BMEL)"
     :legal-basis "Regulation (EC) No 178/2002 (General Food Law)"
     :provenance "https://www.bvl.bund.de/"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "food-safety certificate (HACCP-style processed-food safety compliance)"]}
    :alcohol-excise
    {:name "DEU" :regulatory-class :alcohol-excise
     :owner-authority "Generalzolldirektion (German Customs / Zoll), under Bundesministerium der Finanzen (BMF)"
     :legal-basis "Council Directive 92/83/EEC (structures of excise duty on alcohol); Alkoholsteuergesetz (national implementing act)"
     :provenance "https://www.zoll.de/"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "alcohol excise licence (Alkoholsteuerlager / tax-warehouse authorization)"]}
    :tobacco-excise
    {:name "DEU" :regulatory-class :tobacco-excise
     :owner-authority "Generalzolldirektion (German Customs / Zoll), under Bundesministerium der Finanzen (BMF)"
     :legal-basis "Directive 2011/64/EU (tobacco excise); Tabaksteuergesetz (national implementing act)"
     :provenance "https://www.zoll.de/"
     :required-evidence ["credit-clearance record"
                         "contract/PO"
                         "sanctions-screening (OFAC/equivalent) record"
                         "tobacco excise registration"
                         "retailer/purchaser age-verification record"]}}})

(defn spec-basis
  "The [iso3 consignment-category] requirement map, or nil -- nil means
  NO spec-basis, and the governor must hold any proposal that tries to
  verify, dispatch or invoice on it. `category` is `:food` |
  `:beverage-alcoholic` | `:beverage-non-alcoholic` | `:tobacco`; it is
  routed through `regulatory-class-for` before the catalog lookup."
  [iso3 category]
  (get-in catalog [iso3 (regulatory-class-for category)]))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions
  actually have all THREE regulatory-class spec-basis entries
  (food-safety, alcohol-excise, tobacco-excise). Never report a missing
  jurisdiction (or a jurisdiction missing any class) as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [full? (fn [iso3] (and (get-in catalog [iso3 :food-safety])
                               (get-in catalog [iso3 :alcohol-excise])
                               (get-in catalog [iso3 :tobacco-excise])))
         have (filter full? iso3s)
         missing (remove full? iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-4630 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis for "
                 "ALL THREE regulatory classes (food-safety, alcohol-excise, "
                 "tobacco-excise). This is a starting catalog, not a survey "
                 "of all ~194 jurisdictions -- extend "
                 "`provisiontrade.facts/catalog`, never fabricate a "
                 "jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `[iso3 category]`? Missing spec-basis
  -> never satisfied."
  [iso3 category submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3 category)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3 category]
  (:required-evidence (spec-basis iso3 category) []))
