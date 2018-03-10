package menu.noni.android.noni.model3D.util;

import android.content.Context;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import menu.noni.android.noni.model3D.rendering.ObjectRenderer;

/**
 * Created by mende on 12/12/2017.
 *
 * Purpose of Class is to keep track of the Restaurant Menu that is split in Categories which contains the menu items
 * Menu(Category(MenuItem()))
 *
 * Concern: Currently all data is thrown away after no longer at the restaurant, could link
 * to restaurant class and keep track of menu there to save data, however it takes up space, and
 * its inexpensive to query, way we have it now free's space. Just a thought.
 */

public class Menu {

    public Menu(String key) {
        this.key = key;
    }

    //May not be needed since we don't care about the Menu when we are no longer at the restaurant
    private String key;
    private String restName;

    //Might wanna make private, just annoying to access
    public Hashtable<String, Categories> allCategories = new Hashtable<>();

    //Probably should have a bar or some text at top of model Viewer where we display the resturant name
    public String getRestName() {
        return restName;
    }

    public void setRestName(String restName) {
        this.restName = restName;
    }

    public static class Categories
    {

        public Categories(String name)
        {
            this.name = name;
        }

        //Will need to be used after I know how the category is selected
        private String name;
        private String categoryIconName;

        //keeps a numbered index of the items entered to hashtable, used as key converter
        //when working with adapters that return the position, indicating a menu item
        //These probz need to be private, just annoying to set up for now
        //May be able to remove keyConverter but seems to be useful in downloadAll()Scenario - Keep in mind -  remove if not needed
        public ArrayList<String> keyConverter = new ArrayList<>();
        public Hashtable<String, MenuItem> allItems = new Hashtable<>();

        public String getName() {
            return name;
        }

        public void setCategoryIconName(String categoryIconName) {
            this.categoryIconName = categoryIconName;
        }

        public static class MenuItem {

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

            ObjectRenderer modelAR;

            public MenuItem(String id, String drcPath, String mtlPath, String jpgPath,
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

            public String getDescription()
            {
                return description;
            }

            public String getCost()
            {
                return cost;
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

            public String getBucketPath() {

                //if name contains TwoCapitals, it is one word so return
                if (isOneWord(name))
                    return name;
                else
                    return convertToCamelCase(name);
            }

            //TODO: Should move to Utils
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

            //TODO: Should move to Utils
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

            //Possible method with the purpose to stall app until we the model is downloaded
            //A better solution would be to wait for a callback from the download itself
            //TODO: May no longer be needed: check and delete
            public boolean stallUntilDownloaded(Context cheatCode){

//                String objPath = cheatCode.getFilesDir().toString() + "/model/" + getObjPath();
//                String mtlPath = cheatCode.getFilesDir().toString() + "/model/" + getMtlPath();
//                String jpgPath = cheatCode.getFilesDir().toString() + "/model/" + getJpgPath();
//
//                File objFile = new File(objPath);
//                File mtlFile = new File(mtlPath);
//                File jpgFile = new File(jpgPath);
//
//
//                while(!objFile.exists() || !mtlFile.exists() || !jpgFile.exists() )
//                {
//                    try {
//                        TimeUnit.MINUTES.wait(1);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                //OR Like this, makes more sense lmao
                while(!isDownloaded())
                {
                    try {
                        TimeUnit.MINUTES.wait(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        }
    }
}
