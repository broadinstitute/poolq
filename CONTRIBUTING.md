# Contributor guide

## About this document

This guide is for people who would like to contribute to PoolQ. It assumes that you have some experience with
[Scala](https://scala-lang.org/) development and some basic domain knowledge about FASTQ files and pooled screening.

## How can I help?

PoolQ uses the [fork and pull](https://help.github.com/articles/using-pull-requests/) model for contributions via
GitHub pull requests.

To contribute a feature to PoolQ, we recommend you take these steps:

1. [Identify a feature](#identify-a-feature)
2. [Let us know you are interested](#let-us-know-you-are-interested)
3. [Build PoolQ](#build-poolq)
4. [Implement the feature](#implement-the-feature)
5. [Write tests](#write-tests)
6. [Write documentation](#write-documentation)
7. [Submit pull request](#submit-pull-request)

### Identify a feature

We use the [GitHub issue tracking system](https://github.com/broadinstitute/poolq/issues) to track bugfixes
and improvements to PoolQ. If you believe you have encountered a bug or gap in PoolQ's functionality, please
consult issue tracker and check the PoolQ manual to verify the issue or the feature is not covered there.

### Let us know you are interested

You can register your interest in contributing a fix or a feature by creating or commenting on an issue in the
[GitHub issue tracking system](https://github.com/broadinstitute/poolq/issues). We can help vet and shape your
idea and offer you an indication of whether the feature would be of general interest.

### Build PoolQ

To build PoolQ, clone it using git (if you are submitting a change, you should clone your fork; the example here is for
cloning the PoolQ repository):

```sh
git clone git@github.com:broadinstitute/poolq.git
```

To build PoolQ you will need [sbt](https://www.scala-sbt.org/1.x/docs/Setup.html) and
[GNU Make](https://www.gnu.org/software/make/) installed.

Run `make test-all` to compile PoolQ and run its system tests.

### Implement the feature

Please be sure to follow best practices in software engineering when implementing your change. The PoolQ code is
generally focused on striking a balance between safety and performance. Remember that results must be reproducible but
also that PoolQ must process very large files and small performance impact can add up if it needs to be called 250
million times while processing an input data set.

#### Commits

Please limit the size of commits to bite-sized chunks. The best pull requests have small commits in a logical order,
with meaningful commit messages, and incrementally add functionality.

#### Code quality

PoolQ's build system enforces strict linting rules and warnings are fatal. Do not mark warnings as ignored without
consulting us first. Outside of the core loop, we avoid mutable data structures where possible and strictly limit the
scope of any mutable data structures or mutable references (i.e., `var`).

#### Code formatting

The Scala code in PoolQ is subject to formatting rules enforced by [scalafmt](https://scalameta.org/scalafmt/). We also
use [scalafix](https://github.com/liancheng/scalafix-organize-imports) to organize imports. We recommend you run
`scalafmt` first and then `scalafix` _before_ submitting a pull request.

#### Attribution

Proper citation and attribution is a key aspect of science and of software engineering. If your work is based on or
inspired by some other work, please say so and include attribution in your scaladoc comments. Remember to abide by
the [PoolQ license](LICENSE).

### Write tests

All features must come with automated tests. We prefer [munit](https://scalameta.org/munit/) for all new tests, but we
retain some legacy [ScalaTest](https://www.scalatest.org/) tests as well. Consider also using
[ScalaCheck](https://scalacheck.org/) as part of your test strategy (ScalaCheck tests may be combined with tests in
munit and ScalaTest).

For large features, you may also need to add system tests. For examples, see the [test-data](test-data) directory.

### Write documentation

For public methods, we recommend you write [scaladoc](https://docs.scala-lang.org/style/scaladoc.html). For user-facing
features, you must also add documentation to the [MANUAL.md](docs/MANUAL.md) file. We will take care of updating the
[CHANGELOG.md](docs/CHANGELOG.md) but please consider suggesting a short change description.

### Submit pull request

Remember to format your code using `scalafmt` and `scalafix` before submitting a pull request. If you have added any
new scala files, please also run `sbt headerCreate` to add copyright headers. Once you have submitted a pull request,
make additional or requested changes in subsequent commits. Please refrain from squashing or rebasing unless asked to
do so.
