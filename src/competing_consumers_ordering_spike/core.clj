(ns competing-consumers-ordering-spike.core
  (:gen-class)
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [monger.core :as mg]
            [monger.collection :as mc]
            [clojure.repl])
  (:import [com.mongodb MongoOptions ServerAddress]
           [com.rabbitmq.client QueueingConsumer]
           com.mongodb.WriteConcern))   



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
        (if (= "create" command) (println (str "id " id)))
        (update-state mongo id (or (next-state (load-state mongo id) command) :fuckedup))
        (lb/ack ch delivery-tag))))

(defn make-exit-barrier []
  (let [barrier (promise)]
    (clojure.repl/set-break-handler! (fn [_] (deliver barrier nil)))
    barrier))

(defn acquire-lock [conn]
  (let [channel        (lch/open conn)
        lockqname "lock"
        consumer (QueueingConsumer. channel)]
          (println "acquiring lock")
          (lb/consume channel lockqname consumer)
          (doall (take 1 (lc/deliveries-seq consumer)))
          (println "lock acquired")))

(defn -main
  "Consumer"
  [& args]
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        qname "needsordering"
        mconn (mg/connect)
        mongo   (mg/get-db mconn "things")]
    (mg/set-default-write-concern! WriteConcern/JOURNALED)
    (rmq/add-shutdown-listener conn (fn [_] (println "SHADDAUN")))
    (acquire-lock conn)
    (lb/qos ch 1)
    (lq/declare ch qname {:durable true :auto-delete false})
    (lq/bind    ch qname "things" {:routing-key "events.for.*"})
    (let [consumertag (lc/subscribe ch qname (make-message-handler mongo) {})]
      @(make-exit-barrier)
      (println "Closing")
      (lb/cancel ch consumertag)
      (rmq/close ch)
      (rmq/close conn)
      (mg/disconnect mconn))))
