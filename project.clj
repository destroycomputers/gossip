(defproject gossip "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                 [ring/ring-codec "1.1.2"]
                 [compojure "1.6.1"]
                 [mount "0.1.16"]
                 [javax.servlet/servlet-api "2.5"]
                 [http-kit "2.3.0"]]
  :main ^:skip-aot gossip.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "1.0.0"]
                                  [org.clojure/tools.namespace "1.0.0"]
                                  [org.clojure/java.classpath "1.0.0"]]}})
