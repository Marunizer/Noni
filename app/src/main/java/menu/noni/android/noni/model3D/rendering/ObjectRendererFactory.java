package menu.noni.android.noni.model3D.rendering;


import java.io.File;

import static menu.noni.android.noni.model3D.rendering.ShaderUtil.normalizeFileName;

public class ObjectRendererFactory {
  private static final String TAG = ObjectRendererFactory.class.getSimpleName();
  static final String DEFAULT_FRAGMENT_SHADER_FILE_NAME = "object_fragment.shader";
  static final String DEFAULT_VERTEX_SHADER_FILE_NAME = "object_vertex.shader";

  private final String basepath;
  private final String externalPath;

  public ObjectRendererFactory(String basepath, String externalPath) {
    System.out.println("OBJECT RENDERER FACTORY : basepath:  " + basepath);
    if (!basepath.endsWith("/")) {
      basepath = basepath + "/";
    }
    if (!externalPath.endsWith("/")) {
      externalPath = externalPath + "/";
    }
    System.out.println("OBJECT RENDERER FACTORY : basepath:  " + basepath);

    this.basepath = basepath;
    this.externalPath = externalPath;
  }

  public ObjectRenderer create(String objectFileName) {
    System.out.println("OBJECT RENDERER FACTORY : Create step 1");
    objectFileName = normalizeFileName(objectFileName, basepath);
    return create(
        objectFileName,
        objectFileNameToTextureFileName(objectFileName));
  }

  public ObjectRenderer create(String objectFileName,
                               String textureFileName)
  {
    System.out.println("2:  obj: " + objectFileName + "    texture: " + textureFileName);
    objectFileName = normalizeFileName(objectFileName, basepath);
    textureFileName = normalizeFileName(textureFileName, basepath);
    System.out.println("3:  obj: " + objectFileName + "    texture: " + textureFileName);

    System.out.println("OBJECT RENDERER FACTORY : Create step 2");
    return create(
        objectFileName,
        textureFileName,
        normalizeFileName(DEFAULT_VERTEX_SHADER_FILE_NAME, externalPath),
        normalizeFileName(DEFAULT_FRAGMENT_SHADER_FILE_NAME, externalPath));
  }

  public ObjectRenderer create(String objectFileName,
                               String textureFileName,
                               String vertexShaderFileName,
                               String fragmentShaderFileName) {
    System.out.println("OBJECT RENDERER FACTORY : Create step 3");
    if (!checkExisting(objectFileName)
        || !checkExisting(textureFileName)
        || !checkExisting(vertexShaderFileName)
        || !checkExisting(fragmentShaderFileName)) {
      System.out.println("Checks = " + !checkExisting(objectFileName) + "  " + !checkExisting(textureFileName)+
      "   " + !checkExisting(vertexShaderFileName) + "   " + !checkExisting(fragmentShaderFileName));
      System.out.println("OBJECT RENDERER FACTORY : Create step WRONG: objectFileName = " + objectFileName +"   AND textureFileName = " + textureFileName);
      return null;
    } else {
      System.out.println("OBJECT RENDERER FACTORY : Create FINAL step");
      System.out.println("OBJECT RENDERER FACTORY : Create step RIGHT: objectFileName = " + objectFileName +"   AND textureFileName = " + textureFileName);
      return new ObjectRenderer(objectFileName, textureFileName, fragmentShaderFileName, vertexShaderFileName);
    }
  }

  private String objectFileNameToTextureFileName(String fileName) {
    if (fileName.toLowerCase().endsWith(".obj")) {
      return fileName.substring(0, fileName.length() - 4).concat(".jpg");
    } else {
      return fileName.concat(".jpg");
    }
  }

  private boolean checkExisting(String fileName) {
    return new File(fileName).exists();
  }
}
