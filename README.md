# assertk

[![](https://jitpack.io/v/matfax/assertk3.svg)](https://jitpack.io/#matfax/assertk3)
[![Build Status](https://travis-ci.com/matfax/assertk.svg?branch=master)](https://travis-ci.com/matfax/assertk)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1efeebd7825146e2bbcd24b58e7fca71)](https://www.codacy.com/app/matfax/assertk3?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=matfax/assertk3&amp;utm_campaign=Badge_Grade)
![GitHub License](https://img.shields.io/github/license/matfax/assertk3.svg)
![GitHub last commit](https://img.shields.io/github/last-commit/matfax/assertk3.svg)
![Libraries.io for GitHub](https://img.shields.io/librariesio/github/matfax/assertk3.svg)
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=matfax/assertk3)](https://dependabot.com)

Assertions for Kotlin 1.3 inspired by AssertJ.

## Setup

### Gradle/JVM

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    // Replace "Tag" with latest version and -jvm with the submodule of your choice 
    testImplementation 'com.github.matfax.assertk1.3:assertk-jvm:Tag'
}
```

### Javascript/Common

Replace dependency on `assertk-jvm` with `assertk-js` or `assertk-common` to use it in JavaScript and common projects,
respectively.

## Usage

Simple usage is to wrap the value you are testing in `assert()` and call assertion methods on the result.

```kotlin
import assertk.assert
import assertk.assertions.*

class PersonTest {
    val person = Person(name = "Bob", age = 18)

    @Test
    fun testName() {
        assert(person.name).isEqualTo("Alice")
        // -> expected:<["Alice"]> but was:<["Bob"]>
    }

    @Test
    fun testAge() {
        assert("age", person.age).isGreaterThan(20)
        // -> expected [age] to be greater than:<20> but was:<10>
    }
}
```

You can see all built-in assertions in the [docs](https://willowtreeapps.github.io/assertk/javadoc/assertk-jvm/assertk.assertions/index.html).
(Due to a limitation in dokka this is currently only the jvm assertions)

### Nullability
Since null is a first-class concept in kotlin's type system, you need to be explicit in your assertions.

```kotlin
val nullString: String? = null
assert(nullString).hasLength(4)
```
will not compile, since `hasLength()` only makes sense on non-null values. You can use `isNotNull()` which takes an
optional lambda to handle this.

```kotlin
val nullString: String? = null
assert(nullString).isNotNull {
    it.hasLength(4)
}
// -> expected to not be null
```
This will first ensure the string is not null before running any other checks.

### Multiple assertions

You can assert multiple things on a single value by providing a lambda as the second argument. All assertions will be
run even if the first one fails.

```kotlin
val string = "Test"
assert(string).all {
    startsWith("L")
    hasLength(3)
}
// -> The following 2 assertions failed:
//    - expected to start with:<"L"> but was:<"Test">
//    - expected to have length:<3> but was:<"Test"> (4)
```

You can wrap multiple assertions in an `assertAll` to ensure all of them get run, not just the first one.

```kotlin
assertAll {
    assert(false).isTrue()
    assert(true).isFalse()
}
// -> The following 2 assertions failed:
//    - expected to be true
//    - expected to be false
```

### Exceptions

If you expect an exception to be thrown, you have a couple of options:

The first is to wrap in a `catch` block to store the result, then assert on that.

```kotlin
val exception = catch { throw Exception("error") }
assert(exception).isNotNull {
    it.hasMessage("wrong")
}
// -> expected [message] to be:<["wrong"]> but was:<["error"]>
```

Your other option is to use an `assert` with a single lambda arg to capture the error.

```kotlin
assert {
    throw Exception("error")
}.thrownError {
    hasMessage("wrong")
}
// -> expected [message] to be:<["wrong"]> but was:<["error"]>
```

This method also allows you to assert on return values.
```kotlin
assert { 1 + 1 }.returnedValue {
    isNegative()
}
// -> expected to be negative but was:<2>
```

You can also assert that there no exceptions thrown
```kotlin
assert {
    aMethodThatMightThrow()
}.doesNotThrowAnyException()
```

### Table Assertions

If you have multiple sets of values you want to test with, you can create a table assertion.

```kotlin
tableOf("a", "b", "result")
    .row(0, 0, 1)
    .row(1, 2, 4)
    .forAll { a, b, result ->
        assert(a + b).isEqualTo(result)
    }
// -> the following 2 assertions failed:
//    on row:(a=<0>,b=<0>,result=<1>)
//    - expected:<[1]> but was:<[0]>
//    on row:(a=<1>,b=<2>,result=<4>)
//    - expected:<[4]> but was:<[3]>
```

Up to 4 columns are supported.

## Custom Assertions

One of the goals of this library is to make custom assertions easy to make. All assertions are just extension methods.

```kotlin
fun Assert<Person>.hasAge(expected: Int) {
    assert("age", actual.age).isEqualTo(expected)
}

assert(person).hasAge(10)
// -> expected [age]:<10> but was:<18>
```

If you want to customize the message, you usually want to use `expected()` and `show()`.

```kotlin
fun Assert<Person>.hasAge(expected: Int) {
    if (actual.age == expected) return
    expected("age:${show(expected)} but was age:${show(actual.age)}")
}

assert(person).hasAge(10)
// -> expected age:<10> but was age:<18>
```

### Important Note:
You can't assume that `expected` or `fail` will stop execution in a custom assertion. This is because it might be used
in an `assertAll` block which ensures all assertions are run. Make sure you return early if you want execution to always
stop.

```kotlin
fun Assert<...>.myAssertion() {
  while (true) {
    ...
    expected("to be something but wasn't")
    break // Need to break out of the loop!
  }
}
```

## Contributing to assertk
Contributions are more than welcome! Please see the [Contributing Guidelines](https://github.com/willowtreeapps/assertk/blob/master/Contributing.md) and be mindful of our [Code of Conduct](https://github.com/willowtreeapps/assertk/blob/master/code-of-conduct.md).
