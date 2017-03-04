# Knock Code with Custom Shortcuts
![Knock Code (Logo)](https://raw.githubusercontent.com/Rijul-Ahuja/Xposed-Knock-Code/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

This Xposed module enables LG's Knock Code for Lollipop and Marshmallow devices . The unlock method is highly customizable, from changing colours to visibility, error messages, background, etc. You can even hide the emergency button.

What's more, you can use codes to directly open specific shortcuts from the lockscreen, for example use 11212 to unlock, 221 to open WhatsApp, 1122 for email, etc. These shortcuts don't have to be just apps, they can be anything on your device, like Direct Dial, open a specific Contact, etc.

The module is pretty self explanatory, and will prompt you to set a pattern on the lock screen, because that is what it replaces. Other than that, there are no specific instructions to use it. Should the module or Xposed be disabled for any reason, your phone will still remain secure with that pattern.

Compatibility :
I personally test on CM13, and I will support CM12.0, CM12.1, AOSP 5.x and 6.0.x and derivatives. HTC support is limited unless I find a tester. Support for other OEM ROMs is absent beyond basic working functionality.

The only caveats are because of the way Xposed works.
+ Your code(s) will be visible to any one or any app on your device. No root required. The codes are stored encrypted, but anybody determined to get them will be able to, provided they can lay hands on your device. One way to avoid this is to disable USB debugging to prevent chances of a local exploit.
+ You need root to restart the keyguard after changing the full screen option. It is not mandatory, you could manually reboot if you require. All other changes will be reflected automatically, but not this one.

Screenshots:
------
![Knock Code Entry](https://raw.githubusercontent.com/Rijul-Ahuja/Xposed-Knock-Code/master/screenShots/lockScreen.jpg)
![Custom Shortcuts](https://raw.githubusercontent.com/Rijul-Ahuja/Xposed-Knock-Code/master/screenShots/customShortcutSelector.jpg)
![Settings](https://raw.githubusercontent.com/Rijul-Ahuja/Xposed-Knock-Code/master/screenShots/someSettings.jpg)
[More Screenshots](https://github.com/Rijul-Ahuja/Xposed-Knock-Code/blob/master/screenShots/)

Links:
------
+ [XDA Forum](http://forum.xda-developers.com/xposed/modules/lp-knock-code-screen-t3272679)
+ [Xposed Repo](http://repo.xposed.info/module/me.rijul.knockcode)

Thanks to:
------
+ [ColorPickerPreference](https://github.com/attenzione/android-ColorPickerPreference)
+ [Temasek's ShortcutPickHelper](https://github.com/temasek/android_packages_apps_Settings/blob/cm-13.0/src/com/android/settings/cyanogenmod/ShortcutPickHelper.java)
+ [Tyaginator@XDA for the custom shortcuts idea](http://forum.xda-developers.com/member.php?u=5327227)
+ [PIN/Pattern Shortcuts](http://repo.xposed.info/module/com.hamzah.pinshortcuts)
+ [MaxLock's Module Active Prompt](https://github.com/Maxr1998/MaxLock/blob/master/app/src/main/java/de/Maxr1998/xposed/maxlock/ui/SettingsActivity.java#L75)
+ [MaxLock's Launcher Icon Hider](https://github.com/Maxr1998/MaxLock/blob/master/app/src/main/java/de/Maxr1998/xposed/maxlock/ui/settings/MaxLockPreferenceFragment.java#L310)
+ [MohammadAG's KnockCode for the inspiration](http://repo.xposed.info/module/com.mohammadag.knockcode)
