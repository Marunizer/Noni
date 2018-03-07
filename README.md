# Noni
This Android application is intended to be used as an Augmented Reality Menu.
# Set Up Instructions Part 1
Before Building, you have two options:

Option 1: Download the Draco library and include it's location on your computer
within the CmakeLists.txt like so


add_subdirectory(PATH/TO/DRACO
                 ${CMAKE_BINARY_DIR}/draco_build)

include_directories("${CMAKE_BINARY_DIR}" PATH/TO/DRACO)

I do not recommend this option as building Draco and including the NDK is a painful
painful process and I cannot guarantee the Current Draco Library will build appropriately as the old one I have works

Option 2: (SAFE OPTION) - In the build.gradle, comment out a block of code

    cmake {
            path "CMakeLists.txt"
        }
    }

    ^^ Hit that with the nice ctrl + '/'

I believe this should prevent Android Studio from trying to do anything related to C++
HOWEVER, if this option is used, Will have to comment out a bunch of code related to the JNI in ModelActivity.java

# Set Up Instructions Part 2
You can bet your butt you will probably need to wait hours for Android Studio to download and install
everything required for this app. Be warned.

It's possible one library may not be included properly and will require special instructions for this guy : 'com.joooonho:selectableroundedimageview:1.0.1'

There may be things to google for the NDK / Google Play Store if Android Studio doesn't do it automatically

-------------------------------------

FIREBASE

Once, done with all of the above, the app will still not work and you will have an error with Glide in RestaurantViewActivity.java
until Firebase is properly integrated with Android Studio.

I don't know how Farza want's to do this, if he wants to give more people special permissions to the database/Storage.
If not, You can log in to firebase through my account and use Noni.

IMPORTANT
- Do not ever *write* to the Database or Storage, reading allowed

After Firebase is set up, The app should be functional except you cannot test AR
unless you either follow special instructions provided by google ARCore on setting up
an AR capable emulator, OR if you have a physical phone that supports ARCore

# Bugs and Tasks:
- Models are not dynamically scaled in AR view. (High Priority)

- AR not capable of rendering files with no smooth groups or normal vectors
(Low Priority) - can just make files differently

- Geocoder in LocationHelper.java sometimes times out and stops app completely
(Low Priority) - I believe this is only an issue for emulators. Reset Emulator

- I'm not certain if I ask for permission to use external storage orrr if I just activate it manually
(Low Priority) - For testing, adjusting manually is enough

- Set up a proper rendering system for AR view to have all models rendered
before user clicks on model without disrupting the main UI thread

- Set up a proper model downloading managing system that takes into account all cases
Probz a good idea to reference Farza downloading and rendering as well

- Implement Draco decompression:
Looks like C++ might not be able to access internal storage, may need to write draco and obj file to external storage


# What API's or librarys are used
Google Maps

Firebase realtime database / storage  / geoFire

Glide

OpenGL ES 2.0 - with the help of : https://github.com/andresoviedo/android-3D-model-viewer

JavaGL                           : https://github.com/javagl/Obj

Google ARCore                    : https://github.com/google-ar/arcore-android-sdk

Draco                            : https://github.com/google/draco

Material Library(s):
https://github.com/pungrue26/SelectableRoundedImageView

AND more, will add later...