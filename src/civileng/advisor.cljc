(ns civileng.advisor
  "CivilEngineersAdvisor — proposes a structural design operation
  (approve a design, approve occupancy) for a registered organization.
  Swappable mock/llm; the advisor ONLY proposes — `civileng.governor`
  checks the load-capacity utilization and material-grade membership
  independently. Modeled on cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-design|:approve-occupancy
               :effect :propose :structure-id str :load number
               :grade str :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake structure-id load grade] :as request}]
  {:op op
   :effect :propose
   :structure-id structure-id
   :load load
   :grade grade
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a civil engineering advisor. Given a request, propose an
   :op, the :structure-id, :load and :grade, an honest :confidence and
   a :stake. Never call an over-capacity design or an unapproved
   material grade conforming — the governor computes the utilization
   ratio and checks grade membership.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
