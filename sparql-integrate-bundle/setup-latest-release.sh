#!/bin/bash

#
# Simple self-contained un-/install script for creating multiple commands from a single jar bundle
# cmdToClass is the 'dataset' of shell command to java class mappings
# As root, commands will be created under /usr/local/share/$pkgName/bin and then symlinked to /usr/local/bin
# Uninstalling removes any command from /usr/local/bin that also exists in /usr/local/share/$pkgName/bin
# For non-root users the folders are ~/Downloads/$pkgName and ~/bin
#
# Usage:
# Installation is run by providing no additional argument:
#   ./setup.sh
# To uninstall run
#   ./setup.sh uninstall
#

set -e

arg="$1"

pkgName="sparql-integrate"
gitApiUrl="https://api.github.com/repos/SmartDataAnalytics/Sparqlintegrate/releases/latest"
downloadPattern="download/.*-with-dependencies.jar"

declare -a cmdToClass
cmdToClass[0]="sparql-integrate sparqlintegrate"
cmdToClass[1]="ngs"

if [ "$USER" = "root" ]; then
  jarFolder="/usr/local/share/$pkgName"
  binFolder="/usr/local/bin"
else
  jarFolder="$HOME/Downloads/$pkgName"
  binFolder="$HOME/bin"
fi

# tmpBinFolder must be relative to jarFolder
tmpBinFolder="$jarFolder/bin"


# Safety check to prevent accidental deletetion of unrelated files
# Don't change code below
if [ -z "$pkgName" ]; then
  echo "Package name must not be empty"
  exit 1
fi


# On uninstall, delete all files in the binFolder that are symlinks to the tmpBinFolder
if [ "$arg" = "uninstall" ]; then
  echo "Uninstalling: $pkgName"
  if [ -d "$tmpBinFolder" ]; then
    for item in `ls -A "$tmpBinFolder"`; do
      cmd="$binFolder/$item"
      echo "Uninstalling command: $cmd"
      rm -f "$cmd"
    done
  fi

  echo "Removing package folder: $jarFolder"
  rm -rf "$jarFolder"
elif [ -z "$arg" ]; then
  echo "Installing: $pkgName"

  downloadUrl=`curl -s "$gitApiUrl" | grep "$downloadPattern" | cut -d : -f 2,3 | tr -d ' "'`
  jarFileName=`basename "$downloadUrl"`

  mkdir -p "$tmpBinFolder"

  echo "Downloading: $downloadUrl"
  (cd "$jarFolder" && wget -c "$downloadUrl")

  jarPath="$jarFolder/$jarFileName"

  for item in "${cmdToClass[@]}"
  do
    IFS=" " read -r -a arr <<< "${item}"

    cmd="${arr[0]}"
    class="${arr[1]-$cmd}"

    tmpCmdPath="$tmpBinFolder/$cmd"
    cmdPath="$binFolder/$cmd"
    
    echo "Setting up command: $cmdPath"
    echo -e "#!/bin/bash\njava \$JAVA_OPTS -cp $jarPath $class \"\$@\"" > "$tmpCmdPath"
    chmod +x "$tmpCmdPath"

    ln -s "$tmpCmdPath" "$cmdPath"
  done
else
  echo "Invalid argument: $arg"
  echo "Run '$0' without argument to install $pkgName or '$0 uninstall' to uninstall it"
  exit 1
fi

