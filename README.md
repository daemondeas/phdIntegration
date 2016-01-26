# phdIntegration
An integration of personal health devices (devices communicating with the IEEE 11073-20601 messaging protocol). It consists of an Android application that acts as a gateway and an OData web service written in C#. The project was developed as a proof-of-concept for a thesis.

The Android application is licensed under GNU GPL 2.

The Web service is licensed under the MIT license.

The Android application makes use of the Antidote library (https://github.com/signove/antidote), which is licensed under GNU LGPL 2.1. Two files have been modified (src/healthd_android.c and src/communication/plugin/android/plugin_android.c), to make the JNI exports match the package names of the Android application, therefore this slightly modified version of Antidote is also available in this repository.