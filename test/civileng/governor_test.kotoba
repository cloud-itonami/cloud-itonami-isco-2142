(ns civileng.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [civileng.store :as store]
            [civileng.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Trade"})
    (store/register-structure! st {:structure-id "S-1" :client-id "client-1"
                                   :name "footbridge beam"
                                   :allowable-capacity 1000
                                   :approved-grades #{"C30" "C40"}})
    st))

(defn- design [load grade]
  {:op :approve-design :effect :propose :structure-id "S-1"
   :load load :grade grade :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-capacity-and-approved-grade
  (let [st (fresh-store)
        v (governor/check req {} (design 800 "C30") st)]
    (is (:ok? v))))

(deftest ok-at-exact-capacity
  (testing "utilization ratio of exactly 1.0 is within margin"
    (let [st (fresh-store)
          v (governor/check req {} (design 1000 "C30") st)]
      (is (:ok? v)))))

(deftest hard-on-load-exceeding-capacity
  (testing "load arithmetic is not judgement"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (design 1200 "C30") :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :load-exceeds-capacity (:rule %)) (:violations v))))))

(deftest hard-on-unapproved-grade
  (let [st (fresh-store)
        v (governor/check req {} (design 800 "C20") st)]
    (is (:hard? v))
    (is (some #(= :unapproved-grade (:rule %)) (:violations v)))))

(deftest hard-on-unknown-structure
  (let [st (fresh-store)
        v (governor/check req {} (assoc (design 800 "C30") :structure-id "S-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-structure (:rule %)) (:violations v)))))

(deftest hard-on-foreign-structure
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (design 800 "C30") st)]
      (is (:hard? v))
      (is (some #(= :structure-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (design 800 "C30") st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (design 800 "C30") :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-occupancy-approval
  (let [st (fresh-store)
        v (governor/check req {} {:op :approve-occupancy :effect :propose
                                  :structure-id "S-1" :confidence 0.9 :stake :high} st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (design 800 "C30") :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
