(ns xiana.db.protocol)

(defprotocol DatabaseP 
  (define-container [this])
  (define-migration [this] [this count])
  (connect [this])
  (define-parameters [this sql-map])
  (execute [this sql-map])
  (in-transaction [this tx sql-map])
  (multi-execute [this query-map]))
