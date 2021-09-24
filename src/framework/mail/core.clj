(ns framework.mail.core
  (:require
    [cuerdas.core :as cu]
    [postal.core :as pc]))


(defn- make-body
  [body atts]
  (let [body-payload [:alternative
                      {:type "text/plain"
                       :content (cu/strip-tags body)}
                      {:type "text/html"
                       :content body}]
        attachments (if (string? atts) (vector atts) atts)
        file-map #(hash-map :type :attachment :content (java.io.File. %))]
    (vec (concat body-payload (map file-map attachments)))))


(defn send-email!
  [{mail-config :framework.app/emails}
   {:keys [to cc bcc subject body attachments]}]
  (pc/send-message mail-config
                   {:from (:from mail-config)
                    :to to
                    :cc cc
                    :bcc bcc
                    :subject subject
                    :body (make-body body attachments)}))
