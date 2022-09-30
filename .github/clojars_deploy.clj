;; based on: https://github.com/mauricioszabo/clj-lib-deployer/blob/master/deploy-lein.bb
;; updated for clj-tools: g-krisztian

(def release
  (edn/read-string (slurp "release.edn")))

(println release)

(def project
  (format "%s/%s" (:group-id release) (:artifact-id release)))

(println "Project: " project)

(def version
  (:version release))

(println "Version: " version)

(defn- can-deploy? []
  (let [status (:status (curl/get (str "https://clojars.org/" project
                                       "/versions/" version)
                                  {:throw false}))]
    (= 404 status)))

(defn- tag-name [] (System/getenv "TAG"))

(defn- decode-base64 [string]
  (-> java.util.Base64
      .getDecoder
      (.decode string)))

(defn run-shell-cmd [& args]
  (let [{:keys [exit out err] :as result} (apply shell/sh args)]
    (when-not (zero? exit)
      (println "ERROR running command\nSTDOUT:")
      (println out "\nSTDERR:")
      (println err)
      (throw (ex-info "Error while running shell command" {:status exit})))
    result))

(defn- import-gpg! []
  (let [secret     (System/getenv "GPG_SECRET_KEYS")
        ownertrust (System/getenv "GPG_OWNERTRUST")]
    (when-not (and secret ownertrust) (throw (ex-info "Can't find GPG keys!" {})))
    (run-shell-cmd "gpg" "--import" :in (decode-base64 secret))
    (run-shell-cmd "gpg" "--import-ownertrust" :in (decode-base64 ownertrust))))

(defn deploy! []
  (let [tag  (not-empty (tag-name))]
    (when-not (can-deploy?)
      (throw (ex-info "Can't deploy this version - release version already exist on clojars"
                      {:version version})))

    (when (some-> tag (str/replace-first #"v" "") (not= version))
      (throw (ex-info "Tag version mismatches with release.edn"
                      {:tag-name tag
                       :version  version})))

    (when tag
      (import-gpg!)
      (println "Deploying a release version")

      (run-shell-cmd "clojure" "-M:release" "--version" version)
      (println "Deploy was successful"))))

(deploy!)
