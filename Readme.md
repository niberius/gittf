# Description
This is a fork of git-tf bridge implemented by Microsoft (Official site on [Codeplex](https://archive.codeplex.com/?p=gittf) is archived. And the project was abandoned by the way).
All original functionality is available with some extra stuff.

## The difference from original project
1. Now, you can specify code reviewer for the check-in (`--reviewer-code` param)
2. _[**In progress**, see 'multiple-dirs-workspace' branch]_ New commands were added to work with several Git repositories mapped to the same TFS server. Sometimes it is necessary to put changes from several projects to the same shelve-set (`mshelve` command) / change-set (`mcheckin` command)