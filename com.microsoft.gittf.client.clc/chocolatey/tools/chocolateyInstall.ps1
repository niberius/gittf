try {
    $tools = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
    Install-ChocolateyZipPackage 'Git-TF' '${downloadRoot}/git-tf-${buildNumber}.zip' $tools
    $gitTfDir = (Get-Item $tools\gittf*)
    Install-ChocolateyPath "$GitTfDir"
    Write-ChocolateySuccess 'Git-TF'
} catch {
  Write-ChocolateyFailure 'Git-TF' $($_.Exception.Message)
  throw
}