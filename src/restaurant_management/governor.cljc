(ns restaurant-management.governor
  "RestaurantManagementGovernor — the independent safety/traceability
  layer for the ISCO-08 1412 independent restaurant-management actor.
  Wired as its own `:govern` node in `restaurant-management.actor`'s
  StateGraph, downstream of `:advise` — the Advisor has no notion of
  location provenance or open-flame/food-safety-hold risk, so this
  MUST be a separate system able to reject a proposal (itonami actor
  pattern, per ADR-2607011000 / CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. location provenance  — the request's location must be
       registered.
    2. no-actuation           — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README robotics-premise: operating near open flame or hot surfaces
  and clearing a food-safety hold always require human sign-off):
    3. :op :operate-near-flame.
    4. :op :clear-food-safety-hold.
    5. low confidence (< `confidence-floor`)."
  (:require [restaurant-management.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:operate-near-flame :clear-food-safety-hold})

(defn- hard-violations [{:keys [proposal]} location-record]
  (cond-> []
    (nil? location-record)
    (conj {:rule :no-location :detail "未登録 location"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `restaurant-management.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [location-record (store/location store (:location-id request))
        hard (hard-violations {:proposal proposal} location-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
