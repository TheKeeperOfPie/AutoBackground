package cw.kop.autowallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements Renderer {

    private static final String TAG = "Renderer";

    // Our matrices
    private float[] mtrxProjection = new float[16];
    private float[] mtrxView = new float[16];
    private float[] mtrxProjectionAndView = new float[16];
    private float[] transMatrix = new float[16];

    // Geometric variables
    public float vertices[];
    public short indices[];
    public float uvs[];
    public FloatBuffer vertexBuffer;
    public ShortBuffer drawListBuffer;
    public FloatBuffer uvBuffer;

    // Our screenresolution
    private float screenWidth;
    private float screenHeight;

    private float bitmapWidth;
    private float bitmapHeight;
    private float oldBitmapWidth;
    private float oldBitmapHeight;
    private float offset = 0;
    private float oldOffset = 0;

    // Misc
    Context appContext;
    int mProgram;
    private Bitmap localBitmap;
    private float fadeInAlpha = 0f;
    private float fadeOutAlpha = 1.0f;
    private boolean toFade = false;
    private int[] texturenames = new int[2];
    private boolean firstRun = true;

    public GLRenderer(Context context, int width, int height) {
        appContext = context;
        screenWidth = width;
        screenHeight = height;

        Log.i(TAG, "ID_0: " + texturenames[0]);
        Log.i(TAG, "ID_1: " + texturenames[1]);

        Downloader.getNextImage(appContext);
        localBitmap = Downloader.getBitmap();

        if (localBitmap == null) {
            // If no bitmap available, set wallpaper as app_icon, prevents null pointer checks
            int id = appContext.getResources().getIdentifier("drawable/app_icon", null, appContext.getPackageName());
            localBitmap =  BitmapFactory.decodeResource(appContext.getResources(), id);
        }

        bitmapWidth = localBitmap.getWidth();
        bitmapHeight = localBitmap.getHeight();

    }

    public void onPause()
    {
        /* Do stuff to pause the renderer */
    }

    public void onResume()
    {
        /* Do stuff to resume the renderer */
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        if (localBitmap != null) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // Get handle to textures locations
            int mAlphaHandle = GLES20.glGetUniformLocation(GLShaders.programImage, "opacity");
            int mTextureUniformHandle = GLES20.glGetUniformLocation (GLShaders.programImage, "s_texture");

            if (toFade) {

                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                setupContainer(oldBitmapWidth, oldBitmapHeight);

                GLES20.glUniform1f(mAlphaHandle, fadeOutAlpha);
                fadeOutAlpha -= 0.01f;

//                GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
//                Log.i(TAG, "Binding " + texturenames[0]);
//                GLES20.glUniform1i(mTextureUniformHandle, 0);
                Matrix.setIdentityM(transMatrix, 0);
                Matrix.translateM(transMatrix, 0, oldOffset, 0, 0);

                Render(mtrxProjectionAndView);
                Log.i(TAG, "Fade out");

                setupContainer(bitmapWidth, bitmapHeight);

                GLES20.glUniform1f(mAlphaHandle, fadeInAlpha);
                fadeInAlpha += 0.01f;

//                GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[1]);
//                Log.i(TAG, "Binding " + texturenames[1]);
//                GLES20.glUniform1i(mTextureUniformHandle, 1);
                Matrix.setIdentityM(transMatrix, 0);
                Matrix.translateM(transMatrix, 0, offset, 0, 0);

                Render(mtrxProjectionAndView);
                Log.i(TAG, "Fade in");

                GLES20.glDisable(GLES20.GL_BLEND);

                if (fadeInAlpha > 1.0f || fadeOutAlpha < 0.0f) {
                    fadeInAlpha = 0.0f;
                    fadeOutAlpha = 1.0f;
                    int storeId = texturenames[0];
                    texturenames[0] = texturenames[1];
                    texturenames[1] = storeId;
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
                    toFade = false;
                }

            }
            else {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
                Matrix.setIdentityM(transMatrix, 0);
                Matrix.translateM(transMatrix, 0, offset, 0, 0);
                Render(mtrxProjectionAndView);
            }
        }

    }

    private void Render(float[] m) {

        // clear Screen and Depth Buffer,

        if (localBitmap != null) {

            // get handle to vertex shader's vPosition member
            int mPositionHandle = GLES20.glGetAttribLocation(GLShaders.programImage, "vPosition");

            // Enable generic vertex attribute array
            GLES20.glEnableVertexAttribArray(mPositionHandle);

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            // Get handle to texture coordinates location
            int mTexCoordLoc = GLES20.glGetAttribLocation(GLShaders.programImage, "a_texCoord");

            // Enable generic vertex attribute array
            GLES20.glEnableVertexAttribArray(mTexCoordLoc);

            // Prepare the texturecoordinates
            GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

            // Get handle to shape's transformation matrix
            int mtrxhandle = GLES20.glGetUniformLocation(GLShaders.programImage, "uMVPMatrix");

            Matrix.multiplyMM(m, 0, mtrxView, 0, transMatrix, 0);

            // Apply the projection and view transformation
            GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

            Matrix.multiplyMM(m, 0, mtrxProjection, 0, transMatrix, 0);

            GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

            // Draw the triangle
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(mTexCoordLoc);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (height == 0) {
            height = 1;
        }

        screenWidth = width;
        screenHeight = height;

        for(int i=0;i<16;i++)
        {
            mtrxProjection[i] = 0.0f;
            mtrxView[i] = 0.0f;
            mtrxProjectionAndView[i] = 0.0f;
        }

        Matrix.orthoM(mtrxProjection, 0, 0f, screenWidth, 0.0f, screenHeight, 0, 50);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0.0f, 0f, 1f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        Log.i(TAG, "onSurfaceCreated called");

        if (firstRun) {
            // Generate Textures, if more needed, alter these numbers.
            GLES20.glGenTextures(2, texturenames, 0);
            firstRun = false;
        }

        // Create the container
        setupContainer(bitmapWidth, bitmapHeight);

        // Create image texture
        setupImage();

        // Set the clear color to black
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);

        int vertexShader = GLShaders.loadShader(GLES20.GL_VERTEX_SHADER, GLShaders.vertexShaderImage);
        int fragmentShader = GLShaders.loadShader(GLES20.GL_FRAGMENT_SHADER, GLShaders.fragmentShaderImage);

        GLShaders.programImage = GLES20.glCreateProgram();
        GLES20.glAttachShader(GLShaders.programImage, vertexShader);
        GLES20.glAttachShader(GLShaders.programImage, fragmentShader);
        GLES20.glLinkProgram(GLShaders.programImage);

        GLES20.glUseProgram(GLShaders.programImage);

    }

    public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
        offset = (screenWidth - bitmapWidth) * (xOffset);
    }

    public void setupContainer(float width, float height)
    {
        // We have create the vertices of our view.
        vertices = new float[] {
                0.0f,
                (screenHeight + ((height - screenHeight) / 2)),
                0.0f,
                0.0f,
                (-(height - screenHeight) / 2),
                0.0f,
                width,
                (-(height - screenHeight) / 2),
                0.0f,
                width,
                (screenHeight + (height - screenHeight) / 2),
                0.0f
                };

        indices = new short[] {0, 1, 2, 0, 2, 3}; // render order of vertices

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

    }

    public void setBitmap(Bitmap bitmap) {
        localBitmap = bitmap;

        oldOffset = offset;

        offset += (bitmapWidth / 2) - (localBitmap.getWidth() / 2);

        oldBitmapWidth = bitmapWidth;
        oldBitmapHeight = bitmapHeight;

        bitmapWidth = localBitmap.getWidth();
        bitmapHeight = localBitmap.getHeight();

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[1]);
        Log.i(TAG, "Binding " + texturenames[1]);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // Set wrapping mode
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, localBitmap, 0);

        toFade = true;
    }

    public void setOffset(float xOffset) {
        offset = xOffset;
    }

    public void setupImage()
    {
        Log.i(TAG, "Setup image");

        // Create our UV coordinates.
        uvs = new float[] {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f
        };

        // The texture buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        // Bind texture to texturename
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
        Log.i(TAG, "Binding " + texturenames[0]);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // Set wrapping mode
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, localBitmap, 0);
    }

    /**
     * Called when the engine is destroyed. Do any necessary clean up because
     * at this point your renderer instance is now done for.
     */
    public void release() {
        Log.i(TAG, "release");
        localBitmap.recycle();
    }
}