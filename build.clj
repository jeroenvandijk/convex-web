(ns build
  (:require
    [clojure.tools.build.api :as b]))
    
;; More info see https://clojure.org/guides/tools_build

;; Project settings

(def lib           'convex/convex-web)
(def main-class    'convex-web.core)
(def src-dirs      ["src/main/clojure"])
(def resource-dirs ["src/main/resources"])

;; General settings

(def version (format "0.1.%s" (b/git-count-revs nil)))
 
(def build-path "build")
(def class-dir (str build-path "/classes"))

(def basis (b/create-basis {:project "deps.edn"}))
(def dirs-to-copy (concat src-dirs resource-dirs))


(defn clean [_]
  (b/delete {:path build-path}))


(defn uberjar [{:keys [target-dir] :as opts}]
  (let [class-dir (str target-dir "classes")
        name-file (str target-dir "JAR_FILENAME")
        jar-name (format "%s-%s-standalone.jar" (name lib) version)
        jar-full-path (str target-dir jar-name)]
    (b/write-pom {:class-dir class-dir
                  :lib       lib
                  :version   version
                  :basis     basis
                  :src-dirs  src-dirs})
    (b/copy-dir {:src-dirs   dirs-to-copy
                 :target-dir class-dir})
    (b/compile-clj {:basis     basis
                    :src-dirs  src-dirs
                    :class-dir class-dir  })
    (b/uber {:class-dir class-dir
             :uber-file jar-full-path
             :basis     basis
             :main      main-class})
    (spit name-file jar-name)))


(defn jar [{:keys [target-dir] :as opts}]
  (let [class-dir (str target-dir "classes")
        name-file (str target-dir "JAR_FILENAME")
        jar-name (format "%s-%s.jar" (name lib) version)
        jar-full-path (str target-dir jar-name)]
    (b/write-pom {:class-dir class-dir
                  :lib       lib
                  :version   version
                  :basis     basis
                  :src-dirs  src-dirs})
    (b/copy-dir {:src-dirs   dirs-to-copy
                 :target-dir class-dir})
    (b/compile-clj {:basis     basis
                    :src-dirs  src-dirs
                    :class-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file  jar-full-path
            :basis     basis})
    (spit name-file jar-name)))


(defn deps-jar [{:keys [target-dir]:as opts}]
  (let [class-dir (str target-dir "classes")
        name-file (str target-dir "JAR_FILENAME")
         jar-name (format "%s-%s-deps.jar" (name lib) version)
         jar-full-path (str target-dir jar-name)]
    (b/write-pom {:class-dir class-dir
                  :lib       lib
                  :version   version
                  :basis     basis
                  :src-dirs  []})
    (b/uber {:class-dir class-dir
             :uber-file jar-full-path
             :basis     basis})
    (spit name-file jar-name)))


(defn jars [_]
  (clean nil)
  (uberjar  {:target-dir (str build-path "/uberjar/")})
  (jar      {:target-dir (str build-path "/jar/")})
  (deps-jar {:target-dir (str build-path "/depsjar/")}))