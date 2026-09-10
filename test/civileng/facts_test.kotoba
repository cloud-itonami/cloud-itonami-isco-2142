(ns civileng.facts-test
  "Tests for the statutory ceilings ingested in `civileng.facts`, and for the
  Governor refusals they make possible.

  Each refusal test pins the RULE KEYWORD, not merely `:ok? false`. A test
  that only asserts refusal passes when the proposal is rejected for some
  unrelated reason, which would make these tests report a discrimination they
  never performed. Where a rule is asserted, a sibling case shows the same
  input conforming once the offending field alone is corrected."
  (:require [clojure.test :refer [deftest is testing]]
            [civileng.facts :as facts]
            [civileng.governor :as gov]
            [civileng.store :as store]))

(defn- rules [result] (set (map :rule (:violations result))))

(defn- store-with [structure]
  (-> (store/mem-store)
      (store/register-client! {:client-id "c1" :name "Acme Engineering"})
      (store/register-structure! (merge {:structure-id "st1"
                                         :client-id "c1"
                                         :name "Bridge deck"
                                         :allowable-capacity 1000
                                         :approved-grades #{"SD345"}}
                                        structure))))

(defn- check [structure proposal]
  (gov/check {:client-id "c1"} {}
             (merge {:op :approve-design :effect :propose :structure-id "st1"
                     :load 500 :grade "SD345" :confidence 0.9}
                    proposal)
             (store-with structure)))

;; --- Article 93: ground bearing ------------------------------------------

(deftest ground-table-matches-the-order
  (testing "the values are the ones Article 93 tabulates"
    (is (= 1000 (facts/ground-bearing-kn-m2 :rock)))
    (is (= 20 (facts/ground-bearing-kn-m2 :cohesive)))
    (is (= 50 (facts/ground-bearing-kn-m2 :sandy))))
  (testing "short-term is twice long-term, per the same table"
    (is (= 2000 (facts/short-term-bearing-kn-m2 :rock)))
    (is (= 40 (facts/short-term-bearing-kn-m2 :cohesive))))
  (testing "a ground type outside the table has no value at all"
    (is (nil? (facts/short-term-bearing-kn-m2 :martian-regolith)))
    (is (not (facts/statutory-ground-type? :martian-regolith)))))

(deftest ground-type-must-be-one-the-table-names
  (let [r (check {:ground-type :martian-regolith} {})]
    (is (contains? (rules r) :unknown-ground-type)
        "an invented ground type is refused by name")
    (is (:hard? r))
    (is (false? (:ok? r))))
  (testing "the same structure conforms once the ground type is a real row"
    (let [r (check {:ground-type :dense-gravel} {})]
      (is (not (contains? (rules r) :unknown-ground-type)))
      (is (:ok? r)))))

(deftest registered-bearing-pressure-cannot-exceed-the-table
  (let [r (check {:ground-type :cohesive :bearing-pressure-kn-m2 900} {})]
    (is (contains? (rules r) :bearing-exceeds-statutory)
        "900 kN/m² on 粘土質地盤 is above the statutory 20")
    (is (:hard? r)))
  (testing "at the statutory value it conforms — the ceiling is inclusive"
    (let [r (check {:ground-type :cohesive :bearing-pressure-kn-m2 20} {})]
      (is (empty? (rules r)))
      (is (:ok? r))))
  (testing "the ceiling is per ground type, not global"
    (let [r (check {:ground-type :rock :bearing-pressure-kn-m2 900} {})]
      (is (empty? (rules r))
          "900 is fine on 岩盤 (1000) and refused on 粘土質地盤 (20)"))))

(deftest structures-that-do-not-elect-the-table-are-untouched
  (testing "no :ground-type means the survey route, which this actor cannot recompute"
    (let [r (check {:bearing-pressure-kn-m2 999999} {})]
      (is (empty? (rules r)))
      (is (:ok? r)))))

;; --- Article 90 table 2: reinforcement stress -----------------------------

(deftest rebar-ceilings-match-the-order
  (is (= 215 (facts/rebar-cap-n-mm2 :deformed 25)) "異形鉄筋 径28mm以下")
  (is (= 215 (facts/rebar-cap-n-mm2 :deformed 28)) "28mm itself is 以下")
  (is (= 195 (facts/rebar-cap-n-mm2 :deformed 32)) "径28mmを超えるもの")
  (is (= 155 (facts/rebar-cap-n-mm2 :round 16)) "丸鋼")
  (is (nil? (facts/rebar-cap-n-mm2 :unobtainium 16))))

(deftest rebar-type-must-be-one-the-table-names
  (let [r (check {:rebar-type :unobtainium} {})]
    (is (contains? (rules r) :unknown-rebar-type))
    (is (:hard? r))))

(deftest rebar-stress-cannot-exceed-the-statutory-ceiling
  (let [r (check {:rebar-type :deformed :rebar-diameter-mm 32}
                 {:steel-stress-n-mm2 210})]
    (is (contains? (rules r) :rebar-stress-exceeds-cap)
        "210 exceeds the 195 ceiling for deformed bars over 28mm"))
  (testing "the very same stress conforms on a bar 28mm or under (215 ceiling)"
    (let [r (check {:rebar-type :deformed :rebar-diameter-mm 25}
                   {:steel-stress-n-mm2 210})]
      (is (empty? (rules r)))
      (is (:ok? r))))
  (testing "diameter alone decides it — the split is a real boundary"
    (is (empty? (rules (check {:rebar-type :deformed :rebar-diameter-mm 28}
                              {:steel-stress-n-mm2 215}))))
    (is (contains? (rules (check {:rebar-type :deformed :rebar-diameter-mm 29}
                                 {:steel-stress-n-mm2 215}))
                   :rebar-stress-exceeds-cap))))

;; --- provenance -----------------------------------------------------------

(deftest citation-is-carried-as-data
  (testing "a refusal can quote where its number came from"
    (is (= "325CO0000000338" (:law-id facts/order-citation)))
    (is (= "https://elaws.e-gov.go.jp/api/1/lawdata/325CO0000000338"
           (:source-url facts/order-citation)))))
