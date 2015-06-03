## Fathom Documentation Site

This documentation is built using [MkDocs][], a Python-based static-site generator using Markdown.

## Installation

### Ubuntu

Installation on Ubuntu is very easy.

```
sudo apt-get install build-essential python-dev
sudo pip install mkdocs
```

### Fedora

I have not tested installation on Fedora. :(

```
sudo pip install mkdocs
```

## Adding Documents & Configuring the Structure

The documentation configuration and structure is maintained in the `mkdocs.yml` file.

## Workflow

From the `fathom-site` directory ...

```
mkdocs serve
```

... and then launch your browser ...

*I control-click the url from the terminal*

... and then launch your preferred editor.

```
atom ~/git/fathom/fathom-site
```

## Deploying

From the `fathom-site` directory...

```
mkdocs gh-deploy
```

**Sadly**, this [does not work properly yet](https://github.com/mkdocs/mkdocs/issues/578).

[MkDocs]: http://www.mkdocs.org
[Atom]: https://atom.io
