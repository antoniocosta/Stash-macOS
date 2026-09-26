/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.annotation

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FILE
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.annotation.AnnotationTarget.TYPEALIAS
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.reflect.KClass

/**
 * Desktop shim for `androidx.annotation.OptIn` from `androidx.annotation:annotation-experimental`,
 * an Android-only (AAR) artifact with no JVM variant. Same retention, targets and signature as
 * annotation-experimental 1.4.x. Like on Android, it is checked only by Lint — the Kotlin compiler
 * attaches no semantics to it.
 *
 * Allows use of an opt-in API denoted by the given markers in the annotated file, declaration, or
 * expression.
 */
@Retention(BINARY)
@Target(
    CLASS,
    PROPERTY,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    FILE,
    TYPEALIAS,
)
public annotation class OptIn(
    /** Defines the opt-in API(s) whose usage this annotation allows. */
    @get:Suppress("ArrayReturn") vararg val markerClass: KClass<out Annotation>
)
