#define MyAppName "M2M XML EDITOR"
#ifndef MyAppVersion
  #error MyAppVersion must be supplied by build-installer.bat
#endif

#ifndef MyNumericVersion
  #error MyNumericVersion must be supplied by build-installer.bat
#endif

#ifndef MyBuildTimestamp
  #error MyBuildTimestamp must be supplied by build-installer.bat
#endif

#ifndef MySourceDir
  #define MySourceDir "."
#endif

#ifndef MyOutputDir
  #define MyOutputDir "."
#endif

#ifndef MyProjectDir
  #define MyProjectDir "."
#endif

[Setup]
AppId={{A5C5E6B9-7A5A-4A1D-B2A8-2A8A2E4D9C31}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
VersionInfoVersion={#MyNumericVersion}.0
VersionInfoTextVersion={#MyAppVersion}
AppPublisher=M2M XML EDITOR
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableDirPage=no
DisableProgramGroupPage=yes
OutputDir={#MyOutputDir}
OutputBaseFilename=M2M-XML-EDITOR-Payload-Setup-{#MyAppVersion}
Compression=lzma2/max
SolidCompression=no
; A nagyméretű alkalmazás külön .bin adatfájlba kerül.
; A payload Setup EXE és a nagy BIN adatfájl együtt alkotják a tényleges telepítőt.
; A felhasználó az ezek elé épített külön bootstrapper EXE-t indítja.
DiskSpanning=yes
DiskSliceSize=1900000000
SlicesPerDisk=1
WizardStyle=modern
WizardResizable=yes
WizardSizePercent=135,125
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog commandline
SetupIconFile=app.ico
UninstallDisplayIcon={app}\M2M XML EDITOR.exe
LZMAUseSeparateProcess=yes
ShowLanguageDialog=yes
DisableWelcomePage=no
SetupLogging=yes

[Languages]
Name: "hungarian"; MessagesFile: "compiler:Languages\Hungarian.isl"; LicenseFile: "licenses\LICENSE"
;Name: "english"; MessagesFile: "compiler:Default.isl"; LicenseFile: "licenses\EUPL-1.2_en.txt"

[CustomMessages]
hungarian.DesktopIconTask=Asztali ikon létrehozása
;english.DesktopIconTask=Create a desktop shortcut
hungarian.AdditionalTasks=További feladatok:
;english.AdditionalTasks=Additional tasks:
hungarian.LaunchProgram=A M2M XML EDITOR indítása
;english.LaunchProgram=Launch M2M XML EDITOR

[Tasks]
Name: "desktopicon"; Description: "{cm:DesktopIconTask}"; GroupDescription: "{cm:AdditionalTasks}"; Flags: unchecked

[Files]
Source: "{#MySourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#MyProjectDir}\LICENSE"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#MyProjectDir}\README-LICENC.md"; DestDir: "{app}"; Flags: ignoreversion
;Source: "{#MyProjectDir}\licenses\*"; DestDir: "{app}\licenses"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#MyProjectDir}\admin-tools\*"; DestDir: "{app}\admin-tools"; Flags: ignoreversion recursesubdirs createallsubdirs

[Dirs]
Name: "{code:GetDataRoot}"
Name: "{code:GetDataRoot}\config"
Name: "{code:GetDataRoot}\data"
Name: "{code:GetDataRoot}\logs"
Name: "{code:GetDataRoot}\data\xml"
Name: "{code:GetDataRoot}\data\archive"
Name: "{code:GetDataRoot}\data\xml-index"
Name: "{code:GetDataRoot}\data\xpath\results"
Name: "{code:GetDataRoot}\data\attachments"
Name: "{code:GetDataRoot}\data\exports"
Name: "{code:GetDataRoot}\database"
Name: "{code:GetDataRoot}\certificates"
Name: "{code:GetDataRoot}\backup"
Name: "{code:GetDataRoot}\repo"
Name: "{code:GetDataRoot}\repo\xsd"
Name: "{code:GetDataRoot}\repo\uimodel"
Name: "{code:GetDataRoot}\repo\xpath"
Name: "{code:GetDataRoot}\repo\xsd\common"
Name: "{code:GetDataRoot}\repo\rule-xsl"

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\M2M XML EDITOR.exe"; IconFilename: "{app}\M2M XML EDITOR.exe"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\M2M XML EDITOR.exe"; Tasks: desktopicon; IconFilename: "{app}\M2M XML EDITOR.exe"
Name: "{group}\Adatbázis-kapcsolat javítása"; Filename: "{app}\admin-tools\update-datasource-config.cmd"; WorkingDir: "{app}\admin-tools"

[InstallDelete]
; A jpackage alkalmazásmagot frissítéskor tisztán cseréljük, hogy eltávolított régi JAR/runtime fájlok ne maradjanak vissza.
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"

[Run]
Filename: "{app}\M2M XML EDITOR.exe"; Description: "{cm:LaunchProgram}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}\app"

[Code]
var
  InstallTypePage: TInputOptionWizardPage;
  SecurityPage: TInputOptionWizardPage;
  DatabasePage: TInputOptionWizardPage;
  DataRootPage: TInputDirWizardPage;
  DbPage: TInputQueryWizardPage;
  PathPage: TInputDirWizardPage;
  RepoPathPage: TInputDirWizardPage;
  AppPage: TInputQueryWizardPage;
  DesktopPage: TInputOptionWizardPage;
  AdminPage: TInputQueryWizardPage;
  IntegrationPage: TInputQueryWizardPage;
  DbTestButton: TNewButton;
  DbDownloadLabel: TNewStaticText;
  DbDownloadMySqlButton: TNewButton;
  DbDownloadPostgreSqlButton: TNewButton;
  DbDownloadOracleButton: TNewButton;
  UpdateInfoPage: TOutputMsgMemoWizardPage;
  ExistingInstallerConfig: Boolean;
  ExistingDatabaseType: String;
  DataRootDefaultsApplied: Boolean;

function L(HungarianText: String; EnglishText: String): String;
begin
  if ActiveLanguage = 'hungarian' then
    Result := HungarianText
  else
    Result := EnglishText;
end;

procedure MakeWizardResizable();
begin
  WizardForm.BorderStyle := bsSizeable;
  WizardForm.BorderIcons := [biSystemMenu, biMinimize, biMaximize];
end;

function BoolToProp(Value: Boolean): String;
begin
  if Value then
    Result := 'true'
  else
    Result := 'false';
end;

function NormalizePropPath(Value: String): String;
var
  Temp: String;
begin
  Temp := Value;
  StringChangeEx(Temp, '\', '/', True);
  Result := Temp;
end;

function DefaultDataRoot(): String;
begin
  if IsAdminInstallMode then
    Result := ExpandConstant('{commonappdata}\M2M-XML-EDITOR')
  else
    Result := ExpandConstant('{localappdata}\M2M-XML-EDITOR');
end;

function GetDataRoot(Param: String): String;
begin
  if (DataRootPage <> nil) and (Trim(DataRootPage.Values[0]) <> '') then
    Result := DataRootPage.Values[0]
  else
    Result := DefaultDataRoot();
end;

function GetConfigRoot(): String;
begin
  Result := GetDataRoot('') + '\config';
end;


procedure ApplyDataRootDefaults();
var
  BaseData: String;
begin
  if DataRootDefaultsApplied then
    Exit;

  BaseData := DefaultDataRoot();
  DataRootPage.Values[0] := BaseData;

  { A technikai lapok egyszeru telepitesnel rejtettek, de az ertekeiket a
    bootstrap iro ugyanugy felhasznalja. Ezert a telepitesi hatokor
    vegleges valasztasa utan ezeket is ugyanarra az adatgyokerre kell
    ujraszamolni. }
  PathPage.Values[0] := BaseData + '\data\xml';
  PathPage.Values[1] := BaseData + '\data\archive';
  PathPage.Values[2] := BaseData + '\data\xpath\results';
  PathPage.Values[3] := BaseData + '\data\import';
  RepoPathPage.Values[0] := BaseData + '\repo\xsd';
  RepoPathPage.Values[1] := BaseData + '\repo\uimodel';
  RepoPathPage.Values[2] := BaseData + '\repo\xpath';
  RepoPathPage.Values[3] := BaseData + '\repo\xsd\common';
  RepoPathPage.Values[4] := BaseData + '\repo\rule-xsl';

  DataRootDefaultsApplied := True;
end;

procedure WriteBootstrapLocator();
var
  LocatorDir: String;
  LocatorFile: String;
  BootstrapFile: String;
  Content: String;
begin
  { Az alkalmazas mar az elso inditaskor ugyanazt a bootstrap fajlt kell,
    hogy megtalalja, amelyet az installer letrehozott. Ez kulonosen fontos
    current-user telepitesnel, ahol a konfiguracio a LOCALAPPDATA alatt van,
    mikozben az alkalmazas OS fallbackje egy irhato ProgramData-t is valaszthat. }
  LocatorDir := GetEnv('USERPROFILE');
  if Trim(LocatorDir) = '' then
    RaiseException('A USERPROFILE kornyezeti valtozo nem erheto el; a bootstrap locator nem hozhato letre.');
  LocatorDir := AddBackslash(LocatorDir) + '.m2m-xml-editor';
  LocatorFile := LocatorDir + '\bootstrap-location.properties';
  BootstrapFile := NormalizePropPath(GetConfigRoot() + '\application-bootstrap.properties');
  Content := '# M2M XML EDITOR bootstrap location' + #13#10 +
    'bootstrap.file=' + BootstrapFile + #13#10;
  if not DirExists(LocatorDir) then
    ForceDirectories(LocatorDir);
  SaveStringToFile(LocatorFile, AnsiString(Content), False);
end;

function IsSimpleInstall(): Boolean;
begin
  Result := InstallTypePage.Values[0];
end;

function SelectedSecurityMode(): String;
begin
  if SecurityPage.Values[0] then
    Result := 'STANDALONE'
  else
    Result := 'MULTI_USER';
end;

function SelectedDatabaseType(): String;
begin
  if DatabasePage.Values[0] then
    Result := 'H2'
  else if DatabasePage.Values[1] then
    Result := 'MYSQL'
  else if DatabasePage.Values[2] then
    Result := 'POSTGRESQL'
  else
    Result := 'ORACLE';
end;

function IsDigits(Value: String): Boolean;
var
  I: Integer;
begin
  Result := (Length(Value) > 0);
  for I := 1 to Length(Value) do
  begin
    if (Value[I] < '0') or (Value[I] > '9') then
    begin
      Result := False;
      Exit;
    end;
  end;
end;

function IsValidThreshold(Value: String): Boolean;
var
  U: String;
  P: Integer;
  NumberPart: String;
  UnitPart: String;
begin
  U := Trim(Uppercase(Value));
  P := Pos(' ', U);
  if P <= 1 then
  begin
    Result := False;
    Exit;
  end;
  NumberPart := Copy(U, 1, P - 1);
  UnitPart := Trim(Copy(U, P + 1, Length(U)));
  Result := IsDigits(NumberPart) and ((UnitPart = 'KB') or (UnitPart = 'MB') or (UnitPart = 'GB'));
end;

procedure SetDbDefaults();
var
  DbType: String;
begin
  DbType := SelectedDatabaseType();
  if DbType = 'H2' then
  begin
    DbPage.Values[0] := 'localhost';
    DbPage.Values[1] := '';
    DbPage.Values[2] := 'nav-xsd-parser-tool';
    DbPage.Values[3] := 'sa';
    DbPage.Values[4] := '';
  end
  else if DbType = 'MYSQL' then
  begin
    DbPage.Values[0] := 'localhost';
    DbPage.Values[1] := '3306';
    DbPage.Values[2] := 'nav_xsd_parser_tool';
    DbPage.Values[3] := 'nav_user';
    DbPage.Values[4] := 'nav_password';
  end
  else if DbType = 'POSTGRESQL' then
  begin
    DbPage.Values[0] := 'localhost';
    DbPage.Values[1] := '5432';
    DbPage.Values[2] := 'nav_xsd_parser_tool';
    DbPage.Values[3] := 'nav_user';
    DbPage.Values[4] := 'nav_password';
  end
  else
  begin
    DbPage.Values[0] := 'localhost';
    DbPage.Values[1] := '1521';
    DbPage.Values[2] := 'FREEPDB1';
    DbPage.Values[3] := 'nav_user';
    DbPage.Values[4] := 'nav_password';
  end;
end;


function TestTcpConnection(Host: String; Port: String): Boolean;
var
  ResultCode: Integer;
  Args: String;
begin
  Args := '-NoProfile -ExecutionPolicy Bypass -Command "if (Test-NetConnection -ComputerName ''' + Host + ''' -Port ' + Port + ' -InformationLevel Quiet) { exit 0 } else { exit 1 }"';
  Result := Exec('powershell.exe', Args, '', SW_HIDE, ewWaitUntilTerminated, ResultCode) and (ResultCode = 0);
end;

procedure OpenExternalUrl(Url: String);
var
  ErrorCode: Integer;
begin
  if not ShellExec('open', Url, '', '', SW_SHOWNORMAL, ewNoWait, ErrorCode) then
    MsgBox(L('Nem sikerült megnyitni a hivatkozást. Hibakód: ', 'Could not open the link. Error code: ') + IntToStr(ErrorCode), mbError, MB_OK);
end;

procedure DbTestButtonClick(Sender: TObject);
var
  DbType: String;
  Host: String;
  Port: String;
begin
  DbType := SelectedDatabaseType();

  if DbType = 'H2' then
  begin
    MsgBox(L('H2 beágyazott fájlalapú adatbázis lett kiválasztva. Külső szerverkapcsolatot nem kell tesztelni. Az adatbázisfájl az alkalmazás adatkönyvtárában jön létre.', 'The embedded file-based H2 database is selected. No external server connection test is required. The database file will be created in the application data directory.'), mbInformation, MB_OK);
    Exit;
  end;

  Host := Trim(DbPage.Values[0]);
  Port := Trim(DbPage.Values[1]);

  if Host = '' then
  begin
    MsgBox(L('A kapcsolatteszteléshez add meg az adatbázis kiszolgálójának nevét.', 'Enter the database host name for the connection test.'), mbError, MB_OK);
    Exit;
  end;

  if not IsDigits(Port) then
  begin
    MsgBox(L('A kapcsolatteszteléshez érvényes numerikus port szükséges.', 'A valid numeric port is required for the connection test.'), mbError, MB_OK);
    Exit;
  end;

  if TestTcpConnection(Host, Port) then
    MsgBox(L('Sikeres alapszintű kapcsolatteszt: a(z) ', 'Basic connection test succeeded: ') + Host + ':' + Port + L(' végpont elérhető. Fontos: ez TCP/port elérhetőségi teszt; a felhasználó, jelszó, séma és jogosultság ellenőrzését az alkalmazás induláskor végzi el JDBC-kapcsolaton keresztül.', ' endpoint is reachable. Note: this is a TCP/port reachability test; the application validates the user, password, schema and permissions through JDBC during startup.'), mbInformation, MB_OK)
  else
    MsgBox(L('Sikertelen kapcsolatteszt: a(z) ', 'Connection test failed: ') + Host + ':' + Port + L(' végpont nem érhető el. Ellenőrizd, hogy az adatbázis-kiszolgáló fut-e, a port helyes-e, és a tűzfal engedi-e a kapcsolatot.', ' endpoint is not reachable. Verify that the database server is running, the port is correct and the firewall allows the connection.'), mbError, MB_OK);
end;

procedure DbDownloadMySqlButtonClick(Sender: TObject);
begin
  OpenExternalUrl('https://dev.mysql.com/downloads/mysql/');
end;

procedure DbDownloadPostgreSqlButtonClick(Sender: TObject);
begin
  OpenExternalUrl('https://www.postgresql.org/download/windows/');
end;

procedure DbDownloadOracleButtonClick(Sender: TObject);
begin
  OpenExternalUrl('https://www.oracle.com/database/technologies/oracle-database-software-downloads.html');
end;

function PropertyKeyFromLine(Line: String): String;
var
  P: Integer;
  T: String;
begin
  T := Trim(Line);
  Result := '';
  if (T = '') or (T[1] = '#') or (T[1] = '!') then
    Exit;
  P := Pos('=', T);
  if P <= 1 then
    Exit;
  Result := Trim(Copy(T, 1, P - 1));
end;

function FindPropertyValue(PropertiesText: String; Key: String): String;
var
  Rest: String;
  Line: String;
  P: Integer;
  LineKey: String;
begin
  Result := '';
  Rest := PropertiesText;
  while Rest <> '' do
  begin
    P := Pos(#10, Rest);
    if P > 0 then
    begin
      Line := Copy(Rest, 1, P - 1);
      Delete(Rest, 1, P);
    end
    else
    begin
      Line := Rest;
      Rest := '';
    end;
    if (Length(Line) > 0) and (Line[Length(Line)] = #13) then
      Delete(Line, Length(Line), 1);
    LineKey := PropertyKeyFromLine(Line);
    if CompareText(LineKey, Key) = 0 then
    begin
      P := Pos('=', Line);
      Result := Trim(Copy(Line, P + 1, Length(Line)));
      Exit;
    end;
  end;
end;

function HasPropertyKey(PropertiesText: String; Key: String): Boolean;
var
  Rest: String;
  Line: String;
  P: Integer;
begin
  Result := False;
  Rest := PropertiesText;
  while Rest <> '' do
  begin
    P := Pos(#10, Rest);
    if P > 0 then
    begin
      Line := Copy(Rest, 1, P - 1);
      Delete(Rest, 1, P);
    end
    else
    begin
      Line := Rest;
      Rest := '';
    end;
    if (Length(Line) > 0) and (Line[Length(Line)] = #13) then
      Delete(Line, Length(Line), 1);
    if CompareText(PropertyKeyFromLine(Line), Key) = 0 then
    begin
      Result := True;
      Exit;
    end;
  end;
end;

function DetectExistingInstallerConfig(): Boolean;
var
  ConfigFile: String;
  ConfigText: AnsiString;
begin
  ConfigFile := GetConfigRoot() + '\application-bootstrap.properties';
  if not FileExists(ConfigFile) then
    ConfigFile := GetConfigRoot() + '\nav-xsd-parser-tool-paths.properties';
  Result := FileExists(ConfigFile);
  ExistingDatabaseType := '';
  if Result and LoadStringFromFile(ConfigFile, ConfigText) then
    ExistingDatabaseType := Uppercase(FindPropertyValue(String(ConfigText), 'nav.xsdparsertool.database.type'));
end;

procedure InitializeWizard();
var
  BaseData: String;
  ButtonTop: Integer;
  DownloadLabelLeft: Integer;
begin
  MakeWizardResizable();
  BaseData := DefaultDataRoot();
  DataRootDefaultsApplied := False;
  ExistingInstallerConfig := DetectExistingInstallerConfig();

  UpdateInfoPage := CreateOutputMsgMemoPage(wpSelectDir,
    L('Telepített rendszer frissítése', 'Update installed system'),
    L('Meglévő telepítés észlelve.', 'Existing installation detected.'),
    L('A programfájlok frissülnek, a meglévő konfigurációs értékek és adatok megmaradnak.', 'Application files will be updated while existing configuration values and data are preserved.'),
    L('Frissítési szabályok:', 'Update rules:') + #13#10 +
    '- ' + L('A meglévő konfigurációs fájlokat a telepítő nem írja felül.', 'Existing configuration files are not overwritten.') + #13#10 +
    '- ' + L('Az új verzióban megjelent hiányzó konfigurációs kulcsok a fájl végére kerülnek.', 'Configuration keys introduced by the new version are appended when missing.') + #13#10 +
    '- ' + L('A hiányzó új könyvtárak automatikusan létrejönnek.', 'New missing directories are created automatically.') + #13#10 +
    '- ' + L('Az alkalmazásfájlok frissülnek; az adatbázis-sémát az alkalmazás Flyway migrációi kezelik.', 'Application files are updated; database schema changes are handled by Flyway migrations.')
  );
  InstallTypePage := CreateInputOptionPage(wpSelectDir,
    L('Telepítés típusa', 'Installation type'),
    L('Válaszd ki az egyszerű vagy a részletesen beállítható telepítést.', 'Choose the simple installation or the fully configurable advanced installation.'),
    L('Az Egyszerű telepítés az ajánlott alapbeállításokat használja. A Haladó telepítésben minden fontos működési, adatbázis- és könyvtárbeállítás megadható.', 'Simple installation uses the recommended defaults. Advanced installation exposes the important runtime, database and directory settings.'),
    True, False);
  InstallTypePage.Add(L('Egyszerű telepítés (ajánlott)', 'Simple installation (recommended)'));
  InstallTypePage.Add(L('Haladó telepítés', 'Advanced installation'));
  InstallTypePage.Values[0] := True;

  DataRootPage := CreateInputDirPage(InstallTypePage.ID,
    L('Alapkönyvtár', 'Base data directory'),
    L('Add meg az alkalmazás teljes írható adatkönyvtárát.', 'Select the writable application data root.'),
    L('A config, database, logs, repo, backup és data könyvtárak ez alatt jönnek létre.', 'The config, database, logs, repo, backup and data directories are created below this root.'),
    False, L('Új mappa', 'New folder'));
  DataRootPage.Add(L('Alapkönyvtár:', 'Base directory:'));
  DataRootPage.Values[0] := BaseData;

  SecurityPage := CreateInputOptionPage(DataRootPage.ID,
    L('Alkalmazás üzemmódja', 'Application mode'),
    L('Válaszd ki, hogy az alkalmazás egy- vagy többfelhasználós módban fusson.', 'Choose whether the application should run in single-user or multi-user mode.'),
    L('STANDALONE módban nincs bejelentkezés. MULTI_USER módban bejelentkezés és szerepkör-kezelés működik.', 'STANDALONE mode has no sign-in. MULTI_USER mode provides sign-in and role management.'),
    True, False);
  SecurityPage.Add(L('Egyfelhasználós / STANDALONE', 'Single-user / STANDALONE'));
  SecurityPage.Add(L('Többfelhasználós / MULTI_USER', 'Multi-user / MULTI_USER'));
  SecurityPage.Values[0] := True;

  DatabasePage := CreateInputOptionPage(SecurityPage.ID,
    L('Adatbázis típusa', 'Database type'),
    L('Válaszd ki a használni kívánt adatbázist.', 'Choose the database to use.'),
    L('STANDALONE kipróbálásához a H2 javasolt. MULTI_USER használathoz MySQL, PostgreSQL vagy Oracle javasolt.', 'H2 is recommended for STANDALONE use. MySQL, PostgreSQL or Oracle is recommended for MULTI_USER use.'),
    True, False);
  DatabasePage.Add(L('H2 – beépített fájlalapú adatbázis', 'H2 – embedded file-based database'));
  DatabasePage.Add('MySQL');
  DatabasePage.Add('PostgreSQL');
  DatabasePage.Add('Oracle');
  DatabasePage.Values[0] := True;

  DbPage := CreateInputQueryPage(DatabasePage.ID,
    L('Adatbázis-kapcsolat', 'Database connection'),
    L('Add meg az adatbázis-kapcsolat alapadatait.', 'Enter the basic database connection details.'),
    L('H2 esetén a host/port mezők nem kerülnek felhasználásra. A mezők alapértelmezett értékekkel vannak kitöltve.', 'For H2, the host and port fields are not used. The fields are prefilled with defaults.'));
  DbPage.Add('Host:', False);
  DbPage.Add('Port:', False);
  DbPage.Add(L('Adatbázis / service neve:', 'Database / service name:'), False);
  DbPage.Add(L('Felhasználó:', 'User:'), False);
  DbPage.Add(L('Jelszó:', 'Password:'), True);
  SetDbDefaults();

  ButtonTop := DbPage.Edits[4].Top + DbPage.Edits[4].Height + ScaleY(14);

  DbTestButton := TNewButton.Create(WizardForm);
  DbTestButton.Parent := DbPage.Surface;
  DbTestButton.Left := DbPage.Edits[4].Left;
  DbTestButton.Top := ButtonTop;
  DbTestButton.Width := ScaleX(170);
  DbTestButton.Caption := L('Kapcsolat ellenőrzése', 'Test connection');
  DbTestButton.OnClick := @DbTestButtonClick;

  DownloadLabelLeft := DbTestButton.Left + DbTestButton.Width + ScaleX(24);

  DbDownloadLabel := TNewStaticText.Create(WizardForm);
  DbDownloadLabel.Parent := DbPage.Surface;
  DbDownloadLabel.Left := DownloadLabelLeft;
  DbDownloadLabel.Top := ButtonTop + ScaleY(5);
  DbDownloadLabel.Width := ScaleX(70);
  DbDownloadLabel.Caption := L('Letöltés:', 'Download:');

  DbDownloadMySqlButton := TNewButton.Create(WizardForm);
  DbDownloadMySqlButton.Parent := DbPage.Surface;
  DbDownloadMySqlButton.Left := DownloadLabelLeft + ScaleX(72);
  DbDownloadMySqlButton.Top := ButtonTop;
  DbDownloadMySqlButton.Width := ScaleX(90);
  DbDownloadMySqlButton.Caption := 'MySQL';
  DbDownloadMySqlButton.OnClick := @DbDownloadMySqlButtonClick;

  DbDownloadPostgreSqlButton := TNewButton.Create(WizardForm);
  DbDownloadPostgreSqlButton.Parent := DbPage.Surface;
  DbDownloadPostgreSqlButton.Left := DownloadLabelLeft + ScaleX(172);
  DbDownloadPostgreSqlButton.Top := ButtonTop;
  DbDownloadPostgreSqlButton.Width := ScaleX(100);
  DbDownloadPostgreSqlButton.Caption := 'PostgreSQL';
  DbDownloadPostgreSqlButton.OnClick := @DbDownloadPostgreSqlButtonClick;

  DbDownloadOracleButton := TNewButton.Create(WizardForm);
  DbDownloadOracleButton.Parent := DbPage.Surface;
  DbDownloadOracleButton.Left := DownloadLabelLeft + ScaleX(282);
  DbDownloadOracleButton.Top := ButtonTop;
  DbDownloadOracleButton.Width := ScaleX(90);
  DbDownloadOracleButton.Caption := 'Oracle';
  DbDownloadOracleButton.OnClick := @DbDownloadOracleButtonClick;

  PathPage := CreateInputDirPage(DbPage.ID,
    L('Könyvtárak – állományok', 'Directories – files'),
    L('Add meg az XML-állományokhoz és eredményekhez használt könyvtárakat.', 'Select the directories used for XML files and results.'),
    L('A telepítő létrehozza a megadott mappákat, ha még nem léteznek.', 'The installer creates the selected folders if they do not exist.'),
    False, L('Új mappa', 'New folder'));
  PathPage.Add(L('XML-állományok gyökérkönyvtára:', 'XML files root directory:'));
  PathPage.Add(L('Archív könyvtár:', 'Archive directory:'));
  PathPage.Add(L('XPath eredménykönyvtára:', 'XPath result directory:'));
  PathPage.Add(L('Szerveroldali XML import könyvtára:', 'Server-side XML import directory:'));
  PathPage.Values[0] := BaseData + '\data\xml';
  PathPage.Values[1] := BaseData + '\data\archive';
  PathPage.Values[2] := BaseData + '\data\xpath\results';
  PathPage.Values[3] := BaseData + '\data\import';

  RepoPathPage := CreateInputDirPage(PathPage.ID,
    L('Könyvtárak – sémák és UI-modellek', 'Directories – schemas and UI models'),
    L('Add meg az XSD-, UI-model-, XPath-, common- és Rule-XSL-repozitóriumok könyvtárait.', 'Select the XSD, UI model, XPath, common and Rule-XSL repository directories.'),
    L('A mezők két oldalra vannak bontva, hogy a telepítőablakban minden látható maradjon.', 'The fields are split across pages so everything remains visible in the installer window.'),
    False, L('Új mappa', 'New folder'));
  RepoPathPage.Add(L('XSD-repozitórium könyvtára:', 'XSD repository directory:'));
  RepoPathPage.Add(L('UI-model-repozitórium könyvtára:', 'UI model repository directory:'));
  RepoPathPage.Add(L('XPath-szabályrepozitórium könyvtára:', 'XPath rule repository directory:'));
  RepoPathPage.Add(L('Közös XSD-k könyvtára:', 'Common XSD directory:'));
  RepoPathPage.Add(L('Rule-XSL könyvtára:', 'Rule-XSL directory:'));
  RepoPathPage.Values[0] := BaseData + '\repo\xsd';
  RepoPathPage.Values[1] := BaseData + '\repo\uimodel';
  RepoPathPage.Values[2] := BaseData + '\repo\xpath';
  RepoPathPage.Values[3] := BaseData + '\repo\xsd\common';
  RepoPathPage.Values[4] := BaseData + '\repo\rule-xsl';

  AppPage := CreateInputQueryPage(RepoPathPage.ID,
    L('Alkalmazásbeállítások', 'Application settings'),
    L('Általános működési beállítások.', 'General runtime settings.'),
    L('A nagy XML küszöb formátuma például: 100 MB, 1 GB vagy 500 KB.', 'Large XML threshold examples: 100 MB, 1 GB or 500 KB.'));
  AppPage.Add('HTTP port:', False);
  AppPage.Add(L('Nagy XML küszöb:', 'Large XML threshold:'), False);
  AppPage.Add('Session timeout:', False);
  AppPage.Add(L('Zárolási időkorlát percben:', 'Lock timeout in minutes:'), False);
  AppPage.Add(L('XSD-validáció hibalimitje:', 'XSD validation error limit:'), False);
  AppPage.Values[0] := '8080';
  AppPage.Values[1] := '100 MB';
  AppPage.Values[2] := '30m';
  AppPage.Values[3] := '30';
  AppPage.Values[4] := '500';

  DesktopPage := CreateInputOptionPage(AppPage.ID,
    L('Asztali integráció', 'Desktop integration'),
    L('Válaszd ki az asztali integrációs funkciókat.', 'Choose desktop integration features.'),
    L('Ezek a beállítások a külső properties fájlba kerülnek.', 'These settings are written to the external properties file.'),
    False, False);
  DesktopPage.Add(L('Asztali integráció engedélyezése', 'Enable desktop integration'));
  DesktopPage.Add(L('Nyitóképernyő indításkor', 'Show splash screen on startup'));
  DesktopPage.Add(L('Böngésző automatikus megnyitása', 'Open browser automatically'));
  DesktopPage.Add(L('Tálcaikon engedélyezése', 'Enable tray icon'));
  DesktopPage.Values[0] := True;
  DesktopPage.Values[1] := True;
  DesktopPage.Values[2] := True;
  DesktopPage.Values[3] := True;

  AdminPage := CreateInputQueryPage(DesktopPage.ID,
    L('Kezdeti adminisztrátor', 'Bootstrap administrator'),
    L('A rendszer első adminisztrátor-felhasználója.', 'Initial administrator of the system.'),
    L('A kezdeti adminisztrátor csak akkor jön létre, ha még nincs felhasználó az adatbázisban.', 'The initial administrator is created only when the database has no users.'));
  AdminPage.Add(L('Felhasználónév:', 'Username:'), False);
  AdminPage.Add(L('Jelszó:', 'Password:'), True);
  AdminPage.Add(L('Jelszó megerősítése:', 'Confirm password:'), True);
  AdminPage.Add(L('Megjelenített név:', 'Display name:'), False);
  AdminPage.Add('Email:', False);
  AdminPage.Values[0] := 'admin';
  AdminPage.Values[1] := '';
  AdminPage.Values[2] := '';
  AdminPage.Values[3] := 'System Administrator';
  AdminPage.Values[4] := '';

  IntegrationPage := CreateInputQueryPage(AdminPage.ID,
    L('Külső szolgáltatások hozzáférési adatai', 'External service credentials'),
    L('Opcionálisan add meg a GitHub- és NAV M2M-hozzáférési adatokat.', 'Optionally enter GitHub and NAV M2M credentials.'),
    L('Minden mező opcionális. Az M2M API-kulcsot a NAV által átadott teljes formában add meg; a rendszer a kötőjelek alapján automatikusan szétbontja.',
      'All fields are optional. Enter the complete NAV M2M API key; the application will split it automatically at the hyphens.'));
  IntegrationPage.Add(L('GitHub token:', 'GitHub token:'), True);
  IntegrationPage.Add(L('M2M API-kulcs:', 'M2M API key:'), True);
  IntegrationPage.Add('Client ID:', False);
  IntegrationPage.Add('Client Secret:', True);
  IntegrationPage.Values[0] := '';
  IntegrationPage.Values[1] := '';
  IntegrationPage.Values[2] := '';
  IntegrationPage.Values[3] := '';
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;

  if PageID = UpdateInfoPage.ID then
  begin
    Result := not ExistingInstallerConfig;
    Exit;
  end;

  if ExistingInstallerConfig then
  begin
    { Update mode: never ask for values that could overwrite existing configuration. }
    Result :=
      (PageID = InstallTypePage.ID) or
      (PageID = SecurityPage.ID) or
      (PageID = DatabasePage.ID) or
      (PageID = DbPage.ID) or
      (PageID = PathPage.ID) or
      (PageID = RepoPathPage.ID) or
      (PageID = AppPage.ID) or
      (PageID = DesktopPage.ID) or
      (PageID = AdminPage.ID) or
      (PageID = IntegrationPage.ID);
    Exit;
  end;

  { Fresh simple install: skip technical configuration pages. }
  if IsSimpleInstall() and
     ((PageID = SecurityPage.ID) or
      (PageID = PathPage.ID) or
      (PageID = RepoPathPage.ID) or
      (PageID = AppPage.ID) or
      (PageID = DesktopPage.ID)) then
  begin
    Result := True;
    Exit;
  end;

  if (PageID = DbPage.ID) and (SelectedDatabaseType() = 'H2') then
    Result := True;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  MakeWizardResizable();
  if (CurPageID = DataRootPage.ID) and (not ExistingInstallerConfig) then
    ApplyDataRootDefaults();
  if CurPageID = DbPage.ID then
    SetDbDefaults();
end;

function IsValidM2mApiKey(Value: String): Boolean;
var
  I: Integer;
  SeparatorCount: Integer;
  SegmentLength: Integer;
begin
  Value := Trim(Value);
  if Value = '' then
  begin
    Result := True;
    Exit;
  end;

  SeparatorCount := 0;
  SegmentLength := 0;
  for I := 1 to Length(Value) do
  begin
    if Value[I] = '-' then
    begin
      if SegmentLength = 0 then
      begin
        Result := False;
        Exit;
      end;
      SeparatorCount := SeparatorCount + 1;
      SegmentLength := 0;
    end
    else
      SegmentLength := SegmentLength + 1;
  end;

  Result := (SeparatorCount = 3) and (SegmentLength > 0);
end;

function IsValidSetupPassword(Value: String): Boolean;
var
  I: Integer;
  HasLower, HasUpper, HasDigit, HasSpecial: Boolean;
begin
  HasLower := False; HasUpper := False; HasDigit := False; HasSpecial := False;
  for I := 1 to Length(Value) do
  begin
    if (Value[I] >= 'a') and (Value[I] <= 'z') then HasLower := True
    else if (Value[I] >= 'A') and (Value[I] <= 'Z') then HasUpper := True
    else if (Value[I] >= '0') and (Value[I] <= '9') then HasDigit := True
    else HasSpecial := True;
  end;
  Result := (Length(Value) >= 8) and HasLower and HasUpper and HasDigit and HasSpecial;
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;

  if CurPageID = AppPage.ID then
  begin
    if not IsDigits(AppPage.Values[0]) then
    begin
      MsgBox(L('A HTTP-port csak szám lehet.', 'The HTTP port must be numeric.'), mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if not IsValidThreshold(AppPage.Values[1]) then
    begin
      MsgBox(L('A nagy XML küszöb formátuma hibás. Példa: 100 MB, 1 GB vagy 500 KB.', 'Invalid large XML threshold. Examples: 100 MB, 1 GB or 500 KB.'), mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if not IsDigits(AppPage.Values[3]) then
    begin
      MsgBox(L('A zárolási időkorlát csak szám lehet.', 'The lock timeout must be numeric.'), mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if not IsDigits(AppPage.Values[4]) then
    begin
      MsgBox(L('Az XSD-validáció hibalimitje csak szám lehet.', 'The XSD validation error limit must be numeric.'), mbError, MB_OK);
      Result := False;
      Exit;
    end;
  end;

  if CurPageID = DbPage.ID then
  begin
    if SelectedDatabaseType() <> 'H2' then
    begin
      if Trim(DbPage.Values[0]) = '' then
      begin
        MsgBox(L('Az adatbázis-kiszolgáló megadása kötelező.', 'The database host is required.'), mbError, MB_OK);
        Result := False;
        Exit;
      end;
      if not IsDigits(DbPage.Values[1]) then
      begin
        MsgBox(L('Az adatbázis portja csak szám lehet.', 'The database port must be numeric.'), mbError, MB_OK);
        Result := False;
        Exit;
      end;
      if Trim(DbPage.Values[2]) = '' then
      begin
        MsgBox(L('Az adatbázis neve kötelező.', 'The database name is required.'), mbError, MB_OK);
        Result := False;
        Exit;
      end;
      if Trim(DbPage.Values[3]) = '' then
      begin
        MsgBox(L('Az adatbázis-felhasználó megadása kötelező.', 'The database user is required.'), mbError, MB_OK);
        Result := False;
        Exit;
      end;
    end;
  end;

  if CurPageID = IntegrationPage.ID then
  begin
    if not IsValidM2mApiKey(IntegrationPage.Values[1]) then
    begin
      MsgBox(L('Az M2M API-kulcsnak négy, kötőjellel elválasztott, nem üres részből kell állnia: userId-password-aláírókulcsElsőFele-nonce.',
               'The M2M API key must contain four non-empty parts separated by hyphens: userId-password-signingKeyFirstPart-nonce.'), mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if ((Trim(IntegrationPage.Values[2]) = '') and (Trim(IntegrationPage.Values[3]) <> '')) or
       ((Trim(IntegrationPage.Values[2]) <> '') and (Trim(IntegrationPage.Values[3]) = '')) then
    begin
      MsgBox(L('A Client ID és a Client Secret csak együtt adható meg.', 'Client ID and Client Secret must be provided together.'), mbError, MB_OK);
      Result := False;
      Exit;
    end;
  end;

  if CurPageID = AdminPage.ID then
  begin
    if Trim(AdminPage.Values[0]) = '' then
    begin
      MsgBox(L('Az adminisztrátori felhasználónév megadása kötelező.', 'The administrator username is required.'), mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(AdminPage.Values[1]) = '' then
    begin
      MsgBox(L('Az adminisztrátori jelszó megadása kötelező.', 'The administrator password is required.'), mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if AdminPage.Values[1] <> AdminPage.Values[2] then
    begin
      MsgBox(L('Az adminisztrátori jelszavak nem egyeznek.', 'The administrator passwords do not match.'), mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if not IsValidSetupPassword(AdminPage.Values[1]) then
    begin
      MsgBox(L('A jelszó legalább 8 karakteres legyen, és tartalmazzon kisbetűt, nagybetűt, számot és speciális karaktert.', 'The password must be at least 8 characters and contain lowercase, uppercase, a number and a special character.'), mbError, MB_OK);
      Result := False;
      Exit;
    end;
  end;
end;

procedure EnsureDir(Path: String);
begin
  if not DirExists(Path) then
    ForceDirectories(Path);
end;

function MainConfigText(DbTypeOverride: String): String;
var
  Nl: String;
  SecurityMode: String;
  DbType: String;
  BootstrapEnabled: String;
  DataDir: String;
  BaseData: String;
begin
  Nl := #13#10;
  SecurityMode := SelectedSecurityMode();
  if Trim(DbTypeOverride) <> '' then
    DbType := Uppercase(Trim(DbTypeOverride))
  else
    DbType := SelectedDatabaseType();
  BaseData := GetDataRoot('');
  DataDir := NormalizePropPath(BaseData);
  { Az első webes setup hozza létre a kezdő admint minden üzemmódban. }
  BootstrapEnabled := 'false';

  Result :=
    '# M2M XML EDITOR - external configuration' + Nl +
    '# Generated by Windows installer' + Nl +
    Nl +
    'server.port=' + AppPage.Values[0] + Nl +
    'server.servlet.context-path=/' + Nl +
    'server.servlet.session.timeout=' + AppPage.Values[2] + Nl +
    Nl +
    'nav.xsdparsertool.security.mode=' + SecurityMode + Nl +
    'nav.xsdparsertool.security.standalone.username=local-user' + Nl +
    'nav.xsdparsertool.security.bootstrap-admin.enabled=' + BootstrapEnabled + Nl +
    'nav.xsdparsertool.security.bootstrap-admin.username=' + AdminPage.Values[0] + Nl +
    'nav.xsdparsertool.security.bootstrap-admin.display-name=' + AdminPage.Values[3] + Nl +
    'nav.xsdparsertool.security.bootstrap-admin.email=' + AdminPage.Values[4] + Nl +
    Nl +
    'nav.xsdparsertool.database.type=' + DbType + Nl +
    'spring.h2.console.path=/h2-console' + Nl +
    Nl +
    'app.data.dir=' + DataDir + Nl +
    'nav.xsdparsertool.data-directory=' + DataDir + Nl +
    'nav.xsdparsertool.bootstrap-config-file=' + NormalizePropPath(GetConfigRoot()) + '/application-bootstrap.properties' + Nl +
    'nav.xsdparsertool.setup.completed=false' + Nl +
    'app.log.level=INFO' + Nl +
    'logging.level.root=${app.log.level}' + Nl +
    'logging.file.name=${app.data.dir}/logs/app.log' + Nl +
    'logging.pattern.console=%d{HH:mm:ss.SSS} %-5level [%X{sessionId}] [%X{requestId}] %msg%n' + Nl +
    'logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{sessionId}] [%X{requestId}] %logger - %msg%n' + Nl +
    Nl +
    'nav.xsdparsertool.xml-file.upload-dir=' + NormalizePropPath(PathPage.Values[0]) + Nl +
    'nav.xsdparsertool.xml-file.archive-dir=' + NormalizePropPath(PathPage.Values[1]) + Nl +
    'nav.xsdparsertool.xml-file.backup-dir=' + NormalizePropPath(BaseData + '\backup') + Nl +
    'nav.xsdparsertool.xml-file.xml-index-dir=' + NormalizePropPath(BaseData + '\data\xml-index') + Nl +
    'nav.xsdparsertool.xml-file.server-browser.enabled=true' + Nl +
    'nav.xsdparsertool.xml-file.server-import.root-dir=' + NormalizePropPath(PathPage.Values[3]) + Nl +
    'nav.xsdparsertool.xml-file.server-browser.auto-register-enabled=true' + Nl +
    'nav.xsdparsertool.xml-file.server-browser.auto-register-on-startup=true' + Nl +
    'nav.xsdparsertool.xml-file.server-browser.auto-register-interval-ms=30000' + Nl +
    Nl +
    'nav.xsdparsertool.paths.schema-dir=' + NormalizePropPath(RepoPathPage.Values[0]) + Nl +
    'nav.xsdparsertool.paths.ui-model-dir=' + NormalizePropPath(RepoPathPage.Values[1]) + Nl +
    'nav.xsdparsertool.paths.common-xsd-dir=' + NormalizePropPath(RepoPathPage.Values[3]) + Nl +
    Nl +
    'nav.xsdparsertool.xpath-validator.xsl-root-dir=' + NormalizePropPath(RepoPathPage.Values[4]) + Nl +
    'nav.xsdparsertool.xpath-validator.rule-root-dir=' + NormalizePropPath(RepoPathPage.Values[2]) + Nl +
    'nav.xsdparsertool.xpath-validator.result-dir=' + NormalizePropPath(PathPage.Values[2]) + Nl +
    'nav.xsdparsertool.xpath-validator.sync-timeout-seconds=60' + Nl +
    'nav.xsdparsertool.xpath-validator.async-thread-count=4' + Nl +
    'nav.xsdparsertool.xpath-validator.async-queue-capacity=500' + Nl +
    'nav.xsdparsertool.xpath-validator.default-auto-refresh-seconds=10' + Nl +
    'nav.xsdparsertool.xpath-validator.default-page-size=10' + Nl +
    'nav.xsdparsertool.xpath-validator.fixed-xsl-name=full_check_core_public.xsl' + Nl +
    Nl +
    'nav.xsdparsertool.xml-file.lock.timeout-minutes=' + AppPage.Values[3] + Nl +
    'nav.xsdparsertool.xml-file.lock.renew-minutes=' + AppPage.Values[3] + Nl +
    'nav.xsdparsertool.xsd-validation.max-errors=' + AppPage.Values[4] + Nl +
    Nl +
    'nav.xsdparsertool.xml-file.large-file.threshold=' + AppPage.Values[1] + Nl +
    'nav.xsdparsertool.xml-file.large-file.disable-xml-tree=true' + Nl +
    'nav.xsdparsertool.xml-file.large-file.disable-xml-source=true' + Nl +
    Nl +
    'nav.xsdparsertool.desktop.enabled=' + BoolToProp(DesktopPage.Values[0]) + Nl +
    'nav.xsdparsertool.desktop.splash.enabled=' + BoolToProp(DesktopPage.Values[1]) + Nl +
    'nav.xsdparsertool.desktop.browser-open-enabled=' + BoolToProp(DesktopPage.Values[2]) + Nl +
    'nav.xsdparsertool.desktop.tray-enabled=' + BoolToProp(DesktopPage.Values[3]) + Nl +
    Nl +
    'nav.xsdparsertool.xml-index.config-path=' + NormalizePropPath(GetConfigRoot()) + '/xml-index-config.xml' + Nl +
    Nl +
    'nav.xsdparsertool.ui.menu.home=true' + Nl +
    'nav.xsdparsertool.ui.menu.validate=false' + Nl +
    'nav.xsdparsertool.ui.menu.xml-files=true' + Nl +
    'nav.xsdparsertool.ui.menu.xpath-validator=false' + Nl +
    'nav.xsdparsertool.ui.menu.form=true' + Nl +
    'nav.xsdparsertool.ui.menu.admin=true' + Nl +
    'nav.xsdparsertool.form.renderer.default=uimodel' + Nl;
end;

function DbConfigText(DbType: String): String;
var
  Nl: String;
  Host: String;
  Port: String;
  DbName: String;
  UserName: String;
  Password: String;
begin
  Nl := #13#10;
  Host := DbPage.Values[0];
  Port := DbPage.Values[1];
  DbName := DbPage.Values[2];
  UserName := DbPage.Values[3];
  Password := DbPage.Values[4];

  if DbType = 'H2' then
  begin
    Result :=
      'nav.xsdparsertool.database.schema=PUBLIC' + Nl +
      'nav.xsdparsertool.database.encoding=UTF-8' + Nl +
      'spring.datasource.url=jdbc:h2:file:' + NormalizePropPath(GetDataRoot('')) + '/database/schema-explorer;AUTO_SERVER=TRUE' + Nl +
      'spring.datasource.driver-class-name=org.h2.Driver' + Nl +
      'spring.datasource.username=sa' + Nl +
      'spring.datasource.password=' + Nl +
      'spring.jpa.database-platform=org.hibernate.dialect.H2Dialect' + Nl +
      'spring.jpa.hibernate.ddl-auto=none' + Nl +
      'spring.jpa.open-in-view=false' + Nl +
      'spring.jpa.show-sql=false' + Nl +
      'spring.flyway.enabled=true' + Nl +
      'spring.flyway.encoding=UTF-8' + Nl +
      'spring.flyway.baseline-on-migrate=true' + Nl +
      'spring.flyway.locations=classpath:db/migration/H2' + Nl +
      'spring.h2.console.enabled=true' + Nl;
  end
  else if DbType = 'MYSQL' then
  begin
    Result :=
      'nav.xsdparsertool.database.schema=' + DbName + Nl +
      'nav.xsdparsertool.database.encoding=UTF-8' + Nl +
      'spring.datasource.url=jdbc:mysql://' + Host + ':' + Port + '/' + DbName + '?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Budapest&allowPublicKeyRetrieval=true&useSSL=false' + Nl +
      'spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver' + Nl +
      'spring.datasource.username=' + UserName + Nl +
      'spring.datasource.password=' + Password + Nl +
      'spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect' + Nl +
      'spring.jpa.hibernate.ddl-auto=none' + Nl +
      'spring.jpa.open-in-view=false' + Nl +
      'spring.jpa.show-sql=false' + Nl +
      'spring.flyway.enabled=true' + Nl +
      'spring.flyway.encoding=UTF-8' + Nl +
      'spring.flyway.baseline-on-migrate=true' + Nl +
      'spring.flyway.locations=classpath:db/migration/MYSQL' + Nl +
      'spring.h2.console.enabled=false' + Nl;
  end
  else if DbType = 'POSTGRESQL' then
  begin
    Result :=
      'nav.xsdparsertool.database.schema=public' + Nl +
      'nav.xsdparsertool.database.encoding=UTF-8' + Nl +
      'spring.datasource.url=jdbc:postgresql://' + Host + ':' + Port + '/' + DbName + Nl +
      'spring.datasource.driver-class-name=org.postgresql.Driver' + Nl +
      'spring.datasource.username=' + UserName + Nl +
      'spring.datasource.password=' + Password + Nl +
      'spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect' + Nl +
      'spring.jpa.hibernate.ddl-auto=none' + Nl +
      'spring.jpa.open-in-view=false' + Nl +
      'spring.jpa.show-sql=false' + Nl +
      'spring.flyway.enabled=true' + Nl +
      'spring.flyway.encoding=UTF-8' + Nl +
      'spring.flyway.baseline-on-migrate=true' + Nl +
      'spring.flyway.locations=classpath:db/migration/POSTGRESQL' + Nl +
      'spring.h2.console.enabled=false' + Nl;
  end
  else
  begin
    Result :=
      'nav.xsdparsertool.database.schema=' + UserName + Nl +
      'nav.xsdparsertool.database.encoding=UTF-8' + Nl +
      'spring.datasource.url=jdbc:oracle:thin:@' + Host + ':' + Port + '/' + DbName + Nl +
      'spring.datasource.driver-class-name=oracle.jdbc.OracleDriver' + Nl +
      'spring.datasource.username=' + UserName + Nl +
      'spring.datasource.password=' + Password + Nl +
      'spring.jpa.database-platform=org.hibernate.dialect.OracleDialect' + Nl +
      'spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type=TIMESTAMP' + Nl +
      'spring.jpa.hibernate.ddl-auto=none' + Nl +
      'spring.jpa.open-in-view=false' + Nl +
      'spring.jpa.show-sql=false' + Nl +
      'spring.flyway.enabled=true' + Nl +
      'spring.flyway.encoding=UTF-8' + Nl +
      'spring.flyway.baseline-on-migrate=true' + Nl +
      'spring.flyway.locations=classpath:db/migration/ORACLE' + Nl +
      'spring.h2.console.enabled=false' + Nl;
  end;
end;

procedure MergeMissingProperties(TargetFile: String; DefaultsText: String);
var
  ExistingRaw: AnsiString;
  ExistingText: String;
  Rest: String;
  Line: String;
  Key: String;
  P: Integer;
  Added: String;
  Nl: String;
begin
  if not FileExists(TargetFile) then
  begin
    SaveStringToFile(TargetFile, AnsiString(DefaultsText), False);
    Exit;
  end;

  if not LoadStringFromFile(TargetFile, ExistingRaw) then
  begin
    RaiseException(L('A konfigurációs fájl nem olvasható: ', 'Configuration file cannot be read: ') + TargetFile);
    Exit;
  end;
  ExistingText := String(ExistingRaw);

  Nl := #13#10;
  Added := '';
  Rest := DefaultsText;
  while Rest <> '' do
  begin
    P := Pos(#10, Rest);
    if P > 0 then
    begin
      Line := Copy(Rest, 1, P - 1);
      Delete(Rest, 1, P);
    end
    else
    begin
      Line := Rest;
      Rest := '';
    end;
    if (Length(Line) > 0) and (Line[Length(Line)] = #13) then
      Delete(Line, Length(Line), 1);

    Key := PropertyKeyFromLine(Line);
    if (Key <> '') and (not HasPropertyKey(ExistingText, Key)) then
    begin
      Added := Added + Line + Nl;
      ExistingText := ExistingText + Nl + Line;
    end;
  end;

  if Added <> '' then
  begin
    if (Length(ExistingText) > 0) and (Copy(ExistingText, Length(ExistingText), 1) <> #10) then
      SaveStringToFile(TargetFile, AnsiString(Nl), True);
    SaveStringToFile(TargetFile,
      AnsiString(Nl + '# Added by M2M XML EDITOR installer update {#MyAppVersion}' + Nl + Added),
      True);
  end;
end;

procedure WritePendingIntegrationCredentials();
var
  TargetFile: String;
  Nl: String;
  Content: String;
begin
  if ExistingInstallerConfig then
    Exit;

  TargetFile := GetConfigRoot() + '\setup-integrations.properties';
  Nl := #13#10;
  Content :=
    '# One-time installer handoff. Imported and deleted by the application setup.' + Nl +
    'adminUsername=' + AdminPage.Values[0] + Nl +
    'adminPassword=' + AdminPage.Values[1] + Nl +
    'adminDisplayName=' + AdminPage.Values[3] + Nl +
    'adminEmail=' + AdminPage.Values[4] + Nl +
    'githubToken=' + IntegrationPage.Values[0] + Nl +
    'm2mApiKey=' + IntegrationPage.Values[1] + Nl +
    'm2mClientId=' + IntegrationPage.Values[2] + Nl +
    'm2mClientSecret=' + IntegrationPage.Values[3] + Nl;
  SaveStringToFile(TargetFile, AnsiString(Content), False);
end;

procedure WriteInstallerConfig();
var
  ConfigDir: String;
  DbDir: String;
  DbType: String;
  MainConfigFile: String;
  DbConfigFile: String;
begin
  ConfigDir := GetConfigRoot();
  DbDir := ConfigDir + '\database';

  if ExistingInstallerConfig and (Trim(ExistingDatabaseType) <> '') then
    DbType := ExistingDatabaseType
  else
    DbType := SelectedDatabaseType();

  EnsureDir(GetDataRoot(''));
  EnsureDir(ConfigDir);
  EnsureDir(GetDataRoot('') + '\data');
  EnsureDir(GetDataRoot('') + '\logs');
  EnsureDir(GetDataRoot('') + '\backup');
  EnsureDir(GetDataRoot('') + '\data\xml');
  EnsureDir(GetDataRoot('') + '\data\archive');
  EnsureDir(GetDataRoot('') + '\data\xml-index');
  EnsureDir(GetDataRoot('') + '\data\import');
  EnsureDir(GetDataRoot('') + '\data\xpath\results');
  EnsureDir(GetDataRoot('') + '\data\attachments');
  EnsureDir(GetDataRoot('') + '\data\exports');
  EnsureDir(GetDataRoot('') + '\database');
  EnsureDir(GetDataRoot('') + '\certificates');
  EnsureDir(GetDataRoot('') + '\repo');
  EnsureDir(GetDataRoot('') + '\repo\xsd');
  EnsureDir(GetDataRoot('') + '\repo\uimodel');
  EnsureDir(GetDataRoot('') + '\repo\xpath');
  EnsureDir(GetDataRoot('') + '\repo\xsd\common');
  EnsureDir(GetDataRoot('') + '\repo\rule-xsl');

  MainConfigFile := ConfigDir + '\application-bootstrap.properties';
  if ExistingInstallerConfig and (not FileExists(MainConfigFile)) and FileExists(ConfigDir + '\nav-xsd-parser-tool-paths.properties') then
    MainConfigFile := ConfigDir + '\nav-xsd-parser-tool-paths.properties';
  DbConfigFile := DbDir + '\' + DbType + '.properties';

  if ExistingInstallerConfig then
  begin
    { Upgrade: preserve all user/admin values and append only keys that do not yet exist. }
    MergeMissingProperties(MainConfigFile, MainConfigText(DbType));
    if FileExists(DbConfigFile) then
      MergeMissingProperties(DbConfigFile, DbConfigText(DbType));
  end
  else
  begin
    { Fresh install: the wizard owns the initial configuration. }
    EnsureDir(PathPage.Values[0]);
    EnsureDir(PathPage.Values[1]);
    EnsureDir(PathPage.Values[2]);
    EnsureDir(PathPage.Values[3]);
    EnsureDir(RepoPathPage.Values[0]);
    EnsureDir(RepoPathPage.Values[1]);
    EnsureDir(RepoPathPage.Values[2]);
    EnsureDir(RepoPathPage.Values[3]);
    EnsureDir(RepoPathPage.Values[4]);
    SaveStringToFile(MainConfigFile, AnsiString(MainConfigText(DbType) + #13#10 + DbConfigText(DbType)), False);
  end;

  WritePendingIntegrationCredentials();
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    WriteInstallerConfig();
    WriteBootstrapLocator();
  end;
end;
