<<<<<<< HEAD
(defproject chapel-clojure-app "0.1.0"
  :description "Fractal: Mandelbrot"
=======

(defproject chapel-clojure-app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
>>>>>>> 23f9f2b13a47684e639c01bee8f3de9a3db6e285
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.443"]
                 [seesaw "1.4.5"]]
<<<<<<< HEAD
  :profiles {:uberjar {:main chapel-clojure-app.core, :aot :all}
             :dev {:resource-paths ["resources"]}}
=======
  :injections [(clojure.lang.RT/loadLibrary org.opencv.core.Core/NATIVE_LIBRARY_NAME)]
>>>>>>> 23f9f2b13a47684e639c01bee8f3de9a3db6e285
  :main chapel-clojure-app.core)
