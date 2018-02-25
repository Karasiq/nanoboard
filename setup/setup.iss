#define OutputName "nanoboard-server"
#define MyAppName "Nanoboard"
#define MyAppVersion "1.3.0"
#define MyAppPublisher "Karasiq, Inc."
#define MyAppURL "http://www.github.com/Karasiq/nanoboard"
#define MyAppExeName "nanoboard.exe"
#define ProjectFolder "..\"

[Setup]
AppId={{4e1629c6-a53d-48ee-80b8-bd6644e62a1f}}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\{#MyAppName}
DefaultGroupName={#MyAppName}
OutputDir={#ProjectFolder}\target\iss
OutputBaseFilename={#OutputName}-{#MyAppVersion}
SetupIconFile={#ProjectFolder}\frontend\files\favicon.ico
Compression=lzma
SolidCompression=true
PrivilegesRequired=admin

[Languages]
Name: english; MessagesFile: compiler:Default.isl
Name: russian; MessagesFile: compiler:Languages\Russian.isl

[Tasks]
Name: desktopicon; Description: {cm:CreateDesktopIcon}; GroupDescription: {cm:AdditionalIcons}; Languages: 

[Files]
Source: {#ProjectFolder}\target\universal\nanoboard.exe; DestDir: {app}; Flags: ignoreversion
Source: G:\Temp\Java\jre1.8.0_74\*; DestDir: {app}\jre1.8.0_74; Flags: recursesubdirs ignoreversion
Source: {#ProjectFolder}\setup\places.txt; DestDir: {app}; Flags: ignoreversion
Source: {#ProjectFolder}\setup\categories.txt; DestDir: {app}; Flags: ignoreversion
Source: {#ProjectFolder}\frontend\files\favicon.ico; DestDir: {app}; Flags: ignoreversion

[Icons]
Name: {group}\{#MyAppName}; Filename: {app}\{#MyAppExeName}; IconFilename: {app}\favicon.ico; WorkingDir: {app}
Name: {commondesktop}\{#MyAppName}; Filename: {app}\{#MyAppExeName}; Tasks: desktopicon; IconFilename: {app}\favicon.ico; WorkingDir: {app}

[Run]
Filename: {app}\{#MyAppExeName}; Description: {cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}; Flags: shellexec postinstall skipifsilent; WorkingDir: {app}