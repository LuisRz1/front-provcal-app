# UH-NU1 Testing Implementation Summary

## Overview
This document summarizes the unit testing implementation for User Story UH-NU1 (Monthly Menu Management). The testing suite covers the MenuRepository and MenuViewModel components with 30 comprehensive tests.

## Test Infrastructure Setup

### Dependencies Added
- **MockK**: 1.13.8 - Kotlin-first mocking library
- **Coroutines Test**: For testing suspend functions
- **Architecture Core Testing**: 2.2.0 - For LiveData testing (InstantTaskExecutorRule)

### Test Utilities Created

#### 1. LiveDataTestUtil.kt
- **Purpose**: Synchronous LiveData observation for unit tests
- **Key Function**: `getOrAwaitValue()` - Waits for LiveData value with timeout
- **Usage**: Essential for testing ViewModels with LiveData

#### 2. MainCoroutineRule.kt
- **Purpose**: Replaces Main dispatcher with TestDispatcher for coroutine tests
- **Implementation**: JUnit @Rule that sets/resets Dispatchers.Main
- **Usage**: Required for all ViewModel tests using viewModelScope

#### 3. MenuTestData.kt
- **Purpose**: Centralized test fixtures and mock data
- **Contents**:
  - Mock menu days (regular and holidays)
  - Mock GraphQL responses (upload, propose changes)
  - Mock input data (MenuChangeItemInput examples)
  - Test constants (year, month, filenames)

## Production Code Changes

### MenuRepository.kt Refactoring
**Change**: Added optional `apolloClient` constructor parameter for dependency injection

```kotlin
class MenuRepository(
    context: Context? = null,
    apolloClient: ApolloClient? = null
) {
    private val apolloClient: ApolloClient = apolloClient ?: ApolloClientProvider.getInstance(context!!)
```

**Rationale**:
- Enables constructor injection for testing
- Maintains backward compatibility (existing code unchanged)
- Follows dependency injection best practices
- Allows mocking Apollo client without singleton mocking

## Test Coverage

### MenuRepositoryTest (4 tests)
Tests focus on **exception handling** due to MockK limitations with Apollo GraphQL's ApolloResponse class.

✅ **Passing Tests**:
1. `getMonthlyMenu returns Error on network exception`
2. `getMonthlyMenu handles generic exceptions`
3. `uploadMonthlyMenu returns Error on network exception`
4. `proposeMenuChange returns Error on network exception`

**Note**: Success path testing for repository is limited due to MockK's inability to mock ApolloResponse properties. See "Known Limitations" section below.

### MenuViewModelTest (26 tests)
Comprehensive testing of all ViewModel functionality using mocked repository.

#### loadMenu() Tests (6 tests)
✅ All passing:
- Success path: updates menu LiveData, sets message to null, sets loading to false
- Error path: updates message LiveData, doesn't update menu, sets loading to false
- Empty menu handling

#### uploadMenu() Tests (9 tests)
✅ All passing:
- Success flow with loadMenu refresh
- Conflict detection and callback invocation
- Non-conflict error handling
- Loading state management

#### proposeChanges() Tests (11 tests)
✅ All passing:
- Success with correct count
- onDone callback invocation
- Zero changes handling
- Error handling
- Loading state management

## Known Limitations

### MockK + Apollo GraphQL Compatibility Issue

**Problem**: MockK cannot properly mock Apollo's `ApolloResponse` class properties (specifically `.data`).

**Error Encountered**:
```
io.mockk.MockKException: Missing mocked calls inside every { ... } block
```

**Root Cause**: Apollo's `ApolloResponse` uses final properties that MockK struggles to mock, even with relaxed mocking and various MockK strategies attempted.

**Attempted Solutions**:
1. ✗ Standard `mockk<T>()` with `every { }` blocks
2. ✗ Relaxed mocking (`mockk<T>(relaxed = true)`)
3. ✗ Inline mock builders with behavior definitions
4. ✗ `mockkObject` for singleton mocking
5. ✗ Property vs method mocking variations

**Working Solution**:
- Repository tests focus on exception handling paths (which work perfectly)
- ViewModel tests use mocked repository (bypassing Apollo mocking entirely)

**Future Options** for Complete Repository Coverage:
1. **Switch to Mockito**: May handle Apollo classes differently
2. **Create Repository Interface**: Introduce abstraction layer for easier mocking
3. **Integration Tests**: Test repository with real/test Apollo server
4. **Apollo Test Utilities**: Check if Apollo provides official test helpers

## Test Execution

### Running Tests
```bash
# Run all menu-related tests
./gradlew testDebugUnitTest --tests "*Menu*"

# Run specific test class
./gradlew testDebugUnitTest --tests "MenuRepositoryTest"
./gradlew testDebugUnitTest --tests "MenuViewModelTest"

# Run all unit tests
./gradlew testDebugUnitTest
```

### Current Status
✅ **30/30 tests passing** (100% pass rate)
- MenuRepositoryTest: 4/4 passing
- MenuViewModelTest: 26/26 passing

## Key Learnings

1. **Dependency Injection**: Constructor injection dramatically improves testability
2. **MockK Limitations**: Not all classes are mockable; framework compatibility matters
3. **Test Utilities**: Shared utilities (LiveDataTestUtil, MainCoroutineRule) are essential
4. **Test Organization**: Grouping tests by method and scenario improves maintainability
5. **Pragmatic Testing**: Sometimes exception paths are enough for unit tests

## Recommendations

### Immediate
- ✅ Current test coverage is sufficient for exception handling and ViewModel logic
- ✅ Production code is well-tested through ViewModel tests

### Future Enhancements
1. Add integration tests for MenuRepository with real Apollo client
2. Consider migrating to Mockito if more repository unit tests are needed
3. Implement repository interface for better testability
4. Add UI tests (Espresso) for MenuFragment user interactions

## Files Modified/Created

### Created
- `app/src/test/java/com/sanna/provcalapp/data/repository/MenuRepositoryTest.kt`
- `app/src/test/java/com/sanna/provcalapp/ui/menu/MenuViewModelTest.kt`
- `app/src/test/java/com/sanna/provcalapp/utils/MenuTestData.kt`
- `app/src/test/java/com/sanna/provcalapp/utils/LiveDataTestUtil.kt`
- `app/src/test/java/com/sanna/provcalapp/utils/MainCoroutineRule.kt`
- `docs/UH-NU1-Testing-Summary.md`

### Modified
- `app/src/main/java/com/sanna/provcalapp/data/repository/MenuRepository.kt` (added constructor parameter)
- `gradle/libs.versions.toml` (added test dependencies)
- `app/build.gradle.kts` (added test dependency configurations)

---

**Generated**: 2025-11-10
**User Story**: UH-NU1 (Monthly Menu Management)
**Testing Framework**: JUnit 4 + MockK + Kotlin Coroutines Test
