/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.glview.view;

import android.content.Context;

import java.util.ArrayList;

import com.glview.animation.LayoutTransition;
import com.glview.graphics.Rect;
import com.glview.graphics.drawable.Drawable;
import com.glview.hwui.GLCanvas;

/**
 * An overlay is an extra layer that sits on top of a View (the "host view")
 * which is drawn after all other content in that view (including children,
 * if the view is a ViewGroup). Interaction with the overlay layer is done
 * by adding and removing drawables.
 *
 * <p>An overlay requested from a ViewGroup is of type {@link ViewGroupOverlay},
 * which also supports adding and removing views.</p>
 *
 * @see View#getOverlay() View.getOverlay()
 * @see ViewGroup#getOverlay() ViewGroup.getOverlay()
 * @see ViewGroupOverlay
 */
public class ViewOverlay {

    /**
     * The actual container for the drawables (and views, if it's a ViewGroupOverlay).
     * All of the management and rendering details for the overlay are handled in
     * OverlayViewGroup.
     */
    OverlayViewGroup mOverlayViewGroup;

    ViewOverlay(Context context, View hostView) {
        mOverlayViewGroup = new OverlayViewGroup(context, hostView);
    }

    /**
     * Used internally by View and ViewGroup to handle drawing and invalidation
     * of the overlay
     * @return
     */
    ViewGroup getOverlayView() {
        return mOverlayViewGroup;
    }

    /**
     * Adds a Drawable to the overlay. The bounds of the drawable should be relative to
     * the host view. Any drawable added to the overlay should be removed when it is no longer
     * needed or no longer visible.
     *
     * @param drawable The Drawable to be added to the overlay. This drawable will be
     * drawn when the view redraws its overlay.
     * @see #remove(Drawable)
     */
    public void add(Drawable drawable) {
        mOverlayViewGroup.add(drawable);
    }

    /**
     * Removes the specified Drawable from the overlay.
     *
     * @param drawable The Drawable to be removed from the overlay.
     * @see #add(Drawable)
     */
    public void remove(Drawable drawable) {
        mOverlayViewGroup.remove(drawable);
    }

    /**
     * Removes all content from the overlay.
     */
    public void clear() {
        mOverlayViewGroup.clear();
    }

    boolean isEmpty() {
        return mOverlayViewGroup.isEmpty();
    }

    /**
     * OverlayViewGroup is a container that View and ViewGroup use to host
     * drawables and views added to their overlays  ({@link ViewOverlay} and
     * {@link ViewGroupOverlay}, respectively). Drawables are added to the overlay
     * via the add/remove methods in ViewOverlay, Views are added/removed via
     * ViewGroupOverlay. These drawable and view objects are
     * drawn whenever the view itself is drawn; first the view draws its own
     * content (and children, if it is a ViewGroup), then it draws its overlay
     * (if it has one).
     *
     * <p>Besides managing and drawing the list of drawables, this class serves
     * two purposes:
     * (1) it noops layout calls because children are absolutely positioned and
     * (2) it forwards all invalidation calls to its host view. The invalidation
     * redirect is necessary because the overlay is not a child of the host view
     * and invalidation cannot therefore follow the normal path up through the
     * parent hierarchy.</p>
     *
     * @see View#getOverlay()
     * @see ViewGroup#getOverlay()
     */
    static class OverlayViewGroup extends ViewGroup {

        /**
         * The View for which this is an overlay. Invalidations of the overlay are redirected to
         * this host view.
         */
        View mHostView;

        /**
         * The set of drawables to draw when the overlay is rendered.
         */
        ArrayList<Drawable> mDrawables = null;

        OverlayViewGroup(Context context, View hostView) {
            super(context);
            mHostView = hostView;
            mAttachInfo = mHostView.mAttachInfo;
            mRight = hostView.getWidth();
            mBottom = hostView.getHeight();
        }

        public void add(Drawable drawable) {
            if (mDrawables == null) {

                mDrawables = new ArrayList<Drawable>();
            }
            if (!mDrawables.contains(drawable)) {
                // Make each drawable unique in the overlay; can't add it more than once
                mDrawables.add(drawable);
                invalidate(drawable.getBounds());
                drawable.setCallback(this);
            }
        }

        public void remove(Drawable drawable) {
            if (mDrawables != null) {
                mDrawables.remove(drawable);
                invalidate(drawable.getBounds());
                drawable.setCallback(null);
            }
        }

        @Override
        protected boolean verifyDrawable(Drawable who) {
            return super.verifyDrawable(who) || (mDrawables != null && mDrawables.contains(who));
        }

        public void add(View child) {
            if (child.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) child.getParent();
                if (parent != mHostView && parent.getParent() != null &&
                        parent.mAttachInfo != null) {
                    // Moving to different container; figure out how to position child such that
                    // it is in the same location on the screen
                    int[] parentLocation = new int[2];
                    int[] hostViewLocation = new int[2];
                    parent.getLocationOnScreen(parentLocation);
                    mHostView.getLocationOnScreen(hostViewLocation);
                    child.offsetLeftAndRight(parentLocation[0] - hostViewLocation[0]);
                    child.offsetTopAndBottom(parentLocation[1] - hostViewLocation[1]);
                }
                parent.removeView(child);
                if (parent.getLayoutTransition() != null) {
                    // LayoutTransition will cause the child to delay removal - cancel it
                    parent.getLayoutTransition().cancel(LayoutTransition.DISAPPEARING);
                }
                // fail-safe if view is still attached for any reason
                if (child.getParent() != null) {
                    child.mParent = null;
                }
            }
            super.addView(child);
        }

        public void remove(View view) {
            super.removeView(view);
        }

        public void clear() {
            removeAllViews();
            if (mDrawables != null) {
                mDrawables.clear();
            }
        }

        boolean isEmpty() {
            if (getChildCount() == 0 &&
                    (mDrawables == null || mDrawables.size() == 0)) {
                return true;
            }
            return false;
        }

        @Override
        public void invalidateDrawable(Drawable drawable) {
            invalidate(drawable.getBounds());
        }

        @Override
        protected void dispatchDraw(GLCanvas canvas) {
            super.dispatchDraw(canvas);
            final int numDrawables = (mDrawables == null) ? 0 : mDrawables.size();
            for (int i = 0; i < numDrawables; ++i) {
                mDrawables.get(i).draw(canvas);
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            // Noop: children are positioned absolutely
        }

        /*
         The following invalidation overrides exist for the purpose of redirecting invalidation to
         the host view. The overlay is not parented to the host view (since a View cannot be a
         parent), so the invalidation cannot proceed through the normal parent hierarchy.
         There is a built-in assumption that the overlay exactly covers the host view, therefore
         the invalidation rectangles received do not need to be adjusted when forwarded to
         the host view.
         */

        @Override
        public void invalidate(Rect dirty) {
            super.invalidate(dirty);
            if (mHostView != null) {
                mHostView.invalidate(dirty);
            }
        }

        @Override
        public void invalidate(int l, int t, int r, int b) {
            super.invalidate(l, t, r, b);
            if (mHostView != null) {
                mHostView.invalidate(l, t, r, b);
            }
        }

        @Override
        public void invalidate() {
            super.invalidate();
            if (mHostView != null) {
                mHostView.invalidate();
            }
        }

        @Override
        void invalidate(boolean invalidateCache) {
            super.invalidate(invalidateCache);
            if (mHostView != null) {
                mHostView.invalidate(invalidateCache);
            }
        }

        @Override
        void invalidateViewProperty() {
            super.invalidateViewProperty();
            if (mHostView != null) {
                mHostView.invalidateViewProperty();
            }
        }
        
    }

}
