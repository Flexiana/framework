#!/usr/bin/env bb
;; credits: https://github.com/mauricioszabo/clj-lib-deployer/blob/master/deploy-lein.bb

(defn- get-project! []
  (-> "project.clj"
      slurp
      edn/read-string
      (nth 1)))

(defn- get-version! []
  (-> "project.clj"
      slurp
      edn/read-string
      (nth 2)))

(defn- can-deploy? []
  (let [curr-version (get-version!)
        status (:status (curl/get (str "https://clojars.org/" (get-project!)
                                       "/versions/" (get-version!))
                                  {:throw false}))]
    (= 404 status)))

(defn- tag-name [] (System/getenv "TAG"))

(defn- decode-base64 [string]
  (-> java.util.Base64
      .getDecoder
      (.decode string)))

(defn run-shell-cmd [ & args]
  (let [{:keys [exit out err] :as result} (apply shell/sh args)]
    (when-not (zero? exit)
      (println "ERROR running command\nSTDOUT:")
      (println out "\nSTDERR:")
      (println err)
      (throw (ex-info "Error while runing shell command" {:status exit})))
    result))

(defn- import-gpg! []
  (let [secret (System/getenv "GPG_SECRET_KEYS")
        ownertrust (System/getenv "GPG_OWNERTRUST")]
    (when-not (and secret ownertrust) (throw (ex-info "Can't find GPG keys!" {})))
    (run-shell-cmd "gpg" "--import" :in (decode-base64 secret))
    (run-shell-cmd "gpg" "--import-ownertrust" :in (decode-base64 ownertrust))))

(defn deploy! []
  (let [tag (not-empty (tag-name))]
    (when-not (can-deploy?)
      (throw (ex-info "Can't deploy this version - release version already exist on clojars"
                      {:version (get-version!)})))

    (when (some-> tag (str/replace-first #"v" "") (not= (get-version!)))
      (throw (ex-info "Tag version mismatches with project.clj"
                      {:tag-name tag
                       :version (get-version!)})))

    (if tag
      (do
        (import-gpg!)
        (println "Deploying a release version"))
      (do
        (println "Deploying a snapshot version")
        (run-shell-cmd "lein" "change" "version" "str" "\"-SNAPSHOT\"")))

    (run-shell-cmd "lein" "change" ":deploy-repositories" "concat"
              (pr-str [["releases" {:url "https://repo.clojars.org/"
                                    :username :env/clojars_login
                                    :password :env/clojars_password}]]))
    (run-shell-cmd "lein" "deploy" "releases")
    (println "Deploy was successful")))

(deploy!)
