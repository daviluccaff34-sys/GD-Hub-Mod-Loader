# DL Loader sounds

The Android runtime sound player is `DLSoundManager`.

The five original OGG files from the approved `sounds.zip` archive belong in:

`app/src/main/assets/sounds/`

Expected filenames:

- `newNotif00.ogg`
- `newNotif01.ogg`
- `newNotif02.ogg`
- `newNotif03.ogg`
- `byeNotif00.ogg`

The manager copies the packaged assets into the app cache and loads them with Android `SoundPool`. The in-game panel currently uses `newNotif00` when opened and `byeNotif00` when closed.

Missing audio files are tolerated during development: the game launcher continues to work, but playback is silent. The OGG files themselves are not yet committed in this revision; do not treat sound integration as complete until the five original files are present in the asset directory and playback is tested on an Android device.

No native `.so` files are required for this sound system.

## Import helper

The repository includes `tools/import_dl_sounds.py` for desktop development environments with Python 3. From the repository root, run:

```sh
python3 tools/import_dl_sounds.py /path/to/sounds.zip
```

The helper copies only the five expected OGG files; it does not extract arbitrary archive entries. After importing, build the Android APK in Android Studio or through the repository's build workflow and test audio playback on an Android device.

**Termux is not part of the DL Loader development or build workflow.** The helper does not upload files to GitHub by itself.
