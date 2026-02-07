(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'social.mushin/spike)
(def version "0.6.9")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src/clj" "src/cljs" "src/cljc"]
                :description "Clojure/ClojureScript HTTP request/response library"
                :url "https://github.com/Mushinman/spike"
                :licenses [{:name "Apache License 2.0"
                            :url "https://www.apache.org/licenses/LICENSE-2.0"}]
                :scm {:url "https://github.com/Mushinman/spike"
                      :connection "scm:git:git://github.com/Mushinman/spike.git"
                      :developerConnection "scm:git:ssh://git@github.com/Mushinman/spike.git"
                      :tag (str "v" version)}})
  (b/copy-dir {:src-dirs ["src/clj" "src/cljs" "src/cljc" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file})
  (println "Built" jar-file))

(defn install [_]
  (jar nil)
  (b/install {:basis basis
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir})
  (println "Installed" lib version))

(defn deploy [_]
  (jar nil)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   {:installer :remote
    :artifact jar-file
    :pom-file (b/pom-path {:lib lib :class-dir class-dir})})
  (println "Deployed" lib version "to Clojars"))
