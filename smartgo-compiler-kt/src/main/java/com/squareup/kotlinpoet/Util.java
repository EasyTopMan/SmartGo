/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Character.isISOControl;

/**
 * Like Guava, but worse and standalone. This makes it easier to mix JavaPoet with libraries that
 * bring their own version of Guava.
 */
final class Util {
  private Util() {
  }

  static <K, V> Map<K, List<V>> immutableMultimap(Map<K, List<V>> multimap) {
    LinkedHashMap<K, List<V>> result = new LinkedHashMap<>();
    for (Map.Entry<K, List<V>> entry : multimap.entrySet()) {
      if (entry.getValue().isEmpty()) continue;
      result.put(entry.getKey(), immutableList(entry.getValue()));
    }
    return Collections.unmodifiableMap(result);
  }

  static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(map));
  }

  static <T> List<T> immutableList(Collection<T> collection) {
    return Collections.unmodifiableList(new ArrayList<>(collection));
  }

  static <T> Set<T> immutableSet(Collection<T> set) {
    return Collections.unmodifiableSet(new LinkedHashSet<>(set));
  }

  static void requireExactlyOneOf(Set<KModifier> modifiers, KModifier... mutuallyExclusive) {
    int count = 0;
    for (KModifier modifier : mutuallyExclusive) {
      if (modifiers.contains(modifier)) count++;
    }
    if (count != 1) {
      throw new IllegalArgumentException(
          String.format("modifiers %s must contain one of %s", modifiers,
              Arrays.toString(mutuallyExclusive)));
    }
  }

  static String characterLiteralWithoutSingleQuotes(char c) {
    // see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
    switch (c) {
      case '\b': return "\\b"; /* \u0008: backspace (BS) */
      case '\t': return "\\t"; /* \u0009: horizontal tab (HT) */
      case '\n': return "\\n"; /* \u000a: linefeed (LF) */
      case '\f': return "\\f"; /* \u000c: form feed (FF) */
      case '\r': return "\\r"; /* \u000d: carriage return (CR) */
      case '\"': return "\"";  /* \u0022: double quote (") */
      case '\'': return "\\'"; /* \u0027: single quote (') */
      case '\\': return "\\\\";  /* \u005c: backslash (\) */
      default:
        return isISOControl(c) ? String.format("\\u%04x", (int) c) : Character.toString(c);
    }
  }

  /** Returns the string literal representing {@code value}, including wrapping double quotes. */
  static String stringLiteralWithQuotes(String value, String indent) {
    if (value.contains("\n")) {
      StringBuilder result = new StringBuilder(value.length() + 32);
      result.append("\"\"\"\n|");
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        if (value.regionMatches(i, "\"\"\"", 0, 3)) {
          // Don't inadvertently end the raw string too early
          result.append("\"\"${'\"'}");
          i += 2;
        } else if (c == '\n') {
          // Add a '|' after newlines. This pipe will be removed by trimMargin().
          result.append("\n|");
        } else {
          result.append(c);
        }
      }
      // If the last-emitted character wasn't a margin '|', add a blank line. This will get removed
      // by trimMargin().
      if (!value.endsWith("\n")) result.append("\n");
      result.append("\"\"\".trimMargin()");
      return result.toString();
    } else {
      StringBuilder result = new StringBuilder(value.length() + 32);
      result.append('"');
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        // Trivial case: single quote must not be escaped.
        if (c == '\'') {
          result.append("'");
          continue;
        }
        // Trivial case: double quotes must be escaped.
        if (c == '\"') {
          result.append("\\\"");
          continue;
        }
        // Default case: just let character literal do its work.
        result.append(characterLiteralWithoutSingleQuotes(c));
        // Need to append indent after linefeed?
      }
      result.append('"');
      return result.toString();
    }
  }
}
