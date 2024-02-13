fullversion := $(shell grep -m 1 'ThisBuild / version :=' ./version.sbt | perl -pe 's/^ThisBuild \/ version := "([0-9]+\.[0-9]+\.[0-9]+).*$$/$$1/g')

version := $(shell grep -m 1 'ThisBuild / version :=' ./version.sbt | perl -pe 's/^ThisBuild \/ version := "([0-9]+\.[0-9+]).*$$/$$1/g')

.phony: print-version print-fullversion
print-version:
	@echo $(version)

print-fullversion:
	@echo $(fullversion)

target-dir := target/dist/poolq-$(fullversion)

default:
	echo "there is no default target"

$(target-dir):
	mkdir -p $@

test-all: package
	(cd test-data; make)

# make a distribution zip file
.PHONY: dist
dist: prep-dist
	(cd target/dist; zip -r ../poolq-$(fullversion).zip poolq-$(fullversion)/*)

.phony: launch-scripts
launch-scripts: $(target-dir)
	cp launch-scripts/* $(target-dir)

# this target assumes you have installed pandoc and LaTeX
.PHONY: docs
docs: LICENSE CHANGELOG.md docs/MANUAL.md $(target-dir)
	pandoc docs/MANUAL.md -s --metadata pagetitle="PoolQ 3 Manual" -o $(target-dir)/index.html
	pandoc docs/MANUAL.md -s -o $(target-dir)/poolq-$(fullversion)-manual.pdf
	pandoc CHANGELOG.md -s -o $(target-dir)/poolq-$(fullversion)-changelog.pdf
	cp LICENSE $(target-dir)

# prepare to make a distribution zip file by assembling everything in target/dist/poolq-$(fullversion)/
.PHONY: prep-dist
prep-dist: clean test-all $(target-dir) launch-scripts docs
	cp target/bin/poolq3.jar $(target-dir)
	cp -R test-data $(target-dir)

# tell me the current version
.PHONY: version fullversion
version:
	@echo $(version)

fullversion:
	@echo $(fullversion)

.PHONY: package
package:
	sbt clean test assembly

target/bin/poolq3.jar: package

clean:
	rm -rf $(target-dir)
	sbt clean
