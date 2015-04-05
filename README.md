# mobi-com-kit

Integrate messaging into your android app.

Note: Under progress, please visit http://mobicomkit.com/ to get notified for latest updates.

Project contains the following 3 modules:

i) mobicom - Code related to backend server interaction

ii) mobicomkitui - Sample client UI code

iii) mobicommons - Common utility framework


Clone the repository and import all the 3 modules in your project.

Add the following in build.gradle file:

    compile project(':mobicommons')
    compile project(':mobicom')
    compile project(':mobicomkitui')

Step 1: Register at http://mobicomkit.com/ to get the application key.
       Goto HttpRequestUtils.java set the value of APPLICATION_KEY_HEADER_VALUE = application key

Step 2: Replace SQLiteOpenHelper with MobiComDatabaseHelper.
       and call from your starting activity, MobiComDatabaseHelper.init(this, DATABASE_NAME, DATABASE_VERSION);

Step 3: Addition to androidmanifest.xml:

       Permissions:
                    <uses-permission android:name="android.permission.READ_CONTACTS" />
                    <uses-permission android:name="android.permission.WRITE_CONTACTS" />
                    <uses-permission android:name="android.permission.VIBRATE" />
                    <uses-permission android:name="android.permission.CALL_PHONE" />
                    <uses-permission android:name="android.permission.READ_PROFILE" />
                    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
                    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
                    <uses-permission android:name="android.permission.INTERNET" />


       Services:
               <service
                   android:name="com.mobicomkit.client.ui.MessageIntentService"
                   android:exported="false" />

       
       Activities:
       
              <activity
                   android:name="com.mobicomkit.client.ui.activity.SlidingPaneActivity"
                   android:configChanges="keyboardHidden|orientation|screenSize"
                   android:label="@string/app_name"
                   android:parentActivityName="<APP_PARENT_ACTIVITY>"
                   android:theme="@style/MobiComAppBaseTheme" >
                   <!-- Parent activity meta-data to support API level 7+ -->
                   <meta-data
                       android:name="android.support.PARENT_ACTIVITY"
                       android:value="<APP_PARENT_ACTIVITY>" />
                   <intent-filter>
                       <action android:name="android.intent.action.SEND" />
                       <category android:name="android.intent.category.DEFAULT" />
                       <data android:mimeType="text/plain" />
                   </intent-filter>
                   <intent-filter>
                       <action android:name="android.intent.action.SEND" />
                       <action android:name="android.intent.action.SENDTO" />
                       <category android:name="android.intent.category.DEFAULT" />
                       <category android:name="android.intent.category.BROWSABLE" />
                   </intent-filter>
              </activity>
               
              <activity
                   android:name="net.mobitexter.mobiframework.people.activity.PeopleActivity"
                   android:configChanges="keyboardHidden|orientation|screenSize"
                   android:label="@string/activity_contacts_list"
                   android:parentActivityName="com.mobicomkit.client.ui.activity.SlidingPaneActivity"
                   android:theme="@style/ContactTheme"
                   android:windowSoftInputMode="adjustResize">
                   <meta-data
                       android:name="android.support.PARENT_ACTIVITY"
                       android:value="com.mobicomkit.client.ui.activity.SlidingPaneActivity" />
                   <intent-filter>
                       <action android:name="android.intent.action.SEARCH" />
                   </intent-filter>
                   <meta-data
                       android:name="android.app.searchable"
                       android:resource="@xml/searchable_contacts" />
              </activity>

Replace <APP_PARENT_ACTIVITY> with your app's parent activity.

Step 4: Register user account (using an AsyncTask or thread): 

new RegisterUserClientService(activity).createAccount(USER_EMAIL, USER_PHONE_NUMBER, GCM_REGISTRATION_ID); 

If it is a new user, new user account will get created else existing user will be logged in to the application.


Step 5: Start SlidingPaneActivity from your app activity to open messaging interface.

Intent intent = new Intent(this, SlidingPaneActivity.class);
startActivity(intent);

