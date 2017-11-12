(defproject chapel-clojure-app "0.1.0"
  :description "Fractal: Mandelbrot"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.443"]
                 [seesaw "1.4.5"]]
  :profiles {:uberjar {:main chapel-clojure-app.core, :aot :all}
             :dev {:resource-paths ["resources"]}}
  :main chapel-clojure-app.core)
