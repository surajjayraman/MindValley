ImageLoading Library 

Interface Summary

ielite.app.imageloadlibrary.Cache.java - A memory cache for storing the most recently used images.
ielite.app.imageloadlibrary.Downloader.java - A mechanism to load images from external resources such as a disk cache and/or the internet.

Class Summary

ielite.app.imageloadlibrary.LruCache.java - A memory cache which uses a least-recently used eviction policy.
ielite.app.imageloadlibrary.UrlConnectionDownloader.java - A Downloader which uses HttpURLConnection to download images. A disk cache of 2% of the total available space will be used (capped at 50MB) will automatically be installed in the application's cache directory, when available.



