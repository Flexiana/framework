(ns framework.app.view.css.tailwind.preparers
  (:refer-clojure :exclude [bases])
  (:require
    [com.wsscode.tailwind-garden.components.accessibility :as accessibility]
    [com.wsscode.tailwind-garden.components.backgrounds :as backgrounds]
    [com.wsscode.tailwind-garden.components.base :as base]
    [com.wsscode.tailwind-garden.components.borders :as borders]
    [com.wsscode.tailwind-garden.components.box-alignment :as box-alignment]
    [com.wsscode.tailwind-garden.components.effects :as effects]
    [com.wsscode.tailwind-garden.components.flexbox :as flexbox]
    [com.wsscode.tailwind-garden.components.grid :as grid]
    [com.wsscode.tailwind-garden.components.interactivity :as interactivity]
    [com.wsscode.tailwind-garden.components.layout :as layout]
    [com.wsscode.tailwind-garden.components.sizing :as sizing]
    [com.wsscode.tailwind-garden.components.spacing :as spacing]
    [com.wsscode.tailwind-garden.components.svg :as svg]
    [com.wsscode.tailwind-garden.components.tables :as tables]
    [com.wsscode.tailwind-garden.components.transforms :as transforms]
    [com.wsscode.tailwind-garden.components.transitions :as transitions]
    [com.wsscode.tailwind-garden.components.typography :as typography]
    [com.wsscode.tailwind-garden.expanders :as exp]
    [framework.app.view.css.tailwind.helpers :as hlp]))

(defn layout'
  []
  (reduce into
    [(layout/box-sizing)
     (layout/display)
     (layout/floats)
     (layout/clear)
     (layout/object-fit)
     (layout/object-position)
     (layout/overflow)
     (layout/overscroll-behavior)
     (layout/position)
     (layout/top-right-left-bottom)
     (layout/visibility)
     (layout/z-index)]))

(defn flexbox'
  []
  (reduce into
    [(flexbox/flex-direction)
     (flexbox/flex-wrap)
     (flexbox/flex)
     (flexbox/flex-grow)
     (flexbox/flex-shrink)
     (flexbox/order)]))

(defn grid'
  []
  (reduce into
    [(grid/grid-template-columns)
     (grid/grid-column-start-end)
     (grid/grid-template-rows)
     (grid/grid-row-start-end)
     (grid/grid-auto-flow)
     (grid/grid-auto-columns)
     (grid/grid-auto-rows)
     (grid/gap)]))

(defn box-alignment'
  []
  (reduce into
    [(box-alignment/justify-content)
     (box-alignment/justify-items)
     (box-alignment/justify-self)
     (box-alignment/align-content)
     (box-alignment/align-items)
     (box-alignment/align-self)
     (box-alignment/place-content)
     (box-alignment/place-items)
     (box-alignment/place-self)]))

(defn spacing'
  []
  (reduce into
    [(spacing/margin)
     (spacing/padding)
     (spacing/space-between)]))

(defn sizing'
  []
  (reduce into
    [(sizing/width)
     (sizing/min-width)
     (sizing/max-width)
     (sizing/height)
     (sizing/min-height)
     (sizing/max-height)]))

(defn typography'
  []
  (reduce into
    [(typography/font-family)
     (typography/font-size)
     (typography/font-smoothing)
     (typography/font-style)
     (typography/font-weight)
     (typography/font-variant-numeric)
     (typography/letter-spacing)
     (typography/line-height)
     (typography/list-style-type)
     (typography/list-style-position)
     (exp/with-variants ["focus"]
                        (typography/placeholder-color))
     (exp/with-variants ["focus"]
                        (typography/placeholder-opacity))
     (typography/text-align)
     (exp/with-variants ["hover"]
                        (typography/text-color))
     (exp/with-variants ["hover"]
                        (typography/text-opacity))
     (exp/with-variants ["hover"]
                        (typography/text-decoration))
     (typography/text-transform)
     (typography/text-overflow)
     (typography/vertical-align)
     (typography/whitespace)
     (typography/word-break)]))

(defn backgrounds'
  []
  (reduce into
    [(backgrounds/background-attachment)
     (backgrounds/background-color)
     (backgrounds/background-clip)
     (exp/with-variants ["hover" "focus" "active"]
                        (backgrounds/background-color))
     (exp/with-variants ["hover" "focus" "active"]
                        (backgrounds/background-opacity))
     (backgrounds/background-position)
     (backgrounds/background-repeat)
     (backgrounds/background-size)
     (backgrounds/background-image)
     (exp/with-variants ["hover" "focus" "active"]
                        (backgrounds/gradient-color-stops))]))

(defn borders'
  []
  (reduce into
    [(borders/border-radius)
     (exp/with-variants ["hover" "focus"]
                        (borders/border-color))
     (borders/border-width)
     (exp/with-variants ["hover" "focus"]
                        (borders/border-opacity))
     (borders/border-style)
     (borders/divide-width)
     (borders/divide-color)
     (borders/divide-opacity)
     (borders/divide-style)
     (exp/with-variants ["hover" "focus"]
                        (borders/ring-width))
     (exp/with-variants ["hover" "focus"]
                        (borders/ring-color))
     (exp/with-variants ["hover" "focus"]
                        (borders/ring-opacity))
     (exp/with-variants ["hover" "focus"]
                        (borders/ring-offset-width))
     (exp/with-variants ["hover" "focus"]
                        (borders/ring-offset-color))

     (exp/with-variants ["hover" "focus"]
                        (effects/box-shadow))
     (exp/with-variants ["hover" "focus" "disabled"]
                        (effects/opacity))]))

(defn tables'
  []
  (reduce into
    [(tables/border-collapse)
     (tables/table-layout)]))

(defn transitions'
  []
  (reduce into
    [(transitions/transition-property)
     (transitions/transition-duration)
     (transitions/transition-timing-function)
     (transitions/transition-delay)
     (transitions/animation)

     (transforms/transform)
     (transforms/transform-origin)
     (exp/with-variants ["hover"]
                        (transforms/scale))
     (exp/with-variants ["hover"]
                        (transforms/rotate))
     (exp/with-variants ["hover"]
                        (transforms/translate))
     (exp/with-variants ["hover"]
                        (transforms/skew))]))

(defn transforms'
  []
  (reduce into
    [(transforms/transform)
     (transforms/transform-origin)
     (exp/with-variants ["hover"]
                        (transforms/scale))
     (exp/with-variants ["hover"]
                        (transforms/rotate))
     (exp/with-variants ["hover"]
                        (transforms/translate))
     (exp/with-variants ["hover"]
                        (transforms/skew))]))

(defn interactivity'
  []
  (reduce into
    [(interactivity/appearance)
     (interactivity/cursor)
     (exp/with-variants ["focus"]
                        (interactivity/outline))
     (interactivity/pointer-events)
     (interactivity/resize)
     (interactivity/user-select)]))

(defn svg'
  []
  (reduce into
    [(svg/fill)
     (svg/stroke)
     (svg/stroke-width)

     (exp/with-variants ["focus"]
                        (accessibility/screen-readers))]))

(def css-keys-in-hiccup (atom #{}))
(def user-css (atom {}))

(def css-db
  (atom (hash-map :bases {:layout (layout')
                          :flexbox (flexbox')
                          :grid (grid')
                          :box-alignment (box-alignment')
                          :spacing (spacing')
                          :sizing (sizing')
                          :typography (typography')
                          :backgrounds (backgrounds')
                          :borders (borders')
                          :tables (tables')
                          :transitions (transitions')
                          :transforms (transforms')
                          :interactivity (interactivity')
                          :svg (svg')}
                  :ring-vars (borders/ring-vars)
                  :animation (transitions/animation-frames)
                  :theme base/preflight
                  :container (layout/container))))

(defn generate-default-components
  [{:keys [theme ring-vars]}]
  (reduce into [theme ring-vars]))

(defn generate-base-components-no-mqueries
  "A function that returns the map of base components without media queries.
  e.g {:bases {:.class-name ['.class-name' {:css-properties 'css-args'}]}}"
  [{:keys [bases]}]
  (let [{:keys [layout flexbox grid box-alignment spacing sizing typography
                backgrounds borders tables transitions transforms interactivity
                svg]} bases
        reduced-bases (reduce into [layout flexbox grid box-alignment spacing sizing typography
                                    backgrounds borders tables transitions transforms interactivity
                                    svg])]
    (hlp/garden->map reduced-bases)))

(defn generate-base-components-with-sm
  [{:keys [bases]}]
  (let [{:keys [layout flexbox grid box-alignment spacing sizing typography
                backgrounds borders tables transitions transforms interactivity
                svg]} bases
        reduced-bases (reduce into [layout flexbox grid box-alignment spacing sizing typography
                                    backgrounds borders tables transitions transforms interactivity
                                    svg])]
    (hlp/unfold-responsive-selectors "640px" "sm" reduced-bases)))

(defn generate-base-components-with-md
  [{:keys [bases]}]
  (let [{:keys [layout flexbox grid box-alignment spacing sizing typography
                backgrounds borders tables transitions transforms interactivity
                svg]} bases
        reduced-bases (reduce into [layout flexbox grid box-alignment spacing sizing typography
                                    backgrounds borders tables transitions transforms interactivity
                                    svg])]
    (hlp/unfold-responsive-selectors "768px" "md" reduced-bases)))

(defn generate-base-components-with-lg
  [{:keys [bases]}]
  (let [{:keys [layout flexbox grid box-alignment spacing sizing typography
                backgrounds borders tables transitions transforms interactivity
                svg]} bases
        reduced-bases (reduce into [layout flexbox grid box-alignment spacing sizing typography
                                    backgrounds borders tables transitions transforms interactivity
                                    svg])]
    (hlp/unfold-responsive-selectors "1024px" "lg" reduced-bases)))

(defn generate-base-components-with-xl
  [{:keys [bases]}]
  (let [{:keys [layout flexbox grid box-alignment spacing sizing typography
                backgrounds borders tables transitions transforms interactivity
                svg]} bases
        reduced-bases (reduce into [layout flexbox grid box-alignment spacing sizing typography
                                    backgrounds borders tables transitions transforms interactivity
                                    svg])]
    (hlp/unfold-responsive-selectors "1280px" "xl" reduced-bases)))

(defn generate-base-components-with-2xl
  [{:keys [bases]}]
  (let [{:keys [layout flexbox grid box-alignment spacing sizing typography
                backgrounds borders tables transitions transforms interactivity
                svg]} bases
        reduced-bases (reduce into [layout flexbox grid box-alignment spacing sizing typography
                                    backgrounds borders tables transitions transforms interactivity
                                    svg])]
    (hlp/unfold-responsive-selectors "1536px" "2xl" reduced-bases)))
