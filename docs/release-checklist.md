# Release checklist

Replace `<version>`, `<next-version>`, and `<previous-version>` with the intended values. Nothing in this checklist authorizes deployment, Central publication, tag creation, tag push, or GitHub release publication. Each action requires explicit maintainer approval.

## Release preparation

- [ ] Create `release/<version>` from a green `main` branch.
- [ ] Close milestone issues or explicitly record their deferral.
- [ ] Set the parent, all reactor modules, and `consumer-pom.xml` to `<version>`.
- [ ] Set the published POM SCM tag and README examples to `<version>`.
- [ ] Finalize a dated changelog section and compatibility or migration notes.
- [ ] Compare the public API with `<previous-version>`.

## Release gates

- [ ] `mvn --batch-mode clean verify` passes with QuickFIX/J 2.3.2.
- [ ] The same command passes with `-Dquickfixj.version=3.0.2`.
- [ ] CI passes Java 17 with both QuickFIX/J versions and Java 25 with QuickFIX/J 3.0.2.
- [ ] Peer-side acceptor and initiator transport gates pass.
- [ ] JUnit, REST, lifecycle, example, and integration gates pass.
- [ ] The packaged consumer passes for both QuickFIX/J versions.
- [ ] Dependency convergence passes.
- [ ] Core JaCoCo line coverage remains at or above 80 percent.
- [ ] japicmp reports no binary or source incompatibility with `<previous-version>`.
- [ ] `mvn --batch-mode -Prelease -Dgpg.skip=true clean verify` produces valid main, source, and Javadoc artifacts.
- [ ] The release profile rejects SNAPSHOT project versions and dependencies.
- [ ] Normal verification does not load the Central publishing extension.
- [ ] POM metadata, licenses, checksums, signatures, Maven coordinates, dependency scopes, and reproducibility are reviewed.
- [ ] Central-only dependency resolution and both packaged-consumer lanes pass.
- [ ] Required ecosystem interoperability passes or its milestone deferral is recorded.

## Approval gates

- [ ] The release-preparation pull request passes all required checks and is approved for merge.
- [ ] The exact merged release commit is checked out in a clean worktree.
- [ ] A signed bundle is created with `-DskipUpload=true` and inspected locally.
- [ ] The maintainer explicitly approves upload to Central.
- [ ] Central validates the deployment without automatically publishing it.
- [ ] The maintainer inspects the validated deployment and explicitly approves publication.
- [ ] Maven Central reports the deployment as `PUBLISHED`.
- [ ] Published artifacts resolve from a fresh local Maven repository.
- [ ] The maintainer explicitly approves the signed `<version>` tag and push.
- [ ] The maintainer explicitly approves the GitHub release.

## After publication

- [ ] Verify each published POM, main JAR, source JAR, Javadoc JAR, signature, and checksum from Maven Central.
- [ ] Create and push the approved signed `<version>` tag.
- [ ] Publish the approved GitHub release notes.
- [ ] Prepare `<next-version>-SNAPSHOT` in a separate pull request.
- [ ] Update downstream consumers, including the Spring Boot starter, through separate pull requests.
