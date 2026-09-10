(ns civileng.store
  "SSoT for the ISCO-08 2142 community civil engineers actor (itonami
  actor pattern, ADR-2607011000 / CLAUDE.md Actors section). Modeled
  on cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client    — a registered organization (:client-id, :name)
    structure — a registered structural element {:structure-id
                :client-id :name :allowable-capacity number
                :approved-grades #{grade-str}}. `:allowable-capacity`
                is the registered load rating (kN or equivalent) a
                design must not exceed; `:approved-grades` is the
                registered set of material grades this structure may
                be built from.
    record    — a committed operating record (approved load design) —
                written ONLY via commit-record!.
    ledger    — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (structure [s structure-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-structure! [s st])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (structure [_ structure-id] (get-in @a [:structures structure-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-structure! [s st]
    (swap! a assoc-in [:structures (:structure-id st)] st) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :structures {} :records [] :ledger []}
                                   seed)))))
