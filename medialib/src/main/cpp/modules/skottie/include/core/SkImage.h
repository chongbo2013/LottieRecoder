/*
 * Copyright 2012 Google Inc.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#ifndef SkImage_DEFINED
#define SkImage_DEFINED

#include "../core/SkFilterQuality.h"
#include "../core/SkImageEncoder.h"
#include "../core/SkImageInfo.h"
#include "../core/SkRefCnt.h"
#include "../core/SkScalar.h"
#include "../core/SkShader.h"
#include "../core/SkTileMode.h"
#include "../gpu/GrTypes.h"
#include <functional>  // std::function

#if defined(SK_BUILD_FOR_ANDROID) && __ANDROID_API__ >= 26
#include <android/hardware_buffer.h>
#endif

class SkData;
class SkCanvas;
class SkImageFilter;
class SkImageGenerator;
class SkMipmap;
class SkPaint;
class SkPicture;
class SkSurface;
class GrBackendTexture;
class GrContext;
class GrDirectContext;
class GrRecordingContext;
class GrContextThreadSafeProxy;
class GrRecordingContext;

struct SkYUVAIndex;

enum class SkSamplingMode {
    kNearest,   // single sample point (nearest neighbor)
    kLinear,    // interporate between 2x2 sample points (bilinear interpolation)
};

enum class SkMipmapMode {
    kNone,      // ignore mipmap levels, sample from the "base"
    kNearest,   // sample from the nearest level
    kLinear,    // interpolate between the two nearest levels
};

struct SkFilterOptions {
    SkSamplingMode  fSampling;
    SkMipmapMode    fMipmap;
};

class SkMipmapBuilder {
public:
    SkMipmapBuilder(const SkImageInfo&);
    ~SkMipmapBuilder();

    int countLevels() const;
    SkPixmap level(int index) const;

    sk_sp<SkMipmap> detach();

private:
    sk_sp<SkMipmap> fMM;
};

/** \class SkImage
    SkImage describes a two dimensional array of pixels to draw. The pixels may be
    decoded in a raster bitmap, encoded in a SkPicture or compressed data stream,
    or located in GPU memory as a GPU texture.

    SkImage cannot be modified after it is created. SkImage may allocate additional
    storage as needed; for instance, an encoded SkImage may decode when drawn.

    SkImage width and height are greater than zero. Creating an SkImage with zero width
    or height returns SkImage equal to nullptr.

    SkImage may be created from SkBitmap, SkPixmap, SkSurface, SkPicture, encoded streams,
    GPU texture, YUV_ColorSpace data, or hardware buffer. Encoded streams supported
    include BMP, GIF, HEIF, ICO, JPEG, PNG, WBMP, WebP. Supported encoding details
    vary with platform.
*/
class SK_API SkImage : public SkRefCnt {
public:

    /** Caller data passed to RasterReleaseProc; may be nullptr.
    */
    typedef void* ReleaseContext;

    /** Creates SkImage from SkPixmap and copy of pixels. Since pixels are copied, SkPixmap
        pixels may be modified or deleted without affecting SkImage.

        SkImage is returned if SkPixmap is valid. Valid SkPixmap parameters include:
        dimensions are greater than zero;
        each dimension fits in 29 bits;
        SkColorType and SkAlphaType are valid, and SkColorType is not kUnknown_SkColorType;
        row bytes are large enough to hold one row of pixels;
        pixel address is not nullptr.

        @param pixmap  SkImageInfo, pixel address, and row bytes
        @return        copy of SkPixmap pixels, or nullptr

        example: https://fiddle.skia.org/c/@Image_MakeRasterCopy
    */
    static sk_sp<SkImage> MakeRasterCopy(const SkPixmap& pixmap);

    /** Creates SkImage from SkImageInfo, sharing pixels.

        SkImage is returned if SkImageInfo is valid. Valid SkImageInfo parameters include:
        dimensions are greater than zero;
        each dimension fits in 29 bits;
        SkColorType and SkAlphaType are valid, and SkColorType is not kUnknown_SkColorType;
        rowBytes are large enough to hold one row of pixels;
        pixels is not nullptr, and contains enough data for SkImage.

        @param info      contains width, height, SkAlphaType, SkColorType, SkColorSpace
        @param pixels    address or pixel storage
        @param rowBytes  size of pixel row or larger
        @return          SkImage sharing pixels, or nullptr
    */
    static sk_sp<SkImage> MakeRasterData(const SkImageInfo& info, sk_sp<SkData> pixels,
                                         size_t rowBytes);

    /** Function called when SkImage no longer shares pixels. ReleaseContext is
        provided by caller when SkImage is created, and may be nullptr.
    */
    typedef void (*RasterReleaseProc)(const void* pixels, ReleaseContext);

    /** Creates SkImage from pixmap, sharing SkPixmap pixels. Pixels must remain valid and
        unchanged until rasterReleaseProc is called. rasterReleaseProc is passed
        releaseContext when SkImage is deleted or no longer refers to pixmap pixels.

        Pass nullptr for rasterReleaseProc to share SkPixmap without requiring a callback
        when SkImage is released. Pass nullptr for releaseContext if rasterReleaseProc
        does not require state.

        SkImage is returned if pixmap is valid. Valid SkPixmap parameters include:
        dimensions are greater than zero;
        each dimension fits in 29 bits;
        SkColorType and SkAlphaType are valid, and SkColorType is not kUnknown_SkColorType;
        row bytes are large enough to hold one row of pixels;
        pixel address is not nullptr.

        @param pixmap             SkImageInfo, pixel address, and row bytes
        @param rasterReleaseProc  function called when pixels can be released; or nullptr
        @param releaseContext     state passed to rasterReleaseProc; or nullptr
        @return                   SkImage sharing pixmap
    */
    static sk_sp<SkImage> MakeFromRaster(const SkPixmap& pixmap,
                                         RasterReleaseProc rasterReleaseProc,
                                         ReleaseContext releaseContext);

    /** Creates SkImage from bitmap, sharing or copying bitmap pixels. If the bitmap
        is marked immutable, and its pixel memory is shareable, it may be shared
        instead of copied.

        SkImage is returned if bitmap is valid. Valid SkBitmap parameters include:
        dimensions are greater than zero;
        each dimension fits in 29 bits;
        SkColorType and SkAlphaType are valid, and SkColorType is not kUnknown_SkColorType;
        row bytes are large enough to hold one row of pixels;
        pixel address is not nullptr.

        @param bitmap  SkImageInfo, row bytes, and pixels
        @return        created SkImage, or nullptr

        example: https://fiddle.skia.org/c/@Image_MakeFromBitmap
    */
    static sk_sp<SkImage> MakeFromBitmap(const SkBitmap& bitmap);

    /** Creates SkImage from data returned by imageGenerator. Generated data is owned by SkImage and
        may not be shared or accessed.

        subset allows selecting a portion of the full image. Pass nullptr to select the entire
        image; otherwise, subset must be contained by image bounds.

        SkImage is returned if generator data is valid. Valid data parameters vary by type of data
        and platform.

        imageGenerator may wrap SkPicture data, codec data, or custom data.

        @param imageGenerator  stock or custom routines to retrieve SkImage
        @param subset          bounds of returned SkImage; may be nullptr
        @return                created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromGenerator(std::unique_ptr<SkImageGenerator> imageGenerator,
                                            const SkIRect* subset = nullptr);

    /**
     *  Return an image backed by the encoded data, but attempt to defer decoding until the image
     *  is actually used/drawn. This deferral allows the system to cache the result, either on the
     *  CPU or on the GPU, depending on where the image is drawn. If memory is low, the cache may
     *  be purged, causing the next draw of the image to have to re-decode.
     *
     *  The subset parameter specifies a area within the decoded image to create the image from.
     *  If subset is null, then the entire image is returned.
     *
     *  This is similar to DecodeTo[Raster,Texture], but this method will attempt to defer the
     *  actual decode, while the DecodeTo... method explicitly decode and allocate the backend
     *  when the call is made.
     *
     *  If the encoded format is not supported, or subset is outside of the bounds of the decoded
     *  image, nullptr is returned.
     *
     *  @param encoded  the encoded data
     *  @param length   the number of bytes of encoded data
     *  @param subset   the bounds of the pixels within the decoded image to return. may be null.
     *  @return         created SkImage, or nullptr

        example: https://fiddle.skia.org/c/@Image_MakeFromEncoded
    */
    static sk_sp<SkImage> MakeFromEncoded(sk_sp<SkData> encoded, const SkIRect* subset = nullptr);

    /**
     *  Decode the data in encoded/length into a raster image.
     *
     *  The subset parameter specifies a area within the decoded image to create the image from.
     *  If subset is null, then the entire image is returned.
     *
     *  This is similar to MakeFromEncoded, but this method will always decode immediately, and
     *  allocate the memory for the pixels for the lifetime of the returned image.
     *
     *  If the encoded format is not supported, or subset is outside of the bounds of the decoded
     *  image, nullptr is returned.
     *
     *  @param encoded  the encoded data
     *  @param length   the number of bytes of encoded data
     *  @param subset   the bounds of the pixels within the decoded image to return. may be null.
     *  @return         created SkImage, or nullptr
     */
    static sk_sp<SkImage> DecodeToRaster(const void* encoded, size_t length,
                                         const SkIRect* subset = nullptr);
    static sk_sp<SkImage> DecodeToRaster(const sk_sp<SkData>& data,
                                         const SkIRect* subset = nullptr) {
        return DecodeToRaster(data->data(), data->size(), subset);
    }

    /**
     *  Decode the data in encoded/length into a texture-backed image.
     *
     *  The subset parameter specifies a area within the decoded image to create the image from.
     *  If subset is null, then the entire image is returned.
     *
     *  This is similar to MakeFromEncoded, but this method will always decode immediately, and
     *  allocate the texture for the pixels for the lifetime of the returned image.
     *
     *  If the encoded format is not supported, or subset is outside of the bounds of the decoded
     *  image, nullptr is returned.
     *
     *  @param direct   the GrDirectContext in play
     *  @param encoded  the encoded data
     *  @param length   the number of bytes of encoded data
     *  @param subset   the bounds of the pixels within the decoded image to return. may be null.
     *  @return         created SkImage, or nullptr
     */
    static sk_sp<SkImage> DecodeToTexture(GrDirectContext* direct, const void* encoded,
                                          size_t length, const SkIRect* subset = nullptr);
    static sk_sp<SkImage> DecodeToTexture(GrDirectContext* direct, const sk_sp<SkData>& data,
                                          const SkIRect* subset = nullptr) {
        return DecodeToTexture(direct, data->data(), data->size(), subset);
    }

    /*
     * Experimental:
     *   Skia                | GL_COMPRESSED_*     | MTLPixelFormat*      | VK_FORMAT_*_BLOCK
     *  --------------------------------------------------------------------------------------
     *   kETC2_RGB8_UNORM    | ETC1_RGB8           | ETC2_RGB8 (iOS-only) | ETC2_R8G8B8_UNORM
     *                       | RGB8_ETC2           |                      |
     *  --------------------------------------------------------------------------------------
     *   kBC1_RGB8_UNORM     | RGB_S3TC_DXT1_EXT   | N/A                  | BC1_RGB_UNORM
     *  --------------------------------------------------------------------------------------
     *   kBC1_RGBA8_UNORM    | RGBA_S3TC_DXT1_EXT  | BC1_RGBA (macOS-only)| BC1_RGBA_UNORM
     */
    enum class CompressionType {
        kNone,
        kETC2_RGB8_UNORM, // the same as ETC1

        kBC1_RGB8_UNORM,
        kBC1_RGBA8_UNORM,
        kLast = kBC1_RGBA8_UNORM,
    };

    static constexpr int kCompressionTypeCount = static_cast<int>(CompressionType::kLast) + 1;

    static const CompressionType kETC1_CompressionType = CompressionType::kETC2_RGB8_UNORM;

    /** Creates a GPU-backed SkImage from compressed data.

        This method will return an SkImage representing the compressed data.
        If the GPU doesn't support the specified compression method, the data
        will be decompressed and then wrapped in a GPU-backed image.

        Note: one can query the supported compression formats via
        GrContext::compressedBackendFormat.

        @param context     GPU context
        @param data        compressed data to store in SkImage
        @param width       width of full SkImage
        @param height      height of full SkImage
        @param type        type of compression used
        @param mipMapped   does 'data' contain data for all the mipmap levels?
        @param isProtected do the contents of 'data' require DRM protection (on Vulkan)?
        @return            created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeTextureFromCompressed(GrDirectContext* direct,
                                                    sk_sp<SkData> data,
                                                    int width, int height,
                                                    CompressionType type,
                                                    GrMipMapped mipMapped = GrMipMapped::kNo,
                                                    GrProtected isProtected = GrProtected::kNo);

    /** Creates a CPU-backed SkImage from compressed data.

        This method will decompress the compressed data and create an image wrapping
        it. Any mipmap levels present in the compressed data are discarded.

        @param data     compressed data to store in SkImage
        @param width    width of full SkImage
        @param height   height of full SkImage
        @param type     type of compression used
        @return         created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeRasterFromCompressed(sk_sp<SkData> data,
                                                   int width, int height,
                                                   CompressionType type);

    /** User function called when supplied texture may be deleted.
    */
    typedef void (*TextureReleaseProc)(ReleaseContext releaseContext);

    /** Creates SkImage from GPU texture associated with context. Caller is responsible for
        managing the lifetime of GPU texture.

        SkImage is returned if format of backendTexture is recognized and supported.
        Recognized formats vary by GPU back-end.

        @param context         GPU context
        @param backendTexture  texture residing on GPU
        @param colorSpace      range of colors; may be nullptr
        @return                created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromTexture(GrContext* context,
                                          const GrBackendTexture& backendTexture,
                                          GrSurfaceOrigin origin,
                                          SkColorType colorType,
                                          SkAlphaType alphaType,
                                          sk_sp<SkColorSpace> colorSpace) {
        return MakeFromTexture(context, backendTexture, origin, colorType, alphaType, colorSpace,
                               nullptr, nullptr);
    }

    /** Creates SkImage from GPU texture associated with context. GPU texture must stay
        valid and unchanged until textureReleaseProc is called. textureReleaseProc is
        passed releaseContext when SkImage is deleted or no longer refers to texture.

        SkImage is returned if format of backendTexture is recognized and supported.
        Recognized formats vary by GPU back-end.

        @param context             GPU context
        @param backendTexture      texture residing on GPU
        @param colorSpace          This describes the color space of this image's contents, as
                                   seen after sampling. In general, if the format of the backend
                                   texture is SRGB, some linear colorSpace should be supplied
                                   (e.g., SkColorSpace::MakeSRGBLinear()). If the format of the
                                   backend texture is linear, then the colorSpace should include
                                   a description of the transfer function as
                                   well (e.g., SkColorSpace::MakeSRGB()).
        @param textureReleaseProc  function called when texture can be released
        @param releaseContext      state passed to textureReleaseProc
        @return                    created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromTexture(GrContext* context,
                                          const GrBackendTexture& backendTexture,
                                          GrSurfaceOrigin origin,
                                          SkColorType colorType,
                                          SkAlphaType alphaType,
                                          sk_sp<SkColorSpace> colorSpace,
                                          TextureReleaseProc textureReleaseProc,
                                          ReleaseContext releaseContext);

    /** Creates an SkImage from a GPU backend texture. The backend texture must stay
        valid and unchanged until textureReleaseProc is called. The textureReleaseProc is
        called when the SkImage is deleted or no longer refers to the texture and will be
        passed the releaseContext.

        An SkImage is returned if the format of backendTexture is recognized and supported.
        Recognized formats vary by GPU back-end.

        @param context             the GPU context
        @param backendTexture      a texture already allocated by the GPU
        @param alphaType           This characterizes the nature of the alpha values in the
                                   backend texture. For opaque compressed formats (e.g., ETC1)
                                   this should usually be set to kOpaque_SkAlphaType.
        @param colorSpace          This describes the color space of this image's contents, as
                                   seen after sampling. In general, if the format of the backend
                                   texture is SRGB, some linear colorSpace should be supplied
                                   (e.g., SkColorSpace::MakeSRGBLinear()). If the format of the
                                   backend texture is linear, then the colorSpace should include
                                   a description of the transfer function as
                                   well (e.g., SkColorSpace::MakeSRGB()).
        @param textureReleaseProc  function called when the backend texture can be released
        @param releaseContext      state passed to textureReleaseProc
        @return                    created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromCompressedTexture(GrContext* context,
                                                    const GrBackendTexture& backendTexture,
                                                    GrSurfaceOrigin origin,
                                                    SkAlphaType alphaType,
                                                    sk_sp<SkColorSpace> colorSpace,
                                                    TextureReleaseProc textureReleaseProc = nullptr,
                                                    ReleaseContext releaseContext = nullptr);

    /** Creates SkImage from pixmap. SkImage is uploaded to GPU back-end using context.

        Created SkImage is available to other GPU contexts, and is available across thread
        boundaries. All contexts must be in the same GPU share group, or otherwise
        share resources.

        When SkImage is no longer referenced, context releases texture memory
        asynchronously.

        GrBackendTexture created from pixmap is uploaded to match SkSurface created with
        dstColorSpace. SkColorSpace of SkImage is determined by pixmap.colorSpace().

        SkImage is returned referring to GPU back-end if context is not nullptr,
        format of data is recognized and supported, and if context supports moving
        resources between contexts. Otherwise, pixmap pixel data is copied and SkImage
        as returned in raster format if possible; nullptr may be returned.
        Recognized GPU formats vary by platform and GPU back-end.

        @param context                GPU context
        @param pixmap                 SkImageInfo, pixel address, and row bytes
        @param buildMips              create SkImage as mip map if true
        @param dstColorSpace          range of colors of matching SkSurface on GPU
        @param limitToMaxTextureSize  downscale image to GPU maximum texture size, if necessary
        @return                       created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeCrossContextFromPixmap(GrContext* context, const SkPixmap& pixmap,
                                                     bool buildMips,
                                                     bool limitToMaxTextureSize = false);

    /** Creates SkImage from backendTexture associated with context. backendTexture and
        returned SkImage are managed internally, and are released when no longer needed.

        SkImage is returned if format of backendTexture is recognized and supported.
        Recognized formats vary by GPU back-end.

        @param context         GPU context
        @param backendTexture  texture residing on GPU
        @param colorSpace      range of colors; may be nullptr
        @return                created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromAdoptedTexture(GrContext* context,
                                                 const GrBackendTexture& backendTexture,
                                                 GrSurfaceOrigin surfaceOrigin,
                                                 SkColorType colorType,
                                                 SkAlphaType alphaType = kPremul_SkAlphaType,
                                                 sk_sp<SkColorSpace> colorSpace = nullptr);

    /** Creates an SkImage by flattening the specified YUVA planes into a single, interleaved RGBA
        image.

        @param context         GPU context
        @param yuvColorSpace   How the YUV values are converted to RGB
        @param yuvaTextures    array of (up to four) YUVA textures on GPU which contain the,
                               possibly interleaved, YUVA planes
        @param yuvaIndices     array indicating which texture in yuvaTextures, and channel
                               in that texture, maps to each component of YUVA.
        @param imageSize       size of the resulting image
        @param imageOrigin     origin of the resulting image.
        @param imageColorSpace range of colors of the resulting image; may be nullptr
        @return                created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromYUVATexturesCopy(GrContext* context,
                                                   SkYUVColorSpace yuvColorSpace,
                                                   const GrBackendTexture yuvaTextures[],
                                                   const SkYUVAIndex yuvaIndices[4],
                                                   SkISize imageSize,
                                                   GrSurfaceOrigin imageOrigin,
                                                   sk_sp<SkColorSpace> imageColorSpace = nullptr);

    /** Creates an SkImage by flattening the specified YUVA planes into a single, interleaved RGBA
        image. 'backendTexture' is used to store the result of the flattening.

        @param context            GPU context
        @param yuvColorSpace      How the YUV values are converted to RGB
        @param yuvaTextures       array of (up to four) YUVA textures on GPU which contain the,
                                  possibly interleaved, YUVA planes
        @param yuvaIndices        array indicating which texture in yuvaTextures, and channel
                                  in that texture, maps to each component of YUVA.
        @param imageSize          size of the resulting image
        @param imageOrigin        origin of the resulting image.
        @param backendTexture     the resource that stores the final pixels
        @param imageColorSpace    range of colors of the resulting image; may be nullptr
        @param textureReleaseProc function called when backendTexture can be released
        @param releaseContext     state passed to textureReleaseProc
        @return                   created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromYUVATexturesCopyWithExternalBackend(
            GrContext* context,
            SkYUVColorSpace yuvColorSpace,
            const GrBackendTexture yuvaTextures[],
            const SkYUVAIndex yuvaIndices[4],
            SkISize imageSize,
            GrSurfaceOrigin imageOrigin,
            const GrBackendTexture& backendTexture,
            sk_sp<SkColorSpace> imageColorSpace = nullptr,
            TextureReleaseProc textureReleaseProc = nullptr,
            ReleaseContext releaseContext = nullptr);

    /** Creates an SkImage by storing the specified YUVA planes into an image, to be rendered
        via multitexturing.

        When all the provided backend textures can be released 'textureReleaseProc' will be called
        with 'releaseContext'. It will be called even if this method fails.

        @param context            GPU context
        @param yuvColorSpace      How the YUV values are converted to RGB
        @param yuvaTextures       array of (up to four) YUVA textures on GPU which contain the,
                                  possibly interleaved, YUVA planes
        @param yuvaIndices        array indicating which texture in yuvaTextures, and channel
                                  in that texture, maps to each component of YUVA.
        @param imageSize          size of the resulting image
        @param imageOrigin        origin of the resulting image.
        @param imageColorSpace    range of colors of the resulting image; may be nullptr
        @param textureReleaseProc called when the backend textures can be released
        @param releaseContext     state passed to textureReleaseProc
        @return                   created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromYUVATextures(GrContext* context,
                                               SkYUVColorSpace yuvColorSpace,
                                               const GrBackendTexture yuvaTextures[],
                                               const SkYUVAIndex yuvaIndices[4],
                                               SkISize imageSize,
                                               GrSurfaceOrigin imageOrigin,
                                               sk_sp<SkColorSpace> imageColorSpace = nullptr,
                                               TextureReleaseProc textureReleaseProc = nullptr,
                                               ReleaseContext releaseContext = nullptr);

    /** Creates SkImage from pixmap array representing YUVA data.
        SkImage is uploaded to GPU back-end using context.

        Each GrBackendTexture created from yuvaPixmaps array is uploaded to match SkSurface
        using SkColorSpace of SkPixmap. SkColorSpace of SkImage is determined by imageColorSpace.

        SkImage is returned referring to GPU back-end if context is not nullptr and
        format of data is recognized and supported. Otherwise, nullptr is returned.
        Recognized GPU formats vary by platform and GPU back-end.

        @param context                GPU context
        @param yuvColorSpace          How the YUV values are converted to RGB
        @param yuvaPixmaps            array of (up to four) SkPixmap which contain the,
                                      possibly interleaved, YUVA planes
        @param yuvaIndices            array indicating which pixmap in yuvaPixmaps, and channel
                                      in that pixmap, maps to each component of YUVA.
        @param imageSize              size of the resulting image
        @param imageOrigin            origin of the resulting image.
        @param buildMips              create internal YUVA textures as mip map if true
        @param limitToMaxTextureSize  downscale image to GPU maximum texture size, if necessary
        @param imageColorSpace        range of colors of the resulting image; may be nullptr
        @return                       created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromYUVAPixmaps(
            GrContext* context, SkYUVColorSpace yuvColorSpace, const SkPixmap yuvaPixmaps[],
            const SkYUVAIndex yuvaIndices[4], SkISize imageSize, GrSurfaceOrigin imageOrigin,
            bool buildMips, bool limitToMaxTextureSize = false,
            sk_sp<SkColorSpace> imageColorSpace = nullptr);

    /** To be deprecated.
    */
    static sk_sp<SkImage> MakeFromYUVTexturesCopyWithExternalBackend(
            GrContext* context,
            SkYUVColorSpace yuvColorSpace,
            const GrBackendTexture yuvTextures[3],
            GrSurfaceOrigin imageOrigin,
            const GrBackendTexture& backendTexture,
            sk_sp<SkColorSpace> imageColorSpace = nullptr);

    /** Creates SkImage from copy of nv12Textures, an array of textures on GPU.
        nv12Textures[0] contains pixels for YUV component y plane.
        nv12Textures[1] contains pixels for YUV component u plane,
        followed by pixels for YUV component v plane.
        Returned SkImage has the dimensions nv12Textures[2].
        yuvColorSpace describes how YUV colors convert to RGB colors.

        @param context         GPU context
        @param yuvColorSpace   How the YUV values are converted to RGB
        @param nv12Textures    array of YUV textures on GPU
        @param imageColorSpace range of colors; may be nullptr
        @return                created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromNV12TexturesCopy(GrContext* context,
                                                   SkYUVColorSpace yuvColorSpace,
                                                   const GrBackendTexture nv12Textures[2],
                                                   GrSurfaceOrigin imageOrigin,
                                                   sk_sp<SkColorSpace> imageColorSpace = nullptr);

    /** Creates SkImage from copy of nv12Textures, an array of textures on GPU.
        nv12Textures[0] contains pixels for YUV component y plane.
        nv12Textures[1] contains pixels for YUV component u plane,
        followed by pixels for YUV component v plane.
        Returned SkImage has the dimensions nv12Textures[2] and stores pixels in backendTexture.
        yuvColorSpace describes how YUV colors convert to RGB colors.

        @param context            GPU context
        @param yuvColorSpace   How the YUV values are converted to RGB
        @param nv12Textures       array of YUV textures on GPU
        @param backendTexture     the resource that stores the final pixels
        @param imageColorSpace    range of colors; may be nullptr
        @param textureReleaseProc function called when backendTexture can be released
        @param releaseContext     state passed to textureReleaseProc
        @return                   created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromNV12TexturesCopyWithExternalBackend(
            GrContext* context,
            SkYUVColorSpace yuvColorSpace,
            const GrBackendTexture nv12Textures[2],
            GrSurfaceOrigin imageOrigin,
            const GrBackendTexture& backendTexture,
            sk_sp<SkColorSpace> imageColorSpace = nullptr,
            TextureReleaseProc textureReleaseProc = nullptr,
            ReleaseContext releaseContext = nullptr);

    enum class BitDepth {
        kU8,  //!< uses 8-bit unsigned int per color component
        kF16, //!< uses 16-bit float per color component
    };

    /** Creates SkImage from picture. Returned SkImage width and height are set by dimensions.
        SkImage draws picture with matrix and paint, set to bitDepth and colorSpace.

        If matrix is nullptr, draws with identity SkMatrix. If paint is nullptr, draws
        with default SkPaint. colorSpace may be nullptr.

        @param picture     stream of drawing commands
        @param dimensions  width and height
        @param matrix      SkMatrix to rotate, scale, translate, and so on; may be nullptr
        @param paint       SkPaint to apply transparency, filtering, and so on; may be nullptr
        @param bitDepth    8-bit integer or 16-bit float: per component
        @param colorSpace  range of colors; may be nullptr
        @return            created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromPicture(sk_sp<SkPicture> picture, const SkISize& dimensions,
                                          const SkMatrix* matrix, const SkPaint* paint,
                                          BitDepth bitDepth,
                                          sk_sp<SkColorSpace> colorSpace);

#if defined(SK_BUILD_FOR_ANDROID) && __ANDROID_API__ >= 26
    /** (See Skia bug 7447)
        Creates SkImage from Android hardware buffer.
        Returned SkImage takes a reference on the buffer.

        Only available on Android, when __ANDROID_API__ is defined to be 26 or greater.

        @param hardwareBuffer  AHardwareBuffer Android hardware buffer
        @param colorSpace      range of colors; may be nullptr
        @return                created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromAHardwareBuffer(
            AHardwareBuffer* hardwareBuffer,
            SkAlphaType alphaType = kPremul_SkAlphaType,
            sk_sp<SkColorSpace> colorSpace = nullptr,
            GrSurfaceOrigin surfaceOrigin = kTopLeft_GrSurfaceOrigin);

    /** Creates SkImage from Android hardware buffer and uploads the data from the SkPixmap to it.
        Returned SkImage takes a reference on the buffer.

        Only available on Android, when __ANDROID_API__ is defined to be 26 or greater.

        @param pixmap          SkPixmap that contains data to be uploaded to the AHardwareBuffer
        @param hardwareBuffer  AHardwareBuffer Android hardware buffer
        @return                created SkImage, or nullptr
    */
    static sk_sp<SkImage> MakeFromAHardwareBufferWithData(
            GrContext* context,
            const SkPixmap& pixmap,
            AHardwareBuffer* hardwareBuffer,
            GrSurfaceOrigin surfaceOrigin = kTopLeft_GrSurfaceOrigin);
#endif

    /** Returns a SkImageInfo describing the width, height, color type, alpha type, and color space
        of the SkImage.

        @return  image info of SkImage.
    */
    const SkImageInfo& imageInfo() const { return fInfo; }

    /** Returns pixel count in each row.

        @return  pixel width in SkImage
    */
    int width() const { return fInfo.width(); }

    /** Returns pixel row count.

        @return  pixel height in SkImage
    */
    int height() const { return fInfo.height(); }

    /** Returns SkISize { width(), height() }.

        @return  integral size of width() and height()
    */
    SkISize dimensions() const { return SkISize::Make(fInfo.width(), fInfo.height()); }

    /** Returns SkIRect { 0, 0, width(), height() }.

        @return  integral rectangle from origin to width() and height()
    */
    SkIRect bounds() const { return SkIRect::MakeWH(fInfo.width(), fInfo.height()); }

    /** Returns value unique to image. SkImage contents cannot change after SkImage is
        created. Any operation to create a new SkImage will receive generate a new
        unique number.

        @return  unique identifier
    */
    uint32_t uniqueID() const { return fUniqueID; }

    /** Returns SkAlphaType.

        SkAlphaType returned was a parameter to an SkImage constructor,
        or was parsed from encoded data.

        @return  SkAlphaType in SkImage

        example: https://fiddle.skia.org/c/@Image_alphaType
    */
    SkAlphaType alphaType() const;

    /** Returns SkColorType if known; otherwise, returns kUnknown_SkColorType.

        @return  SkColorType of SkImage

        example: https://fiddle.skia.org/c/@Image_colorType
    */
    SkColorType colorType() const;

    /** Returns SkColorSpace, the range of colors, associated with SkImage.  The
        reference count of SkColorSpace is unchanged. The returned SkColorSpace is
        immutable.

        SkColorSpace returned was passed to an SkImage constructor,
        or was parsed from encoded data. SkColorSpace returned may be ignored when SkImage
        is drawn, depending on the capabilities of the SkSurface receiving the drawing.

        @return  SkColorSpace in SkImage, or nullptr

        example: https://fiddle.skia.org/c/@Image_colorSpace
    */
    SkColorSpace* colorSpace() const;

    /** Returns a smart pointer to SkColorSpace, the range of colors, associated with
        SkImage.  The smart pointer tracks the number of objects sharing this
        SkColorSpace reference so the memory is released when the owners destruct.

        The returned SkColorSpace is immutable.

        SkColorSpace returned was passed to an SkImage constructor,
        or was parsed from encoded data. SkColorSpace returned may be ignored when SkImage
        is drawn, depending on the capabilities of the SkSurface receiving the drawing.

        @return  SkColorSpace in SkImage, or nullptr, wrapped in a smart pointer

        example: https://fiddle.skia.org/c/@Image_refColorSpace
    */
    sk_sp<SkColorSpace> refColorSpace() const;

    /** Returns true if SkImage pixels represent transparency only. If true, each pixel
        is packed in 8 bits as defined by kAlpha_8_SkColorType.

        @return  true if pixels represent a transparency mask

        example: https://fiddle.skia.org/c/@Image_isAlphaOnly
    */
    bool isAlphaOnly() const;

    /** Returns true if pixels ignore their alpha value and are treated as fully opaque.

        @return  true if SkAlphaType is kOpaque_SkAlphaType
    */
    bool isOpaque() const { return SkAlphaTypeIsOpaque(this->alphaType()); }

    sk_sp<SkShader> makeShader(SkTileMode tmx, SkTileMode tmy, const SkFilterOptions&,
                               const SkMatrix* localMatrix = nullptr) const;

    /** Creates SkShader from SkImage. SkShader dimensions are taken from SkImage. SkShader uses
        SkTileMode rules to fill drawn area outside SkImage. localMatrix permits
        transforming SkImage before SkCanvas matrix is applied.

        Note: since no filter-quality is specified, it will be determined at draw time using
              the paint.

        @param tmx          tiling in the x direction
        @param tmy          tiling in the y direction
        @param localMatrix  SkImage transformation, or nullptr
        @return             SkShader containing SkImage
    */
    sk_sp<SkShader> makeShader(SkTileMode tmx, SkTileMode tmy,
                               const SkMatrix* localMatrix = nullptr) const;
    sk_sp<SkShader> makeShader(SkTileMode tmx, SkTileMode tmy, const SkMatrix& localMatrix) const {
        return this->makeShader(tmx, tmy, &localMatrix);
    }

    sk_sp<SkShader> makeShader(SkTileMode tmx, SkTileMode tmy, const SkMatrix* localMatrix,
                               SkFilterQuality) const;

    /** Creates SkShader from SkImage. SkShader dimensions are taken from SkImage. SkShader uses
        SkShader::kClamp_TileMode to fill drawn area outside SkImage. localMatrix permits
        transforming SkImage before SkCanvas matrix is applied.

        @param localMatrix  SkImage transformation, or nullptr
        @return             SkShader containing SkImage
    */
    sk_sp<SkShader> makeShader(const SkMatrix* localMatrix = nullptr) const {
        return this->makeShader(SkTileMode::kClamp, SkTileMode::kClamp, localMatrix);
    }
    sk_sp<SkShader> makeShader(const SkMatrix& localMatrix) const {
        return this->makeShader(SkTileMode::kClamp, SkTileMode::kClamp, &localMatrix);
    }


    /** Copies SkImage pixel address, row bytes, and SkImageInfo to pixmap, if address
        is available, and returns true. If pixel address is not available, return
        false and leave pixmap unchanged.

        @param pixmap  storage for pixel state if pixels are readable; otherwise, ignored
        @return        true if SkImage has direct access to pixels

        example: https://fiddle.skia.org/c/@Image_peekPixels
    */
    bool peekPixels(SkPixmap* pixmap) const;

    /** Returns true the contents of SkImage was created on or uploaded to GPU memory,
        and is available as a GPU texture.

        @return  true if SkImage is a GPU texture

        example: https://fiddle.skia.org/c/@Image_isTextureBacked
    */
    bool isTextureBacked() const;

    /** Returns true if SkImage can be drawn on either raster surface or GPU surface.
        If context is nullptr, tests if SkImage draws on raster surface;
        otherwise, tests if SkImage draws on GPU surface associated with context.

        SkImage backed by GPU texture may become invalid if associated context is
        invalid. lazy image may be invalid and may not draw to raster surface or
        GPU surface or both.

        @param context  GPU context
        @return         true if SkImage can be drawn

        example: https://fiddle.skia.org/c/@Image_isValid
    */
    bool isValid(GrRecordingContext* context) const;

    /** Deprecated.
     */
    bool isValid(GrContext* context) const;

    /** Flushes any pending uses of texture-backed images in the GPU backend. If the image is not
        texture-backed (including promise texture images) or if the the GrContext does not
        have the same context ID as the context backing the image then this is a no-op.

        If the image was not used in any non-culled draws recorded on the passed GrContext then
        this is a no-op unless the GrFlushInfo contains semaphores or  a finish proc. Those are
        respected even when the image has not been used.

        @param context  the context on which to flush pending usages of the image.
        @param info     flush options
     */
    GrSemaphoresSubmitted flush(GrContext* context, const GrFlushInfo& flushInfo);

    void flush(GrContext* context) { this->flush(context, {}); }

    /** Version of flush() that uses a default GrFlushInfo. Also submits the flushed work to the
        GPU.
    */
    void flushAndSubmit(GrContext*);

    /** Retrieves the back-end texture. If SkImage has no back-end texture, an invalid
        object is returned. Call GrBackendTexture::isValid to determine if the result
        is valid.

        If flushPendingGrContextIO is true, completes deferred I/O operations.

        If origin in not nullptr, copies location of content drawn into SkImage.

        @param flushPendingGrContextIO  flag to flush outstanding requests
        @return                         back-end API texture handle; invalid on failure
    */
    GrBackendTexture getBackendTexture(bool flushPendingGrContextIO,
                                       GrSurfaceOrigin* origin = nullptr) const;

    /** \enum SkImage::CachingHint
        CachingHint selects whether Skia may internally cache SkBitmap generated by
        decoding SkImage, or by copying SkImage from GPU to CPU. The default behavior
        allows caching SkBitmap.

        Choose kDisallow_CachingHint if SkImage pixels are to be used only once, or
        if SkImage pixels reside in a cache outside of Skia, or to reduce memory pressure.

        Choosing kAllow_CachingHint does not ensure that pixels will be cached.
        SkImage pixels may not be cached if memory requirements are too large or
        pixels are not accessible.
    */
    enum CachingHint {
        kAllow_CachingHint,    //!< allows internally caching decoded and copied pixels
        kDisallow_CachingHint, //!< disallows internally caching decoded and copied pixels
    };

    /** Copies SkRect of pixels from SkImage to dstPixels. Copy starts at offset (srcX, srcY),
        and does not exceed SkImage (width(), height()).

        dstInfo specifies width, height, SkColorType, SkAlphaType, and SkColorSpace of
        destination. dstRowBytes specifies the gap from one destination row to the next.
        Returns true if pixels are copied. Returns false if:
        - dstInfo.addr() equals nullptr
        - dstRowBytes is less than dstInfo.minRowBytes()
        - SkPixelRef is nullptr

        Pixels are copied only if pixel conversion is possible. If SkImage SkColorType is
        kGray_8_SkColorType, or kAlpha_8_SkColorType; dstInfo.colorType() must match.
        If SkImage SkColorType is kGray_8_SkColorType, dstInfo.colorSpace() must match.
        If SkImage SkAlphaType is kOpaque_SkAlphaType, dstInfo.alphaType() must
        match. If SkImage SkColorSpace is nullptr, dstInfo.colorSpace() must match. Returns
        false if pixel conversion is not possible.

        srcX and srcY may be negative to copy only top or left of source. Returns
        false if width() or height() is zero or negative.
        Returns false if abs(srcX) >= Image width(), or if abs(srcY) >= Image height().

        If cachingHint is kAllow_CachingHint, pixels may be retained locally.
        If cachingHint is kDisallow_CachingHint, pixels are not added to the local cache.

        @param dstInfo      destination width, height, SkColorType, SkAlphaType, SkColorSpace
        @param dstPixels    destination pixel storage
        @param dstRowBytes  destination row length
        @param srcX         column index whose absolute value is less than width()
        @param srcY         row index whose absolute value is less than height()
        @return             true if pixels are copied to dstPixels
    */
    bool readPixels(const SkImageInfo& dstInfo, void* dstPixels, size_t dstRowBytes,
                    int srcX, int srcY, CachingHint cachingHint = kAllow_CachingHint) const;

    /** Copies a SkRect of pixels from SkImage to dst. Copy starts at (srcX, srcY), and
        does not exceed SkImage (width(), height()).

        dst specifies width, height, SkColorType, SkAlphaType, SkColorSpace, pixel storage,
        and row bytes of destination. dst.rowBytes() specifics the gap from one destination
        row to the next. Returns true if pixels are copied. Returns false if:
        - dst pixel storage equals nullptr
        - dst.rowBytes is less than SkImageInfo::minRowBytes
        - SkPixelRef is nullptr

        Pixels are copied only if pixel conversion is possible. If SkImage SkColorType is
        kGray_8_SkColorType, or kAlpha_8_SkColorType; dst.colorType() must match.
        If SkImage SkColorType is kGray_8_SkColorType, dst.colorSpace() must match.
        If SkImage SkAlphaType is kOpaque_SkAlphaType, dst.alphaType() must
        match. If SkImage SkColorSpace is nullptr, dst.colorSpace() must match. Returns
        false if pixel conversion is not possible.

        srcX and srcY may be negative to copy only top or left of source. Returns
        false if width() or height() is zero or negative.
        Returns false if abs(srcX) >= Image width(), or if abs(srcY) >= Image height().

        If cachingHint is kAllow_CachingHint, pixels may be retained locally.
        If cachingHint is kDisallow_CachingHint, pixels are not added to the local cache.

        @param dst          destination SkPixmap: SkImageInfo, pixels, row bytes
        @param srcX         column index whose absolute value is less than width()
        @param srcY         row index whose absolute value is less than height()
        @return             true if pixels are copied to dst
    */
    bool readPixels(const SkPixmap& dst, int srcX, int srcY,
                    CachingHint cachingHint = kAllow_CachingHint) const;

    /** The result from asyncRescaleAndReadPixels() or asyncRescaleAndReadPixelsYUV420(). */
    class AsyncReadResult {
    public:
        AsyncReadResult(const AsyncReadResult&) = delete;
        AsyncReadResult(AsyncReadResult&&) = delete;
        AsyncReadResult& operator=(const AsyncReadResult&) = delete;
        AsyncReadResult& operator=(AsyncReadResult&&) = delete;

        virtual ~AsyncReadResult() = default;
        virtual int count() const = 0;
        virtual const void* data(int i) const = 0;
        virtual size_t rowBytes(int i) const = 0;

    protected:
        AsyncReadResult() = default;
    };

    /** Client-provided context that is passed to client-provided ReadPixelsContext. */
    using ReadPixelsContext = void*;

    /**  Client-provided callback to asyncRescaleAndReadPixels() or
         asyncRescaleAndReadPixelsYUV420() that is called when read result is ready or on failure.
     */
    using ReadPixelsCallback = void(ReadPixelsContext, std::unique_ptr<const AsyncReadResult>);

    enum class RescaleGamma : bool { kSrc, kLinear };

    /** Makes image pixel data available to caller, possibly asynchronously. It can also rescale
        the image pixels.

        Currently asynchronous reads are only supported on the GPU backend and only when the
        underlying 3D API supports transfer buffers and CPU/GPU synchronization primitives. In all
        other cases this operates synchronously.

        Data is read from the source sub-rectangle, is optionally converted to a linear gamma, is
        rescaled to the size indicated by 'info', is then converted to the color space, color type,
        and alpha type of 'info'. A 'srcRect' that is not contained by the bounds of the image
        causes failure.

        When the pixel data is ready the caller's ReadPixelsCallback is called with a
        AsyncReadResult containing pixel data in the requested color type, alpha type, and color
        space. The AsyncReadResult will have count() == 1. Upon failure the callback is called with
        nullptr for AsyncReadResult. For a GPU image this flushes work but a submit must occur to
        guarantee a finite time before the callback is called.

        The data is valid for the lifetime of AsyncReadResult with the exception that if the SkImage
        is GPU-backed the data is immediately invalidated if the GrContext is abandoned or
        destroyed.

        @param info            info of the requested pixels
        @param srcRect         subrectangle of image to read
        @param rescaleGamma    controls whether rescaling is done in the image's gamma or whether
                               the source data is transformed to a linear gamma before rescaling.
        @param rescaleQuality  controls the quality (and cost) of the rescaling
        @param callback        function to call with result of the read
        @param context         passed to callback
    */
    void asyncRescaleAndReadPixels(const SkImageInfo& info,
                                   const SkIRect& srcRect,
                                   RescaleGamma rescaleGamma,
                                   SkFilterQuality rescaleQuality,
                                   ReadPixelsCallback callback,
                                   ReadPixelsContext context);

    /**
        Similar to asyncRescaleAndReadPixels but performs an additional conversion to YUV. The
        RGB->YUV conversion is controlled by 'yuvColorSpace'. The YUV data is returned as three
        planes ordered y, u, v. The u and v planes are half the width and height of the resized
        rectangle. The y, u, and v values are single bytes. Currently this fails if 'dstSize'
        width and height are not even. A 'srcRect' that is not contained by the bounds of the
        image causes failure.

        When the pixel data is ready the caller's ReadPixelsCallback is called with a
        AsyncReadResult containing the planar data. The AsyncReadResult will have count() == 3.
        Upon failure the callback is called with nullptr for AsyncReadResult. For a GPU image this
        flushes work but a submit must occur to guarantee a finite time before the callback is
        called.

        The data is valid for the lifetime of AsyncReadResult with the exception that if the SkImage
        is GPU-backed the data is immediately invalidated if the GrContext is abandoned or
        destroyed.

        @param yuvColorSpace  The transformation from RGB to YUV. Applied to the resized image
                              after it is converted to dstColorSpace.
        @param dstColorSpace  The color space to convert the resized image to, after rescaling.
        @param srcRect        The portion of the image to rescale and convert to YUV planes.
        @param dstSize        The size to rescale srcRect to
        @param rescaleGamma   controls whether rescaling is done in the image's gamma or whether
                              the source data is transformed to a linear gamma before rescaling.
        @param rescaleQuality controls the quality (and cost) of the rescaling
        @param callback       function to call with the planar read result
        @param context        passed to callback
     */
    void asyncRescaleAndReadPixelsYUV420(SkYUVColorSpace yuvColorSpace,
                                         sk_sp<SkColorSpace> dstColorSpace,
                                         const SkIRect& srcRect,
                                         const SkISize& dstSize,
                                         RescaleGamma rescaleGamma,
                                         SkFilterQuality rescaleQuality,
                                         ReadPixelsCallback callback,
                                         ReadPixelsContext context);

    /** Copies SkImage to dst, scaling pixels to fit dst.width() and dst.height(), and
        converting pixels to match dst.colorType() and dst.alphaType(). Returns true if
        pixels are copied. Returns false if dst.addr() is nullptr, or dst.rowBytes() is
        less than dst SkImageInfo::minRowBytes.

        Pixels are copied only if pixel conversion is possible. If SkImage SkColorType is
        kGray_8_SkColorType, or kAlpha_8_SkColorType; dst.colorType() must match.
        If SkImage SkColorType is kGray_8_SkColorType, dst.colorSpace() must match.
        If SkImage SkAlphaType is kOpaque_SkAlphaType, dst.alphaType() must
        match. If SkImage SkColorSpace is nullptr, dst.colorSpace() must match. Returns
        false if pixel conversion is not possible.

        Scales the image, with filterQuality, to match dst.width() and dst.height().
        filterQuality kNone_SkFilterQuality is fastest, typically implemented with
        nearest neighbor filter. kLow_SkFilterQuality is typically implemented with
        bilerp filter. kMedium_SkFilterQuality is typically implemented with
        bilerp filter, and mip-map filter when size is reduced.
        kHigh_SkFilterQuality is slowest, typically implemented with bicubic filter.

        If cachingHint is kAllow_CachingHint, pixels may be retained locally.
        If cachingHint is kDisallow_CachingHint, pixels are not added to the local cache.

        @param dst            destination SkPixmap: SkImageInfo, pixels, row bytes
        @return               true if pixels are scaled to fit dst
    */
    bool scalePixels(const SkPixmap& dst, SkFilterQuality filterQuality,
                     CachingHint cachingHint = kAllow_CachingHint) const;

    /** Encodes SkImage pixels, returning result as SkData.

        Returns nullptr if encoding fails, or if encodedImageFormat is not supported.

        SkImage encoding in a format requires both building with one or more of:
        SK_ENCODE_JPEG, SK_ENCODE_PNG, SK_ENCODE_WEBP; and platform support
        for the encoded format.

        If SK_BUILD_FOR_MAC or SK_BUILD_FOR_IOS is defined, encodedImageFormat can
        additionally be one of: SkEncodedImageFormat::kICO, SkEncodedImageFormat::kBMP,
        SkEncodedImageFormat::kGIF.

        quality is a platform and format specific metric trading off size and encoding
        error. When used, quality equaling 100 encodes with the least error. quality may
        be ignored by the encoder.

        @param encodedImageFormat  one of: SkEncodedImageFormat::kJPEG, SkEncodedImageFormat::kPNG,
                                   SkEncodedImageFormat::kWEBP
        @param quality             encoder specific metric with 100 equaling best
        @return                    encoded SkImage, or nullptr

        example: https://fiddle.skia.org/c/@Image_encodeToData
    */
    sk_sp<SkData> encodeToData(SkEncodedImageFormat encodedImageFormat, int quality) const;

    /** Encodes SkImage pixels, returning result as SkData. Returns existing encoded data
        if present; otherwise, SkImage is encoded with SkEncodedImageFormat::kPNG. Skia
        must be built with SK_ENCODE_PNG to encode SkImage.

        Returns nullptr if existing encoded data is missing or invalid, and
        encoding fails.

        @return  encoded SkImage, or nullptr

        example: https://fiddle.skia.org/c/@Image_encodeToData_2
    */
    sk_sp<SkData> encodeToData() const;

    /** Returns encoded SkImage pixels as SkData, if SkImage was created from supported
        encoded stream format. Platform support for formats vary and may require building
        with one or more of: SK_ENCODE_JPEG, SK_ENCODE_PNG, SK_ENCODE_WEBP.

        Returns nullptr if SkImage contents are not encoded.

        @return  encoded SkImage, or nullptr

        example: https://fiddle.skia.org/c/@Image_refEncodedData
    */
    sk_sp<SkData> refEncodedData() const;

    /** Returns subset of SkImage. subset must be fully contained by SkImage dimensions().
        The implementation may share pixels, or may copy them.

        Returns nullptr if any of the following are true:
          - Subset is empty
          - Subset is not contained by bounds
          - Pixels in SkImage could not be read or copied

        If this image is texture-backed, the context parameter is required and must match the
        context of the source image.

        @param subset  bounds of returned SkImage
        @param context the GrDirectContext in play, if it exists
        @return        partial or full SkImage, or nullptr

        example: https://fiddle.skia.org/c/@Image_makeSubset
    */
    sk_sp<SkImage> makeSubset(const SkIRect& subset, GrDirectContext* direct = nullptr) const;

    /**
     *  Returns true if the image has mipmap levels.
     */
    bool hasMipmaps() const;

    /**
     *  Returns an image with the same "base" pixels as the this image, but with mipmap levels
     *  as well. If this image already has mipmap levels, they will be replaced with new ones.
     *
     *  If data == nullptr, the mipmap levels are computed automatically.
     *  If data != nullptr, then the caller has provided the data for each level.
     */
    sk_sp<SkImage> withMipmaps(sk_sp<SkMipmap> data) const;

    /** Returns SkImage backed by GPU texture associated with context. Returned SkImage is
        compatible with SkSurface created with dstColorSpace. The returned SkImage respects
        mipMapped setting; if mipMapped equals GrMipMapped::kYes, the backing texture
        allocates mip map levels.

        The mipMapped parameter is effectively treated as kNo if MIP maps are not supported by the
        GPU.

        Returns original SkImage if the image is already texture-backed, the context matches, and
        mipMapped is compatible with the backing GPU texture. SkBudgeted is ignored in this case.

        Returns nullptr if context is nullptr, or if SkImage was created with another
        GrDirectContext.

        @param GrDirectContext the GrDirectContext in play, if it exists
        @param GrMipMapped     whether created SkImage texture must allocate mip map levels
        @param SkBudgeted      whether to count a newly created texture for the returned image
                               counts against the GrContext's budget.
        @return                created SkImage, or nullptr
    */
#ifndef SK_IMAGE_MAKE_TEXTURE_IMAGE_ALLOW_GR_CONTEXT
    sk_sp<SkImage> makeTextureImage(GrDirectContext*,
                                    GrMipMapped = GrMipMapped::kNo,
                                    SkBudgeted = SkBudgeted::kYes) const;
#else
    sk_sp<SkImage> makeTextureImage(GrContext*,
                                    GrMipMapped = GrMipMapped::kNo,
                                    SkBudgeted = SkBudgeted::kYes) const;
#endif

    /** Returns raster image or lazy image. Copies SkImage backed by GPU texture into
        CPU memory if needed. Returns original SkImage if decoded in raster bitmap,
        or if encoded in a stream.

        Returns nullptr if backed by GPU texture and copy fails.

        @return  raster image, lazy image, or nullptr

        example: https://fiddle.skia.org/c/@Image_makeNonTextureImage
    */
    sk_sp<SkImage> makeNonTextureImage() const;

    /** Returns raster image. Copies SkImage backed by GPU texture into CPU memory,
        or decodes SkImage from lazy image. Returns original SkImage if decoded in
        raster bitmap.

        Returns nullptr if copy, decode, or pixel read fails.

        If cachingHint is kAllow_CachingHint, pixels may be retained locally.
        If cachingHint is kDisallow_CachingHint, pixels are not added to the local cache.

        @return  raster image, or nullptr

        example: https://fiddle.skia.org/c/@Image_makeRasterImage
    */
    sk_sp<SkImage> makeRasterImage(CachingHint cachingHint = kDisallow_CachingHint) const;

    /** Creates filtered SkImage. filter processes original SkImage, potentially changing
        color, position, and size. subset is the bounds of original SkImage processed
        by filter. clipBounds is the expected bounds of the filtered SkImage. outSubset
        is required storage for the actual bounds of the filtered SkImage. offset is
        required storage for translation of returned SkImage.

        Returns nullptr if SkImage could not be created. If nullptr is returned, outSubset
        and offset are undefined.

        Useful for animation of SkImageFilter that varies size from frame to frame.
        Returned SkImage is created larger than required by filter so that GPU texture
        can be reused with different sized effects. outSubset describes the valid bounds
        of GPU texture returned. offset translates the returned SkImage to keep subsequent
        animation frames aligned with respect to each other.

        @param context     the GrContext in play - if it exists
        @param filter      how SkImage is sampled when transformed
        @param subset      bounds of SkImage processed by filter
        @param clipBounds  expected bounds of filtered SkImage
        @param outSubset   storage for returned SkImage bounds
        @param offset      storage for returned SkImage translation
        @return            filtered SkImage, or nullptr
    */
    sk_sp<SkImage> makeWithFilter(GrContext* context,
                                  const SkImageFilter* filter, const SkIRect& subset,
                                  const SkIRect& clipBounds, SkIRect* outSubset,
                                  SkIPoint* offset) const;

    /** To be deprecated.
    */
    sk_sp<SkImage> makeWithFilter(const SkImageFilter* filter, const SkIRect& subset,
                                  const SkIRect& clipBounds, SkIRect* outSubset,
                                  SkIPoint* offset) const;

    /** Defines a callback function, taking one parameter of type GrBackendTexture with
        no return value. Function is called when back-end texture is to be released.
    */
    typedef std::function<void(GrBackendTexture)> BackendTextureReleaseProc;

    /** Creates a GrBackendTexture from the provided SkImage. Returns true and
        stores result in backendTexture and backendTextureReleaseProc if
        texture is created; otherwise, returns false and leaves
        backendTexture and backendTextureReleaseProc unmodified.

        Call backendTextureReleaseProc after deleting backendTexture.
        backendTextureReleaseProc cleans up auxiliary data related to returned
        backendTexture. The caller must delete returned backendTexture after use.

        If SkImage is both texture backed and singly referenced, image is returned in
        backendTexture without conversion or making a copy. SkImage is singly referenced
        if its was transferred solely using std::move().

        If SkImage is not texture backed, returns texture with SkImage contents.

        @param context                    GPU context
        @param image                      SkImage used for texture
        @param backendTexture             storage for back-end texture
        @param backendTextureReleaseProc  storage for clean up function
        @return                           true if back-end texture was created
    */
    static bool MakeBackendTextureFromSkImage(GrContext* context,
                                              sk_sp<SkImage> image,
                                              GrBackendTexture* backendTexture,
                                              BackendTextureReleaseProc* backendTextureReleaseProc);

    /** Deprecated.
     */
    enum LegacyBitmapMode {
        kRO_LegacyBitmapMode, //!< returned bitmap is read-only and immutable
    };

    /** Deprecated.
        Creates raster SkBitmap with same pixels as SkImage. If legacyBitmapMode is
        kRO_LegacyBitmapMode, returned bitmap is read-only and immutable.
        Returns true if SkBitmap is stored in bitmap. Returns false and resets bitmap if
        SkBitmap write did not succeed.

        @param bitmap            storage for legacy SkBitmap
        @param legacyBitmapMode  bitmap is read-only and immutable
        @return                  true if SkBitmap was created
    */
    bool asLegacyBitmap(SkBitmap* bitmap,
                        LegacyBitmapMode legacyBitmapMode = kRO_LegacyBitmapMode) const;

    /** Returns true if SkImage is backed by an image-generator or other service that creates
        and caches its pixels or texture on-demand.

        @return  true if SkImage is created as needed

        example: https://fiddle.skia.org/c/@Image_isLazyGenerated_a
        example: https://fiddle.skia.org/c/@Image_isLazyGenerated_b
    */
    bool isLazyGenerated() const;

    /** Creates SkImage in target SkColorSpace.
        Returns nullptr if SkImage could not be created.

        Returns original SkImage if it is in target SkColorSpace.
        Otherwise, converts pixels from SkImage SkColorSpace to target SkColorSpace.
        If SkImage colorSpace() returns nullptr, SkImage SkColorSpace is assumed to be sRGB.

        If this image is texture-backed, the context parameter is required and must match the
        context of the source image.

        @param target  SkColorSpace describing color range of returned SkImage
        @param direct  The GrDirectContext in play, if it exists
        @return        created SkImage in target SkColorSpace

        example: https://fiddle.skia.org/c/@Image_makeColorSpace
    */
    sk_sp<SkImage> makeColorSpace(sk_sp<SkColorSpace> target,
                                  GrDirectContext* direct = nullptr) const;

    /** Experimental.
        Creates SkImage in target SkColorType and SkColorSpace.
        Returns nullptr if SkImage could not be created.

        Returns original SkImage if it is in target SkColorType and SkColorSpace.

        If this image is texture-backed, the context parameter is required and must match the
        context of the source image.

        @param targetColorType  SkColorType of returned SkImage
        @param targetColorSpace SkColorSpace of returned SkImage
        @param direct           The GrDirectContext in play, if it exists
        @return                 created SkImage in target SkColorType and SkColorSpace
    */
    sk_sp<SkImage> makeColorTypeAndColorSpace(SkColorType targetColorType,
                                              sk_sp<SkColorSpace> targetColorSpace,
                                              GrDirectContext* direct = nullptr) const;

    /** Creates a new SkImage identical to this one, but with a different SkColorSpace.
        This does not convert the underlying pixel data, so the resulting image will draw
        differently.
    */
    sk_sp<SkImage> reinterpretColorSpace(sk_sp<SkColorSpace> newColorSpace) const;

private:
    SkImage(const SkImageInfo& info, uint32_t uniqueID);
    friend class SkImage_Base;

    SkImageInfo     fInfo;
    const uint32_t  fUniqueID;

    typedef SkRefCnt INHERITED;
};

#endif
