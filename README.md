<!-- !/usr/bin/env markdown
-*- coding: utf-8 -*- -->

fitHUD
================

Project Information
----------------
Projekt für das wearables Praktikum WS14/15.

Developement Workflow
================
- man macht sich einen branch um ein Feature zu entwickeln:
`git branch featureX`
- dann wechselt man in diesen branch
`git checkout featureX`
- man comitted möglichst oft kleinere Codestücke in diesen branch
`git commit ...`
- (optional) wenn man seinen branch den anderen zeigen möchte pushed man ihn ins repository *(-u == --set.upstream)*
`git push -u origin featureX` 
- wenn man fertig ist wechselt man wieder in den master branch
`git checkout master`
- holt sich die aktuellste version
`git pull`
- merged oder eben rebased den feature branch 
`git rebase featureX`
- und lädt die Änderungen hoch
`git push`



Markdown reference
================
see also:

http://daringfireball.net/projects/markdown/

https://help.github.com/articles/github-flavored-markdown/

### cite (markdown in cite is parsed too)
> Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aliquam hendrerit mi posuere
> lectus.

### ol
1. first item
2. second item

### ul
- first 
  long item
- second item

### code block
```bash
>>> git clone
```
### inline code block
Inline code block `echo "hans"` is inline.

### links
[link](http://http://developer.android.com/samples/)

<http://example.com/>

Headings
================
H1
========
H2
-----------
# H1 alternative
## H2 alternative
### H3 alternative...

