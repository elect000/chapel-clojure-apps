(ns chapel-clojure-app.core
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [seesaw.core :as core]
            [seesaw.graphics :as g]
            [seesaw.dev :as dev])
  (:import [java.awt.image BufferedImage]
           [javax.swing JFrame JLabel ImageIcon WindowConstants SwingUtilities]
           [java.awt.event KeyEvent]
           [java.awt Graphics2D]
           [org.opencv.core Core Mat CvType]
           [org.opencv.imgcodecs Imgcodecs]
           [org.opencv.imgproc Imgproc])
  (:use [clojure.java.shell :only [sh]])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; initial
(clojure.lang.RT/loadLibrary org.opencv.core.Core/NATIVE_LIBRARY_NAME)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; draw mandelbrot with Chapel ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn mat->buffered-image [mat]
  (let [gray? (= (.channels mat) 1)
        image-type (if gray?
               BufferedImage/TYPE_BYTE_GRAY
               BufferedImage/TYPE_3BYTE_BGR)
        buffered-image (g/buffered-image (.width mat) (.height mat) image-type)
        ;; sun.awt.image.BufferedImage
        raster (.getRaster buffered-image)
        ;; java.awt.image.ByteInterleavedRaster
        byte-pixels (-> raster
                        (.getDataBuffer) ;; java.awt.image.DataBufferByte
                        (.getData)) ;; byte array
        buffer (byte-array (* (.width mat) (.height mat) (if gray? 1 3)))
        ;; 3byte -> rgb-color / 1byte -> Gray-color
        ]
    (.get mat 0 0 buffer) ;; buffer <- mat-data
    (System/arraycopy buffer 0 byte-pixels 0 (alength buffer))
    buffered-image))

(defn redraw [{:keys [size xstart ystart]}]
  (let [n "--n=600"
        size (str " --size=" size)
        xstart (str " --xstart=" xstart)
        ystart (str " --ystart=" ystart)
        program (str " " (io/file (io/resource "../resources/my-mandelbrot-chapell")))
        path (str (io/file (io/resource "../resources/image4.pbm")))]
    (sh "bash" "-c" (str program size xstart ystart " --n=600 " " > " path))))

(defn mandelbrot-image []
  (Imgcodecs/imread (str (io/file (io/resource "../resources/image4.pbm")))))
;; org.opencv.core.Mat
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn img []
  (mat->buffered-image (mandelbrot-image)))

(def initial-pos {:size 2.0 :xstart -1.5 :ystart -1.0})
(defonce refpos (ref initial-pos))
(defonce _ (redraw {:size 2.0 :xstart -1.5 :ystart -1.0}))
;; init image
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

(defn -main
  [& args]
  (core/invoke-later
   (-> (core/frame :id :f
                   :title "Fractal: Mandelbrot"
                   :size [600 :by 600]
                   :on-close :exit
                   :listen [:key-pressed
                            (fn [e]
                              (println (= KeyEvent/VK_UP (.getKeyCode e)))
                              (let [key (.getKeyCode e)
                                    pos @refpos
                                    size (:size pos)
                                    xstart (:xstart pos)
                                    ystart (:ystart pos)
                                    new-pos (set-new-pos2 key size xstart ystart)]
                                (dosync
                                 (ref-set refpos new-pos))
                                (redraw @refpos)
                                (core/config! (core/select (core/to-root e) [:#label]) :icon (img))
                                (core/repaint! (core/select (core/to-root e) [:#label])))
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
                                  (ref-set refpos new-pos))
                                 (redraw @refpos)
                                 (core/config! (core/select (core/to-root e) [:#label]) :icon (img))
                                 (core/repaint! (core/select (core/to-root e) [:#label]))
                                 (println new-pos)))]))
       core/pack!
       core/show!)))
