(ns rays-original.core
  (:require [clojure.string :refer [join]]
            [serial.core :as serial]
            [gniazdo.core :as ws]
            [clojure.data.json :as json]
            [clj-http.client :as http]))

(defn send-command [port command & args]
  (let [command (join " " (cons command args))]
    (serial/write port (.getBytes (str command "\n")))))

(defonce serial-port (atom nil))
(defn blink-led
  ([n] (blink-led @serial-port n))
  ([port n] (send-command port "blink" n)))

(defn abcdefm []
  (send-command @serial-port "value"))

(def zip-to-led
  {
   ;; staten island
   10309 0
   10312 0
   10306 0
   10314 0
   10303 0
   10301 0
   10310 0

   10004 1
   10007 1
   10006 1
   10280 1
   10013 1

   10038 2
   10002 2

   10009 3
   10010 3

   10003 4
   10012 4
   10014 4

   10011 5
   10001 5

   10018 6
   10036 6

   10110 7
   10020 7
   10112 7
   10019 7

   10022 8
   10044 8
   10017 8

   10065 9
   10021 9
   10075 9
   10028 9
   10128 9

   10024 10

   10023 11
   10025 11

   10027 12


   10026 13
   10030 13

   10035 14
   10037 14

   10454 15

   10451 16

   10039 17

   10032 18

   10463 19

   10034 20

   10456 21

   10459 22

   10474 23

   10473 24

   10472 25

   10460 26

   10457 26

   10468 27

   10471 28

   10470 29

   10467 30

   10466 31
   10469 31

   10475 32
   10464 32 

   11363 39

   11365 41

   11423 42

   11433 43
   11412 43

   11434 44
   11413 44

   11430 45
   11422 45

   11414 46
   11239 46

   11236 47

   11234 48
   11229 48

   11235 49

   11224 50
   11223 50

   11214 51

   11228 52

   11209 53

   11220 54

   11219 55

   11204 56

   11230 57

   11210 58

   11203 59

   11225 60

   11226 61

   11215 62

   11232 63

   11231 64

   11201 65

   11205 66
   11217 66

   11238 67

   11216 68

   11233 69
   11213 69

   11207 70

   11385 71

   11421 72

   11379 73

   11237 74

   11221 75

   11206 76

   11211 77

   11222 78

   11101 79

   11106 80

   11102 81

   11105 82

   11378 83

   11374 84

   11418 85

   11415 86
   11435 86

   11366 87

   11432 88

   11367 89

   11375 90

   11373 91
   11377 91

   11104 92

   11103 93

   11370 94
   11371 94
   11369 94

   11368 95
   11354 95
   11356 95

   11357 96

   11360 97

   11358 98

   11364 99
   11361 99
})
(defn led-for-zip [zip]
  (zip-to-led (Integer/parseInt zip)))

(def rsvp-url "ws://stream.meetup.com/2/rsvps")

(defonce socket-agent (agent nil))
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

(def valuable-rsvpers
  #{
    6 ;; scott
    1790684 ;; yvette
    11202681;; real brian
    })
(defn rsvp-is-valuable [rsvp]
  (let [memberid (-> rsvp :member :member_id)]
    (valuable-rsvpers memberid)))

(defn rsvp-handler [jsonString]
  (let [rsvp (json/read-str jsonString :key-fn keyword)]
    (cond
      (rsvp-is-valuable rsvp) (abcdefm)
      (rsvp-in-nyc rsvp)
      (let [{:keys [lat lon]} (lat-lon-from-rsvp rsvp)
            zip               (fetch-zip lat lon)
            led               (led-for-zip zip)]
        (blink-led led)
        (println (-> rsvp :event :event_name)
                 (-> rsvp :group :group_city)
                 led))
      :default nil)))


(defn shutdown []
  (unlisten-rsvps!)
  (swap! serial-port #(when-not (nil? %) (serial/close! %) nil)))

(defn list-ports []
  (map #(.getName %) (serial/port-identifiers)))
(defn guess-port []
  (first (filter #(.startsWith % "ttyUSB") (list-ports))))

(defn restart
  ([] (restart (guess-port)))
  ([port]
   (shutdown)
   (reset! serial-port (serial/open port))
   (listen-rsvps! rsvp-handler)))


(comment
  (restart)

  (shutdown)
)
