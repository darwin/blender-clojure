# Tools for generating API files


##### We provide following scripts:

  * **apigen** - which takes the sphinx xml files and generates cljs files

##### You can launch them using our wrapper shell scripts:

  * `./scripts/generate-xml.sh` - produces intermediate xml files in `.workspace/xml`.
  * `./scripts/build-api.sh` - runs apigen to generate cljs files from xml files. Produces cljs sources in `../bcljs/src/gen`.
