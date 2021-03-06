(ns facebook-example.bot
  (:gen-class)
  (:require [clojure.string :as s]
            [environ.core :refer [env]]
            [facebook-example.facebook :as fb]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [haversine.core :refer [haversine]]
            [facebook-example.actions :as actions]))

(defn load-db []
  (if-let [data (slurp (io/file (io/resource "WCANLAGEOGD.json")))]
    (try (json/read-str data :key-fn keyword)
      (catch Exception e {}))))

(defn on-message [payload]
  (println "on-message payload:")
  (println payload)
  (let [sender-id (get-in payload [:sender :id])
        recipient-id (get-in payload [:recipient :id])
        time-of-message (get-in payload [:timestamp])
        message-text (get-in payload [:message :text])]
    (cond
      (s/includes? (s/lower-case message-text) "help")
      (fb/send-message sender-id (fb/text-message "Hi there, happy to help :)"))

      (s/includes? (s/lower-case message-text) "image")
      (fb/send-message sender-id (fb/image-message "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/M101_hires_STScI-PRC2006-10a.jpg/1280px-M101_hires_STScI-PRC2006-10a.jpg"))

      ; If no rules apply echo the user's message-text input
      :else
      (fb/send-message sender-id (fb/text-message message-text)))))

(defn greet [sender-id]
      (fb/send-message sender-id (fb/text-message (str "Welcome, how are you today?")))
      (fb/send-message sender-id (fb/text-message "Gotta go pee? I can show you the way to the closest public toilets. Please send me your location.")))

(defn on-postback [payload]
  (println "on-postback payload:")
  (println payload)
  (let [sender-id (get-in payload [:sender :id])
        recipient-id (get-in payload [:recipient :id])
        time-of-message (get-in payload [:timestamp])
        postback (get-in payload [:postback :payload])
        referral (get-in payload [:postback :referral :ref])]
    (cond
      (= postback "GET_STARTED")
      (greet sender-id)
      ;(fb/send-message sender-id (fb/text-message "Welcome =)"))

      :else
      (fb/send-message sender-id (fb/text-message "Sorry, I don't know how to handle that postback")))))

(defn convert-coords [coords]
  {:latitude (:lat coords) :longitude (:long coords)})

(defn fb-coords [c]
  (let [coords (:coordinates (:geometry c))]
    {:lat (second coords) :long (first coords)}))

(defn coords [c]
  (let [coords (:coordinates (:geometry c))]
    (convert-coords {:lat (second coords) :long (first coords)})))

(defn sorted-klos [klos coordinates]
  (sort-by (fn [klo] (haversine (coords klo) (convert-coords coordinates))) klos))

(defn on-location [sender-id attachment]
  (let [coordinates (get-in attachment [:payload :coordinates])
        klos (sorted-klos (:features (load-db)) coordinates)]
    (actions/send-directions sender-id coordinates (take 3 klos))))

(defn on-attachments [payload]
  (println "on-attachment payload:")
  (println payload)
  (let [sender-id (get-in payload [:sender :id])
        recipient-id (get-in payload [:recipient :id])
        time-of-message (get-in payload [:timestamp])
        attachments (get-in payload [:message :attachments])
        attachment (first attachments)]
    (cond
      (= (:type attachment) "location")
      (on-location sender-id attachment)

      :else
      (fb/send-message sender-id (fb/text-message "Thanks for your attachments :)")))))
