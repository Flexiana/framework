#!/bin/bash
clojure -M:install --version $(bb script/project-version)
for f in ./examples/*; do
    if [ -d "$f" ]; then
        cd $f
        pwd
        if test -f project.clj; then
          lein check-style src test
        else
          clojure -M:format check
        fi
        status=$?
        if ! test $status -eq 0; then
          exit $status
        fi
        if test -f project.clj; then
          lein test
        else
          clojure -M:test
        fi
        status=$?
        if ! test $status -eq 0; then
          exit $status
        fi
        cd $OLDPWD
    fi
done
