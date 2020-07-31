/*
 * Copyright 2020 Google Inc.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#include "../../skottie/src/Adapter.h"
#include "../../skottie/src/SkottieJson.h"
#include "../../skottie/src/SkottiePriv.h"
#include "../../skottie/src/SkottieValue.h"
#include "SkSGPath.h"

namespace skottie {
namespace internal {

namespace  {

class PathAdapter final : public DiscardableAdapterBase<PathAdapter, sksg::Path> {
public:
    PathAdapter(const skjson::Value& jpath, const AnimationBuilder& abuilder)
        : INHERITED(sksg::Path::Make()) {
        this->bind(abuilder, jpath, fShape);
    }

private:
    void onSync() override {
        const auto& path_node = this->node();

        SkPath path = fShape;

        // FillType is tracked in the SG node, not in keyframes -- make sure we preserve it.
        path.setFillType(path_node->getFillType());
        path.setIsVolatile(!this->isStatic());

        path_node->setPath(path);
    }

    ShapeValue fShape;

    using INHERITED = DiscardableAdapterBase<PathAdapter, sksg::Path>;
};

} // namespace

sk_sp<sksg::Path> AnimationBuilder::attachPath(const skjson::Value& jpath) const {
    return this->attachDiscardableAdapter<PathAdapter>(jpath, *this);
}

} // namespace internal
} // namespace skottie
