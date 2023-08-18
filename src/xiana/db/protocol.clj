(ns xiana.db.protocol)

(defprotocol DatabaseP
  (->db-object [this obj])
  (<-db-object [this obj])
  (define-container [this])
  (define-migration [this] [this config count])
  (connect [this])
  (define-parameters [this sql-map])
  (execute [this sql-map])
  (in-transaction [this tx sql-map])
  (multi-execute [this datasource query-map]))
