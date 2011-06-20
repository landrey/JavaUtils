This set of java java utilities provide:
     - A thin layer on top of standard java Properties which provides:
       * A couple of "typed" properties (getBoolean, getOptionalBoolean...)
       * A way to save properties in user file = PREFERENCES
       * A basic swing editor for these preferences (custom for boolean, colors)
       * A way to make a property/preference OBSERVABLE 
       (part of an application configuration is somehow VIEWED by the GUI, 
       and Properties api can be seen as a controller interface)

     - Some swing utilities (action, build menu from properties)

This utility library is used by the AnimJavaExec project
(https://github.com/landrey/animjavaexec).

