from lxml import etree
import sys
import re
import os
import requests

LANG_RE = re.compile('.+/values-?([a-z]+)?$')

if __name__ == "__main__":
    app_id = sys.argv[1]
    for root, dirs, files in os.walk(sys.argv[2]):
        if '/res/' not in root: continue
        if '/build/' in root: continue
        if '/facebook/' in root: continue
        lang = LANG_RE.findall(root)
        if len(lang) != 1:
            continue
        lang = lang[0]
        if lang == '':
            lang = 'en'
        print root,lang
        for filename in files:
            if not filename.endswith('.xml'): continue
            fullpath = os.path.join(root, filename)
            parser = etree.XMLParser(remove_comments=False, strip_cdata=False)
            doc = etree.parse(fullpath, parser=parser)
            tree = doc.getroot()
            strings = tree.findall('string')
            for el in strings:
                params = {
                    'app_id': app_id,
                    'device_id': "0000000000000000",
                    'key': el.get('name'),
                    'string': el.text,
                    'action': 'new',
                    'locale': lang
                }
                r = requests.post("http://jitt-server.appspot.com/api/action",params=params)
                print r.url, r.text=='OK'
