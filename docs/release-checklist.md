# FixMock 0.2.0 release checklist

Nothing in this checklist authorizes deployment, Central publication, tag creation, tag push, or GitHub release publication. Each action requires explicit maintainer approval.

## Release gates

- [ ] Milestone issues are closed or explicitly deferred.
- [ ] The parent, all reactor modules, `consumer-pom.xml`, README examples, and changelog use the intended release version.
- [ ] The published POM SCM tag is `0.2.0`.
- [ ] `mvn --batch-mode clean verify` passes with QuickFIX/J 2.3.2.
- [ ] The same command passes with `-Dquickfixj.version=3.0.2`.
- [ ] CI passes Java 17 with both QuickFIX/J versions and Java 25 with QuickFIX/J 3.0.2.
- [ ] Peer-side acceptor and initiator transport gates pass.
- [ ] JUnit, REST, lifecycle, example, and integration gates pass.
- [ ] The packaged consumer passes for both QuickFIX/J versions.
- [ ] Dependency convergence passes.
- [ ] Core JaCoCo line coverage remains at or above 80 percent.
- [ ] japicmp reports no binary or source incompatibility with 0.1.0.
- [ ] `mvn --batch-mode -Prelease -Dgpg.skip=true clean verify` produces valid main, source, and Javadoc artifacts.
- [ ] The release profile rejects SNAPSHOT project versions and dependencies.
- [ ] Normal verification does not load the Central publishing extension.
- [ ] POM metadata, licenses, checksums, signatures, Maven coordinates, and dependency scopes are reviewed.
- [ ] Central-only dependency resolution and both packaged-consumer lanes pass.
- [ ] README, compatibility table, changelog, and migration notes are final.
- [ ] The Spring Boot starter interoperability fixture is explicitly deferred to the 0.3.0 milestone.

## Approval gates

- [ ] The release-preparation pull request passes all required checks and is approved for merge.
- [ ] The exact merged release commit is checked out in a clean worktree.
- [ ] A signed bundle is created with `-DskipUpload=true` and inspected locally.
- [ ] The maintainer explicitly approves upload to Central.
- [ ] The maintainer inspects the validated Central deployment.
- [ ] The maintainer explicitly approves Central publication.
- [ ] Maven Central reports the deployment as `PUBLISHED`.
- [ ] Published artifacts resolve from a fresh local Maven repository.
- [ ] The maintainer explicitly approves the signed `0.2.0` tag and push.
- [ ] The maintainer explicitly approves the GitHub release.

## After publication

- [ ] Verify each published POM, main JAR, source JAR, Javadoc JAR, signature, and checksum from Maven Central.
- [ ] Create and push the approved signed `0.2.0` tag.
- [ ] Publish the approved GitHub release notes.
- [ ] Prepare `0.3.0-SNAPSHOT` in a separate pull request.
- [ ] Update the Spring Boot starter dependency through a separate pull request.
