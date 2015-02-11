#!/usr/bin/env python
from lxml import etree
import sys
import re
import os
import requests
import json

LANG_RE = re.compile('.+/values-?([a-z]+)?$')

if __name__ == "__main__":
    app_id = sys.argv[1]
    translations = {}
    for root, dirs, files in os.walk(sys.argv[2]):
        if '/res/' not in root: continue
        if '/build/' in root: continue
        if '/facebook/' in root: continue
        if '/demoapp/' in root: continue
        if '/samples/' in root: continue
        if '/console/' in root: continue
        if '/jitt/' in root: continue
        if '/bin/' in root: continue
        lang = LANG_RE.findall(root)
        if len(lang) != 1:
            continue
        lang = lang[0]
        if lang == '':
            lang = 'en'
        for filename in files:
            if not filename.endswith('.xml'): continue
            fullpath = os.path.join(root, filename)
            parser = etree.XMLParser(remove_comments=False, strip_cdata=False)
            doc = etree.parse(fullpath, parser=parser)
            tree = doc.getroot()
            strings = tree.findall('string')
            for el in strings:
                try:
                    translations.setdefault(el.get('name'),{})[lang] = el.text
                except:
                    print el
    translations=list(translations.iteritems())
    translations=json.dumps(translations)
    print requests.post("http://jitt-server.appspot.com/api/upload/%s" % app_id,data=translations).json()
