package menu.noni.android.noni.model3D.util;

import java.util.concurrent.atomic.AtomicInteger;

import menu.noni.android.noni.model3D.rendering.ObjectRenderer;

/**
 * Created by Wiita on 3/9/2018.
 */

public class RestaurantMenuItem {

    private String name; // The name WILL be the Key or ID
    private String drcPath; //Have for now but I do not think we will be storing draco name to be reused, it's a hit it and quit it
    private String objPath;
    private String mtlPath;
    private String jpgPath;
    private String iconPath;
    private String description;
    private String cost;
    private AtomicInteger atomicDownloadCheck = new AtomicInteger(0);
    boolean isDownloaded = false;

    //Have we set up object using the factory
    boolean isFactoryReady = false;
    boolean isRendered = false;
    boolean isDrawn = false;

    private ObjectRenderer modelAR;

    public RestaurantMenuItem(String id, String drcPath, String mtlPath, String jpgPath,
                    String iconPath, String description, String cost) {
        this.name = id;
        this.drcPath = drcPath;
        this.objPath = drcPath.substring(0, drcPath.length() - 4).concat(".obj");
        this.mtlPath = mtlPath;
        this.jpgPath = jpgPath;
        this.iconPath = iconPath;
        this.description = description;
        this.cost = cost;
        this.isDownloaded = false;
        this.isFactoryReady = false;
        this.isDrawn = false;
    }

    public String getObjPath() {
        return objPath;
    }

    public String getMtlPath() {
        return mtlPath;
    }

    public String getJpgPath() {
        return jpgPath;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getName() {
        return name;
    }

    public String getBucketPath() {

        //if name contains TwoCapitals, it is one word so return
        if (isOneWord(name))
            return name;
        else
            return convertToCamelCase(name);
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean isDownloaded) {
        this.isDownloaded = isDownloaded;
    }

    public int getAtomicDownloadCheck() {
        return atomicDownloadCheck.get();
    }

    public void incrementAtomicDownloadCheck() {
        int temp = atomicDownloadCheck.incrementAndGet();
        this.atomicDownloadCheck = new AtomicInteger(temp);
    }

    public String getDescription()
    {
        return description;
    }


    public String getCost()
    {
        return cost;
    }

    public boolean isFactory() {
        return isFactoryReady;
    }

    public void setFactory(boolean factory) {
        isFactoryReady = factory;
    }

    public ObjectRenderer getModelAR() {
        return modelAR;
    }

    public void setModelAR(ObjectRenderer modelAR) {
        this.modelAR = modelAR;
    }

    public boolean isDrawn() {
        return isDrawn;
    }

    public void setDrawn(boolean drawn) {
        isDrawn = drawn;
    }




    private String convertToCamelCase(String text) {
        String result = "", result1 = "";
        for (int i = 0; i < text.length(); i++) {
            String next = text.substring(i, i + 1);
            if (i == 0) {
                result += next.toUpperCase();
            } else {
                result += next.toLowerCase();
            }
        }
        String[] splited = result.split("\\s+");
        String[] splited1 = new String[splited.length];

        for (int i = 0; i < splited.length; i++) {
            int l = splited[i].length();
            result1 = "";
            for (int j = 0; j < splited[i].length(); j++) {
                String next = splited[i].substring(j, j + 1);

                if (j == 0) {
                    result1 += next.toUpperCase();
                } else {
                    result1 += next.toLowerCase();
                }
            }
            splited1[i] = result1;
        }
        result = "";
        for (int i = 0; i < splited1.length; i++) {
            result += "" + splited1[i];
        }
        return result;
    }

    private static boolean isOneWord(String s) {
        return (s.length() > 0 && s.split("\\s+").length == 1);
    }


    //Unused for now, SplitsCamelCase --> Splits Camel Case
//            static String splitCamelCase(String s) {
//                return s.replaceAll(
//                        String.format("%s|%s|%s",
//                                "(?<=[A-Z])(?=[A-Z][a-z])",
//                                "(?<=[^A-Z])(?=[A-Z])",
//                                "(?<=[A-Za-z])(?=[^A-Za-z])"
//                        ),
//                        " "
//                );
//            }




}
