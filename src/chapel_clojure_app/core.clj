(ns chapel-clojure-app.core
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [seesaw.core :as core]
            [seesaw.graphics :as g]
            [seesaw.dev :as dev]
            [clojure.java.io :refer [output-stream input-stream]]
            [clojure.pprint :refer (cl-format)])
  (:import [java.awt.image BufferedImage]
           [javax.swing JFrame JLabel ImageIcon WindowConstants SwingUtilities]
           [java.awt.event KeyEvent]
           [java.awt Graphics2D]
           [java.awt Color])
  (:use [clojure.java.shell :only [sh]])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; init temporary-file ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn copy-file [^java.io.File file ^java.io.BufferedInputStream stream]
  (with-open [in stream
              out (output-stream file)]
    (io/copy in out))
  file)

(defonce exec-file (if-not (.exists (io/as-file "temp"))
                     (let [file (java.io.File/createTempFile "temp" "")
                           _ (.deleteOnExit file)
                           _ (sh "sh" "-c" (str "chmod +x " file))]
                       (copy-file file
                                  (io/input-stream
                                   (io/resource "my-mandelbrot-chapel"))))
                     (copy-file  (io/file "temp")
                                 (io/input-stream
                                  (io/resource "my-mandelbrot-chapel")))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; draw mandelbrot buffered-image with Chapel ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defonce image-data (ref nil))

(defn get-binary
  " require nothing
    return clojure.lang.PersistentVector "
  []
  @image-data
  )

(defn byte-array->color-array
  " return lazy-seq (int (java.awt.color))"
  [^clojure.lang.PersistentVector byte-array]
  (let [binary-array byte-array
        black (.getRGB Color/BLACK)
        white (.getRGB Color/WHITE)
        new-int (for [i binary-array]
                  (loop [c 0
                         acc (list)]
                    (if (< c 8)
                      (if (= 0 (bit-and (bit-shift-right i c) 0x01))
                        (recur (inc c) (conj acc black))
                        (recur (inc c) (conj acc white)))
                      acc)))]
    (reduce into [] new-int)))

(defn color-array->buffered-image
  " return buffered-image"
  [^clojure.lang.PersistentVector color-array]
  (let [array (int-array color-array)
        buffered-image (BufferedImage. 640 640 BufferedImage/TYPE_4BYTE_ABGR)
        _ (.setRGB buffered-image 0 0 640 640 array 0 640)]
    buffered-image))


(defn redraw [{:keys [size xstart ystart]}]
  (let [n " --n=640"
        size (str " --size=" size)
        xstart (str " --xstart=" xstart)
        ystart (str " --ystart=" ystart)
        program (io/file exec-file)
        ]
    (dosync
     (ref-set image-data  (:out (sh "sh" "-c" (str program xstart ystart size n) :out-enc :bytes)))))
  (println ""))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; attributes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn img []
  (color-array->buffered-image (byte-array->color-array (get-binary))))

;; init state
(def initial-pos {:size 2.0 :xstart -1.5 :ystart -1.0})
(defonce refpos (ref initial-pos))
(defonce _ (redraw {:size 2.0 :xstart -1.5 :ystart -1.0}))

(add-watch refpos :watcher
           (fn [key ref old-value new-value]
             (println "old: " old-value
                      " new: " new-value)))


;; declare move direction
(defn position [n]
  (cond (> n 400) 3
        (> n 200) 2
        :default 1))

(defn set-new-pos [x y size xstart ystart]
  (cond
    (and (= 3 x) (= 3 y))
    {:size size :xstart (+ xstart (/ size 8)) :ystart (+ ystart (/ size 8))}
    (and (= 3 x) (= 2 y))
    {:size size :xstart (+ xstart (/ size 8)) :ystart ystart}
    (and (= 3 x) (= 1 y))
    {:size size :xstart (+ xstart (/ size 8)) :ystart (- ystart (/ size 8))}
    (and (= 2 x) (= 3 y))
    {:size size :xstart xstart :ystart (+ ystart (/ size 8))}
    (and (= 2 x) (= 2 y))
    {:size size :xstart xstart :ystart ystart}
    (and (= 2 x) (= 1 y))
    {:size size :xstart xstart :ystart (- ystart (/ size 8))}
    (and (= 1 x) (= 3 y))
    {:size size :xstart (- xstart (/ size 8)) :ystart (+ ystart (/ size 8))}
    (and (= 1 x) (= 2 y))
    {:size size :xstart (- xstart (/ size 8)) :ystart ystart}
    (and (= 1 x) (= 1 y))
    {:size size :xstart (- xstart (/ size 8)) :ystart (- ystart (/ size 8))}
    ))

(defn set-new-pos2 [key size xstart ystart]
  (cond
    (= key KeyEvent/VK_UP)
    {:size (/ size 2) :xstart (+ xstart (/ size 4)) :ystart (+ ystart (/ size 4))}
    (= key KeyEvent/VK_DOWN)
    {:size (* size 2) :xstart (- xstart (/ size 4)) :ystart (- ystart (/ size 4))}
    :default {:size size :xstart xstart :ystart ystart}
    ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; frame ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn -main
  [& args] 
  (core/invoke-later
   (-> (core/frame :id :f
                   :title "Fractal: Mandelbrot"
                   :size [640 :by 640]
                   :on-close :exit
                   :listen [:key-pressed
                            (fn [e]
                              (let [key (.getKeyCode e)
                                    pos @refpos
                                    size (:size pos)
                                    xstart (:xstart pos)
                                    ystart (:ystart pos)
                                    new-pos (set-new-pos2 key size xstart ystart)]
                                (dosync
                                 (ref-set refpos new-pos)
                                 (redraw @refpos))
                                (core/config!
                                 (core/select (core/to-root e) [:#label]) :icon (img))
                                (core/repaint!
                                 (core/select (core/to-root e) [:#label])))
                              )]
                   :content
                   (core/label
                    :id :label
                    :icon (ImageIcon. (img))
                    :listen [:mouse-clicked
                             (fn [e]
                               (let [x (position (.getX e))
                                     y (position (.getY e))
                                     pos @refpos
                                     size (:size pos)
                                     xstart (:xstart pos)
                                     ystart (:ystart pos)
                                     new-pos (set-new-pos x y size xstart ystart)]
                                 (dosync
                                  (ref-set refpos new-pos)
                                  (redraw @refpos))
                                 (core/config!
                                  (core/select (core/to-root e) [:#label]) :icon (img))
                                 (core/repaint!
                                  (core/select (core/to-root e) [:#label]))))]))
       core/pack!
       core/show!)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
