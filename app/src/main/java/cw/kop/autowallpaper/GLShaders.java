package cw.kop.autowallpaper;

import android.opengl.GLES20;

/**
 * Created by TheKeeperOfPie on 6/27/2014.
 */
public class GLShaders {

    // Program variables
    public static int programImage;

    public static final String vertexShaderImage =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "attribute vec2 a_texCoord;" +
        "varying vec2 v_texCoord;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "  v_texCoord = a_texCoord;" +
        "}";
    public static final String fragmentShaderImage =
        "precision mediump float;" +
        "varying vec2 v_texCoord;" +
        "uniform float opacity;" +
        "uniform sampler2D s_texture;" +
        "void main() {" +
        "  gl_FragColor = texture2D(s_texture, v_texCoord);" +
        "  gl_FragColor.a *= opacity;" +
        "}";

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // return the shader
        return shader;
    }
}
