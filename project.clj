(defproject competing-consumers-ordering-spike "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.6.0"]
    [com.novemberain/monger "3.0.2"]
    [com.novemberain/langohr "3.6.1"]
  ]
  :main ^:skip-aot competing-consumers-ordering-spike.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
