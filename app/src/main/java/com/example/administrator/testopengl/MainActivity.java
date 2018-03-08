package com.example.administrator.testopengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;
    private MyRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

/*
        if (!Utils.supportGlEs20(this)) {
            Toast.makeText(this, "GLES 2.0 not supported!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
*/
        mGLSurfaceView = new GLSurfaceView(this);

        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mRenderer = new MyRenderer(this);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        setContentView(mGLSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRenderer.destroy();
    }

    private static class MyRenderer implements GLSurfaceView.Renderer {

        private static final String VERTEX_SHADER =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "attribute vec2 a_texCoord;" +
                        "varying vec2 v_texCoord;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "  v_texCoord = a_texCoord;" +
                        "}";
        private static final String FRAGMENT_SHADER =
                "precision mediump float;" +
                        "varying vec2 v_texCoord;" +
                        "uniform sampler2D s_texture;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(s_texture, v_texCoord);" +
                        "}";
        // 顶点坐标系，需要显示整个图就需要计算这个，假设图片是宽高比是700/100,
        // 要全部显示的话就需要计算.顶点坐标系范围是[-1,1]，原点(0,0)在中间
        private static final float[] VERTEX = {   // in counterclockwise order:
                -0.7f, 1, 0,  // top left
                -0.7f, -1, 0, // bottom left
                0.7f, 1, 0,   // top right
                0.7f, -1, 0,  // bottom righ
        };
        private static final short[] VERTEX_INDEX = {
                0, 1, 2, 0, 2, 3
        };
        //这个是纹理坐标系，表示要显示的纹理，这里是全部都显示了，如果需要剪裁，
        //就需要修改这里的坐标，纹理坐标的范围是[0-1],左上角为原点(0,0).
        //这个坐标系应该要和顶点坐标系一一对应，不然就会出现倒置或者显示部分的问题
        private static final float[] TEX_VERTEX = {   // in clockwise order:
                0, 0,  // top left
                0, 1,  // bottom left
                1f, 0,  // top right
                1f, 1,  //bottom right
        };

        //不做任何变换下的原始矩阵
        private float[] mMVPMatrix= {
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        };
        private final Context mContext;
        private final FloatBuffer mVertexBuffer;
        private final FloatBuffer mTexVertexBuffer;
        //private final ShortBuffer mVertexIndexBuffer;
        //private final float[] mMVPMatrix = new float[16];


        private int mProgram;
        private int mPositionHandle;
        private int mMatrixHandle;
        private int mTexCoordHandle;
        private int mTexSamplerHandle;
        private int mTexName;

        MyRenderer(final Context context) {
            mContext = context;
            mVertexBuffer = ByteBuffer.allocateDirect(VERTEX.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(VERTEX);
            mVertexBuffer.position(0);
/*
            mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .put(VERTEX_INDEX);

            mVertexIndexBuffer.position(0);
*/
            mTexVertexBuffer = ByteBuffer.allocateDirect(TEX_VERTEX.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(TEX_VERTEX);
            mTexVertexBuffer.position(0);
        }

        static int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            mProgram = GLES20.glCreateProgram();
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);

            GLES20.glUseProgram(mProgram);

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
            mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            mTexSamplerHandle = GLES20.glGetUniformLocation(mProgram, "s_texture");

            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
                    12, mVertexBuffer);

            GLES20.glEnableVertexAttribArray(mTexCoordHandle);
            GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
                    mTexVertexBuffer);

            int[] texNames = new int[1];
            GLES20.glGenTextures(1, texNames, 0);
            mTexName = texNames[0];
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.p_300px);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexName);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_REPEAT);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            //这个是做投影变换的，对于2d，根本就不需要
            //Matrix.perspectiveM(mMVPMatrix, 0, 45, (float) width / height, 0.1f, 100f);
            //Matrix.translateM(mMVPMatrix, 0, 0f, 0f, -5f);
        }

        @Override
        public void onDrawFrame(GL10 unused) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniform1i(mTexSamplerHandle, 0);

            // 用 glDrawElements 来绘制，mVertexIndexBuffer 指定了顶点绘制顺序
            //GLES20.glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length,
            //        GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);

            /*
            GLES20.glDrawArrays的第一个参数表示绘制方式，第二个参数表示偏移量，第三个参数表示顶点个数。
            绘制方式有：

            int GL_POINTS       //将传入的顶点坐标作为单独的点绘制
            int GL_LINES        //将传入的坐标作为单独线条绘制，ABCDEFG六个顶点，绘制AB、CD、EF三条线
            int GL_LINE_STRIP   //将传入的顶点作为折线绘制，ABCD四个顶点，绘制AB、BC、CD三条线
            int GL_LINE_LOOP    //将传入的顶点作为闭合折线绘制，ABCD四个顶点，绘制AB、BC、CD、DA四条线。
            int GL_TRIANGLES    //将传入的顶点作为单独的三角形绘制，ABCDEF绘制ABC,DEF两个三角形
            int GL_TRIANGLE_FAN    //将传入的顶点作为扇面绘制，ABCDEF绘制ABC、ACD、ADE、AEF四个三角形
            int GL_TRIANGLE_STRIP   //将传入的顶点作为三角条带绘制，ABCDEF绘制ABC,BCD,CDE,DEF四个三角形
            */
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        void destroy() {
            GLES20.glDeleteTextures(1, new int[] { mTexName }, 0);
        }
    }
}