# Go core AAR

The preferred core source is pinned as the `android/core` submodule. Run
`../scripts/build-go-core.sh` from this directory's parent project, or invoke the
Gradle `buildGoCore` task, to generate `android/core/build/distrust-core.aar`.

For compatibility testing, a legacy AAR may still be placed at
`android/app/libs/zju-connect.aar`. It is used only when the pinned DistrustCore
AAR is absent. AAR binaries are intentionally ignored by Git; release artifacts
must be produced from the pinned AGPL source.
