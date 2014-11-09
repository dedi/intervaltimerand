(
 (nil . ((compile-command-primary . "cd ~/work/inttimer && ant debug install -Dadb.device.arg=-e")
         (compile-command-alternative . "cd ~/work/inttimer && ant debug install -Dadb.device.arg=-d")
         (run-command-primary "cd ~/work/inttimer && ant debug install && adb shell am start -n com.xomzom.androidstuff.timerapp/com.xomzom.androidstuff.timerapp.TimerMainActivity")
         (fill-column . 80)))
)
