/*
 * Copyright 2020 Google Inc.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#include "../../skottie/src/SkottieJson.h"
#include "../../skottie/src/SkottieValue.h"
#include "../../skottie/src/animator/KeyframeAnimator.h"
#include "../../skottie/src/text/TextValue.h"

namespace skottie::internal {

namespace  {

class TextKeyframeAnimator final : public KeyframeAnimator {
public:
    class Builder final : public KeyframeAnimatorBuilder {
    public:
        explicit Builder(TextValue* target) : fTarget(target) {}

        sk_sp<KeyframeAnimator> make(const AnimationBuilder& abuilder,
                                     const skjson::ArrayValue& jkfs) override {
            SkASSERT(jkfs.size() > 0);

            fValues.reserve(jkfs.size());
            if (!this->parseKeyframes(abuilder, jkfs)) {
                return nullptr;
            }
            fValues.shrink_to_fit();

            return sk_sp<TextKeyframeAnimator>(
                        new TextKeyframeAnimator(std::move(fKFs),
                                                 std::move(fCMs),
                                                 std::move(fValues),
                                                 fTarget));
        }

        bool parseValue(const AnimationBuilder& abuilder, const skjson::Value& jv) const override {
            return Parse(jv, abuilder, fTarget);
        }

    private:
        bool parseKFValue(const AnimationBuilder& abuilder,
                          const skjson::ObjectValue&,
                          const skjson::Value& jv,
                          Keyframe::Value* v) override {
            TextValue val;
            if (!Parse(jv, abuilder, &val)) {
                return false;
            }

            // TODO: full deduping?
            if (fValues.empty() || val != fValues.back()) {
                fValues.push_back(std::move(val));
            }

            v->idx = SkToU32(fValues.size() - 1);

            return true;
        }

        std::vector<TextValue> fValues;
        TextValue*             fTarget;
    };

private:
    TextKeyframeAnimator(std::vector<Keyframe> kfs, std::vector<SkCubicMap> cms,
                         std::vector<TextValue> vs, TextValue* target_value)
        : INHERITED(std::move(kfs), std::move(cms))
        , fValues(std::move(vs))
        , fTarget(target_value) {}

    StateChanged onSeek(float t) override {
        const auto& lerp_info = this->getLERPInfo(t);

        // Text value keyframes are treated as selectors, not as interpolated values.
        if (*fTarget != fValues[SkToSizeT(lerp_info.vrec0.idx)]) {
            *fTarget = fValues[SkToSizeT(lerp_info.vrec0.idx)];
            return true;
        }

        return false;
    }

    const std::vector<TextValue> fValues;
    TextValue*                   fTarget;

    using INHERITED = KeyframeAnimator;
};

} // namespace

template <>
bool AnimatablePropertyContainer::bind<TextValue>(const AnimationBuilder& abuilder,
                                                  const skjson::ObjectValue* jprop,
                                                  TextValue* v) {
    TextKeyframeAnimator::Builder builder(v);
    return this->bindImpl(abuilder, jprop, builder);
}

} // namespace skottie::internal
