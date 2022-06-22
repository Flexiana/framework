(ns xiana.migrate
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.tools.cli :refer [parse-opts]]
   [migratus.core :as mig]
   [next.jdbc :as jdbc]
   [taoensso.timbre :as log])
  (:gen-class))

(def cli-options
  [["-c" "--config FILENAME" "Configuration file"
    :id :config]
   ["-d" "--dir DIR" "Directory (only for create action)"
    :id :dir]
   ["-i" "--id ID" "Migration script id (YYYYMMDDHHmmss)"
    :id :id]
   ["-l" "--log LEVEL" "Log level (values: trace, debug, info, error)"
    :id :log
    :default :info
    :parse-fn keyword
    :validate [#(#{:trace :debug :info :error} %) "Level could be only: trace, debug info, or error"]]
   ["-n" "--name NAME" "Migration script name (only for create action)"
    :id :name]
   ["-h" "--help"]])

(defn usage
  [options-summary]
  (str/join
   \newline
   ["Migration CLI."
        ""
        "Usage: migrate [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  migrate  Migrate the db up until id or current version if no id given"
        "  rollback Rollback the db down until id or only the last script if no id given"
        "  create   Create migration script "]))

(defn error-msg
  [errors]
  (str "\nThe following errors occurred while parsing your command:\n"
       (str/join \newline errors)
       "\nUse -h or --help options to see usage"))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      ;; help => exit OK with usage summary
      (:help options) {:exit-message (usage summary) :ok? true}

      ;; errors => exit with description of errors
      errors {:exit-message (error-msg errors)}

      ;; missing action
      (zero? (count arguments)) {:exit-message (error-msg ["Missing action"])}

      ;; wrong action
      (not (#{"migrate" "rollback" "create"} (first arguments))) {:exit-message (error-msg ["Wrong action name"])}

      ;; missing required config opt for migrate and rollback
      (and (some #{"migrate" "rollback"} arguments)
           (nil? (:config options)))
      {:exit-message (error-msg ["Missing required option -c at migration or rollback action"])}

      ;; missing opts for create action
      (and (= (first arguments) "create")
           (or (nil? (:dir options)) (nil? (:name options))))
      {:exit-message (error-msg ["Missing required option(s) at create action"])}

      ;; valid command
      :else {:action (first arguments) :options options})))

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn get-config
  [options]
  (let [config (-> (:config options)
                   slurp
                   edn/read-string)
        db-config (:xiana/postgresql config)
        db (if-let [ds (:datasource db-config)]
             {:datasource (jdbc/get-datasource ds)}
             db-config)]
    (log/debug "config:" config)
    (assoc (:xiana/migration config) :db db)))

(defn migrate
  ([config] (mig/migrate config))
  ([config id] (mig/migrate-until-just-before config (Long/parseLong id))))

(defn rollback
  ([config] (mig/rollback config))
  ([config id] (mig/rollback-until-just-after config (Long/parseLong id))))

(defn create-script
  [dir name]
  (mig/create {:migration-dir dir} name))

(defn migrate-action
  [options]
  (log/info "Migrate database")
  (log/debug "options:" options)
  (let [cfg (get-config options)]
    (log/debug "config:" cfg)
    (if-let [id (:id options)]
      (migrate cfg id)
      (migrate cfg))))

(defn rollback-action
  [options]
  (log/info "Rollback database")
  (log/debug "options:" options)
  (let [cfg (get-config options)]
    (log/debug "config:" cfg)
    (if-let [id (:id options)]
      (rollback cfg id)
      (rollback cfg))))

(defn create-script-action
  [{:keys [dir name] :as options}]
  (log/info "Create migration scripts skeleton")
  (log/debug "options:" options)
  (create-script dir name))

(defn run
  [args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (log/set-level! (:log options))
    (log/debug "args:" args)
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "migrate"  (migrate-action options)
        "rollback" (rollback-action options)
        "create"   (create-script-action options)))))

(defn -main
  [& args]
  (run args))
