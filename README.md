# mobi-com-kit

Integrate messaging into your android app.

Note: Under progress, please visit http://mobicomkit.com/ to get notified for latest updates.

Project contains the following 3 modules:

i) mobicom - Code related to backend server interaction

ii) mobicomkitui - Sample client UI code

iii) mobicommons - Common utility framework


Clone the repository and add all the 3 modules inside your project.

Step 1: Register at http://mobicomkit.com/ to get the application key.

Step 2: In HttpRequestUtils.java set the value of APPLICATION_KEY_HEADER_VALUE = application key generated in Step 1

Step 3: Replace SQLiteOpenHelper with MobiComDatabaseHelper.
       and call from your starting activity, MobiComDatabaseHelper.init(this, DATABASE_NAME, DATABASE_VERSION);


Step 4: Add the following permissions in androidmanifest.xml:


    <uses-permission android:name="android.permission.READ_CONTACTS" />
    <uses-permission android:name="android.permission.WRITE_CONTACTS" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.CALL_PHONE" />
    <uses-permission android:name="android.permission.READ_PROFILE" />

Step 5: Add the following services in androidmanifest.xml

        <service
            android:name="com.mobicomkit.client.ui.message.MessageIntentService"
            android:exported="false" />
        <service
            android:name="com.mobicomkit.client.ui.message.conversation.ConversationLoadingIntentService"
            android:exported="false" />
            
            
Step 6: Add the following activities in androidmanifest.xml
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


Step 7: Login or create user account: 
new RegisterUserClientService(activity).createAccount(<USER_EMAIL>, <USER_PHONE_NUMBER>); 
If it is a new user, new user account will get created else existing user will be logged in to the application.

Step 8: Start SlidingPaneActivity from your app activity to open messaging interface.

Intent intent = new Intent(this, SlidingPaneActivity.class);

startActivity(intent);

