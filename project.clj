(defproject gossip "0.1.0-SNAPSHOT"
  :description "A simple API that responds with silly taunts and gossips with a capability to uwu-ize given text."
  :url "https://github.com/destroycomputers/gossip"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :plugins [[io.taylorwood/lein-native-image "0.3.1"]]
  :native-image {:name "gossip"
                 :opts ["--initialize-at-build-time=gossip,clojure,instaparse,mount,com,org,medley,cheshire,compojure,ring,clout"
                        "--enable-https"
                        "--allow-incomplete-classpath"
                        "--no-fallback"
                        "-H:+ReportExceptionStackTraces"
                        "--report-unsupported-elements-at-runtime"]}
  :dependencies [[cheshire "5.10.2"]
                 [compojure "1.6.1"]
                 [http-kit "2.6.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [mount "0.1.16"]
                 [org.clojure/clojure "1.11.0"]
                 [org.clojure/tools.cli "1.0.206"]
                 [ring/ring-codec "1.1.2"]]
  :main ^:skip-aot gossip.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "1.0.0"]
                                  [org.clojure/tools.namespace "1.0.0"]
                                  [org.clojure/java.classpath "1.0.0"]]}})
