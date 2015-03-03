import webapp2
import jinja2
import os
import logging
import json
import datetime
import md5
import itertools

from random import random
from collections import Counter
from google.appengine.ext import ndb
from google.appengine.api import urlfetch
from google.appengine.api import taskqueue

class Suggestion(ndb.Model):
    app_id = ndb.StringProperty()
    str_key = ndb.StringProperty()
    translated = ndb.StringProperty()
    locale = ndb.StringProperty()
    votes = ndb.IntegerProperty()

class UserAction(ndb.Model):
    deviceid = ndb.StringProperty()
    action = ndb.StringProperty()
    suggestion = ndb.KeyProperty(kind="Suggestion")

def get_action(deviceid,suggestion):
    actions = UserAction.query(UserAction.deviceid==deviceid,UserAction.suggestion==suggestion.key).fetch(1)
    if len(actions) > 0:
        return actions[0].action
    else:
        return "none"

class GetTranslations(webapp2.RequestHandler):
    def get(self):
        app_id = self.request.get('app_id')
        device_id = self.request.get('device_id')
        keys = set(self.request.get_all('key'))
        translated_locales = [ x.split('_')[0] for x in self.request.get_all('locale') ]
        logging.info("locales %r" % translated_locales)

        suggestions = Suggestion.query(Suggestion.app_id==app_id, Suggestion.str_key.IN(keys), Suggestion.locale.IN(translated_locales)).order(-Suggestion.votes).fetch(1000)
        logging.info("suggestions %d, %r" % (len(suggestions),suggestions))
        ret = { k: {l:[] for l in translated_locales} for k in keys}
        for s in suggestions:
            ret.setdefault(s.str_key,{l:[] for l in translated_locales})[s.locale].append({ 'suggested':s.translated, 'votes':s.votes, 'user_selected':get_action(device_id,s) })

        # ret = { key: { lang: [ { 'suggested' : s.translated, 'votes' : s.votes, 'user_selected' : get_action(device_id,s) } for s in strings]
        #                         for lang, strings in itertools.groupby(suggestions.get(key,[]),lambda x:x.locale) }
        #                 for key in keys }

        self.response.write(json.dumps(ret))

class DoAction(webapp2.RequestHandler):
    def post(self):
        app_id = self.request.get('app_id')
        device_id = self.request.get('device_id')
        locale = self.request.get('locale').split('_')[0]
        key = self.request.get('key')
        string = self.request.get('string')
        action = self.request.get('action')
        suggestion = Suggestion.query(Suggestion.app_id==app_id,Suggestion.str_key==key,Suggestion.translated==string,Suggestion.locale==locale).fetch(1)
        if len(suggestion)>0:
            suggestion = suggestion[0]
        else:
            action = "new"
            suggestion = Suggestion(app_id=app_id,str_key=key,translated=string,locale=locale,votes=1)
            suggestion.put()
        user_action = UserAction.query(UserAction.deviceid==device_id,UserAction.suggestion==suggestion.key).fetch(1)
        if len(user_action) > 0:
            user_action = user_action[0]
        else:
            user_action = UserAction(deviceid=device_id, suggestion=suggestion.key, action="xxx")
        orig_votes = suggestion.votes
        orig_action = user_action.action

        if action == "delete" and user_action.action=="new" suggestion.votes==1:
            #I'm trying to delete my own suggestion that still no-one voted on
            user_action.key.delete()
            suggestion.key.delete()
        else:
            if user_action.action != "new":
                if user_action.action != action:
                    if user_action.action == "up":     suggestion.votes -= 1
                    elif user_action.action == "down": suggestion.votes += 1
                    user_action.action = action
                    if user_action.action == "up":     suggestion.votes += 1
                    elif user_action.action == "down": suggestion.votes -= 1

            if orig_action != user_action.action:
                user_action.put()
            if orig_votes != suggestion.votes:
                suggestion.put()

            self.response.write("OK")

class Upload(webapp2.RequestHandler):
    def post(self,app_id):
        data = json.loads(self.request.body)
        num = len(data)
        while len(data)>0:
            chunk = data[:10]
            data = data[10:]
            taskqueue.add(url='/task/upload', params={'translations':json.dumps(chunk), 'app_id':app_id})

        self.response.write(json.dumps({'success':True,'num':num,'app_id':app_id}))

class UploadTask(webapp2.RequestHandler):
    def post(self):
        translations = json.loads(self.request.get('translations'))
        app_id = self.request.get('app_id')
        logging.debug('TASK: handling {0} strings for {1}'.format(len(translations),app_id))
        to_put = []
        to_delete = []
        for key, mapping in translations:
            for locale, translated in mapping.iteritems():
                suggestion = Suggestion.query(Suggestion.app_id==app_id,
                                              Suggestion.str_key==key,
                                              Suggestion.translated==translated,
                                              Suggestion.locale==locale).fetch(100)
                if len(suggestion)==0:
                    suggestion=Suggestion(app_id=app_id,str_key=key,translated=translated,locale=locale,votes=1)
                    to_put.append(suggestion)
                if len(suggestion)>1:
                    to_delete.extend([e.key for e in suggestion[1:]])
        if len(to_put)>0:
            ndb.put_multi(to_put)
        if len(to_delete)>0:
            ndb.delete_multi(to_delete)
        logging.debug('TASK: added {0} suggestions'.format(len(to_put)))
        logging.debug('TASK: deleted {0} suggestions'.format(len(to_delete)))


application = webapp2.WSGIApplication([
    ('/api/translations', GetTranslations),
    ('/api/action', DoAction),
    ('/api/upload/(.+)', Upload),
    ('/task/upload', UploadTask),
], debug=True)
