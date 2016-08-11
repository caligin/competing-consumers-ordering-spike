(ns competing-consumers-ordering-spike.core
  (:gen-class)
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:import [com.mongodb MongoOptions ServerAddress]))   

(defn next-state [current-state command]
    ((keyword command)
        ((keyword current-state)
        {:init {:create :edit}
         :edit {:update :edit :close :closed}
         :closed {}
        })))

(defn load-state [mongo id] (if-let [{:keys [state]} (mc/find-map-by-id mongo "things" id)]
    state
    :init))

(defn update-state [mongo id state] (mc/update mongo "things" {:_id id} {:state state} {:upsert true}))

(defn make-message-handler [mongo]
  (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
      (let [[id, command] (clojure.string/split (String. payload "UTF-8") #":")]
        (update-state mongo id (or (next-state (load-state mongo id) command) :fuckedup)))))

(defn -main
  "Consumer"
  [& args]
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        qname "needsordering"
        mconn (mg/connect)
        mongo   (mg/get-db mconn "things")]
    (println (format "Consumer Connected. Channel id: %d" (.getChannelNumber ch)))
    (lq/declare ch qname {:exclusive false :auto-delete true})
    (lq/bind    ch qname "things" {:routing-key "events.for.*"})
    (lc/subscribe ch qname (make-message-handler mongo) {:auto-ack true})
    (Thread/sleep 60000)
    (println "Closing")
    (rmq/close ch)
    (rmq/close conn)
    (mg/disconnect mconn)))
