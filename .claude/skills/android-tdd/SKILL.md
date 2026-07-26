---
name: android-tdd
description: Test-Driven Development for Android. A sub-agent writes FAILING JVM tests first, then the main model writes the minimum production code to make them pass. All tests run on the JVM (no emulator, no device) using Robolectric, Turbine, kotlinx-coroutines-test, MockK, and in-memory Room. Use when adding or changing a ViewModel, Composable, repository, use-case, DAO, mapper, or any Android/Kotlin behaviour. Triggers on "TDD", "test-first", "red-green-refactor", "write tests then implement", "add feature with tests".
---

# Android TDD (JVM-only, no device)

## Hard rules

1. **Never write production code before a failing test for it exists.** If you catch yourself typing implementation before a red test, stop.
2. Tests live in `app/src/test/java/...` (local JVM unit tests). **Not** `androidTest/` — that requires a device and defeats the purpose.
3. Run `./gradlew :app:testDebugUnitTest`. Never `connectedAndroidTest`.
4. One behaviour change = one RED → GREEN → REFACTOR cycle. Don't batch multiple features into one cycle.

## Workflow

### Phase 1 — RED: sub-agent writes failing tests

Spawn a sub-agent with the `Agent` tool (`subagent_type: general-purpose`) using the prompt template below. The sub-agent's job is to write tests **only** — it must not touch production code.

After the sub-agent returns, run:

```bash
./gradlew :app:testDebugUnitTest
```

Confirm the new tests **fail** (compile error or assertion failure). If they pass, the test isn't actually exercising the new behaviour — send the sub-agent back with that feedback.

### Phase 2 — GREEN: main model writes minimum code

Write the smallest production change that makes the failing tests pass. Re-run `./gradlew :app:testDebugUnitTest` frequently. Do not add code the tests don't require (no extra methods, no defensive branches, no unrelated refactors).

### Phase 3 — REFACTOR

With tests green, clean up names, duplication, and structure. Re-run tests after every change. If a test goes red during refactor, **revert the refactor** — do not edit the test to match.

## Sub-agent prompt template (Phase 1)

Use verbatim, filling in `{FEATURE}` and `{FILES}`:

> You are in Phase 1 of a TDD loop for an Android Kotlin project. Write **failing tests only**. Do NOT create, modify, or delete any production code — no changes under `app/src/main/`.
>
> **Feature / behaviour to test:** {FEATURE}
>
> **Existing files to read (do not modify):** {FILES}
>
> **Test location:** `app/src/test/java/...` mirroring the package of the code under test.
>
> **Stack (use these, nothing else):**
> - JUnit4
> - Robolectric (`@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [34])` for anything touching Android framework or Compose)
> - `kotlinx-coroutines-test` — wrap async tests in `runTest { }`, use `UnconfinedTestDispatcher` for `Dispatchers.Main` replacement
> - Turbine — for `Flow` / `StateFlow` assertions via `flow.test { … }`
> - MockK — for mocks and relaxed mocks
> - `androidx.compose.ui.test` with `createComposeRule()` for Composable tests (runs on JVM via Robolectric)
>
> **Rules:**
> - Each test name describes the behaviour in backticks: `` `emits Error when repository throws` ``.
> - One assertion per test when practical; use Turbine's `awaitItem()` for stream shape.
> - No test depends on another test's state.
> - If a class needs a fake instead of a mock, put it in `app/src/test/java/.../fakes/` — do not add fakes under `main/`.
>
> **Return in your summary:**
> 1. Paths of each test file created.
> 2. One line per test describing the behaviour it pins down.
> 3. The exact command to run them.

## Stack setup (one-time, per module)

### `app/build.gradle.kts`

```kotlin
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true   // required for Robolectric + Compose
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Coroutines / Flow
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("app.cash.turbine:turbine:1.1.0")

    // Mocking
    testImplementation("io.mockk:mockk:1.13.13")

    // Compose testing on JVM (via Robolectric)
    testImplementation(platform("androidx.compose:compose-bom:2024.10.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

Pin versions to whatever the project already uses for Compose BOM / AGP — don't downgrade silently.

## Patterns

### ViewModel + StateFlow + Turbine

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FavouritesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `emits Loaded with items after fetch`() = runTest {
        val repo = mockk<FavRepo> { coEvery { load() } returns listOf(Fav("a")) }
        val vm = FavouritesViewModel(repo)

        vm.state.test {
            assertEquals(UiState.Loading, awaitItem())
            vm.fetch()
            assertEquals(UiState.Loaded(listOf(Fav("a"))), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Composable (runs on JVM via Robolectric)

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `shows empty state when favourites list is empty`() {
        compose.setContent { HomeScreen(state = HomeState(favourites = emptyList())) }
        compose.onNodeWithText("No favourites yet").assertIsDisplayed()
    }

    @Test
    fun `clicking app invokes onAppClick with package name`() {
        var clicked: String? = null
        compose.setContent {
            HomeScreen(
                state = HomeState(favourites = listOf(AppEntry("Chrome", "com.android.chrome"))),
                onAppClick = { clicked = it }
            )
        }
        compose.onNodeWithText("Chrome").performClick()
        assertEquals("com.android.chrome", clicked)
    }
}
```

### Room DAO (in-memory, JVM)

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: FavDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.favDao()
    }

    @After fun teardown() = db.close()

    @Test
    fun `inserted favourite is returned by observe`() = runTest {
        dao.insert(FavEntity("com.x"))
        dao.observe().test {
            assertEquals(listOf(FavEntity("com.x")), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

Known quirk: some Robolectric versions flake on Room in-memory DBs. If tests hang, pin Robolectric ≥ 4.12 and ensure `includeAndroidResources = true`.

### Pure Kotlin (no Android) — skip Robolectric

Mappers, use-cases, and other plain Kotlin classes don't need `@RunWith(RobolectricTestRunner::class)`. Plain JUnit is ~5× faster — reserve Robolectric for code that touches the Android framework or Compose.

## Commands reference

| Goal | Command |
|------|---------|
| Run all JVM tests | `./gradlew :app:testDebugUnitTest` |
| Run one test class | `./gradlew :app:testDebugUnitTest --tests "com.ultimatefocus.app.FavouritesViewModelTest"` |
| Run one test method | `./gradlew :app:testDebugUnitTest --tests "*FavouritesViewModelTest.emits Loaded*"` |
| Continuous (rerun on change) | `./gradlew :app:testDebugUnitTest -t` |

## When a device test is actually required

Keep these in `androidTest/` and out of this workflow — they need hardware or emulator:

- Edge-to-edge / window insets
- Picture-in-picture
- WebView rendering
- Accessibility service behaviour
- Multi-process / cross-app intents

For everything else above the data layer, JVM tests are sufficient.
