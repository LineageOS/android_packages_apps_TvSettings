# TvSettings Design Document
## Important 
This information represents a snapshot in time and is subject to change. Always read code and resources rather than taking this as final source of truth. Use document to direct code searches.

## Overview
TvSettings is the central settings application for Android TV. It provides a user interface for configuring system preferences, network connections, accounts, and device-specific settings. The application is designed with a "Two Panel" layout to optimize for the 10-foot experience, allowing users to navigate categories on the left while viewing details or making adjustments on the right.

## Architecture

### Core Activity
The entry point for the application is primarily `MainSettings` (defined in `Settings/AndroidManifest.xml`). It hosts the main fragment hierarchy.
- **`MainSettings`**: The main activity that handles the initial launch and hosts the `TwoPanelSettingsFragment`.

### TwoPanel UI
The UI is built around the `TwoPanelSettingsFragment` (located in `TwoPanelSettingsLib`), which manages two side-by-side fragments:
1.  **Preference Panel (Left)**: Displays a list of settings categories or items.
2.  **Preview/Detail Panel (Right)**: Displays details, sub-settings, or a preview of the selected item.

**Key Class:** `com.android.tv.twopanelsettings.TwoPanelSettingsFragment`
- Handles focus management between panels.
- Manages fragment transactions for navigating deeper into settings.
- Supports "Slices" for dynamic content.

### Fragment Hierarchy
- **`LeanbackPreferenceFragmentCompat`**: The base fragment for most settings screens, leveraging the Leanback library for TV-optimized lists.
- **`SettingsPreferenceFragment`**: Extends the Leanback fragment to add specific TvSettings logic.

## Directory Structure

### `packages/apps/TvSettings`
- **`Settings/src`**: Contains the core Java/Kotlin source code for the application.
    - `com.android.tv.settings`: Main package.
    - `com.android.tv.settings.connectivity`: Network and Wi-Fi logic.
    - `com.android.tv.settings.system`: System settings (Date & Time, Language, etc.).
- **`Settings/res/xml`**: Defines the structure of settings screens using standard Android Preference XMLs.
    - `main_prefs.xml`: The top-level settings menu.
    - `display_sound.xml`, `network.xml`, etc.: Sub-menus.
- **`TwoPanelSettingsLib`**: Library containing the split-screen UI components.
    - `com.android.tv.twopanelsettings`: Core logic for the two-panel layout.
- **`SettingsAPI`**: Interfaces and shared code for interacting with TvSettings.

### `vendor/google_atv/apps/TvSettingsGoogle`
- Contains Google-specific customizations and overlays.
- **`res/values`**: Resource overlays to customize strings, colors, and configurations for Google TV devices.
- **`res/xml`**: Overlays for preference screens to add or modify settings items (e.g., `main_prefs_vendor.xml`).

## Key Components

### Connectivity
The connectivity stack manages Wi-Fi and Ethernet connections.
- **Location**: `Settings/src/com/android/tv/settings/connectivity`
- **Key Classes**: `ConnectivityListener`, `NetworkFragment`, `WifiConnectionActivity`.

### Wi-Fi State Machine
The Wi-Fi connection flow in TvSettings is managed by a dedicated state machine to handle the complex logic of scanning, authentication, and error handling.

#### Architecture
- **`WifiConnectionActivity`**: The entry point and orchestrator. It initializes the `StateMachine`, registers all possible states, and defines the transitions between them.
- **`StateMachine`**: A utility class that manages the current state and handles transitions based on events (e.g., `CONNECT`, `PASSWORD`, `RESULT_SUCCESS`).
- **`State` Interface**: Each step in the flow implements the `State` interface, providing a `Fragment` to display and handling forward/backward navigation.

#### Key States
1.  **`AddStartState`**: The initial state for adding a new network.
2.  **`KnownNetworkState`**: Handles connecting to a network that is already saved.
3.  **`EnterPasswordState`**: Prompts the user for a password if the network is secured.
4.  **`ConnectState`**: Performs the actual connection attempt. It uses `ConnectToWifiFragment` to interact with `WifiManager` and `ConnectivityManager`.
5.  **`ConnectFailedState`**: Displays error messages if the connection fails (e.g., wrong password, timeout).
6.  **`SuccessState`**: Displays a success message before finishing the activity.

#### Example Flow (New Secured Network)
1.  `AddStartState` -> User selects network -> Event: `PASSWORD`
2.  `EnterPasswordState` -> User enters password -> Event: `OPTIONS_OR_CONNECT`
3.  `OptionsOrConnectState` -> User clicks Connect -> Event: `CONNECT`
4.  `ConnectState` -> Connection successful -> Event: `RESULT_SUCCESS`
5.  `SuccessState` -> Activity finishes.
    - `ConnectivityListener`: Listens for network changes and updates the UI.
    - `NetworkFragment`: Displays the list of available networks.
    - `WifiConfigHelper`: Helper for managing Wi-Fi configurations.
    - `NetworkChangeStateManager`: Tracks network state changes.

### Slices
TvSettings uses Android Slices to allow other apps or system components to inject settings UI or to display dynamic content.
- **Implementation**: `TwoPanelSettingsLib` has support for rendering Slices within the settings hierarchy.
- **Usage**: Used for features that need to be updated independently of the main settings app or provided by external modules.
- **Key Classes**: `SliceFragment`, `SlicePreference`.

### Slices and Customization
TvSettings provides a flexible architecture for integrating dynamic content and external settings via Android Slices. This allows for remote updates to settings UI and modularization of features.

#### Core Components
- **`SliceFragment`**: A `SettingsPreferenceFragment` designed to render a full screen of settings driven by a Slice URI.
    - **Usage**: It delegates most of its logic to `SliceShard`.
    - **Location**: `TwoPanelSettingsLib/src/com/android/tv/twopanelsettings/slices/SliceFragment.kt`
- **`SliceShard`**: The "brain" behind Slice integration. It handles the connection to the SliceProvider, observes changes, and maps `SliceItem` objects to Android X Preferences.
    - **Functionality**:
        - Binds to a Slice URI.
        - Converts Slice rows into `Preference`, `SlicePreference`, or `SliceSwitchPreference`.
        - Handles navigation and actions defined in the Slice.
    - **Customization**: Can be attached to any `LeanbackPreferenceFragmentCompat` (like `MainFragment`) to inject Slice content into a standard fragment.
- **`SlicePreference`**: A custom Preference class used to render a Slice item. It supports standard Slice actions and UI updates.
- **`EmbeddedSlicePreference`**: A specialized `SlicePreference` that manages its own Slice binding.
    - **Use Case**: Useful for inserting a single dynamic preference (like a toggle) into a static list without converting the entire screen to a Slice.
    - **Mechanism**: Uses `EmbeddedSlicePreferenceHelper` to observe a specific Slice URI for just that preference.

#### Customization with `SliceShard`
`SliceShard` allows existing fragments to be "powered" by Slices or to mix static and dynamic content.

**Example: `MainFragment`**
`MainFragment` demonstrates how to use `SliceShard` to potentially replace the entire main menu with a Slice-based implementation, while falling back to XML resources if the Slice is unavailable.

1.  **Initialization**: In `onCreatePreferences`, it checks if a valid Slice URI exists for the main screen.
2.  **Attachment**: If valid, it creates a `SliceShard` instance, passing itself (`this`) as the callback.
3.  **Hybrid Mode**: It can also show specific Slice preferences alongside static ones (e.g., `KEY_DISPLAY_AND_SOUND_SLICE`).

```kotlin
// Example from MainFragment.kt
if (!SliceUtils.isSliceProviderValid(requireContext(), sliceUri)) {
    // Fallback to XML
    setPreferencesFromResource(preferenceScreenResId, null)
    configurePreferences()
} else {
    // Use SliceShard to drive the UI
    mSliceShard = SliceShard(
        this, sliceUri, this,
        getString(R.string.settings_app_name), prefContext, true
    )
}
```

### Resource Overlays
The application relies heavily on resource overlays (RROs) to allow OEMs (and Google) to customize the settings without changing the core code.
- **Mechanism**: `Settings/res/xml` defines the base structure. Vendor overlays in `vendor/.../res/xml` can replace these files or merge with them to add proprietary settings.

- **Mechanism**: `Settings/res/xml` defines the base structure. Vendor overlays in `vendor/.../res/xml` can replace these files or merge with them to add proprietary settings.

### Dialogs and Info Fragments
TvSettings includes specialized fragments for displaying information and full-screen dialogs, often used within the TwoPanel UI or as standalone interactions.

#### `InfoFragment`
Used to display static or dynamic information, typically in the right-hand panel of the split-screen view or as a preview for a Slice.
- **Location**: `TwoPanelSettingsLib/src/com/android/tv/twopanelsettings/slices/InfoFragment.kt`
- **Layout**: `R.layout.info_fragment`
- **Key View IDs**:
    - `info_image`: Large header image or icon.
    - `info_title`: Main title of the info screen.
    - `info_title_icon`: Small icon displayed next to the title.
    - `info_status`: Status text (e.g., "On", "Off"), often color-coded.
    - `info_summary`: Detailed description or summary text.
- **Subclassing**: Subclasses often override `onCreateView` to populate specific text or handle unique logic.
    - **Example**: `FactoryResetInfoFragment` sets a specific warning message for the factory reset screen.

#### `FullScreenDialogFragment`
A standardized full-screen dialog used for confirmations, alerts, or simple flows. It supports a title, message, icon, and up to two buttons.
- **Location**: `TwoPanelSettingsLib/src/com/android/tv/twopanelsettings/FullScreenDialogFragment.java`
- **Layout**: `R.layout.tp_full_screen_dialog`
- **Usage**: Constructed using `DialogBuilder` to set arguments like title, message, and button labels.
- **Key View IDs**:
    - `dialog_icon`: Icon displayed at the top.
    - `dialog_title`: Dialog title.
    - `dialog_message`: Main content text.
    - `custom_view_container`: `FrameLayout` for injecting custom views below the message.
    - `positive_button`: Primary action button (returns `ACTION_POSITIVE`).
    - `negative_button`: Secondary action button (returns `ACTION_NEGATIVE`).

## Design Guidelines for Contributors
- **Adding a New Setting**:
    1.  Define the preference in `Settings/res/xml` (or a new XML file).
    2.  Create a Fragment to handle the logic (extending `SettingsPreferenceFragment`).
    3.  Register the fragment in `Settings/AndroidManifest.xml` if it needs to be a standalone activity, or link it from the preference XML.
- **Modifying Existing Settings**:
    - Check `vendor` directories for potential overlays that might override your changes on specific devices.

## Addendum: Component Implementation Breakdown

### Internal Components (TvSettings Proper)
These components are implemented directly within the `TvSettings` package and its libraries.

#### Core Activities
- **`MainSettings`**: The primary entry point for the settings app.
- **`NetworkActivity`** (and `WifiConnectionActivity`): Handles all Wi-Fi and Ethernet connectivity logic.
- **`AppsActivity` / `AllAppsActivity`**: Manages installed applications, permissions, and special access.
- **`DisplaySoundActivity`**: Container for display and sound settings (though some parts are Slices).
- **`StorageResetActivity`**: Manages storage and factory reset flows.
- **`SecurityActivity`**: Handles restricted profiles and security settings.

#### Key Fragments (XML-defined)
These fragments are referenced directly in `res/xml` files (e.g., `main_prefs.xml`, `display_sound.xml`).
- **Connectivity**: `NetworkFragment`
- **Accounts**: `AccountsFragment` (also has a Slice variant)
- **Device**: `DevicePrefFragment`, `PrivacyFragment`, `AccessibilityFragment`
- **Display & Sound**:
    - `DisplaySoundFragment`
    - `ResolutionSelectionFragment`
    - `AdvancedDisplayFragment`
    - `AdvancedVolumeFragment`
- **Apps**: `AppsFragment`, `AppManagementFragment`
- **Accessories**: `AccessoriesFragment`

### Slices and External Callouts
These settings are often delegated to external modules (like `TvSettingsTwoPanel` or vendor implementations) via Android Slices or Intents.

#### Slice Integrations
- **Accounts**: `AccountsSliceFragment` (often replaces the static `AccountsFragment`).
- **Accessories**: `SliceFragment` is used for "Connected Devices" in some configurations.
- **Display & Sound**:
    - **CEC Settings**: `SliceFragment` (URI: `@string/cec_settings_slice_uri`)
    - **Audio Output**: `SliceFragment` (URI: `@string/default_audio_output_settings_slice_uri`)
- **Help & Feedback**: `SliceFragment` (URI: `@string/help_and_feedback_slice_uri`)

#### Intent-based Callouts
- **Sound Settings**: `com.android.tv.settings.SOUND` (often intercepted by vendor audio handlers).
- **Connected Devices**: `com.android.tv.settings.CONNECTED_DEVICES`.
- **Add Accessory**: `com.android.tv.settings.accessories.AddAccessoryActivity`.
