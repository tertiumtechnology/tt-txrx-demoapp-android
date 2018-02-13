# tt-txrx-demoapp-android

The Demoapp depends on the [txrxlib](https://github.com/tertiumtechnology/tt-txrx-lib-android) base module.

The current project configuration require that the main project and all modules folder should reside in the same directory, as in the example below:

```bash
AndroidProjectsFolder/
¦
+-- tt-txrx-demoapp-android/
  ¦
  +-- app/
  +-- settings.gradle
  ...
+-- tt-txrx-lib-android/
  ¦
  +-- txrxlib/
  ...
...
```

If needed, you can change the settings.gradle file inside the main project folder, according to your project structure.
