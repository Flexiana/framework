(ns framework.app.view.css.tailwind.resolvers
  (:require
    [com.wsscode.pathom3.connect.indexes :as pci]
    [com.wsscode.pathom3.connect.operation :as pco]
    [com.wsscode.pathom3.interface.smart-map :as psm]
    [com.wsscode.tailwind-garden.preparers :as prep]))

(pco/defresolver get-bases []
                 {:bases (prep/generate-base-components-no-mqueries @prep/css-db)})

(pco/defresolver get-bases:sm []
                 {:bases:sm (prep/generate-base-components-with-sm @prep/css-db)})

(pco/defresolver get-bases:md []
                 {:bases:md (prep/generate-base-components-with-md @prep/css-db)})

(pco/defresolver get-bases:lg []
                 {:bases:lg (prep/generate-base-components-with-lg @prep/css-db)})

(pco/defresolver get-bases:xl []
                 {:bases:lg (prep/generate-base-components-with-xl @prep/css-db)})

(pco/defresolver get-bases:2xl []
                 {:bases:2xl (prep/generate-base-components-with-2xl @prep/css-db)})

(pco/defresolver get-theme []
                 {:theme (:theme @prep/css-db)})

(pco/defresolver get-container []
                 {:container (:container @prep/css-db)})

(pco/defresolver get-animation []
                 {:animation (:animation @prep/css-db)})

(pco/defresolver default-components []
                 {:default-components (prep/generate-default-components @prep/css-db)})

(pco/defresolver get-hiccup-classes []
                 {:user-css @prep/user-css})

(def indexes
  (pci/register [get-theme
                 get-container
                 get-animation
                 default-components
                 get-bases
                 get-bases:sm
                 get-bases:md
                 get-bases:lg
                 get-bases:xl
                 get-bases:2xl
                 get-hiccup-classes]))

(def smart-css-map
  (-> (psm/smart-map indexes)
      (psm/sm-touch! [:default-components
                      :theme
                      :container
                      :animation
                      :bases
                      :bases:sm
                      :bases:md
                      :bases:lg
                      :bases:xl
                      :bases:2xl])))
