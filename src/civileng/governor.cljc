(ns civileng.governor
  "CivilEngineersGovernor — the independent safety/traceability layer
  for the ISCO-08 2142 community civil engineers actor (itonami actor
  pattern, ADR-2607011000 / CLAUDE.md Actors section). Modeled on
  cloud-itonami-isco-4311's bookkeeping.governor. Structural twist: a
  design's load-capacity utilization is arithmetic division, not
  engineering judgement — utilization = proposed-load /
  allowable-capacity must not exceed 1.0; and a material grade is
  either a member of the registered approved-grades set or it is not.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose.
    3. structure basis   — a design approval must cite a REGISTERED
                           structure belonging to this client.
    4. load-capacity margin — utilization = load / allowable-capacity
                           must not exceed 1.0 (arithmetic, not
                           judgement).
    5. material grade    — the proposed grade must be a member of the
                           structure's registered approved-grades set
                           (no invented or unapproved material).
  ESCALATION invariants (:escalate? true, human sign-off):
    6. :op :approve-occupancy (issuing an occupancy/safety
                           certificate).
    7. low confidence (< `confidence-floor`)."
  (:require [civileng.store :as store]))

(def confidence-floor 0.6)

(defn- hard-violations [{:keys [request proposal]} client-record s]
  (let [{:keys [op load grade]} proposal
        design? (= :approve-design op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and design? (nil? s))
      (conj {:rule :unknown-structure :detail "未登録 structure への設計承認は不可"})

      (and design? s (not= (:client-id s) (:client-id request)))
      (conj {:rule :structure-wrong-client :detail "structure が別 client のもの"})

      (and design? s (number? load) (> (/ load (:allowable-capacity s)) 1.0))
      (conj {:rule :load-exceeds-capacity
             :detail (str "利用率 " (double (/ load (:allowable-capacity s)))
                          " > 1.0（load " load " / allowable-capacity "
                          (:allowable-capacity s) "。荷重算術は判断ではない）")})

      (and design? s grade (not (contains? (:approved-grades s) grade)))
      (conj {:rule :unapproved-grade
             :detail (str "材料等級 " grade " は登録済み承認集合 "
                          (:approved-grades s) " の外")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `civileng.store/Store`. Pure — never mutates
  the store."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        s (some->> (:structure-id proposal) (store/structure store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record s)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (= :approve-occupancy (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
