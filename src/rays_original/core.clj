(ns rays-original.core
  (:require [clojure.string :refer [join]]
            [serial.core :as serial]))

(defn send-command [port command & args]
  (let [command (join " " (cons command args))]
    (serial/write port (.getBytes (str command "\n")))))

(defn blink-led [port n]
  (send-command port "blink" n))

