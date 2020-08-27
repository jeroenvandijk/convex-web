(ns dev
  (:require [convex-web.system :as system]
            [convex-web.peer :as peer]
            [convex-web.component :as component]
            [convex-web.convex :as convex]
            [convex-web.session :as session]
            [convex-web.account :as account]
            [convex-web.logging :as logging]
            [convex-web.web-server :as web-server]
            [convex-web.command :as command]
            [convex-web.client :as client]

            [clojure.test :refer [is]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.repl :refer [doc]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.stacktrace :as stacktrace]

            [com.stuartsierra.component.repl :refer [set-init reset system]]
            [aero.core :as aero]
            [datascript.core :as d]
            [nano-id.core :as nano-id]
            [org.httpkit.client :as http]
            [expound.alpha :as expound])
  (:import (convex.core Init Peer)
           (convex.core.lang Core Reader Context)
           (org.slf4j.bridge SLF4JBridgeHandler)))

;; -- Logging
(set-init
  (fn [_]
    (component/system :dev)))

(def context (Context/createFake Init/STATE))

(defn ^Peer peer []
  (peer/peer (system/convex-server system)))

(defmacro send-query [& form]
  `(let [conn# (system/convex-conn system)]
     (peer/send-query conn# Init/HERO ~(str/join " " form))))

(defmacro execute [form]
  `(convex/execute context ~form))

(defmacro execute-query [& form]
  `(let [^String source# ~(str/join " " form)]
     (.getResult (.executeQuery (peer) (peer/wrap-do (Reader/readAll source#)) Init/HERO))))

(defn db []
  @(system/datascript-conn system))

(defn commands
  ([]
   (sort-by :convex-web.command/id (d/q '[:find [(pull ?e [*]) ...]
                                          :in $
                                          :where [?e :convex-web.command/id _]]
                                        @(system/datascript-conn system))))
  ([status]
   (filter
     (fn [command]
       (= status (:convex-web.command/status command)))
     (commands))))

(comment

  (clojure.test/run-tests
    'convex-web.specs-test
    'convex-web.http-api-test)


  ;; -- Sessions
  (d/q '[:find [(pull ?e [*
                          {:convex-web.session/accounts
                           [:convex-web.account/address]}]) ...]
         :in $
         :where [?e :convex-web.session/id _]]
       @(system/datascript-conn system))


  (d/q '[:find (pull ?e [{:convex-web.session/accounts
                          [:convex-web.account/address]}]) .
         :in $ ?id
         :where [?e :convex-web.session/id ?id]]
       @(system/datascript-conn system) "mydbOh9wCdTcF_vLvUVHR")

  (session/find-session @(system/datascript-conn system) "iGlF3AZWw0eGuGfL_ib4-")


  (let [a (.getAccounts (.getConsensusState (peer/peer (system/convex-server system))))]
    (doseq [[k v] (convex.core.lang.RT/sequence a)]
      (print k v)))


  (str Init/HERO)
  (str Init/VILLAIN)


  (convex/consensus-point (convex/peer-order (peer)))


  ;; -- Query commands
  (commands)
  (commands :convex-web.command.status/running)
  (commands :convex-web.command.status/success)
  (commands :convex-web.command.status/error)

  ;; -- Execute

  (execute nil)
  (execute 1)
  (execute \h)
  (execute "Hello")
  (execute (map inc [1 2 3]))
  (execute x)

  (convex/execute-scrypt context "def x = 1;")
  (convex/execute-scrypt context "do { inc(1); }")

  (convex/execute-scrypt context "when (true) {}")
  (convex/execute-scrypt context "when (true) { 1; }")
  (convex/execute-scrypt context "if (true) 1;")
  (convex/execute-scrypt context "if (true) { 1; 2; }")
  (convex/execute-scrypt context "if (true) 1; else 2;")
  (convex/execute-scrypt context "if (false) 1; else 2;")
  (convex/execute-scrypt context "do { def x? = true; if (x?) { 1; 2; } 1; }")
  (convex/execute-scrypt context "{ def f = fn (x, y) { map(inc, [x, y]); }; f(1, 2); }")
  (convex/execute-scrypt context "map(fn(x){ inc(x); }, [1, 2])")


  (try
    (Reader/read "(]")
    (catch Throwable e
      (println (.getMessage (stacktrace/root-cause e)))))


  ;; --

  (command/wrap-result #:convex-web.command {:mode :convex-web.command.mode/query})


  (def status (convex/account-status (peer) "3333333333333333333333333333333333333333333333333333333333333333"))

  (convex/environment-data status)

  (convex/account-status-data *1)

  (account/find-by-address (db) "7e66429ca9c10e68efae2dcbf1804f0f6b3369c7164a3187d6233683c258710f")


  ;; -- Session

  @web-server/session-ref

  (session/all (db))
  (session/find-session (db) "4feac0cd-cc06-4a3b-bcad-54596771356b")
  (session/find-account (db) "f7d696Fc1884ed5A7294A4F765206DB32dCDbCAB35C84DF7a8348bc2bF3b8f45")

  ;; --


  Core/ENVIRONMENT

  (convex/core-metadata)
  (convex/reference)


  (client/POST-public-v1-transaction-prepare'
    "http://localhost:8080"
    {:address (.toChecksumHex Init/HERO)
     :source "(map inc [1 2 3])"})

  ;; Hash
  ;; => 4fd279dd67a506bbd987899293d1a4d763f6da04941ccc4748f8dcf548e68bb7

  (client/POST-public-v1-transaction-submit'
    "http://localhost:8080"
    {:address (.toChecksumHex Init/HERO)
     :hash "4cf64e350799858086d05fc003c3fc2b7c8407e8b92574f80fb66a31e8a4e01b"
     :sig (client/sig Init/HERO_KP  "4cf64e350799858086d05fc003c3fc2b7c8407e8b92574f80fb66a31e8a4e01b")})

  @(client/POST-v1-faucet
     "http://localhost:8080"
     {:address "2ef2f47F5F6BC609B416512938bAc7e015788019326f50506beFE05527da2d71"
      :amount 500})

  )