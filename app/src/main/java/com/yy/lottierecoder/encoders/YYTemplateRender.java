package com.yy.lottierecoder.encoders;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import com.yy.lottierecoder.OffscreenAfterEffectView;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * @author ferrisXu
 * 创建日期：2019/5/7
 * 描述：
 */
public class YYTemplateRender  implements Runnable{
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final String TAG = "EncodeAndMuxTest";
    private static final boolean VERBOSE = false;           // lots of logging
    private  String mTemplate;
    private String mAudioPath;
    private String mOutputFile;
    private int frame_rate = 20, num_frames;
    private static final int IFRAME_INTERVAL = 1;          // 10 seconds between I-frames
    OffscreenAfterEffectView offscreenAfterEffectView;
    IRenderProgressListem iRenderProgressListem;
    Context context;
    public YYTemplateRender(Context context,IRenderProgressListem iRenderProgressListem, String template, String audioPath, String outputFile) {
        this.context=context;
        this.mTemplate = template;
        this.mAudioPath = audioPath;
        this.iRenderProgressListem=iRenderProgressListem;
        this.checkOutputFileName(outputFile);
        offscreenAfterEffectView=new OffscreenAfterEffectView(context);
        offscreenAfterEffectView.load();
    }

    private void checkOutputFileName(String outputFile) {
        if (outputFile != null && outputFile.toLowerCase().endsWith(".mp4")) {
            this.mOutputFile = outputFile;
        } else {
            throw new IllegalArgumentException("output file must end with .mp4");
        }
        File outFile=new File(outputFile);
        if(outFile.exists())
            outFile.delete();
    }
    // size of a frame, in pixels
    private int mWidth = -1;
    private int mHeight = -1;
    // bit rate, in bits per second
    private int mBitRate = -1;
    private MediaCodec mEncoder;
    private CodecInputSurface mInputSurface;
    private MediaMuxer mMuxer;
    private int mTrackIndex;
    private boolean mMuxerStarted;
    // allocate one of these up front so we don't need to do it every time
    private MediaCodec.BufferInfo mBufferInfo;
    public void run() {
        if(offscreenAfterEffectView==null||iRenderProgressListem==null)
            return;
        iRenderProgressListem.onProgress(0.0f);
        mWidth = offscreenAfterEffectView.getLottieWidth();
        mHeight = offscreenAfterEffectView.getLottieHeight();
        mBitRate = 2000000;
        num_frames= (int) offscreenAfterEffectView.getDrawable().getComposition().getEndFrame();
        frame_rate= (int) offscreenAfterEffectView.getDrawable().getComposition().getFrameRate();
        try {
            prepareEncoder();
            mInputSurface.makeCurrent();
            offscreenAfterEffectView.initGL();
            for (int i = 0; i < num_frames; i++) {
                // Feed any pending encoder output into the muxer.
                drainEncoder(false);

                // Generate a new frame of input.
                generateSurfaceFrame(i);
                mInputSurface.setPresentationTime(computePresentationTimeNsec(i));

                // Submit it to the encoder.  The eglSwapBuffers call will block if the input
                // is full, which would be bad if it stayed full until we dequeued an output
                // buffer (which we can't do, since we're stuck here).  So long as we fully drain
                // the encoder before supplying additional input, the system guarantees that we
                // can supply another frame without blocking.
                if (VERBOSE) Log.d(TAG, "sending frame " + i + " to encoder");
                mInputSurface.swapBuffers();
                iRenderProgressListem.onProgress(i/(float)num_frames);
            }

            // send end-of-stream to encoder, and drain remaining output
            drainEncoder(true);
        } catch (Exception e){
            e.printStackTrace();
            iRenderProgressListem.onError();
        }finally {
            // release encoder, muxer, and input Surface
            releaseEncoder();
        }

        iRenderProgressListem.success();
    }
    private static int align16(int size) {
        if (size % 16 > 0) {
            size = (size / 16) * 16 + 16;
        }
        return size;
    }
    public interface IRenderProgressListem{
        void onProgress(float progress);

        void onError();

        void success();
    }
    private void generateSurfaceFrame(int frameIndex) {
        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES30.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //drawing
        if(offscreenAfterEffectView!=null)
        offscreenAfterEffectView.generateSurfaceFrame(frameIndex);
    }
    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    private void releaseEncoder() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");
        if(offscreenAfterEffectView!=null){
            offscreenAfterEffectView.release();
            offscreenAfterEffectView=null;
        }
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }
    /**
     * Extracts all pending data from the encoder.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     */
    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            mEncoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    if (VERBOSE) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }
    /**
     * Generates the presentation time for frame N, in nanoseconds.
     */
    private  long computePresentationTimeNsec(int frameIndex) {
        final long ONE_BILLION = 1000000000;
        return (long) (frameIndex * ONE_BILLION /(float)frame_rate);
    }
    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    private void prepareEncoder() throws IOException{
        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, align16(mWidth), align16( mHeight));

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frame_rate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);


        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        //
        // If you want to have two EGL contexts -- one for display, one for recording --
        // you will likely want to defer instantiation of CodecInputSurface until after the
        // "display" EGL context is created, then modify the eglCreateContext call to
        // take eglGetCurrentContext() as the share_context argument.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = new CodecInputSurface(mEncoder.createInputSurface());
        mEncoder.start();

        // Output filename.  Ideally this would use Context.getFilesDir() rather than a
        // hard-coded output directory.
        String outputPath = mOutputFile;


        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        try {
            mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }

        mTrackIndex = -1;
        mMuxerStarted = false;
    }


    public void start() {
        (new Thread(this, "render")).start();
    }

    public void cancel() {
    }


}
