(ns rays-original.core
  (:require [clojure.string :refer [join]]
            [serial.core :as serial]
            [gniazdo.core :as ws]
            [clojure.data.json :as json]
            [clj-http.client :as http]))

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

(defn lat-lon-from-rsvp [rsvp]
  (let [{{venue_lat :lat venue_lon :lon} :venue
         {group_lat :group_lat group_lon :group_lon} :group} rsvp]
    (if (and venue_lat venue_lon)
      {:lat venue_lat :lon venue_lon}
      {:lat group_lat :lon group_lon})))

;; a rough approximation
(defn rsvp-in-nyc [rsvp]
  (let [state (-> rsvp :group :group_state)
        {:keys [lat lon]} (lat-lon-from-rsvp rsvp)]
    (and (= state "NY")
         (<= lon -73.709679)
         (>= lon -74.259338)
         (<= lat 40.904172)
         (>= lat 40.48926))))

;; http://api.geonames.org/findNearbyPostalCodesJSON?lat=40.712692&lng=-74.015076&username=demo
(def zips-url "http://api.geonames.org/findNearbyPostalCodesJSON")
(def zips-username "bgruber")
(defn fetch-zip [lat lon]
  (let [response (http/get zips-url
                           {:query-params {:lat lat,
                                           :lng lon,
                                           :username zips-username}})
        json-str (:body response)
        data     (json/read-str json-str :key-fn keyword)
        results  (:postalCodes data)]
    (:postalCode (first results))))

;; TODO add api-key to avoid rate limit?
(def cities-url "https://api.meetup.com/2/cities")
(defn fetch-city [lat lon]
  (let [response (http/get cities-url
                           {:query-params {:lat lat,
                                           :lon lon,
                                           :page 1}})
        json-str (:body response)
        data     (json/read-str json-str :key-fn keyword)
        results  (:results data)
        city     (first results)]
    city))

(defn rsvp-handler [jsonString]
  (let [rsvp (json/read-str jsonString :key-fn keyword)]
    (if (rsvp-in-nyc rsvp)
      (let [{:keys [lat lon]} (lat-lon-from-rsvp rsvp)
            zip               (fetch-zip lat lon)]
        (println (-> rsvp :event :event_name)
                 (-> rsvp :group :group_city)
                 zip)))))

(comment
  (listen-rsvps! rsvp-handler)

  (unlisten-rsvps!)

  )
