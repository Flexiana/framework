#!/bin/bash
clojure -M:install --version $(bb script/project-version)
for f in ./examples/*; do
    if [ -d "$f" ]; then
        cd $f
        pwd
        lein check-style src test
        status=$?
        if ! test $status -eq 0; then
          exit $status
        fi
        lein test
        status=$?
        if ! test $status -eq 0; then
          exit $status
        fi
        cd $OLDPWD
    fi
done
