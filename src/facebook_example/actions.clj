(ns facebook-example.actions
  (:gen-class)
  (:require [facebook-example.facebook :as fb]
            [haversine.core :refer [haversine]]))

(defn convert-coords [coords]
  {:latitude (:lat coords) :longitude (:long coords)})

(defn fb-coords [c]
  (let [coords (:coordinates (:geometry c))]
    {:lat (second coords) :long (first coords)}))

(defn coords [c]
  (let [coords (:coordinates (:geometry c))]
    (convert-coords {:lat (second coords) :long (first coords)})))

(defn target->carouselelement [coordinates klo]
  (let [target (fb-coords klo)
        distance (Math/round (* 1000 (haversine (convert-coords coordinates) (coords klo))))]
    { :title "See directions:"
      :subtitle  (str "There is a toilet in " distance "m air-line distance")
      :image_url (str "https://maps.googleapis.com/maps/api/staticmap?" "size=500x400" "&markers=color:green%7Clabel:A%7C" (:lat coordinates) "," (:long coordinates) "&markers=color:red%7Clabel:B%7C" (:lat target) "," (:long target) "&key=AIzaSyBuPxyMXFkbeAJK1lIMhNg05TRcKTunUtc")
      :default_action {:type "web_url"
                       :url (str "https://google.com/maps/dir/" (:lat coordinates) "," (:long coordinates) "/" (:lat target) "," (:long target))}}))

(defn send-directions [user-id coordinates targets]
  (fb/send-message user-id (fb/generic-message (map (partial target->carouselelement coordinates) targets))))
