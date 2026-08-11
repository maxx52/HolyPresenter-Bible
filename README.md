This is a Kotlin Multiplatform project targeting Desktop (JVM).

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
      folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and
options:

- Desktop app:
    - Hot reload: `./gradlew :desktopApp:hotRun --auto`
    - Standard run: `./gradlew :desktopApp:run`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

## Bible data

The module bundles the 66-book Russian Synodal Bible for offline use. The text is public domain and comes from
[eBible.org](https://ebible.org/find/details.php?id=russyn).

To regenerate the HolyPresenter JSON resource from the reviewed USFM archive:

```shell
python tools/import_synodal.py \
  /path/to/russyn_usfm.zip \
  desktopApp/src/main/resources/bible/translations/synodal.json
```

The importer verifies the source archive checksum and the expected book, chapter, and verse counts before writing
the resource.
