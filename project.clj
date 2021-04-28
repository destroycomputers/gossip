(defproject gossip "0.1.0-SNAPSHOT"
  :description "A simple API that responds with silly taunts and gossips with a capability to uwu-ize given text."
  :url "https://github.com/destroycomputers/gossip"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[compojure "1.6.1"]
                 [http-kit "2.3.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [mount "0.1.16"]
                 [org.clojure/clojure "1.10.2-alpha1"]
                 [org.clojure/tools.cli "1.0.206"]
                 [ring/ring-codec "1.1.2"]]
  :main ^:skip-aot gossip.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "1.0.0"]
                                  [org.clojure/tools.namespace "1.0.0"]
                                  [org.clojure/java.classpath "1.0.0"]]}})
