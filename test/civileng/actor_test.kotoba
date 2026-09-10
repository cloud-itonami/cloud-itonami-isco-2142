(ns civileng.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [civileng.actor :as actor]
            [civileng.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Trade"})
    (store/register-structure! st {:structure-id "S-1" :client-id "client-1"
                                   :name "footbridge beam"
                                   :allowable-capacity 1000
                                   :approved-grades #{"C30" "C40"}})
    st))

(deftest commits-an-in-margin-design
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-design :stake :low
                 :structure-id "S-1" :load 800 :grade "C30"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-over-capacity-design
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-design :stake :low
                 :structure-id "S-1" :load 1500 :grade "C30"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-issues-occupancy-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-occupancy :stake :high
                 :structure-id "S-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
