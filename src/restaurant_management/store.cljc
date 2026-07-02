(ns restaurant-management.store
  "SSoT for the ISCO-08 1412 independent restaurant-management sole-
  proprietor actor. Store is a protocol injected into the
  `restaurant-management.actor` StateGraph — `MemStore` is the
  default, deterministic, zero-dep backend; a Datomic/kotoba-server-
  backed implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  Domain:

    location — a registered restaurant location (:location-id, :name)
    record   — a committed operating record under a location (staff
               plan, food-safety review, near-flame operation, food-
               safety hold clearance) — written ONLY via
               commit-record!, never mutated in place
    ledger   — an append-only audit trail of every proposal/verdict/
               disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (location [s location-id])
  (records-of [s location-id])
  (ledger [s])
  (register-location! [s location])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (location [_ location-id] (get-in @a [:locations location-id]))
  (records-of [_ location-id] (filter #(= location-id (:location-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-location! [s location]
    (swap! a assoc-in [:locations (:location-id location)] location) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:locations {} :records [] :ledger []} seed)))))
