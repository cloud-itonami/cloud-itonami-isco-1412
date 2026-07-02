(ns restaurant-management.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [restaurant-management.store :as store]
            [restaurant-management.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-location! st {:location-id "location-1" :name "Sunset Bistro"})
    st))

(deftest ok-on-clean-review
  (let [st (fresh-store)
        proposal {:op :review :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:location-id "location-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-location
  (let [st (fresh-store)
        proposal {:op :review :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:location-id "no-such-location"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-location (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :review :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:location-id "location-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-near-flame-operation
  (let [st (fresh-store)
        proposal {:op :operate-near-flame :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:location-id "location-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-food-safety-hold-clearance
  (let [st (fresh-store)
        proposal {:op :clear-food-safety-hold :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:location-id "location-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :review :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:location-id "location-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:location-id "location-1" :op :staff})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "location-1"))))
    (is (= 1 (count (store/ledger st))))))
