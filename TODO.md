# GUI Overhaul & Feature Roadmap — cleanyourtv Launcher

## Changes / Overhauls / Redesigns (20)
1. [ ] Swipeable dock carousel (more than 4 dock apps, horizontal scroll)
2. [ ] Toggleable movable clock widget (position anywhere on screen)
3. [ ] App drawer library screen separate from Home
4. [ ] Vertical carousel like Projectivy channels instead of RecyclerView grid
5. [ ] Edge-to-edge wallpapers (no letterboxing)
6. [ ] Single settings gear + quick pill row replacing top action buttons
7. [ ] Boot animation (lofi pixel art, 3 seconds)
8. [ ] Adaptive accent color from wallpaper dominant color
9. [ ] Animated sky backdrop (cloud drift, sun movement)
10. [ ] Rounded pill focus highlight instead of glass
11. [ ] Now Playing media session strip at bottom
12. [ ] Screen saver mode after N minutes idle
13. [ ] Category chips filter grid in real time
14. [ ] Second dock page (Right on last dock tile)
15. [ ] Pixel-font clock matching lofi theme
16. [ ] Weather widget tile (temp + condition icon) in header
17. [ ] Quote cycling (3 per day instead of 1)
18. [ ] Favorites star toggle on app tiles
19. [ ] Animated gradient mesh background
20. [ ] Per-app custom pixel icons

## Additions / New Features (20)
21. [ ] Home Assistant quick-toggle buttons in Quick Settings
22. [ ] Lofi music visualizer (audio waveform bars)
23. [ ] Sleep timer for lofi music
24. [ ] Local weather alert banner (severe weather)
25. [ ] Moon phase display in clock/date area
26. [ ] Animal sounds toggle (ambient audio matching on-screen animal)
27. [ ] Weather-based lofi stream switching
28. [ ] Custom wallpaper upload via ADB + gallery picker
29. [ ] App usage stats (most opened, time spent)
30. [ ] Parental control PIN for specific apps
31. [ ] HDMI input tiles on Home
32. [ ] Screen calibration test patterns
33. [ ] QR code showing TV IP for ADB pairing
34. [ ] Backup/restore settings to local file
35. [ ] App update notification badge on tiles
36. [ ] Recently added row for new apps
37. [ ] Voice search button
38. [ ] Weather-based greeting
39. [ ] Pixel art icon pack for system apps
40. [ ] Night mode auto-toggle based on sunset/sunrise

## Implementation Log
- Started: 2026-11-01
- Status: Beginning item #1
- Current build: com.gathin.tvhome v3.3 (active Home on TV 192.168.0.123:5555)

## Current Status (2026-11-02)
- Top bar: FIXED - buttons now at very top of screen, accessible via D-pad
- Clock/greeting: STILL BROKEN - leftBlock not in view hierarchy despite being in layout XML
- HomeBootReceiver: created but untested on actual reboot
- Pushed to GitHub: https://github.com/seboss123/cleanyourtv-launcher