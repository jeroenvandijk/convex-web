(ns convex-web.site.session
  (:require [convex-web.site.runtime :refer [disp sub]]
            [convex-web.site.gui :as gui]
            [convex-web.site.stack :as stack]

            [clojure.string :as str]

            [re-frame.core :as re-frame]
            [convex-web.site.db :as db]
            [convex-web.site.backend :as backend]))

(re-frame/reg-sub :session/?session
  (fn [{:site/keys [session]} _]
    session))

(re-frame/reg-sub :session/?id
  :<- [:session/?session]
  (fn [{:convex-web.session/keys [id]} _]
    (or id "-")))

(re-frame/reg-sub :session/?state
  :<- [:session/?session]
  (fn [{:convex-web.session/keys [state]} _]
    state))

(re-frame/reg-event-db :session/!set-state
  (fn [db [_ f args]]
    (update-in db [:site/session :convex-web.session/state] (fn [state]
                                                              (apply f state args)))))

(re-frame/reg-sub :session/?accounts
  :<- [:session/?session]
  (fn [{:convex-web.session/keys [accounts]} _]
    accounts))

(re-frame/reg-sub :session/?default-address
  :<- [:session/?accounts]
  (fn [[{:convex-web.account/keys [address]}] _]
    address))

(re-frame/reg-sub :session/?selected-address
  :<- [:session/?session]
  (fn [{:convex-web.session/keys [selected-address]} _]
    selected-address))

(re-frame/reg-sub :session/?active-address
  :<- [:session/?default-address]
  :<- [:session/?selected-address]
  (fn [[default-address selected-address] _]
    (or selected-address default-address)))

(re-frame/reg-event-db :session/!set-status
  (fn [db [_ status]]
    (assoc-in db [:site/session :ajax/status] status)))

(re-frame/reg-sub :session/?status
  (fn [db _]
    (get-in db [:site/session :ajax/status])))

(re-frame/reg-event-db :session/!create
  (fn [db [_ session]]
    (assoc db :site/session session)))

(re-frame/reg-event-db :session/!pick-address
  (fn [db [_ address]]
    (assoc-in db [:site/session :convex-web.session/selected-address] address)))

(defn pick-address [address]
  (disp :session/!pick-address address))

(re-frame/reg-event-fx :session/!add-account
  (fn [{:keys [db]} [_ account {:keys [active?]}]]
    (merge {:db (update-in db [:site/session :convex-web.session/accounts] conj account)}
           (when active?
             {:dispatch [:session/!pick-address (get account :convex-web.account/address)]}))))

(defn ?status []
  (sub :session/?status))

(defn ?active-address []
  (sub :session/?active-address))

(defn initialize []
  (db/transact assoc-in [:site/session :ajax/status] :ajax.status/pending)

  (backend/GET-session
    {:handler
     (fn [session]
       (db/transact update :site/session merge {:ajax/status :ajax.status/success} session))

     :error-handler
     (fn [error]
       (db/transact update :site/session merge {:ajax/status :ajax.status/error
                                                :ajax/error error}))}))

(defn add-account [account & [active?]]
  (disp :session/!add-account account {:active? active?}))

(defn ?session []
  (sub :session/?session))

(defn ?id []
  (sub :session/?id))

(defn ?state []
  (sub :session/?state))

(defn set-state [f & args]
  (disp :session/!set-state f args))

(defn ?accounts []
  (sub :session/?accounts))

(defn SessionPage [_ {:keys [convex-web.session/id]} set-state]
  [:div.flex.flex-1.justify-center.my-4.mx-10
   [:div.flex.flex-col.flex-1

    (when (?active-address)
      [:<>
       [:span.text-xs.text-indigo-500.uppercase "Session"]
       [:div.flex.items-center
        [:code.text-sm.mr-2 (?id)]
        [gui/ClipboardCopy (?id)]]])

    [:span.text-xs.text-indigo-500.uppercase.mt-10 "Session Key"]
    [:input.text-sm.border
     {:style {:height "26px"}
      :type "text"
      :value id
      :on-change
      #(let [value (gui/event-target-value %)]
         (set-state assoc :convex-web.session/id value))}]

    [:div.flex.justify-center.mt-6
     [gui/DefaultButton
      {:on-click #(stack/pop)}
      [:span.text-xs.uppercase "Cancel"]]

     [:div.mx-2]

     [gui/DefaultButton
      {:disabled (str/blank? id)
       :on-click
       #(do
          (set! (.-cookie js/document) (str "ring-session=" id))
          (.reload (.-location js/document)))}
      [:span.text-xs.uppercase "Restore"]]]]])

(def session-page
  #:page {:id :page.id/session
          :title "Session"
          :component #'SessionPage})
