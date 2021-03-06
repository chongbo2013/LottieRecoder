/*
 * Copyright 2011 Google Inc.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#include "SkOSPath2.h"

SkString SkOSPath2::Join(const char *rootPath, const char *relativePath) {
    SkString result(rootPath);
    if (!result.endsWith(SEPARATOR) && !result.isEmpty()) {
        result.appendUnichar(SEPARATOR);
    }
    result.append(relativePath);
    return result;
}

SkString SkOSPath2::Basename(const char* fullPath) {
    if (!fullPath) {
        return SkString();
    }
    const char* filename = strrchr(fullPath, SEPARATOR);
    if (nullptr == filename) {
        filename = fullPath;
    } else {
        ++filename;
    }
    return SkString(filename);
}

SkString SkOSPath2::Dirname(const char* fullPath) {
    if (!fullPath) {
        return SkString();
    }
    const char* end = strrchr(fullPath, SEPARATOR);
    if (nullptr == end) {
        return SkString();
    }
    if (end == fullPath) {
        SkASSERT(fullPath[0] == SEPARATOR);
        ++end;
    }
    return SkString(fullPath, end - fullPath);
}
