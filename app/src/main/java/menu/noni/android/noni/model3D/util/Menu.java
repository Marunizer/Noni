package menu.noni.android.noni.model3D.util;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by mende on 12/12/2017.
 */

public class Menu {

    private String key;

    public String getRestName() {
        return restName;
    }

    public void setRestName(String restName) {
        this.restName = restName;
    }

    private String restName;
    //public ArrayList<Categories> allCategories = new ArrayList<>();
    public Hashtable<String, Categories> allCategories = new Hashtable<>();

    public Menu(String key) {
        this.key = key;
    }


    public static class Categories
    {

        public String getCategoryIconName() {
            return categoryIconName;
        }

        public void setCategoryIconName(String categoryIconName) {
            this.categoryIconName = categoryIconName;
        }

        private String name;
        private String categoryIconName;
       // public ArrayList<MenuItem> allItems = new ArrayList<>();
        public Hashtable<String, MenuItem> allItems = new Hashtable<>();
        public ArrayList<String> keyConverter = new ArrayList<>();

        public Categories(String name)
        {
            this.name = name;
        }


        public static class MenuItem {

            String id; //somewhat redundant
            String name; // The name WILL be the Key
            String drcPath; //Have for now but I do not think we will be storing draco name to be reused, it's a hit it and quit it
            String objPath;
            String mtlPath;
            String jpgPath;
            String iconPath;
            String description;
            //Maybe remove one, I don't think the float is necessary
            float price;
            String cost;

            //Not being used effectively yet
            int downloadChecker;

            public MenuItem(String id, String drcPath, String mtlPath, String jpgPath,
                            String iconPath, String description, String cost) {
                this.id = id;
                this.name = id;
                this.drcPath = drcPath;
                this.objPath = drcPath.substring(0, drcPath.length() - 4).concat(".obj");
                this.mtlPath = mtlPath;
                this.jpgPath = jpgPath;
                this.iconPath = iconPath;
                this.description = description;
                this.cost = cost;
                this.downloadChecker = 0;
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
                // return splitCamelCase(name);
            }

            public String getBucketPath() {

                if (isOneWord(name))
                    return name;
                else
                    return convertToCamelCase(name);
            }

            public static boolean isOneWord(String s) {
                return (s.length() > 0 && s.split("\\s+").length == 1);
            }

            public String convertToCamelCase(String text) {
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

            public String getDescription()
            {
                return description;
            }

            public float getPrice()
            {
                return price;
            }

            public String getCost()
            {
                return cost;
            }

            static String splitCamelCase(String s) {
                return s.replaceAll(
                        String.format("%s|%s|%s",
                                "(?<=[A-Z])(?=[A-Z][a-z])",
                                "(?<=[^A-Z])(?=[A-Z])",
                                "(?<=[A-Za-z])(?=[^A-Za-z])"
                        ),
                        " "
                );
            }

            public int getDownloadChecker() {
                return downloadChecker;
            }

            public void incrementDownloadChecker() {
                this.downloadChecker++;
            }

        }

    }
}
