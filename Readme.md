# Description
This is a fork of git-tf bridge implemented by Microsoft (Official site on [Codeplex](https://archive.codeplex.com/?p=gittf) is archived. And the project was abandoned by the way).
All original functionality is available with some extra stuff.

## DISCLAMER
THE PROGRAM IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL, BUT WITHOUT ANY WARRANTY. IT IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.

IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW THE AUTHOR WILL BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF THE AUTHOR HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

## The difference from original project
1. Now, you can specify code reviewer for the check-in (`--reviewer-code` param)
2. New commands were added to work with several Git repositories mapped to the same TFS server. Sometimes it is necessary to put changes from several projects to the same shelve-set (`mshelve` command) / change-set (`mcheckin` command)

## Usage
### Build
For building you need JDK7+ and Maven3+. The project is being built by the following command:
```
mvn clean assembly:assembly
```
### Install
When the project is built, the archive is appeared in ./target directory. Just unzip it somewhere and add the destination path to PATH environment variable.
