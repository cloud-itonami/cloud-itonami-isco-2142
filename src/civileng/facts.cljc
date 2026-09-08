(ns civileng.facts
  "Statutory reference values the ISCO-08 2142 civil engineers actor governs
  against, ingested from the primary legal source rather than restated from
  memory.

  Runtime: portable `.cljc` (pure data + pure predicates, no host interop).

  Why this namespace exists. The Governor already refused a material grade
  outside a structure's `:approved-grades` set, and the README described that
  as `no invented or unapproved material`. Measured on the pre-change tree,
  the first half of that claim was false: registering a structure with
  `:approved-grades #{\"BANANA-9000\"}` and proposing `\"BANANA-9000\"` returned
  `{:ok? true :violations []}`. Membership was checked against a set the
  operator typed; nothing asked whether the set's contents corresponded to
  anything real. The same shape held for `:allowable-capacity` — the
  utilization arithmetic in the Governor is exact, but it divides by a number
  no rule had ever constrained.

  A registered value that nothing checks is not a fact, and dividing by it
  does not make it one. What follows are values fixed by statute, so they can
  bound the operator's registrations instead of trusting them.

  Source of every number below (fetched and parsed, not recalled):

    建築基準法施行令 — Order for Enforcement of the Building Standards Act,
    Cabinet Order No. 338 of 1950 (昭和二十五年政令第三百三十八号).

      machine-readable : https://elaws.e-gov.go.jp/api/1/lawdata/325CO0000000338
      human permalink  : https://laws.e-gov.go.jp/law/325CO0000000338

  The API URL is the one the numbers were read from; it serves the law as XML
  and answers 404 for an unknown law id, so a fetch that succeeds is evidence
  about *this* law. The human permalink is a client-rendered page — it is the
  canonical address for a reader, and it is NOT evidence of content, because
  it returns the same shell for every id.

  Enabling statute (the Act the Order is made under), for provenance only:

    建築基準法 — Building Standards Act, Act No. 201 of 1950
    (昭和二十五年法律第二百一号).

      machine-readable : https://elaws.e-gov.go.jp/api/1/lawdata/325AC0000000201
      human permalink  : https://laws.e-gov.go.jp/law/325AC0000000201

  Every function here returns a vector of `{:rule .. :detail ..}` maps — empty
  means conforming — so violations compose with the Governor's own rules
  without a second shape."
  (:require [kotoba.lang.text :as str]))

(def order-citation
  "Provenance for every value in this namespace. Carried as data so a caller
  can attach it to a refusal, rather than re-typing the article number."
  {:law "建築基準法施行令"
   :law-en "Order for Enforcement of the Building Standards Act"
   :law-no "昭和二十五年政令第三百三十八号"
   :law-id "325CO0000000338"
   :source-url "https://elaws.e-gov.go.jp/api/1/lawdata/325CO0000000338"
   :permalink "https://laws.e-gov.go.jp/law/325CO0000000338"
   :enabling-act {:law "建築基準法"
                  :law-no "昭和二十五年法律第二百一号"
                  :source-url "https://elaws.e-gov.go.jp/api/1/lawdata/325AC0000000201"
                  :permalink "https://laws.e-gov.go.jp/law/325AC0000000201"}})

;; ---------------------------------------------------------------------------
;; 第九十三条（地盤及び基礎ぐい） — ground and bearing piles
;; ---------------------------------------------------------------------------

(def ground-bearing-article "第九十三条")

(def ground-bearing-kn-m2
  "Long-term allowable bearing pressure by ground type, kN/m², from the table
  in Article 93.

  Article 93 requires the allowable bearing pressure to be established by a
  ground survey conducted by the method the Minister prescribes; the table is
  an alternative the text permits in its proviso (`次の表の数値によることが
  できる`). So these are the values available WITHOUT a survey, and taking that
  route means the number is fixed by statute rather than chosen — which is
  what makes it checkable here. A structure that declares no `:ground-type` is
  on the survey route and is left alone; see `ground-violations`.

  Short-term (seismic) allowable pressure is twice the long-term value, per
  the same table (`長期に生ずる力に対する許容応力度のそれぞれの数値の二倍と
  する`); see `short-term-bearing-kn-m2`."
  {:rock                  1000   ; 岩盤
   :cemented-sand          500   ; 固結した砂
   :tuffaceous-clay-rock   300   ; 土丹盤
   :dense-gravel           300   ; 密実な礫層
   :dense-sandy            200   ; 密実な砂質地盤
   :sandy                   50   ; 砂質地盤（地震時に液状化のおそれのないものに限る。）
   :stiff-cohesive         100   ; 堅い粘土質地盤
   :cohesive                20   ; 粘土質地盤
   :stiff-loam             100   ; 堅いローム層
   :loam                    50}) ; ローム層

(def ground-type-labels
  "The statutory Japanese label for each ground type, so a refusal can quote
  the table row it is refusing against."
  {:rock "岩盤"
   :cemented-sand "固結した砂"
   :tuffaceous-clay-rock "土丹盤"
   :dense-gravel "密実な礫層"
   :dense-sandy "密実な砂質地盤"
   :sandy "砂質地盤（地震時に液状化のおそれのないものに限る。）"
   :stiff-cohesive "堅い粘土質地盤"
   :cohesive "粘土質地盤"
   :stiff-loam "堅いローム層"
   :loam "ローム層"})

(defn statutory-ground-type?
  "Is `t` a ground type the Article 93 table names?"
  [t]
  (contains? ground-bearing-kn-m2 t))

(defn short-term-bearing-kn-m2
  "Short-term (seismic) allowable bearing pressure for `t`, or nil when the
  table does not name `t`. Twice the long-term value, per Article 93."
  [t]
  (some-> (ground-bearing-kn-m2 t) (* 2)))

(defn ground-violations
  "Check a structure's declared ground against Article 93.

  Opt-in by design: a structure with no `:ground-type` key is on the survey
  route the article's main clause requires, whose result this actor cannot
  recompute, so it is not refused here. Declaring a ground type is what elects
  the statutory table — and having elected it, the operator does not also get
  to choose the number.

  Refuses:
    :unknown-ground-type          — a ground type the table does not name.
    :bearing-exceeds-statutory    — a registered long-term bearing pressure
                                    above the table value for that type."
  [structure]
  (let [t (:ground-type structure)
        p (:bearing-pressure-kn-m2 structure)]
    (cond-> []
      (and (some? t) (not (statutory-ground-type? t)))
      (conj {:rule :unknown-ground-type
             :detail (str "地盤種別 " (pr-str t) " は" ground-bearing-article
                          "の表に無い（表の種別: "
                          (str/join "、" (sort (map name (keys ground-bearing-kn-m2))))
                          "）")})

      (and (some? t) (statutory-ground-type? t) (number? p)
           (> p (ground-bearing-kn-m2 t)))
      (conj {:rule :bearing-exceeds-statutory
             :detail (str "登録された長期許容応力度 " p " kN/m² は、"
                          ground-bearing-article "の表が「"
                          (ground-type-labels t) "」に定める "
                          (ground-bearing-kn-m2 t)
                          " kN/m² を超える（表の数値は選べない）")}))))

;; ---------------------------------------------------------------------------
;; 第九十条（鋼材等）表二 — reinforcement allowable stress ceilings
;; ---------------------------------------------------------------------------

(def rebar-article "第九十条")

(def rebar-long-term-cap-n-mm2
  "Ceiling on the long-term allowable stress of reinforcement, N/mm², from
  table 2 of Article 90, for use OTHER than shear reinforcement.

  The article gives the allowable stress as F/1.5 — F being the reference
  strength the Minister fixes per material grade and quality — and then caps
  the result. This namespace carries the cap, not F: the cap is stated in the
  Order itself, whereas F lives in a ministerial notification outside this
  source, and inventing an F table from memory is the failure this namespace
  exists to stop.

  Keys are `[bar-type oversize?]` where oversize? means a diameter greater
  than 28 mm, the split the table draws for deformed bars."
  {[:round false]    155   ; 丸鋼
   [:round true]     155   ; 丸鋼 — the table draws no diameter split
   [:deformed false] 215   ; 異形鉄筋 径二十八ミリメートル以下のもの
   [:deformed true]  195}) ; 異形鉄筋 径二十八ミリメートルを超えるもの

(def rebar-diameter-split-mm
  "The diameter at which Article 90 table 2 changes the deformed-bar ceiling.
  Bars larger than this take the lower value; `28` itself takes the higher one
  (`径二十八ミリメートル以下のもの` — twenty-eight millimetres or less)."
  28)

(defn rebar-cap-n-mm2
  "Statutory long-term allowable stress ceiling for a bar, or nil when
  `bar-type` is not one the table names."
  [bar-type diameter-mm]
  (let [oversize? (and (number? diameter-mm) (> diameter-mm rebar-diameter-split-mm))]
    (rebar-long-term-cap-n-mm2 [bar-type oversize?])))

(defn rebar-violations
  "Check a proposed reinforcement stress against Article 90 table 2.

  Opt-in on the same principle as `ground-violations`: a structure that
  declares no `:rebar-type` is not making a claim this table can adjudicate.

  Refuses:
    :unknown-rebar-type        — a bar type the table does not name.
    :rebar-stress-exceeds-cap  — a proposed long-term stress above the ceiling."
  [structure proposal]
  (let [bar (:rebar-type structure)
        dia (:rebar-diameter-mm structure)
        stress (:steel-stress-n-mm2 proposal)
        cap (rebar-cap-n-mm2 bar dia)]
    (cond-> []
      (and (some? bar) (nil? cap))
      (conj {:rule :unknown-rebar-type
             :detail (str "鉄筋種別 " (pr-str bar) " は" rebar-article
                          "表二に無い（表の種別: :round（丸鋼）、:deformed（異形鉄筋））")})

      (and (some? cap) (number? stress) (> stress cap))
      (conj {:rule :rebar-stress-exceeds-cap
             :detail (str "長期許容応力度 " stress " N/mm² は、" rebar-article
                          "表二が定める上限 " cap " N/mm² を超える"
                          (when (and (= :deformed bar) (number? dia))
                            (str "（異形鉄筋 径" dia "mm）")))}))))

(defn statutory-violations
  "Every Article 90 / Article 93 refusal available for this structure and
  proposal, in one vector. The Governor calls this rather than the individual
  checks so that adding a statutory rule here does not need a Governor edit."
  [structure proposal]
  (into (ground-violations structure)
        (rebar-violations structure proposal)))
