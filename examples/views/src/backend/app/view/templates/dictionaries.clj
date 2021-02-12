(ns view.templates.dictionaries
  (:require
    [tongue.core :as tongue]))

(def en-string-dates
  {:weekdays-narrow ["S" "M" "T" "W" "T" "F" "S"]
   :weekdays-short  ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"]
   :weekdays-long   ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"]
   :months-narrow   ["J" "F" "M" "A" "M" "J" "J" "A" "S" "O" "N" "D"]
   :months-short    ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
   :months-long     ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"]})

(def fr-string-dates
  {:weekdays-narrow ["D" "L" "M" "M" "J" "V" "S"]
   :weekdays-short  ["Dim" "Lun" "Mar" "Mer" "Jeu" "Ven" "Sam"]
   :weekdays-long   ["Dimanche" "Lundi" "Mardi" "Mercredi" "Jeudi" "Vendredi" "Samedi"]
   :months-narrow   ["J" "F" "M" "A" "M" "J" "J" "A" "S" "O" "N" "D"]
   :months-short    ["Janv" "Fevr" "Mars" "Avr" "Mai" "Juin" "Juil" "Aout" "Sept" "Oct" "Nov" "Dec"]
   :months-long     ["Janvier" "Fevrier" "Mars" "Avril" "Mai" "Juin" "Juillet" "Aout" "Septembre" "Octobre" "Novembre" "Decembre"]})

(def home-dict
  {:en {:greet-title "Hello from {1}!"
        :date (tongue/inst-formatter "{weekdays-long} {day}, {months-long} {year}" en-string-dates)
        :present-date "The current date is {1}"}
   :fr {:greet-title "Bonjour de {1}!"
        :date (tongue/inst-formatter "{weekdays-long} {day}, {months-long} {year}" fr-string-dates)
        :present-date "La date actuelle est {1}"}})
