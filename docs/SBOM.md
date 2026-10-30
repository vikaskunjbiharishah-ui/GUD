# SBOM

Pinned versions from `android-native/gradle/libs.versions.toml`. This is a dependency list, not a penetration test.

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.2.20 |
| Hilt | 2.57.2 |
| Compose BOM | 2025.09.00 |
| Activity | 1.10.1 |
| Lifecycle | 2.9.0 |
| Navigation | 2.9.0 |
| DataStore | 1.1.4 |
| AndroidX core | 1.16.0 |
| Coroutines | 1.10.2 |
| JUnit 5 | 5.11.4 |
| JUnit 4 | 4.13.2 |
| MockK | 1.13.17 |
| AndroidX test | 1.2.1 |
| Espresso | 3.6.1 |
| SQLCipher | 4.17.0 |
| AndroidX SQLite | 2.7.0 |
| Play Billing | 7.1.1 |
| Play services auth | 21.3.0 |
| security-crypto | 1.1.0-alpha06 |
| WorkManager | 2.10.0 |

JDK 17. Gradle 8.13 is installed on the host and is not committed as a wrapper jar. The frozen parser under `src/casparser-main` is not edited. Chaquopy wheels stay inside the app assets.
