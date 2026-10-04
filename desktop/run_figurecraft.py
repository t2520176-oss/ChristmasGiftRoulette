"""Launcher used for development and by PyInstaller (absolute imports only)."""
import sys

from figurecraft.app import main

if __name__ == "__main__":
    sys.exit(main())
