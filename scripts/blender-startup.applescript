-- inspired by https://www.reddit.com/r/blender/comments/10qmg0/osx_dual_monitor_user_here_blender_wasnt_saving

on dlog(s)
  global logfile
  if contents of logfile is not "" then
    do shell script "echo " & (quoted form of s) & " > " & logfile
  end if
end dlog

on is_blender_running()
  global blender_process_name
  tell application "System Events"
    (name of every process) contains blender_process_name
  end tell
end is_blender_running

on get_blender_process()
  global blender_process_name
  tell application "System Events"
    get some process whose name is blender_process_name
  end tell
end get_blender_process

on get_process_pid(p)
  tell application "System Events"
    unix id of p
  end tell
end get_process_pid

on get_first_window(p)
  tell application "System Events"
    get the first window of p
  end tell
end get_first_window

on set_win_geometry(win, x, y, w, h)
  tell application "System Events"
    set win's position to {x, y}
    set win's size to {w, h}
  end tell
end set_win_geometry

on run argv
  global logfile
  global blender_process_name
  set blender_process_name to "Blender"
  set logfile to ""
  --set logfile to ".blender-startup-applescript-debug.txt"
  try
    dlog("-------")
    set x to (item 1 of argv)
    set y to (item 2 of argv)
    set w to (item 3 of argv)
    set h to (item 4 of argv)
    dlog("run [" & x & " " & y & " " & w & " " & h & "]")
    set c to 1
    repeat until is_blender_running()
      dlog("wait " & c)
      delay 1
      set c to c + 1
      if c is greater than 10 then
        dlog("interrupt waiting")
        exit repeat
      end if
    end repeat
    dlog("done waiting")
    set blender to get_blender_process()
    dlog("got blender: PID=" & get_process_pid(blender))
    set win to get_first_window(blender)
    dlog("setting position and size of first window")
    set_win_geometry(win, x, y, w, h)
  on error the msg number the num
    dlog("error: " & msg)
  end try
  dlog("done")
end run
