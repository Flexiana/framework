# Development guide
TODO fill this with everything a developer starting to work on Xiana might need to know

* Ticket System
* Coding standards
* Submitting a PR
* Making a release


## Generating API Docs

This is done automatically using *Codox*

To generate or update the current version run the script:

```shell
script/build-docs.sh
```

This runs the following:

```shell
lein codox
mv docs/new docs/{{version-number}}
```

It also updates the index.html file to point to the new version.
