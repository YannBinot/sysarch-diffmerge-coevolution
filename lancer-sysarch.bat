@echo off
rem Lance l'application Sysarch via Java (contourne le lanceur natif sysarch.exe, bloque sur certains postes Windows)
set PRODUCT=%~dp0releng\com.example.sysarch.product\target\products\com.example.sysarch.product\win32\win32\x86_64\sysarch
for %%f in ("%PRODUCT%\plugins\org.eclipse.equinox.launcher_*.jar") do set LAUNCHER=%%f
cd /d "%PRODUCT%"
start "" javaw -Dlog4j.defaultInitOverride=true -Xmx1024m -jar "%LAUNCHER%" -data "%USERPROFILE%\sysarch-workspace" -clearPersistedState
