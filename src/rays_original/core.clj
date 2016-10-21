(ns rays-original.core
  (:require [clojure.string :refer [join]]
            [serial.core :as serial]
            [gniazdo.core :as ws]))

(defn send-command [port command & args]
  (let [command (join " " (cons command args))]
    (serial/write port (.getBytes (str command "\n")))))

(defn blink-led [port n]
  (send-command port "blink" n))

(def rsvp-url "ws://stream.meetup.com/2/rsvps")

(def socket-agent (agent nil))
(defn listen-rsvps! [handler]
  (send socket-agent
        (fn [socket handler]
          (when (nil? socket)
            (ws/connect rsvp-url :on-receive handler)))
        handler))
(defn unlisten-rsvps! []
  (send socket-agent
        (fn [socket]
          (when-not (nil? socket)
            (ws/close socket)
            nil))))
