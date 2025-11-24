Generate a design document for the TV Settings app, using code under TVSettings folder.  The sources are big, so be careful about running out of context. You may want to invoke gemini cli with --yolo to execute individual subtasks. Use this to ground your understanding of the codebase (use find command or some such to locate files of interest)

- Read AndroidManifest.xml to look at different activities and services
- Look in res/xml to see the different settings available
- Read TwoPanelSettingsFragment to understand basic two panel UI
- Examine MainFragment and DeviceFragment to understand how fragments work in general
- Take a look at state machines and states under connectivity to understand network stack design
- Take a look at SliceFragment/SliceShard/SlicePreference/EmbeddedSlicePreference to understand how slices work.
- Look under vendor/google_atv/apps/TvSettingsGoogle to understand OEM resource overlays and how they define slices
- Look at TvSettingsSliceProvider to understand slices api and ConnectedDevicesSliceProvider to see a sample slice.

Create a design review that gives general overview of TvSettings architecture and where to look for key things (strings, xml layouts, overlays). Explain unique design aspects such as slices and connectivity state machines. AIM for a document that helps human or AI bug triager understand where different UI comes from and where to look for code that implements different features.