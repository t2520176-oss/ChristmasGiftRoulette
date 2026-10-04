; Inno Setup 6 script - optional installer built by build_windows.bat when Inno Setup is installed.
#define AppName "FigureCraft"
#define AppVersion "0.1.0"

[Setup]
AppId={{6C1B8F6E-3C0E-4B67-9D1F-5F1A6F0C7A11}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher=FigureCraft
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
OutputBaseFilename=FigureCraft-Setup
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=lowest
SetupIconFile=..\figurecraft\assets\icon.ico
UninstallDisplayIcon={app}\FigureCraft.exe

[Languages]
Name: "korean"; MessagesFile: "compiler:Languages\Korean.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "..\..\output\FigureCraft\*"; DestDir: "{app}"; Flags: recursesubdirs ignoreversion createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\FigureCraft.exe"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\FigureCraft.exe"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; Flags: unchecked

[Run]
Filename: "{app}\FigureCraft.exe"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent
