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
package androidx.media3.common.util;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Signifies that a public API (class, method or field) is subject to incompatible changes, or even
 * removal, in a future release.
 *
 * <p>Desktop port of media3 1.9.2. Media3 marks this annotation with {@code
 * androidx.annotation.RequiresOptIn}, which lives in the Android-only {@code
 * annotation-experimental} artifact and is enforced by Android Lint only (the Kotlin compiler
 * ignores it). It is therefore omitted here; the annotation is a plain marker with media3's exact
 * retention and targets, so {@code @UnstableApi} and {@code @OptIn(UnstableApi::class)} compile
 * exactly as they do on Android.
 */
@Documented
@Retention(CLASS)
@Target({TYPE, METHOD, CONSTRUCTOR, FIELD})
@UnstableApi
public @interface UnstableApi {}
