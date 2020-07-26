# Journals

A demo social media application for Android written in Java.

## Description

This repository contains a full-featured social media app where users create and upload travel journals, each containing a number of images with very brief descriptions referred to as "spots."  The demonstration provides coding examples for:

* Activity and Fragment Lifecycle
* Running services in the background
* Custom user account creation and management using Android Account Manager and Facebook APIs.
* Networking through backend API calls using JSON objects
* SQLite database setup and access through custom Content Provider
* Numerous examples of scrolling lists and data adapters
* Image / media playback and temporary caching to local storage
* Using location services and displaying locations on a map
* Backup sync to cloud storage
* Android Material Design user interface

## Getting Started

### Dependencies

This project was built using Android Studio.  Download the full installer from https://developer.android.com/studio.

With the exception of the AVD Manager, Android Studio either automatically downloads or displays click-through prompts for most of the dependencies listed below when building and running the project.  They are included mostly for troubleshooting purposes.

The SDK Manager included with the installer is required to download the SDK platform and source code for the different supported versions of Android OS, as well as build tools for compiling the application and performing error checking.  In addition, this application makes heavy use of the Jetpack (AndroidX) and Google Play libraries, which are downloaded separately.

The AVD Manager included with Android Studio (Tools -> AVD Manager) is required to create an emulator to run the application if you don't have a standalone Android device.  To increase the speed of the emulator on x86 devices, downloading the "Intel x86 Emulator Accelerator (HAXM)" from the SDK Manager is strongly recommended.  In order to use HAXM for the first time, you may be required to make changes to your BIOS.  The HAXM installer will notify you of this.

When choosing a system image for the emulator, I also recommend using a "Google Play" version, because it allows you to use common Google APIs like Maps, etc.

This application is dependent on a standalone Android library called Volley for its networking operations.  Information on Volley can be found at https://developer.android.com/training/volley.

The following software versions were used to build this project:

* Android Studio: 4.0.1
* Android SDK Platform-tools: 30.0.3
* Android SDK Build-tools: 30.0.1
* Android SDK: Application currently supports any version between Android 4.4 (API 19) and Android R (API 30).

### Executing program

* After opening Android Studio, click "Open an existing Android Studio project" and select the top-level project folder.
* Click Build -> Make Project.  Android Studio should prompt and download software dependencies.
* Click Tools -> AVD Manager.  Follow the prompts to create an emulator.
* Click Run -> Run 'App'.  Android studio will launch the emulator and install the app.

## Help

The app doesn't seem to do much.

* Unfortunately the backend server this app interacted with no longer exists, and without being able to create a new account there is limited funtionality.  All of the API calls still exist within the source code in order to examine and understand the network operations.

Some parts of the app don't seem finished.

* Some parts are more developed than others.  Elements like networking, account management, database operations and UI flow are the most developed.  Video playback and location mapping were not major requirements for the original demo and are not as far along.

## Authors

Wade Johnson
@wadejohnson1

## License

Parts of this project are licensed under the MIT and Apache 2.0 Licenses - see individual source files for specific license information.

## Acknowledgments

Some code is provided by The Android Open Source Project.  See individual source files for details.
