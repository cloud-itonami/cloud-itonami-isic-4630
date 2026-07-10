(ns provisiontrade.provisiontradeadvisor
  "ProvisionTradeAdvisor client -- the *contained intelligence node* for
  the food/beverage/tobacco wholesale actor.

  It normalizes provision-order intake, drafts a per-jurisdiction, per-
  regulatory-class evidence checklist (food-safety for `:food`/
  `:beverage-non-alcoholic`, alcohol-excise for `:beverage-alcoholic`,
  tobacco-excise for `:tobacco`), drafts the delivery action, and drafts
  the invoice-settlement action. CRITICAL: it is a smart-but-untrusted
  advisor. It returns a *proposal* (with a rationale + the fields it
  cited), never a committed record or a real dispatch/settlement. Every
  output is censored downstream by `provisiontrade.governor` before
  anything touches the SSoT, and `:delivery/dispatch`/`:invoice/settle`
  proposals NEVER auto-commit at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :delivery/dispatch | :invoice/settle | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [provisiontrade.facts :as facts]
            [provisiontrade.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the order-id, counterparty, jurisdiction, consignment
  category or any physical/commercial value. High confidence, low
  stakes."
  [_db {:keys [patch]}]
  {:summary    (str "食品/飲料/たばこ卸売オーダー記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :order/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-regulatory
  "Per-jurisdiction, per-regulatory-class evidence checklist draft.
  `:no-spec?` injects the failure mode we must defend against: proposing
  a checklist for a jurisdiction with NO official spec-basis in
  `provisiontrade.facts` -- the Provision Trading Governor must reject
  this (never invent a jurisdiction's requirements). The consignment
  category (`:food`/`:beverage-non-alcoholic`/`:beverage-alcoholic`/
  `:tobacco`) comes from the order itself, never from advisor judgment:
  the advisor does not get to decide which regulatory regime applies to
  a consignment."
  [db {:keys [subject no-spec?]}]
  (let [po (store/provision-order db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction po))
        category (:consignment-category po)
        sb (facts/spec-basis iso3 category)]
    (if (nil? sb)
      {:summary    (str iso3 "/" category " の公式spec-basisが見つかりません")
       :rationale  "provisiontrade.facts に未登録の法域/品目区分。要件を推測で作らない。"
       :cites      []
       :effect     :regulatory-assessment/set
       :value      {:jurisdiction iso3 :consignment-category category :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 "/" (name category) " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :regulatory-assessment/set
       :value      {:jurisdiction iso3
                    :consignment-category category
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- propose-delivery
  "Draft the actual DELIVERY action -- dispatching real food, beverage
  or tobacco product to a counterparty out of the wholesale warehouse.
  ALWAYS `:stake :delivery/dispatch` -- this is a REAL-WORLD act (an
  autonomous warehouse pallet-picking/staging robot physically performs
  the warehouse handling, or an operator does), never a draft the actor
  may auto-run. See README `Actuation`: no phase ever adds this op to a
  phase's `:auto` set (`provisiontrade.phase`); the governor also always
  escalates on `:delivery/dispatch`. Two independent layers agree,
  deliberately."
  [db {:keys [subject]}]
  (let [po (store/provision-order db subject)
        category (:consignment-category po)
        credit-ok? (and po (true? (:credit-cleared? po)))
        contract-ok? (and po (some? (:contract-terms po))
                          (not= "" (:contract-terms po)))
        cert-ok? (and po (case category
                            (:food :beverage-non-alcoholic) (true? (:food-safety-certificate? po))
                            :beverage-alcoholic (true? (:alcohol-excise-license? po))
                            :tobacco (and (true? (:tobacco-excise-registration? po))
                                          (true? (:age-verification-on-file? po)))
                            false))
        sanctions-ok? (and po (true? (:sanctions-screened? po)))]
    {:summary    (str subject " 向け出荷提案"
                      (when po (str " (counterparty=" (:counterparty po) ", category=" (name category) ")")))
     :rationale  (if po
                   (str "credit-cleared?=" credit-ok?
                        " contract-on-file?=" contract-ok?
                        " regulatory-clearance-on-file?=" cert-ok?
                        " sanctions-screened?=" sanctions-ok?)
                   "provision-orderが見つかりません")
     :cites      (if po [subject] [])
     :effect     :order/mark-dispatched
     :value      {:provision-order-id subject}
     :stake      :delivery/dispatch
     :confidence (if (and credit-ok? contract-ok? cert-ok? sanctions-ok?) 0.9 0.3)}))

(defn- propose-invoice
  "Draft the actual INVOICE-SETTLEMENT action -- settling a real
  food/beverage/tobacco wholesale invoice (the money side of the trade,
  custody/financial transfer). ALWAYS `:stake :invoice/settle` -- this
  is a REAL-WORLD act (real money moves between counterparty and
  trader), never a draft the actor may auto-run. See README `Actuation`:
  no phase ever adds this op to a phase's `:auto` set
  (`provisiontrade.phase`); the governor also always escalates on
  `:invoice/settle`. Two independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [po (store/provision-order db subject)
        dispatched? (and po (:dispatched? po))
        sanctions-ok? (and po (true? (:sanctions-screened? po)))]
    {:summary    (str subject " 向け請求提案"
                      (when po (str " (counterparty=" (:counterparty po) ")")))
     :rationale  (if po
                   (str "dispatched?=" dispatched?
                        " sanctions-screened?=" sanctions-ok?)
                   "provision-orderが見つかりません")
     :cites      (if po [subject] [])
     :effect     :order/mark-invoiced
     :value      {:provision-order-id subject}
     :stake      :invoice/settle
     :confidence (if (and dispatched? sanctions-ok?) 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :order/intake       (normalize-intake db request)
    :regulatory/verify   (verify-regulatory db request)
    :delivery/dispatch  (propose-delivery db request)
    :invoice/settle     (propose-invoice db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは食品・飲料・たばこ卸売事業者の出荷・請求エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。"
       "説明や前置きは一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:order/upsert|:regulatory-assessment/set|:order/mark-dispatched|"
       ":order/mark-invoiced) "
       ":stake(:delivery/dispatch か :invoice/settle か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域・品目区分の食品衛生・酒税・たばこ税/年齢確認要件を"
       "絶対に創作してはいけません。spec-basisが無い場合は :cites を空にし confidence "
       "を上げないこと。食品衛生証明書・酒税免許・たばこ税登録は別物であり、荷口の"
       "consignment-category(:food/:beverage-non-alcoholic/:beverage-alcoholic/:tobacco)"
       "が要求するものと違う証憑を代わりに使ってはいけません。"
       "取引先信用審査・契約有無・制裁スクリーニングの状態を偽って報告してはいけません。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :regulatory/verify  {:provision-order (store/provision-order st subject)}
    :delivery/dispatch  {:provision-order (store/provision-order st subject)}
    :invoice/settle     {:provision-order (store/provision-order st subject)}
    {:provision-order (store/provision-order st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Provision Trading Governor
  escalates/holds -- an LLM hiccup can never auto-dispatch a consignment
  or auto-settle an invoice."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :provisiontradeadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
