package menu.noni.android.noni.model3D.model;

import android.app.Activity;
import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import menu.noni.android.noni.model3D.services.WavefrontLoader.FaceMaterials;
import menu.noni.android.noni.model3D.services.WavefrontLoader.Faces;
import menu.noni.android.noni.model3D.services.WavefrontLoader.Material;
import menu.noni.android.noni.model3D.services.WavefrontLoader.Materials;
import menu.noni.android.noni.model3D.services.WavefrontLoader.Tuple3;
import menu.noni.android.noni.model3D.services.wavefront.WavefrontLoader2;
import menu.noni.android.noni.util.math.Math3DUtils;

//TODO Many unused variables/methods maybe delete
public final class Object3DBuilder {

	public interface Callback {
		void onLoadError(Exception ex);

		void onLoadComplete(Object3DData data);

		void onBuildComplete(Object3DData data);
	}

	private static final int COORDS_PER_VERTEX = 3;
	/**
	 * Default vertices colors
	 */
	private static float[] DEFAULT_COLOR = {1.0f, 1.0f, 0, 1.0f};

	//@formatter:on

	private Object3DV0 object3dv0;
	private Object3DV1 object3dv1;
	private Object3DV2 object3dv2;
	private Object3DV3 object3dv3;
	private Object3DV4 object3dv4;
	private Object3DV5 object3dv5;
	private Object3DV6 object3dv6;
	private Object3DV7 object3dv7;
	private Object3DV8 object3dv8;

	static {
		System.setProperty("java.protocol.handler.pkgs", "menu.noni.android.noni.util.url|"+System.getProperty("java.protocol.handler.pkgs"));
		Log.i("Object3DBuilder", "java.protocol.handler.pkgs=" + System.getProperty("java.protocol.handler.pkgs"));
	}

	public static Object3DData buildPoint(float[] point) {
		return new Object3DData(createNativeByteBuffer(point.length * 4).asFloatBuffer().put(point))
				.setDrawMode(GLES20.GL_POINTS);
	}

	public Object3D getDrawer(Object3DData obj, boolean usingTextures, boolean usingLights) throws IOException {

		if (object3dv1 == null) {
			object3dv1 = new Object3DV1();
			object3dv2 = new Object3DV2();
			object3dv3 = new Object3DV3();
			object3dv4 = new Object3DV4();
			object3dv5 = new Object3DV5();
			object3dv6 = new Object3DV6();
			object3dv7 = new Object3DV7();
			object3dv8 = new Object3DV8();
		}

		if (usingTextures && usingLights && obj.getVertexColorsArrayBuffer() != null && obj.getTextureData() != null
				&& obj.getTextureCoordsArrayBuffer() != null && obj.getVertexNormalsArrayBuffer() != null
				&& obj.getVertexNormalsArrayBuffer() != null) {
			return object3dv6;
		} else if (usingTextures && usingLights && obj.getVertexColorsArrayBuffer() == null && obj.getTextureData() != null
				&& obj.getTextureCoordsArrayBuffer() != null && obj.getVertexNormalsArrayBuffer() != null
				&& obj.getVertexNormalsArrayBuffer() != null) {
			return object3dv8;
		} else if (usingLights && obj.getVertexColorsArrayBuffer() != null
				&& obj.getVertexNormalsArrayBuffer() != null) {
			return object3dv5;
		} else if (usingLights && obj.getVertexNormalsArrayBuffer() != null) {
			return object3dv7;
		} else if (usingTextures && obj.getVertexColorsArrayBuffer() != null && obj.getTextureData() != null
				&& obj.getTextureCoordsArrayBuffer() != null) {
			return object3dv4;
		} else if (usingTextures && obj.getVertexColorsArrayBuffer() == null && obj.getTextureData() != null
				&& obj.getTextureCoordsArrayBuffer() != null) {
			return object3dv3;
		} else if (obj.getVertexColorsArrayBuffer() != null) {
			return object3dv2;
		} else {
			return object3dv1;
		}
	}

	public static Object3DData generateArrays(AssetManager assets, Object3DData obj) throws IOException {

		Faces faces = obj.getFaces();
		FaceMaterials faceMats = obj.getFaceMats();
		Materials materials = obj.getMaterials();

		// TODO: remove this
// 		if (true) return obj;

		Log.i("Object3DBuilder", "Allocating vertex array buffer... Vertices ("+faces.getVerticesReferencesCount()+")");
		final FloatBuffer vertexArrayBuffer = createNativeByteBuffer(faces.getVerticesReferencesCount() * 3 * 4).asFloatBuffer();
		obj.setVertexArrayBuffer(vertexArrayBuffer);
		obj.setDrawUsingArrays(true);

		Log.i("Object3DBuilder", "Populating vertex array...");
		final FloatBuffer vertexBuffer = obj.getVerts();
		final IntBuffer indexBuffer = faces.getIndexBuffer();
		for (int i = 0; i < faces.getVerticesReferencesCount(); i++) {
			vertexArrayBuffer.put(i*3,vertexBuffer.get(indexBuffer.get(i) * 3));
			vertexArrayBuffer.put(i*3+1,vertexBuffer.get(indexBuffer.get(i) * 3 + 1));
			vertexArrayBuffer.put(i*3+2,vertexBuffer.get(indexBuffer.get(i) * 3 + 2));
		}



		Log.i("Object3DBuilder", "Allocating vertex normals buffer... Total normals ("+faces.facesNormIdxs.size()+")");
		// Normals buffer size = Number_of_faces X 3 (vertices_per_face) X 3 (coords_per_normal) X 4 (bytes_per_float)
		final FloatBuffer vertexNormalsArrayBuffer = createNativeByteBuffer(faces.getSize() * 3 * 3 * 4).asFloatBuffer();
		obj.setVertexNormalsArrayBuffer(vertexNormalsArrayBuffer);

		// build file normals
		final FloatBuffer vertexNormalsBuffer = obj.getNormals();
		if (vertexNormalsBuffer.capacity() > 0) {
			Log.i("Object3DBuilder", "Populating normals buffer...");
			for (int n=0; n<faces.facesNormIdxs.size(); n++) {
				int[] normal = faces.facesNormIdxs.get(n);
				for (int i = 0; i < normal.length; i++) {
					vertexNormalsArrayBuffer.put(n*9+i*3,vertexNormalsBuffer.get(normal[i] * 3));
					vertexNormalsArrayBuffer.put(n*9+i*3+1,vertexNormalsBuffer.get(normal[i] * 3 + 1));
					vertexNormalsArrayBuffer.put(n*9+i*3+2,vertexNormalsBuffer.get(normal[i] * 3 + 2));
				}
			}
		} else {
			// calculate normals for all triangles
			Log.i("Object3DBuilder", "Model without normals. Calculating [" + faces.getIndexBuffer().capacity() / 3 + "] normals...");

			final float[] v0 = new float[3], v1 = new float[3], v2 = new float[3];
			for (int i = 0; i < faces.getIndexBuffer().capacity(); i += 3) {
				try {
					v0[0] = vertexBuffer.get(faces.getIndexBuffer().get(i) * 3);
					v0[1] = vertexBuffer.get(faces.getIndexBuffer().get(i) * 3 + 1);
					v0[2] = vertexBuffer.get(faces.getIndexBuffer().get(i) * 3 + 2);

					v1[0] = vertexBuffer.get(faces.getIndexBuffer().get(i + 1) * 3);
					v1[1] = vertexBuffer.get(faces.getIndexBuffer().get(i + 1) * 3 + 1);
					v1[2] = vertexBuffer.get(faces.getIndexBuffer().get(i + 1) * 3 + 2);

					v2[0] = vertexBuffer.get(faces.getIndexBuffer().get(i + 2) * 3);
					v2[1] = vertexBuffer.get(faces.getIndexBuffer().get(i + 2) * 3 + 1);
					v2[2] = vertexBuffer.get(faces.getIndexBuffer().get(i + 2) * 3 + 2);

					float[] normal = Math3DUtils.calculateFaceNormal2(v0, v1, v2);

					vertexNormalsArrayBuffer.put(i*3,normal[0]);
					vertexNormalsArrayBuffer.put(i*3+1,normal[1]);
					vertexNormalsArrayBuffer.put(i*3+2,normal[2]);
					vertexNormalsArrayBuffer.put(i*3+3,normal[0]);
					vertexNormalsArrayBuffer.put(i*3+4,normal[1]);
					vertexNormalsArrayBuffer.put(i*3+5,normal[2]);
					vertexNormalsArrayBuffer.put(i*3+6,normal[0]);
					vertexNormalsArrayBuffer.put(i*3+7,normal[1]);
					vertexNormalsArrayBuffer.put(i*3+8,normal[2]);
				} catch (BufferOverflowException ex) {
					throw new RuntimeException("Error calculating mormal for face ["+i/3+"]");
				}
			}
		}


		FloatBuffer colorArrayBuffer = null;
		if (materials != null) {
			Log.i("Object3DBuilder", "Reading materials...");
			materials.readMaterials(obj.getCurrentDir(), obj.getAssetsDir(), assets);
		}

		if (materials != null && !faceMats.isEmpty()) {
			Log.i("Object3DBuilder", "Processing face materials...");
			colorArrayBuffer = createNativeByteBuffer(4 * faces.getVerticesReferencesCount() * 4)
					.asFloatBuffer();
			boolean anyOk = false;
			float[] currentColor = DEFAULT_COLOR;
			for (int i = 0; i < faces.getSize(); i++) {
				if (faceMats.findMaterial(i) != null) {
					Material mat = materials.getMaterial(faceMats.findMaterial(i));
					if (mat != null) {
						currentColor = mat.getKdColor() != null ? mat.getKdColor() : currentColor;
						anyOk = anyOk || mat.getKdColor() != null;
					}
				}
				colorArrayBuffer.put(currentColor);
				colorArrayBuffer.put(currentColor);
				colorArrayBuffer.put(currentColor);
			}
			if (!anyOk) {
				Log.i("Object3DBuilder", "Using single color.");
				colorArrayBuffer = null;
			}
		}
		obj.setVertexColorsArrayBuffer(colorArrayBuffer);


		String texture = null;
		byte[] textureData = null;
		if (materials != null && !materials.materials.isEmpty()) {

			// TODO: process all textures
			for (Material mat : materials.materials.values()) {
				if (mat.getTexture() != null) {
					texture = mat.getTexture();
					break;
				}
			}
			if (texture != null) {
				if (obj.getCurrentDir() != null) {
					//Pretty sure this is not actually accessed
					File file = new File(obj.getCurrentDir() + "/model/", texture);
					Log.i("Object3DBuilder", "Loading this texture '" + file + " from " + obj.getCurrentDir() +"...");
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					FileInputStream fis = new FileInputStream(file);
					IOUtils.copy(fis, bos);
					fis.close();
					textureData = bos.toByteArray();
					bos.close();
				} else {
					//I believe this is what is being used
					String assetResourceName = obj.getAssetsDir() + "/model/" + texture;
					Log.i("Object3DBuilder", "Loading that texture '" + assetResourceName + "'...");

					//Might be able to delete this
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					InputStream fis = new FileInputStream(new File(assetResourceName));

					Log.i("Object3DBuilder", "Loading fis'" + fis + "'...");

					textureData = IOUtils.toByteArray(fis);//fis.readAllBytes(); //toByteArray(); //buffer.toByteArray();//bos.toByteArray();
					fis.close();
					Log.i("Object3DBuilder", "Length of jpg bytes:'" + textureData.length + "'..." + Arrays.toString(textureData));
					buffer.close();
				}
			} else {
				Log.i("Object3DBuilder", "Found material(s) but no texture");
			}
		} else{
			Log.i("Object3DBuilder", "No materials -> No texture");
		}


		if (textureData != null) {
			ArrayList<Tuple3> texCoords = obj.getTexCoords();
			if (texCoords != null && texCoords.size() > 0) {

				Log.i("Object3DBuilder", "Allocating/populating texture buffer...");
				FloatBuffer textureCoordsBuffer = createNativeByteBuffer(texCoords.size() * 2 * 4).asFloatBuffer();
				for (Tuple3 texCor : texCoords) {
					textureCoordsBuffer.put(texCor.getX());
					textureCoordsBuffer.put(obj.isFlipTextCoords() ? 1 - texCor.getY() : texCor.getY());
				}

				Log.i("Object3DBuilder", "Populating texture array buffer...");
				FloatBuffer textureCoordsArraysBuffer = createNativeByteBuffer(2 * faces.getVerticesReferencesCount() * 4).asFloatBuffer();
				obj.setTextureCoordsArrayBuffer(textureCoordsArraysBuffer);

				try {

					boolean anyTextureOk = false;
					String currentTexture = null;

					Log.i("Object3DBuilder", "Populating texture array buffer...");
					int counter = 0;
					for (int i = 0; i < faces.facesTexIdxs.size(); i++) {

						// get current texture
						if (!faceMats.isEmpty() && faceMats.findMaterial(i) != null) {
							Material mat = materials.getMaterial(faceMats.findMaterial(i));
							if (mat != null && mat.getTexture() != null) {
								currentTexture = mat.getTexture();
							}
						}

						// check if texture is ok (Because we only support 1 texture currently)
						boolean textureOk = false;
						if (currentTexture != null && currentTexture.equals(texture)) {
							textureOk = true;
						}

						// populate texture coords if ok
						int[] text = faces.facesTexIdxs.get(i);
						for (int j = 0; j < text.length; j++) {
							if (textureOk) {
								anyTextureOk = true;
								textureCoordsArraysBuffer.put(counter++, textureCoordsBuffer.get(text[j] * 2));
								textureCoordsArraysBuffer.put(counter++, textureCoordsBuffer.get(text[j] * 2 + 1));
							} else {
								textureCoordsArraysBuffer.put(counter++, 0f);
								textureCoordsArraysBuffer.put(counter++, 0f);
							}
						}
					}

					if (!anyTextureOk) {
						Log.i("Object3DBuilder", "Texture is wrong. Applying global texture");
						counter = 0;
						for (int j=0; j<faces.facesTexIdxs.size(); j++) {
							int[] text = faces.facesTexIdxs.get(j);
							for (int i = 0; i < text.length; i++) {
								textureCoordsArraysBuffer.put(counter++, textureCoordsBuffer.get(text[i] * 2));
								textureCoordsArraysBuffer.put(counter++, textureCoordsBuffer.get(text[i] * 2 + 1));
							}
						}
					}
				} catch (Exception ex) {
					Log.e("Object3DBuilder", "Failure to load texture coordinates", ex);
				}
			}
		}
		obj.setTextureData(textureData);

		return obj;
	}

	public Object3D getBoundingBoxDrawer() {
		return object3dv2;
	}

	public Object3D getFaceNormalsDrawer() {
		return object3dv1;
	}

	public Object3D getPointDrawer() {
		if (object3dv0 == null) {
			object3dv0 = new Object3DV0();
		}
		return object3dv0;
	}

	public static Object3DData buildBoundingBox(Object3DData obj) {
		BoundingBox boundingBox = new BoundingBox(
				obj.getVertexArrayBuffer() != null ? obj.getVertexArrayBuffer() : obj.getVertexBuffer(),
				obj.getColor());
		return new Object3DData(boundingBox.getVertices()).setDrawModeList(boundingBox.getDrawModeList())
				.setVertexColorsArrayBuffer(boundingBox.getColors()).setDrawOrder(boundingBox.getDrawOrder())
				.setDrawMode(boundingBox.getDrawMode())
				.setPosition(obj.getPosition()).setRotation(obj.getRotation()).setScale(obj.getScale())
				.setColor(obj.getColor()).setId(obj.getId() + "_boundingBox");
	}

	/**
	 * Builds a wireframe of the model by drawing all lines (3) of the triangles. This method uses
	 * the drawOrder buffer.
	 * @param objData the 3d model
	 * @return the 3d wireframe
	 */
	public static Object3DData buildWireframe(Object3DData objData) {

		if (objData.getDrawOrder() != null) {

			try {
				Log.i("Object3DBuilder", "Building wireframe...");
				IntBuffer drawBuffer = objData.getDrawOrder();
				IntBuffer wireframeDrawOrder = createNativeByteBuffer(drawBuffer.capacity() * 2 * 4).asIntBuffer();
				for (int i = 0; i < drawBuffer.capacity(); i += 3) {
					int v0 = drawBuffer.get(i);
					int v1 = drawBuffer.get(i + 1);
					int v2 = drawBuffer.get(i + 2);
					if (objData.isDrawUsingArrays()) {
						v0 = i;
						v1 = i + 1;
						v2 = i + 2;
					}
					wireframeDrawOrder.put(v0);
					wireframeDrawOrder.put(v1);
					wireframeDrawOrder.put(v1);
					wireframeDrawOrder.put(v2);
					wireframeDrawOrder.put(v2);
					wireframeDrawOrder.put(v0);
				}
				return new Object3DData(objData.getVertexArrayBuffer()).setVertexBuffer(objData.getVertexBuffer()).setDrawOrder(wireframeDrawOrder).
						setVertexNormalsArrayBuffer(objData.getVertexNormalsArrayBuffer()).setColor(objData.getColor())
						.setVertexColorsArrayBuffer(objData.getVertexColorsArrayBuffer()).setTextureCoordsArrayBuffer(objData.getTextureCoordsArrayBuffer())
						.setPosition(objData.getPosition()).setRotation(objData.getRotation()).setScale(objData.getScale())
						.setDrawMode(GLES20.GL_LINES).setDrawUsingArrays(false);
			} catch (Exception ex) {
				Log.e("Object3DBuilder", ex.getMessage(), ex);
			}
		}
		else if (objData.getVertexArrayBuffer() != null){
			Log.i("Object3DBuilder", "Building wireframe...");
			FloatBuffer vertexBuffer = objData.getVertexArrayBuffer();
			IntBuffer wireframeDrawOrder = createNativeByteBuffer(vertexBuffer.capacity()/3 * 2 * 4).asIntBuffer();
			for (int i = 0; i < vertexBuffer.capacity()/3; i += 3) {
				wireframeDrawOrder.put(i);
				wireframeDrawOrder.put(i+1);
				wireframeDrawOrder.put(i+1);
				wireframeDrawOrder.put(i+2);
				wireframeDrawOrder.put(i+2);
				wireframeDrawOrder.put(i);
			}
			return new Object3DData(objData.getVertexArrayBuffer()).setVertexBuffer(objData.getVertexBuffer()).setDrawOrder(wireframeDrawOrder).
					setVertexNormalsArrayBuffer(objData.getVertexNormalsArrayBuffer()).setColor(objData.getColor())
					.setVertexColorsArrayBuffer(objData.getVertexColorsArrayBuffer()).setTextureCoordsArrayBuffer(objData.getTextureCoordsArrayBuffer())
					.setPosition(objData.getPosition()).setRotation(objData.getRotation()).setScale(objData.getScale())
					.setDrawMode(GLES20.GL_LINES).setDrawUsingArrays(false);
		}
		return objData;
	}

	/**
	 * Generate a new object that contains all the line normals for all the faces for the specified object
	 * <p>
	 * TODO: This only works for objects made of triangles. Make it useful for any kind of polygonal face
	 *
	 * @param obj the object to which we calculate the normals.
	 * @return the model with all the normal lines
	 */
	public static Object3DData buildFaceNormals(Object3DData obj) {
		if (obj.getDrawMode() != GLES20.GL_TRIANGLES) {
			return null;
		}

		FloatBuffer vertexBuffer = obj.getVertexArrayBuffer() != null ? obj.getVertexArrayBuffer()
				: obj.getVertexBuffer();
		if (vertexBuffer == null) {
			Log.v("Builder", "Generating face normals for '" + obj.getId() + "' I found that there is no vertex data");
			return null;
		}

		FloatBuffer normalsLines;
		IntBuffer drawBuffer = obj.getDrawOrder();
		if (drawBuffer != null) {
			Log.v("Builder", "Generating face normals for '" + obj.getId() + "' using indices...");
			int size = /* 2 points */ 2 * 3 * /* 3 points per face */ (drawBuffer.capacity() / 3)
					* /* bytes per float */4;
			normalsLines = createNativeByteBuffer(size).asFloatBuffer();
			drawBuffer.position(0);
			for (int i = 0; i < drawBuffer.capacity(); i += 3) {
				int v1 = drawBuffer.get() * COORDS_PER_VERTEX;
				int v2 = drawBuffer.get() * COORDS_PER_VERTEX;
				int v3 = drawBuffer.get() * COORDS_PER_VERTEX;
				float[][] normalLine = Math3DUtils.calculateFaceNormal(
						new float[]{vertexBuffer.get(v1), vertexBuffer.get(v1 + 1), vertexBuffer.get(v1 + 2)},
						new float[]{vertexBuffer.get(v2), vertexBuffer.get(v2 + 1), vertexBuffer.get(v2 + 2)},
						new float[]{vertexBuffer.get(v3), vertexBuffer.get(v3 + 1), vertexBuffer.get(v3 + 2)});
				normalsLines.put(normalLine[0]).put(normalLine[1]);
			}
		} else {
			if (vertexBuffer.capacity() % (/* COORDS_PER_VERTEX */3 * /* VERTEX_PER_FACE */ 3) != 0) {
				// something in the data is wrong
				Log.v("Builder", "Generating face normals for '" + obj.getId()
						+ "' I found that vertices are not multiple of 9 (3*3): " + vertexBuffer.capacity());
				return null;
			}

			Log.v("Builder", "Generating face normals for '" + obj.getId() + "'...");
			normalsLines = createNativeByteBuffer(6 * vertexBuffer.capacity() / 9 * 4).asFloatBuffer();
			vertexBuffer.position(0);
			for (int i = 0; i < vertexBuffer.capacity() / /* COORDS_PER_VERTEX */ 3 / /* VERTEX_PER_FACE */3; i++) {
				float[][] normalLine = Math3DUtils.calculateFaceNormal(
						new float[]{vertexBuffer.get(), vertexBuffer.get(), vertexBuffer.get()},
						new float[]{vertexBuffer.get(), vertexBuffer.get(), vertexBuffer.get()},
						new float[]{vertexBuffer.get(), vertexBuffer.get(), vertexBuffer.get()});
				normalsLines.put(normalLine[0]).put(normalLine[1]);

				// debug
				@SuppressWarnings("unused")
				String normal = new StringBuilder().append(normalLine[0][0]).append(",").append(normalLine[0][1])
						.append(",").append(normalLine[0][2]).append("-").append(normalLine[1][0]).append(",")
						.append(normalLine[1][1]).append(",").append(normalLine[1][2]).toString();
				// Log.v("Builder", "fNormal[" + i + "]:(" + normal + ")");
			}
		}

		return new Object3DData(normalsLines).setDrawMode(GLES20.GL_LINES).setColor(obj.getColor())
				.setPosition(obj.getPosition()).setVersion(1);
	}

	private static ByteBuffer createNativeByteBuffer(int length) {
		// initialize vertex byte buffer for shape coordinates
		ByteBuffer bb = ByteBuffer.allocateDirect(length);
		// use the device hardware's native byte order
		bb.order(ByteOrder.nativeOrder());
		return bb;
	}

	public static void loadV6AsyncParallel(final Activity parent, final URL url, final File file, final String assetsDir, final String assetName,
										   final Callback callback) {

		final String modelId = file != null ? file.getName() : assetName;

		Log.i("Object3DBuilder", "Loading model " + modelId + ". async and parallel..");
		loadV6AsyncParallel_Obj(parent, file, assetsDir, assetName, callback);
	}



	private static void loadV6AsyncParallel_Obj(final Activity parent, final File file, final String assetsDir, final String assetName,
												final Callback callback) {

		String modelId = file != null ? file.getName() : assetName;
		final File currentDir = file != null ? file.getParentFile() : null;

		Log.i("Object3DBuilder", "Loading model "+modelId+". async and parallel..");
		WavefrontLoader2.loadAsync(parent, null, currentDir, assetsDir, modelId, callback);
	}
}


class BoundingBox {

	public FloatBuffer vertices;
	private FloatBuffer colors;
	private IntBuffer drawOrder;
	/**
	 * Build a bounding box for the specified 3D object vertex buffer.
	 *
	 * @param vertexBuffer the 3D object vertex buffer
	 * @param color        the color of the bounding box
	 */
	BoundingBox(FloatBuffer vertexBuffer, float[] color) {

	}

	BoundingBox(String id, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
	}

	IntBuffer getDrawOrder() {
		return drawOrder;
	}

	FloatBuffer getColors() {
		return colors;
	}

	int getDrawMode() {
		return GLES20.GL_LINE_LOOP;
	}

	List<int[]> getDrawModeList() {
		List<int[]> ret = new ArrayList<>();
		int drawOrderPos = 0;
		for (int i = 0; i < drawOrder.capacity(); i += 4) {
			ret.add(new int[]{GLES20.GL_LINE_LOOP, drawOrderPos, 4});
			drawOrderPos += 4;
		}
		return ret;
	}

	public FloatBuffer getVertices() {
		return vertices;
	}

}