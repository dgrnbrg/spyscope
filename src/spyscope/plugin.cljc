(ns spyscope.plugin)

(defn middleware [project]
  (update-in project [:injections] concat
             `[(spit "resources/project.clj" ~(prn-str project))]))
