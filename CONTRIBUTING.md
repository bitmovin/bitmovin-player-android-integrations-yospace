# Contributing

Thanks for contributing to this project. Your time and input are appreciated. To get the most out of the project, please consider the following.

## Code of Conduct

Please note we have a [Code of Conduct](CODE_OF_CONDUCT.md), please follow it in all your interactions with the project.

## Pull Request Process

Pull requests are welcome and pair well with bug reports and feature requests. Here are some tips to follow before submitting your first PR:

- Fork the repository to your own account if you haven't already.
- Stay consistent to the file formats of this project.
- Develop in a fix or feature branch (`fix/describe-your-fix`, `feature/describe-your-feature`), not in `main`.
- Make your changes in your fork.
- Validate your changes according to the [Yospace Validation Procedure](https://developer.yospace.com/sdk-documentation/android/userguide/latest/en/validate-your-app.html).
- Add an entry to the [CHANGELOG.md](CHANGELOG.md) file in the `[Unreleased]` section to describe the changes to the project.
- Submit a pull request to the main repository.

The versioning scheme we use is [SemVer](http://semver.org/).

## Releasing

Releases are automated and tag-driven; `main` is the only long-lived branch. Do **not** bump
the version or edit release headings in `CHANGELOG.md` manually — just add your changes under
the `## [Unreleased]` section in the appropriate group (`Added`, `Changed`, `Fixed`, etc.).

To cut a release, a maintainer triggers the **Release new version** GitHub Action
(`workflow_dispatch`) on `main`. It will:

- determine the next version from the `## [Unreleased]` entries — `Added`/`Changed`/`Removed`
  yield a minor bump, `Fixed`/`Security`/`Deprecated` a patch bump;
- bump `libraryVersion` and `libraryCode` in `gradle.properties`;
- promote the `## [Unreleased]` section to a dated version heading via the
  [`org.jetbrains.changelog`](https://github.com/JetBrains/gradle-changelog-plugin) plugin;
- commit, tag (`X.Y.Z`), and push `main`;
- publish the AAR to Artifactory and create a GitHub release.

All additions, modifications and fixes that are submitted will be reviewed. The project owners reserve the right to reject any pull request that does not meet our standards. We may not be able to respond to all pull requests immediately and provide no timeframes to do so.
