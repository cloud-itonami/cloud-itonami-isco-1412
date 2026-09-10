(ns restaurant-management.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [restaurant-management.actor :as actor]
            [restaurant-management.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-location! st {:location-id "location-1" :name "Sunset Bistro"})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:location-id "location-1" :op :review :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "location-1"))))))

(deftest holds-on-unregistered-location-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:location-id "no-such-location" :op :review :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-location")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; near-flame operation always escalates (governor invariant)
        request {:location-id "location-1" :op :operate-near-flame :stake :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "location-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "location-1")))))))
